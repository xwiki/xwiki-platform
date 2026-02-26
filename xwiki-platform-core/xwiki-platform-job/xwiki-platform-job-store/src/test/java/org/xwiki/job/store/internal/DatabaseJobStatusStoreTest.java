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

import java.io.File;
import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Named;
import javax.sql.DataSource;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.hsqldb.jdbc.JDBCDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.DefaultRequest;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.internal.JobStatusFolderResolver;
import org.xwiki.job.internal.JobStatusSerializer;
import org.xwiki.job.internal.PersistentJobStatusStore;
import org.xwiki.job.store.internal.hibernate.JobStatusHibernateExecutor;
import org.xwiki.job.store.internal.hibernate.JobStatusHibernateStore;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.tail.LoggerTail;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.store.hibernate.HibernateDataSourceProvider;
import org.xwiki.store.hibernate.internal.HibernateCfgXmlLoader;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.xstream.internal.SafeXStream;
import org.xwiki.xstream.internal.XStreamUtils;

import com.xpn.xwiki.internal.store.hibernate.HibernateConfiguration;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.DatabaseProduct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
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
    DatabaseLoggerTail.class,
    JobStatusBlobStore.class,
    JobStatusIdentifierSerializer.class,
    JobStatusSerializer.class,
    HibernateCfgXmlLoader.class,
    JobStatusHibernateStore.class,
    JobStatusHibernateExecutor.class,
    SafeXStream.class,
    XStreamUtils.class,
    TestEnvironment.class
})
class DatabaseJobStatusStoreTest
{
    private static final String NODE_ID = "node-job-store";

    @InjectMockComponents
    private DatabaseJobStatusStore store;

    @MockComponent
    private HibernateDataSourceProvider dataSourceProvider;

    @MockComponent
    private HibernateStore hibernateStore;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private HibernateConfiguration hibernateConfiguration;

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

    @XWikiTempDir
    private File tmpDir;

    @BeforeComponent
    void configureComponents() throws Exception
    {
        DataSource dataSource = createDataSource();
        when(this.hibernateConfiguration.getPath()).thenReturn(createHibernateConfigurationFile().toString());
        when(this.dataSourceProvider.getDataSource()).thenReturn(dataSource);
        when(this.wikiDescriptorManager.getMainWikiId()).thenReturn("xwiki");
        when(this.hibernateStore.getDatabaseProductName()).thenReturn(DatabaseProduct.HSQLDB);
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

    private DataSource createDataSource()
    {
        JDBCDataSource dataSource = new JDBCDataSource();
        dataSource.setUrl("jdbc:hsqldb:mem:jobstore_status_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private Path createHibernateConfigurationFile() throws Exception
    {
        Path target = this.tmpDir.toPath().resolve("hibernate-job-store.cfg.xml");
        Files.createDirectories(target.getParent());

        VelocityContext context = new VelocityContext();
        context.put("xwikiDbConnectionUrl", "none");
        context.put("xwikiDbDbcpMaxTotal", "");
        context.put("xwikiDbHbmCommonExtraMappings", "");
        context.put("xwikiDbHbmDefaultExtraMappings", "");

        Velocity.init();
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream("hibernate.cfg.xml.vm");
            Writer writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8))
        {
            String template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            Velocity.evaluate(context, writer, "hibernate.cfg.xml.vm", template);
        }

        return target;
    }
}
