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
package org.xwiki.index.migration;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

import javax.inject.Provider;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.query.spi.NativeQueryImplementor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.doc.tasks.XWikiDocumentIndexingTask;
import org.xwiki.index.internal.TasksStore;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.store.migration.DataMigrationException;
import com.xpn.xwiki.store.migration.hibernate.HibernateDataMigration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link R140300001XWIKI19571DataMigration}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
class R140300001XWIKI19571DataMigrationTest
{
    @InjectMockComponents(role = HibernateDataMigration.class)
    private R140300001XWIKI19571DataMigration dataMigration;

    @MockComponent
    private HibernateStore hibernateStore;

    @MockComponent
    private Provider<TasksStore> tasksStoreProvider;

    @MockComponent
    private Execution execution;

    @Mock
    private TasksStore tasksStore;

    @Mock
    private SessionFactory sessionFactory;

    @Mock
    private SessionImplementor session;

    @Mock
    private JdbcConnectionAccess jdbcConnectionAccess;

    @Mock
    private Connection connection;

    @Mock
    private Metadata metadata;

    @Mock
    private PersistentClass persistentClass;

    @Mock
    private DatabaseMetaData databaseMetaData;

    @Mock
    private NativeQueryImplementor nativeQueryImplementor;

    @Mock
    private ExecutionContext context;

    @Mock
    private XWikiContext xWikiContext;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.hibernateStore.getSessionFactory()).thenReturn(this.sessionFactory);
        when(this.tasksStoreProvider.get()).thenReturn(this.tasksStore);
        when(this.sessionFactory.openSession()).thenReturn(this.session);
        when(this.session.getJdbcConnectionAccess()).thenReturn(this.jdbcConnectionAccess);
        when(this.jdbcConnectionAccess.obtainConnection()).thenReturn(this.connection);
        when(this.hibernateStore.getConfigurationMetadata()).thenReturn(this.metadata);
        when(this.metadata.getEntityBinding(XWikiDocumentIndexingTask.class.getName()))
            .thenReturn(this.persistentClass);
        when(this.hibernateStore.getConfiguredTableName(this.persistentClass)).thenReturn("XWIKIDOCUMENTINDEXINGQUEUE");
        when(this.hibernateStore.getDatabaseFromWikiName()).thenReturn("dbname");
        when(this.connection.getMetaData()).thenReturn(this.databaseMetaData);
        when(this.hibernateStore.getConfiguredColumnName(this.persistentClass, "docId")).thenReturn("DOC_ID");
        when(this.hibernateStore.getConfiguredColumnName(this.persistentClass, "version")).thenReturn("VERSION");
        when(this.hibernateStore.getConfiguredColumnName(this.persistentClass, "type")).thenReturn("TYPE");
        when(this.hibernateStore.getConfiguredColumnName(this.persistentClass, "instanceId")).thenReturn("INSTANCE_ID");
        when(this.hibernateStore.getConfiguredColumnName(this.persistentClass, "timestamp")).thenReturn("TIMESTAMP");
        when(this.execution.getContext()).thenReturn(this.context);
        when(this.context.getProperty("xwikicontext")).thenReturn(this.xWikiContext);
        when(this.xWikiContext.getWikiId()).thenReturn("wikidi");
    }

    @Test
    void hibernateMigrateExistsIsCatalog() throws Exception
    {
        when(this.hibernateStore.isCatalog()).thenReturn(true);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(this.databaseMetaData.getTables("dbname", null, "XWIKIDOCUMENTINDEXINGQUEUE", null)).thenReturn(resultSet);
        when(this.session.createSQLQuery(
            "select DOC_ID, VERSION, TYPE, INSTANCE_ID, TIMESTAMP from XWIKIDOCUMENTINDEXINGQUEUE"))
            .thenReturn(this.nativeQueryImplementor);

        Timestamp now = new Timestamp(new Date().getTime());
        when(this.nativeQueryImplementor.getResultList()).thenReturn(List.<Object[]>of(new Object[] {
            // Oracle returns BigDecimal values.
            BigDecimal.valueOf(1000L),
            "-4.3",
            "testtype",
            "testinstanceid",
            now
        }));

        assertEquals("<changeSet author=\"xwikiorg\" id=\"R140300001XWIKI195710\">\n"
            + "  <dropTable tableName=\"XWIKIDOCUMENTINDEXINGQUEUE\"/>\n"
            + "</changeSet>\n"
            + "\n", this.dataMigration.getPreHibernateLiquibaseChangeLog());

        this.dataMigration.hibernateMigrate();
        XWikiDocumentIndexingTask expected = new XWikiDocumentIndexingTask();
        expected.setDocId(1000L);
        expected.setVersion("4.3");
        expected.setType("testtype");
        expected.setType("testtype");
        expected.setInstanceId("testinstanceid");
        expected.setTimestamp(now);
        verify(this.tasksStore).addTask("wikidi", expected);
        verify(this.hibernateStore).setWiki(this.session);
    }

    @Test
    void hibernateMigrateExistsIsNotCatalog() throws Exception
    {
        when(this.hibernateStore.isCatalog()).thenReturn(false);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(this.databaseMetaData.getTables(null, "dbname", "XWIKIDOCUMENTINDEXINGQUEUE", null)).thenReturn(resultSet);
        when(this.session.createSQLQuery(
            "select DOC_ID, VERSION, TYPE, INSTANCE_ID, TIMESTAMP from XWIKIDOCUMENTINDEXINGQUEUE"))
            .thenReturn(this.nativeQueryImplementor);
        Timestamp now = new Timestamp(new Date().getTime());
        when(this.nativeQueryImplementor.getResultList()).thenReturn(List.<Object[]>of(new Object[] {
            BigInteger.valueOf(1000L),
            "-4.3",
            "testtype",
            "testinstanceid",
            now
        }));
        assertEquals("<changeSet author=\"xwikiorg\" id=\"R140300001XWIKI195710\">\n"
            + "  <dropTable tableName=\"XWIKIDOCUMENTINDEXINGQUEUE\"/>\n"
            + "</changeSet>\n"
            + "\n", this.dataMigration.getPreHibernateLiquibaseChangeLog());
        verify(this.hibernateStore).setWiki(this.session);
    }

    @Test
    void hibernateMigrateSQLException() throws Exception
    {
        when(this.jdbcConnectionAccess.obtainConnection()).thenThrow(SQLException.class);
        DataMigrationException dataMigrationException =
            assertThrows(DataMigrationException.class,
                () -> this.dataMigration.getPreHibernateLiquibaseChangeLog());
        assertEquals("Error while loading the existing tasks.", dataMigrationException.getMessage());
        assertEquals(SQLException.class, dataMigrationException.getCause().getClass());
        verify(this.hibernateStore).setWiki(this.session);
    }

    @Test
    void hibernateMigrateXWikiException() throws Exception
    {
        doThrow(XWikiException.class).when(this.hibernateStore).setWiki(any(Session.class));
        DataMigrationException dataMigrationException =
            assertThrows(DataMigrationException.class,
                () -> this.dataMigration.getPreHibernateLiquibaseChangeLog());
        assertEquals("Error while setting the current wiki in the hibernate session.",
            dataMigrationException.getMessage());
        assertEquals(XWikiException.class, dataMigrationException.getCause().getClass());
    }
}
