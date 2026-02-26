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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.DefaultRequest;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.internal.JobStatusFolderResolver;
import org.xwiki.job.internal.JobStatusSerializer;
import org.xwiki.job.internal.PersistentJobStatusStore;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.tail.LoggerTail;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component integration tests for {@link DatabaseJobStatusStore}.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({
    DatabaseJobStatusStore.class,
    JobStatusBlobStore.class,
    JobStatusIdentifierSerializer.class,
    JobStatusSerializer.class
})
class DatabaseJobStatusStoreTest extends AbstractJobStatusHibernateTest
{
    private static final String NODE_ID = "node-job-store";

    @InjectMockComponents
    private DatabaseJobStatusStore store;

    @MockComponent
    private BlobStoreManager blobStoreManager;

    @MockComponent
    @Named("filesystem")
    private PersistentJobStatusStore filesystemStore;

    @MockComponent
    private RemoteObservationManagerConfiguration remoteObservationManagerConfiguration;

    @MockComponent
    @Named("version3")
    private JobStatusFolderResolver folderResolver;

    @Override
    protected void configureAdditionalComponents() throws Exception
    {
        when(this.remoteObservationManagerConfiguration.getId()).thenReturn(NODE_ID);
        when(this.filesystemStore.loadJobStatus(ArgumentMatchers.anyList())).thenReturn(null);
        when(this.folderResolver.getFolderSegments(ArgumentMatchers.anyList()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        Map<String, FileSystemBlobStore> blobStores = new HashMap<>();
        when(this.blobStoreManager.getBlobStore(ArgumentMatchers.anyString())).thenAnswer(invocation -> blobStores
            .computeIfAbsent(invocation.getArgument(0), key -> {
                try {
                    FileSystemBlobStoreProperties properties = new FileSystemBlobStoreProperties();
                    properties.setRootDirectory(this.tmpDir.toPath().resolve("blob").resolve(key));
                    return new FileSystemBlobStore(key, properties);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
    }

    @Test
    void saveLoadAndRemoveStatusWithPaginatedLogs() throws Exception
    {
        DefaultRequest request = new DefaultRequest();
        request.setId(List.of("jobs", "integration", "status"));

        DefaultJobStatus<DefaultRequest> status = new DefaultJobStatus<>("test-job-type", request, null,
            mock(), mock());
        status.setState(JobStatus.State.FINISHED);
        status.setStartDate(new Date(1000L));
        status.setEndDate(new Date(2000L));

        LogQueue logs = new LogQueue();
        for (int i = 0; i < 260; ++i) {
            logs.log(new LogEvent(LogLevel.INFO, "status-message-" + i, null, null));
        }
        status.setLoggerTail(logs);

        this.store.saveJobStatus(status);

        JobStatus loaded = this.store.loadJobStatus(request.getId());
        assertNotSame(status, loaded);
        assertInstanceOf(DefaultJobStatus.class, loaded);
        @SuppressWarnings("unchecked")
        DefaultJobStatus<DefaultRequest> defaultStatus = (DefaultJobStatus<DefaultRequest>) loaded;
        assertEquals("test-job-type", defaultStatus.getJobType());
        assertEquals(JobStatus.State.FINISHED, defaultStatus.getState());
        assertNotNull(defaultStatus.getLoggerTail());
        assertEquals(260, defaultStatus.getLoggerTail().size());

        List<String> messages = new ArrayList<>();
        LoggerTail loggerTail = defaultStatus.getLoggerTail();
        loggerTail.iterator().forEachRemaining(event -> messages.add(event.getFormattedMessage()));
        assertEquals(260, messages.size());
        assertEquals("status-message-0", messages.getFirst());
        assertEquals("status-message-259", messages.getLast());

        LogEvent logEvent = new LogEvent(LogLevel.INFO, "cannot-write", null, null);
        assertThrows(UnsupportedOperationException.class, () -> loggerTail.log(logEvent));

        // We should also be able to access the logs from the database.
        LoggerTail databaseLoggerTail = this.store.createLoggerTail(request.getId(), true);
        assertEquals(260, databaseLoggerTail.size());

        this.store.removeJobStatus(request.getId());
        assertNull(this.store.loadJobStatus(request.getId()));

        // Now the logs should be empty.
        assertEquals(0, databaseLoggerTail.size());

        // The filesystem job status should be removed twice, once when loading and once when removing.
        verify(this.filesystemStore, times(2)).removeJobStatus(request.getId());
    }

    @Test
    void loadJobStatusMigratesFromFilesystem() throws Exception
    {
        List<String> id = List.of("jobs", "migration", "load");
        DefaultRequest request = new DefaultRequest();
        request.setId(id);

        DefaultJobStatus<DefaultRequest> filesystemStatus = new DefaultJobStatus<>("migrate-job", request, null,
            mock(), mock());
        filesystemStatus.setState(JobStatus.State.FINISHED);
        filesystemStatus.setStartDate(new Date(3000L));
        filesystemStatus.setEndDate(new Date(4000L));

        LogQueue logs = new LogQueue();
        for (int i = 0; i < 5; ++i) {
            logs.log(new LogEvent(LogLevel.INFO, "migrated-message-" + i, null, null));
        }
        filesystemStatus.setLoggerTail(logs);

        when(this.filesystemStore.loadJobStatus(id)).thenReturn(filesystemStatus).thenReturn(null);

        // First load: status should be migrated from filesystem to database. The same instance is returned
        // with the logger tail replaced with a DatabaseLoggerTail.
        JobStatus loaded = this.store.loadJobStatus(id);
        assertNotNull(loaded);
        assertInstanceOf(DefaultJobStatus.class, loaded);
        @SuppressWarnings("unchecked")
        DefaultJobStatus<DefaultRequest> defaultStatus = (DefaultJobStatus<DefaultRequest>) loaded;
        assertEquals("migrate-job", defaultStatus.getJobType());
        assertEquals(JobStatus.State.FINISHED, defaultStatus.getState());

        // The migrated status should have a DatabaseLoggerTail attached with all migrated log entries.
        assertNotNull(defaultStatus.getLoggerTail());
        assertInstanceOf(DatabaseLoggerTail.class, defaultStatus.getLoggerTail());
        assertEquals(5, defaultStatus.getLoggerTail().size());

        List<String> messages = new ArrayList<>();
        defaultStatus.getLoggerTail().iterator().forEachRemaining(event -> messages.add(event.getFormattedMessage()));
        assertEquals("migrated-message-0", messages.getFirst());
        assertEquals("migrated-message-4", messages.getLast());

        // The filesystem store should have been asked to remove the status (via saveJobStatus).
        verify(this.filesystemStore).removeJobStatus(id);

        // Second load: status should now come from the database without consulting the filesystem again.
        JobStatus reloaded = this.store.loadJobStatus(id);
        assertNotNull(reloaded);
        assertInstanceOf(DefaultJobStatus.class, reloaded);
        // filesystemStore.loadJobStatus should have been called once only (already returned null on second call).
        verify(this.filesystemStore, times(1)).loadJobStatus(id);
    }

    @Test
    void loadJobStatusReturnsNullWhenNotFoundInDatabaseOrFilesystem() throws Exception
    {
        List<String> id = List.of("jobs", "missing", "status");

        // filesystemStore already returns null by default from configureComponents.
        JobStatus result = this.store.loadJobStatus(id);

        assertNull(result);
        // saveJobStatus on a null status shouldn't write anything - the filesystem store's removeJobStatus
        // should never have been called.
        verify(this.filesystemStore, never()).removeJobStatus(id);
    }

    @Test
    void createReadOnlyLoggerTailMigratesFromFilesystem() throws Exception
    {
        List<String> id = List.of("jobs", "migration", "logtail");
        DefaultRequest request = new DefaultRequest();
        request.setId(id);

        DefaultJobStatus<DefaultRequest> filesystemStatus = new DefaultJobStatus<>("migrate-log-job", request, null,
            mock(), mock());
        filesystemStatus.setState(JobStatus.State.FINISHED);

        LogQueue logs = new LogQueue();
        for (int i = 0; i < 3; ++i) {
            logs.log(new LogEvent(LogLevel.WARN, "warn-message-" + i, null, null));
        }
        filesystemStatus.setLoggerTail(logs);

        when(this.filesystemStore.loadJobStatus(id)).thenReturn(filesystemStatus).thenReturn(null);

        // Creating a read-only logger tail should trigger migration of the filesystem status to the database.
        LoggerTail loggerTail = this.store.createLoggerTail(id, true);

        assertNotNull(loggerTail);
        assertInstanceOf(DatabaseLoggerTail.class, loggerTail);
        assertEquals(3, loggerTail.size());

        List<String> messages = new ArrayList<>();
        loggerTail.iterator().forEachRemaining(event -> messages.add(event.getFormattedMessage()));
        assertEquals("warn-message-0", messages.getFirst());
        assertEquals("warn-message-2", messages.getLast());

        // The filesystem status should have been removed as part of the migration (via saveJobStatus).
        verify(this.filesystemStore).removeJobStatus(id);
    }

    @Test
    void createWritableLoggerTailRemovesFilesystemStatus() throws Exception
    {
        List<String> id = List.of("jobs", "migration", "writable");

        // Creating a writable logger tail should remove the filesystem status to prevent future migration.
        LoggerTail loggerTail = this.store.createLoggerTail(id, false);

        assertNotNull(loggerTail);
        assertInstanceOf(DatabaseLoggerTail.class, loggerTail);

        verify(this.filesystemStore).removeJobStatus(id);
    }
}
