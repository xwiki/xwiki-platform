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
package com.xpn.xwiki.store;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.sql.Statement;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.DataMigrationManager;

/**
 * Unit tests for the {@link XWikiHibernateStore} class.
 * 
 * @version $Id$
 */
public class XWikiHibernateStoreTest extends AbstractXWikiHibernateStoreTest<XWikiStoreInterface>
{
    /**
     * A special component manager that mocks automatically all the dependencies of the component under test.
     */
    @Rule
    public MockitoComponentMockingRule<XWikiStoreInterface> mocker =
        new MockitoComponentMockingRule<XWikiStoreInterface>(XWikiHibernateStore.class);

    /**
     * The component being tested.
     */
    private XWikiHibernateStore store;

    @Override
    protected MockitoComponentMockingRule<XWikiStoreInterface> getMocker()
    {
        return mocker;
    }

    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        store = (XWikiHibernateStore) mocker.getComponentUnderTest();
    }

    @Test
    public void testGetColumnsForSelectStatement() throws Exception
    {
        assertEquals(", doc.date", store.getColumnsForSelectStatement("where 1=1 order by doc.date desc"));
        assertEquals(", doc.date", store.getColumnsForSelectStatement("where 1=1 order by doc.date asc"));
        assertEquals(", doc.date", store.getColumnsForSelectStatement("where 1=1 order by doc.date"));
        assertEquals(", description", store.getColumnsForSelectStatement("where 1=1 order by description desc"));
        assertEquals(", ascendent", store.getColumnsForSelectStatement("where 1=1 order by ascendent asc"));
        assertEquals(", doc.date, doc.name",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date, doc.name"));
        assertEquals(", doc.date, doc.name",
            store.getColumnsForSelectStatement("where 1=1 order by doc.date ASC, doc.name DESC"));
        assertEquals("", store.getColumnsForSelectStatement(", BaseObject as obj where obj.name=doc.fullName"));
    }

    @Test
    public void testCreateSQLQuery()
    {
        assertEquals("select distinct doc.space, doc.name from XWikiDocument as doc",
            store.createSQLQuery("select distinct doc.space, doc.name", ""));
        assertEquals("select distinct doc.space, doc.name, doc.date from XWikiDocument as doc "
            + "where 1=1 order by doc.date desc",
            store.createSQLQuery("select distinct doc.space, doc.name", "where 1=1 order by doc.date desc"));
    }

    @Test
    public void testEndTransactionWhenSQLBatchUpdateExceptionThrown() throws Exception
    {
        SQLException sqlException2 = new SQLException("sqlexception2");
        sqlException2.setNextException(new SQLException("nextexception2"));

        SQLException sqlException1 = new SQLException("sqlexception1");
        sqlException1.initCause(sqlException2);
        sqlException1.setNextException(new SQLException("nextexception1"));

        // Assume the transaction is already created.
        when(context.get("hibtransaction")).thenReturn(transaction);
        doThrow(new HibernateException("exception1", sqlException1)).when(transaction).commit();

        try {
            store.endTransaction(context, true);
            fail("Should have thrown an exception here");
        } catch (HibernateException e) {
            assertEquals("Failed to commit or rollback transaction. Root cause [\n"
                + "SQL next exception = [java.sql.SQLException: nextexception1]\n"
                + "SQL next exception = [java.sql.SQLException: nextexception2]]", e.getMessage());
        }
    }

    @Test
    public void executeDeleteWikiStatementForPostgreSQLWhenInSchemaMode() throws Exception
    {
        HibernateSessionFactory sessionFactory = mocker.getInstance(HibernateSessionFactory.class);
        when(sessionFactory.getConfiguration().getProperty("xwiki.virtual_mode")).thenReturn("schema");

        Statement statement = mock(Statement.class);
        DatabaseProduct databaseProduct = DatabaseProduct.POSTGRESQL;

        store.executeDeleteWikiStatement(statement, databaseProduct, "schema");

        verify(statement).execute("DROP SCHEMA schema CASCADE");
    }

    @Test
    public void executeDeleteWikiStatementForPostgreSQLWhenInDatabaseMode() throws Exception
    {
        HibernateSessionFactory sessionFactory = mocker.getInstance(HibernateSessionFactory.class);
        when(sessionFactory.getConfiguration().getProperty("xwiki.virtual_mode")).thenReturn("database");

        Statement statement = mock(Statement.class);
        DatabaseProduct databaseProduct = DatabaseProduct.POSTGRESQL;

        store.executeDeleteWikiStatement(statement, databaseProduct, "schema");

        verify(mocker.getMockedLogger()).warn("Subwiki deletion not yet supported in Database mode for PostgreSQL");
        verify(statement, never()).execute(any(String.class));
    }

    @Test
    public void testLocksAreReleasedOnLogout() throws Exception
    {
        // Capture the event listener.
        ObservationManager observationManager = getMocker().getInstance(ObservationManager.class);
        ArgumentCaptor<EventListener> eventListenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        verify(observationManager).addListener(eventListenerCaptor.capture());
        assertEquals("deleteLocksOnLogoutListener", eventListenerCaptor.getValue().getName());

        Query query = mock(Query.class);
        when(session.createQuery("delete from XWikiLock as lock where lock.userName=:userName")).thenReturn(query);
        when(context.getUserReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "LoggerOutter"));
        when(context.getUser()).thenReturn("XWiki.LoggerOutter");

        // Fire the logout event.
        eventListenerCaptor.getValue().onEvent(new ActionExecutingEvent("logout"), null, context);

        verify(session, times(2)).setFlushMode(FlushMode.COMMIT);
        verify(query).setString("userName", "XWiki.LoggerOutter");
        verify(query).executeUpdate();
        verify(transaction).commit();
        verify(session).close();

        // setDatabase() is called for each transaction and that calls checkDatabase().
        DataMigrationManager dataMigrationManager = mocker.getInstance(DataMigrationManager.class, "hibernate");
        verify(dataMigrationManager).checkDatabase();
    }
}
