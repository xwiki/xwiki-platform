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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import javax.persistence.LockModeType;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.AbstractJobStatus;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.CancelableJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.internal.JobStatusSerializer;
import org.xwiki.job.internal.JobUtils;
import org.xwiki.job.internal.PersistentJobStatusStore;
import org.xwiki.job.store.internal.entity.JobStatusSummaryEntity;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.tail.LogTail;
import org.xwiki.logging.tail.LoggerTail;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.BlobWriteMode;

/**
 * Database-backed {@link JobStatusStore} that replaces the filesystem implementation.
 *
 * @version $Id$
 * @since 18.1.0RC1
 */
// TODO: Refactor this class to reduce its complexity, currently at 25.
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
@Component
@Singleton
public class DatabaseJobStatusStore implements PersistentJobStatusStore
{
    private static final String BLOB_STORE_NAME = "jobstatus-db";

    private static final String BLOB_FILENAME = "status.xml.zip";

    private static final String NODE_ID = "nodeId";

    private static final String NODE_AND_STATUS_WHERE =
        "where nodeId = :nodeId and statusKey = :statusKey";

    private static final String SELECT_SUMMARY_HQL =
        "from org.xwiki.job.store.internal.entity.JobStatusSummaryEntity " + NODE_AND_STATUS_WHERE;

    private static final String DELETE_LOGS_HQL =
        "delete from org.xwiki.job.store.internal.entity.JobStatusLogEntryEntity " + NODE_AND_STATUS_WHERE;

    private static final String DELETE_SUMMARY_HQL =
        "delete from org.xwiki.job.store.internal.entity.JobStatusSummaryEntity " + NODE_AND_STATUS_WHERE;

    private static final String STATUS_KEY = "statusKey";

    private static final String PATH_SEPARATOR = "/";

    @Inject
    @Named("filesystem")
    private PersistentJobStatusStore filesystemStore;

    @Inject
    private Logger logger;

    @Inject
    private JobStatusIdentifierSerializer identifierResolver;

    @Inject
    private JobStatusSerializer serializer;

    @Inject
    private BlobStoreManager blobStoreManager;

    @Inject
    private RemoteObservationManagerConfiguration remoteObservationManagerConfiguration;

    @Inject
    private Provider<DatabaseLoggerTail> databaseLoggerTailProvider;

    @Inject
    private MainWikiHibernateExecutor hibernateExecutor;

    private final ReentrantLock fileSystemLock = new ReentrantLock();

    private volatile String cachedNodeId;

    private String getNodeId()
    {
        String nodeId = this.cachedNodeId;
        if (nodeId == null) {
            nodeId = this.remoteObservationManagerConfiguration.getId();
            this.cachedNodeId = nodeId;
        }
        return nodeId;
    }

