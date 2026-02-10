/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.job.store.internal;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.LockModeType;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJobStatus;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.CancelableJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.internal.JobUtils;
import org.xwiki.job.internal.PersistentJobStatusStore;
import org.xwiki.job.store.internal.entity.JobStatusSummaryEntity;
import org.xwiki.job.store.internal.hibernate.JobStatusHibernateExecutor;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.tail.LogTail;
import org.xwiki.logging.tail.LoggerTail;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.store.blob.BlobStoreException;

/**
 * Database-backed {@link JobStatusStore} that replaces the filesystem implementation.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
@Component
@Singleton
public class DatabaseJobStatusStore implements PersistentJobStatusStore
{
    private static final String BLOB_FILENAME = "status.xml.zip";

    private static final String NODE_ID = "nodeId";

    private static final String STATUS_KEY = "statusKey";

    private static final String SELECT_SUMMARY_HQL =
        "from org.xwiki.job.store.internal.entity.JobStatusSummaryEntity "
            + "where nodeId = :nodeId and statusKey = :statusKey";

    private static final String PATH_SEPARATOR = "/";

    @Inject
    @Named("filesystem")
    private PersistentJobStatusStore filesystemStore;

    @Inject
    private Logger logger;

    @Inject
    private JobStatusIdentifierSerializer identifierResolver;

    @Inject
    private JobStatusBlobStore jobStatusBlobStore;

    @Inject
    private RemoteObservationManagerConfiguration remoteObservationManagerConfiguration;

    @Inject
    private Provider<DatabaseLoggerTail> databaseLoggerTailProvider;

    @Inject
    private JobStatusHibernateExecutor hibernateExecutor;

    // Lock around filesystem operations as they expect to avoid concurrency issues.
    private final ReentrantLock fileSystemLock = new ReentrantLock();

    private String getNodeId()
    {
        return this.remoteObservationManagerConfiguration.getId();
    }

    @Override
    public JobStatus loadJobStatusWithLock(List<String> id) throws IOException
    {
        try {
            JobStatus status = loadFromDatabase(id);
            if (status != null) {
                return status;
            }
        } catch (JobStatusStoreException e) {
            throw new IOException("Failed to load job status [%s] from the database."
                .formatted(this.identifierResolver.getRawId(id)), e);
        }

        this.fileSystemLock.lock();
        try {
            JobStatus legacyStatus = this.filesystemStore.loadJobStatusWithLock(id);
            // Migrate the job status to the database if found in the filesystem, so that next time it can be loaded
            // directly from the database.
            saveJobStatusWithLock(legacyStatus);
            // As the logs have been migrated to the database, we need to re-attach the logger tail to read the logs
            // from the database instead of the filesystem.
            attachReadOnlyLoggerTail(legacyStatus, getNodeId(), this.identifierResolver.getDatabaseKey(id));
            return legacyStatus;
        } finally {
            this.fileSystemLock.unlock();
        }
    }

    @Override
    public void removeJobStatusWithLock(List<String> id) throws IOException
    {
        if (id == null) {
            return;
        }

        this.fileSystemLock.lock();
        try {
            this.filesystemStore.removeJobStatusWithLock(id);
        } finally {
            this.fileSystemLock.unlock();
        }

        String nodeId = getNodeId();
        String statusKey = this.identifierResolver.getDatabaseKey(id);

        try {
            JobStatusSummaryEntity entity = this.hibernateExecutor.executeWrite(session -> {
                JobStatusSummaryEntity result = session
                    .createQuery(SELECT_SUMMARY_HQL, JobStatusSummaryEntity.class)
                    .setParameter(NODE_ID, nodeId)
                    .setParameter(STATUS_KEY, statusKey)
                    // Use pessimistic lock to make sure that the job status won't be modified by another thread
                    // while we are deleting it.
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .uniqueResult();

                // Initialize a DatabaseLoggerTail to delete the logs from the database.
                try (DatabaseLoggerTail tail = this.databaseLoggerTailProvider.get()) {
                    tail.initialize(nodeId, statusKey, session);
                }

                if (result != null) {
                    session.delete(result);
                }

                return result;
            });

            if (entity != null) {
                String blobLocator = entity.getBlobLocator();
                this.jobStatusBlobStore.delete(blobLocator);
            }
        } catch (JobStatusStoreException e) {
            throw new IOException("Failed to remove job status [%s] from the database."
                .formatted(this.identifierResolver.getRawId(id)), e);
        }
    }

    @Override
    public LoggerTail createLoggerTail(List<String> jobId, boolean readonly)
    {
        if (jobId == null) {
            return new LogQueue();
        }

        String nodeId = getNodeId();
        String statusKey = this.identifierResolver.getDatabaseKey(jobId);

        if (readonly) {
            // Trigger on-demand migration to make sure the logs are moved to the database if needed.
            this.fileSystemLock.lock();
            try {
                JobStatus legacyStatus = this.filesystemStore.loadJobStatusWithLock(jobId);
                if (legacyStatus != null) {
                    saveJobStatusWithLock(legacyStatus);
                }
            } catch (Exception e) {
                this.logger.warn("Failed to migrate job status [{}] from filesystem: [{}].", jobId,
                    ExceptionUtils.getRootCauseMessage(e));
                return this.filesystemStore.createLoggerTail(jobId, true);
            } finally {
                this.fileSystemLock.unlock();
            }
        } else {
            // Try deleting the logs from the filesystem to avoid wrongly migrating the logs to the database later
            // when the logs shall be read. We only log a warning here because in most cases, it shouldn't cause any
            // issues.
            try {
                this.filesystemStore.removeJobStatusWithLock(jobId);
            } catch (IOException e) {
                this.logger.warn("Failed to remove legacy job status [{}] from filesystem: [{}].", jobId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return this.databaseLoggerTailProvider.get().initialize(nodeId, statusKey, readonly);
    }

    @Override
    public void saveJobStatusWithLock(JobStatus status) throws IOException
    {
        if (!JobUtils.isSerializable(status)) {
            return;
        }

        String nodeId = getNodeId();
        Request request = status.getRequest();
        String statusKey = this.identifierResolver.getDatabaseKey(request.getId());
        String blobLocator = buildBlobLocator(this.identifierResolver.getBlobKey(request.getId()));

        try {
            LogTail logTail = status.getLogTail();
            boolean rewriteLogs = !(logTail instanceof DatabaseLoggerTail);

            this.hibernateExecutor.executeWrite(session -> {
                JobStatusSummaryEntity entity = session
                    .createQuery(SELECT_SUMMARY_HQL, JobStatusSummaryEntity.class)
                    .setParameter(NODE_ID, nodeId)
                    .setParameter(STATUS_KEY, statusKey)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .setMaxResults(1)
                    .uniqueResult();

                if (entity == null) {
                    entity = new JobStatusSummaryEntity();
                    entity.setStatusKey(statusKey);
                    entity.setNodeId(nodeId);
                }

                entity.setFullJobId(this.identifierResolver.getRawId(request.getId()));
                entity.setJobType(status.getJobType());
                entity.setState(status.getState() != null ? status.getState().name() : null);
                entity.setStartDate(status.getStartDate());
                entity.setEndDate(status.getEndDate());
                entity.setSerialized(status.isSerialized());
                entity.setIsolated(status.isIsolated());
                entity.setCancelable(isCancelable(status));
                entity.setCanceled(isCanceled(status));
                entity.setBlobLocator(blobLocator);

                session.saveOrUpdate(entity);

                if (rewriteLogs) {
                    persistLogs(session, nodeId, statusKey, logTail);
                }

                return null;
            });

            this.jobStatusBlobStore.store(status, blobLocator);

            this.filesystemStore.removeJobStatusWithLock(request.getId());
        } catch (JobStatusStoreException e) {
            throw new IOException(
                "Failed to persist job status metadata for [%s].".formatted(status.getRequest().getId()), e);
        } catch (BlobStoreException e) {
            throw new IOException("Failed to write job status blob for [%s].".formatted(status.getRequest().getId()),
                e);
        }
    }

    private void persistLogs(Session session, String nodeId, String statusKey, LogTail loggerTail)
    {
        // Skip if the log is already stored in the database.
        if (loggerTail == null || loggerTail instanceof DatabaseLoggerTail) {
            return;
        }

        // This initialization deletes the existing logs for the given job status, so we can be sure that only the
        // new logs will be stored in the database.
        try (DatabaseLoggerTail tail = this.databaseLoggerTailProvider.get().initialize(nodeId, statusKey, false)) {
            long lineIndex = 0;
            for (LogEvent event : loggerTail) {
                if (event == null) {
                    continue;
                }
                tail.appendLogEntry(lineIndex++, event, session);
            }
        }
    }

    private JobStatus loadFromDatabase(List<String> id) throws JobStatusStoreException
    {
        String nodeId = getNodeId();
        String statusKey = this.identifierResolver.getDatabaseKey(id);
        JobStatusSummaryEntity entity = this.hibernateExecutor.executeRead(session -> session
            .createQuery(SELECT_SUMMARY_HQL, JobStatusSummaryEntity.class)
            .setParameter(NODE_ID, nodeId)
            .setParameter(STATUS_KEY, statusKey)
            .setMaxResults(1)
            .uniqueResult());
        if (entity == null) {
            return null;
        }

        try {
            String blobLocator = entity.getBlobLocator();
            JobStatus status = this.jobStatusBlobStore.load(blobLocator);
            attachReadOnlyLoggerTail(status, nodeId, statusKey);
            return status;
        } catch (BlobStoreException e) {
            throw new JobStatusStoreException("Failed to read job status [%s] from blob store."
                .formatted(this.identifierResolver.getRawId(id)), e);
        }
    }

    private void attachReadOnlyLoggerTail(JobStatus status, String nodeId, String statusKey)
    {
        if (status instanceof AbstractJobStatus<?> abstractJobStatus) {
            LoggerTail tail = this.databaseLoggerTailProvider.get().initialize(nodeId, statusKey, true);
            abstractJobStatus.setLoggerTail(tail);
        }
    }

    private boolean isCancelable(JobStatus status)
    {
        return status instanceof CancelableJobStatus c && c.isCancelable();
    }

    private boolean isCanceled(JobStatus status)
    {
        return status instanceof CancelableJobStatus c && c.isCanceled();
    }

    private String buildBlobLocator(String escapedKey)
    {
        return PATH_SEPARATOR + escapedKey + PATH_SEPARATOR + BLOB_FILENAME;
    }

}
