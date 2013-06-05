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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.id.SequenceGenerator;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.DoubleProperty;
import com.xpn.xwiki.objects.IntegerProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
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

    /**
     * Save an XClass that has a Number property whose type has changed and there is an instance of this class that has
     * no value set for that Number property.
     * 
     * @see <a href="http://jira.xwiki.org/browse/XWIKI-8649">XWIKI-8649: Error when changing the number type of a field
     *      from an application</a>
     */
    @Test
    public void saveXWikiDocWithXClassAndNumberPropertyTypeChange() throws Exception
    {
        // The number property whose type has changed from Double to Integer.
        IntegerProperty integerProperty = mock(IntegerProperty.class);
        NumberClass numberField = mock(NumberClass.class);
        when(numberField.newProperty()).thenReturn(integerProperty);
        when(numberField.getNumberType()).thenReturn("integer");

        // The XClass that has only the number property.
        List<NumberClass> fieldList = Collections.singletonList(numberField);
        BaseClass baseClass = mock(BaseClass.class);
        when(baseClass.getFieldList()).thenReturn(fieldList);

        // The document that is being saved.
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getXClass()).thenReturn(baseClass);

        // Assume there are two objects of the XClass previously defined: one that has no value set for the number
        // property and one that has a value.
        Query query = mock(Query.class);
        DoubleProperty doubleProperty = mock(DoubleProperty.class);
        when(doubleProperty.getValue()).thenReturn(3.5);
        DoubleProperty doublePropertyUnset = mock(DoubleProperty.class, "unset");
        List<DoubleProperty> properties = Arrays.asList(doublePropertyUnset, doubleProperty);
        when(session.createQuery(anyString())).thenReturn(query);
        when(query.setString(anyInt(), anyString())).thenReturn(query);
        when(query.list()).thenReturn(properties);

        store.saveXWikiDoc(document, context);

        // 4 times, for each number type (Integer, Long, Double and Float).
        verify(integerProperty, times(4)).setValue(3);
    }

    @Test
    public void addHibernateSequenceIfRequired() throws Exception
    {
        Session session = mock(Session.class);
        SessionFactoryImplementor sessionFactory = mock(SessionFactoryImplementor.class);
        Dialect dialect = mock(Dialect.class);
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(dialect);
        when(dialect.getNativeIdentifierGeneratorClass()).thenReturn(SequenceGenerator.class);

        String[] result = this.store.addHibernateSequenceIfRequired(new String[0], "schema", session);
        assertEquals(1, result.length);
        assertEquals("create sequence schema.hibernate_sequence", result[0]);
    }

    @Test
    public void addHibernateSequenceIfRequiredWhenSequenceAlreadyPresent() throws Exception
    {
        Session session = mock(Session.class);
        SessionFactoryImplementor sessionFactory = mock(SessionFactoryImplementor.class);
        Dialect dialect = mock(Dialect.class);
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(dialect);
        when(dialect.getNativeIdentifierGeneratorClass()).thenReturn(SequenceGenerator.class);

        String[] result = this.store.addHibernateSequenceIfRequired(
            new String[] {"create sequence schema.hibernate_sequence"}, "schema", session);
        assertEquals(1, result.length);
    }
}