    @Override
    public JobStatus loadStatusWithLock(List<String> id) throws IOException
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
            JobStatus legacyStatus = this.filesystemStore.loadStatusWithLock(id);
            migrateFromFilesystem(id, legacyStatus);
            return legacyStatus;
        } finally {
            this.fileSystemLock.unlock();
        }
    }

    @Override
    public void removeWithLock(List<String> id) throws IOException
    {
        if (id == null) {
            return;
        }

        this.fileSystemLock.lock();
        try {
            this.filesystemStore.removeWithLock(id);
        } finally {
            this.fileSystemLock.unlock();
        }

        String nodeId = getNodeId();
        String statusKey = this.identifierResolver.getDatabaseKey(id);

        try {
            JobStatusSummaryEntity entity = this.hibernateExecutor.executeRead(session -> session
                .createQuery(SELECT_SUMMARY_HQL, JobStatusSummaryEntity.class)
                .setParameter(NODE_ID, nodeId)
                .setParameter(STATUS_KEY, statusKey)
                .setMaxResults(1)
                .uniqueResult());

            this.hibernateExecutor.executeWrite(session -> {
                deleteLogs(session, nodeId, statusKey);
                Query<?> deleteSummary = session.createQuery(DELETE_SUMMARY_HQL);
                deleteSummary.setParameter(NODE_ID, nodeId);
                deleteSummary.setParameter(STATUS_KEY, statusKey);
                deleteSummary.executeUpdate();
                return null;
            });

            if (entity != null) {
                deleteBlob(entity.getBlobLocator());
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
                JobStatus legacyStatus = this.filesystemStore.loadStatusWithLock(jobId);
                if (legacyStatus != null) {
                    migrateFromFilesystem(jobId, legacyStatus);
                }
            } catch (Exception e) {
                this.logger.warn("Failed to load job status [{}] from filesystem: [{}].", jobId,
                    ExceptionUtils.getRootCauseMessage(e));
                return this.filesystemStore.createLoggerTail(jobId, true);
            } finally {
                this.fileSystemLock.unlock();
            }
        } else {
            // TODO: also delete logs from filesystem.
            this.hibernateExecutor.executeWrite(session -> {
                deleteLogs(session, nodeId, statusKey);
                return null;
            });
        }

        return this.databaseLoggerTailProvider.get().initialize(nodeId, statusKey, readonly);
    }

    private void migrateFromFilesystem(List<String> id, JobStatus legacyStatus) throws IOException
    {
        if (legacyStatus == null || id == null) {
            return;
        }

        try {
            saveJobStatusWithLock(legacyStatus);
            this.filesystemStore.removeWithLock(id);
        } catch (Exception e) {
            throw new IOException("Failed to migrate filesystem job status [%s] to database.".formatted(id), e);
        }
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
                    deleteLogs(session, nodeId, statusKey);
                    persistLogs(session, nodeId, statusKey, logTail);
                }

                return null;
            });

            persistBlob(status, blobLocator);

            // TODO: delete filesystem job status files to avoid duplication that could cause confusion.
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

    private void deleteLogs(Session session, String nodeId, String statusKey)
    {
        Query<?> deleteLogs = session.createQuery(DELETE_LOGS_HQL);
        deleteLogs.setParameter(NODE_ID, nodeId);
        deleteLogs.setParameter(STATUS_KEY, statusKey);
        deleteLogs.executeUpdate();
    }

    private void persistBlob(JobStatus status, String blobLocator) throws BlobStoreException, IOException
    {
        BlobStore store = this.blobStoreManager.getBlobStore(BLOB_STORE_NAME);
        Blob blob = store.getBlob(BlobPath.parse(blobLocator));
        try (OutputStream outputStream = blob.getOutputStream(BlobWriteMode.REPLACE_EXISTING)) {
            this.serializer.write(status, outputStream, isZipFile(blobLocator));
        }
    }

    private static boolean isZipFile(String blobLocator)
    {
        return blobLocator.endsWith(".zip");
    }

    private void deleteBlob(String blobLocator)
    {
        if (StringUtils.isBlank(blobLocator)) {
            return;
        }

        try {
            BlobStore store = this.blobStoreManager.getBlobStore(BLOB_STORE_NAME);
            store.deleteBlob(BlobPath.parse(blobLocator));
        } catch (BlobStoreException e) {
            this.logger.warn("Failed to delete job status blob [{}].", blobLocator, e);
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
            JobStatus status = loadStatusFromBlob(entity.getBlobLocator());
            if (status instanceof AbstractJobStatus<?> abstractJobStatus) {
                LoggerTail tail = this.databaseLoggerTailProvider.get().initialize(nodeId, statusKey, true);
                abstractJobStatus.setLoggerTail(tail);
            }
            return status;
        } catch (BlobStoreException e) {
            throw new JobStatusStoreException("Failed to read job status [%s] from blob store."
                .formatted(this.identifierResolver.getRawId(id)), e);
        }
    }

    private JobStatus loadStatusFromBlob(String blobLocator) throws BlobStoreException
    {
        BlobStore store = this.blobStoreManager.getBlobStore(BLOB_STORE_NAME);
        Blob blob = store.getBlob(BlobPath.parse(blobLocator));

        try (InputStream stream = blob.getStream()) {
            return this.serializer.read(stream, isZipFile(blobLocator));
        } catch (Exception e) {
            throw new BlobStoreException("Failed to deserialize job status blob at [%s].".formatted(blobLocator), e);
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
