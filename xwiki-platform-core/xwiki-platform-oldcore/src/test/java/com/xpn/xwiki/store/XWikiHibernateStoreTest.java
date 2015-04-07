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

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
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
import com.xpn.xwiki.internal.store.PropertyConverter;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.store.hibernate.HibernateSessionFactory;
import com.xpn.xwiki.store.migration.DataMigrationManager;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

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

    @Test
    public void createHibernateSequenceIfRequiredWhenNotInUpdateCommands() throws Exception
    {
        Session session = mock(Session.class);
        SessionFactoryImplementor sessionFactory = mock(SessionFactoryImplementor.class);
        Dialect dialect = mock(Dialect.class);
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(dialect);
        when(dialect.getNativeIdentifierGeneratorClass()).thenReturn(SequenceGenerator.class);
        SQLQuery sqlQuery = mock(SQLQuery.class);
        when(session.createSQLQuery("create sequence schema.hibernate_sequence")).thenReturn(sqlQuery);
        when(sqlQuery.executeUpdate()).thenReturn(0);

        this.store.createHibernateSequenceIfRequired(new String[] {}, "schema", session);

        verify(session).createSQLQuery("create sequence schema.hibernate_sequence");
        verify(sqlQuery).executeUpdate();
    }

    /**
     * We verify that the sequence is not created if it's already in the update script.
     */
    @Test
    public void createHibernateSequenceIfRequiredWhenInUpdateCommands() throws Exception
    {
        Session session = mock(Session.class);
        SessionFactoryImplementor sessionFactory = mock(SessionFactoryImplementor.class);
        Dialect dialect = mock(Dialect.class);
        when(session.getSessionFactory()).thenReturn(sessionFactory);
        when(sessionFactory.getDialect()).thenReturn(dialect);
        when(dialect.getNativeIdentifierGeneratorClass()).thenReturn(SequenceGenerator.class);
        SQLQuery sqlQuery = mock(SQLQuery.class);
        when(session.createSQLQuery("create sequence schema.hibernate_sequence")).thenReturn(sqlQuery);
        when(sqlQuery.executeUpdate()).thenReturn(0);

        this.store.createHibernateSequenceIfRequired(
            new String[] {"whatever", "create sequence schema.hibernate_sequence"}, "schema", session);

        verify(session, never()).createSQLQuery("create sequence schema.hibernate_sequence");
        verify(sqlQuery, never()).executeUpdate();
    }

    /**
     * Save an object that has a property whose type has changed.
     * 
     * @see "XWIKI-9716: Error while migrating SearchSuggestConfig page from 4.1.4 to 5.2.1 with DW"
     */
    @Test
    public void saveObjectWithPropertyTypeChange() throws Exception
    {
        // The class must be local.
        DocumentReference classReference = new DocumentReference("myWiki", "mySpace", "myClass");
        when(context.getWikiId()).thenReturn(classReference.getWikiReference().getName());
        BaseObject object = mock(BaseObject.class);
        when(object.getXClassReference()).thenReturn(classReference);

        // Query to check if the object exists already (save versus update).
        when(context.get("hibsession")).thenReturn(session);
        when(session.createQuery("select obj.id from BaseObject as obj where obj.id = :id")).thenReturn(
            mock(Query.class));

        // Save each object property.
        String propertyName = "query";
        long propertyId = 1234567890L;
        when(object.getPropertyList()).thenReturn(Collections.singleton(propertyName));

        // The property name must match the key in the property list.
        BaseProperty property = mock(BaseProperty.class);
        when(object.getField(propertyName)).thenReturn(property);
        when(property.getId()).thenReturn(propertyId);
        when(property.getName()).thenReturn(propertyName);
        when(property.getClassType()).thenReturn(LargeStringProperty.class.getName());

        Query oldClassTypeQuery = mock(Query.class);
        when(session.createQuery("select prop.classType from BaseProperty as prop "
            + "where prop.id.id = :id and prop.id.name= :name")).thenReturn(oldClassTypeQuery);
        // The old value has a different type (String -> TextArea).
        when(oldClassTypeQuery.uniqueResult()).thenReturn(StringProperty.class.getName());

        // The old property must be loaded from the corresponding table.
        Query oldPropertyQuery = mock(Query.class);
        when(session.createQuery("select prop from " + StringProperty.class.getName()
            + " as prop where prop.id.id = :id and prop.id.name= :name")).thenReturn(oldPropertyQuery);
        BaseProperty oldProperty = mock(BaseProperty.class);
        when(oldPropertyQuery.uniqueResult()).thenReturn(oldProperty);

        store.saveXWikiCollection(object, context, false);

        verify(oldClassTypeQuery).setLong("id", propertyId);
        verify(oldClassTypeQuery).setString("name", propertyName);

        verify(oldPropertyQuery).setLong("id", propertyId);
        verify(oldPropertyQuery).setString("name", propertyName);

        // Delete the old property value and then save the new one.
        verify(session).delete(oldProperty);
        verify(session).save(property);
    }

    @Test
    public void existsWithRootLocale() throws Exception
    {
        String fullName = "foo";
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getLocale()).thenReturn(Locale.ROOT);
        when(doc.getFullName()).thenReturn(fullName);

        Query query = mock(Query.class);
        when(session.createQuery("select doc.fullName from XWikiDocument as doc where doc.fullName=:fullName"))
            .thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(fullName));

        assertTrue(store.exists(doc, context));

        verify(query).setString("fullName", fullName);
    }

    @Test
    public void existsWithNonRootLocale() throws Exception
    {
        String fullName = "bar";
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getLocale()).thenReturn(Locale.ENGLISH);
        when(doc.getFullName()).thenReturn(fullName);

        Query query = mock(Query.class);
        String statement = "select doc.fullName from XWikiDocument as doc where doc.fullName=:fullName"
            + " and doc.language=:language";
        when(session.createQuery(statement)).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(fullName));

        assertTrue(store.exists(doc, context));

        verify(query).setString("fullName", fullName);
        verify(query).setString("language", Locale.ENGLISH.toString());
    }

    @Test
    public void migrateProperty() throws Exception
    {
        BaseProperty storedProperty = mock(BaseProperty.class, "stored");
        BaseProperty newProperty = mock(BaseProperty.class, "new");
        PropertyClass modifiedPropertyClass = mock(PropertyClass.class);

        PropertyConverter propertyConverter = mocker.getInstance(PropertyConverter.class);
        when(propertyConverter.convertProperty(storedProperty, modifiedPropertyClass)).thenReturn(newProperty);

        migrateProperty(storedProperty, modifiedPropertyClass, session, Collections.<Long, BaseObject>emptyMap());

        verify(session).delete(storedProperty);
        verify(session).save(newProperty);
    }

    @Test
    public void migrateUnsetProperty() throws Exception
    {
        BaseProperty storedProperty = mock(BaseProperty.class);

        migrateProperty(storedProperty, mock(PropertyClass.class), session, Collections.<Long, BaseObject>emptyMap());

        verify(session).delete(storedProperty);
        verify(session, never()).save(any(BaseProperty.class));
    }
    
    @Test
    public void migrateLocalProperty() throws Exception
    {
        BaseProperty localProperty = mock(BaseProperty.class, "local");
        BaseObject localObject = mock(BaseObject.class);
        when(localObject.get("color")).thenReturn(localProperty);

        Map<Long, BaseObject> localObjects = Collections.singletonMap(1L, localObject);

        PropertyClass modifiedPropertyClass = mock(PropertyClass.class);
        when(modifiedPropertyClass.getName()).thenReturn("color");

        BaseProperty newProperty = mock(BaseProperty.class, "new");
        PropertyConverter propertyConverter = mocker.getInstance(PropertyConverter.class);
        when(propertyConverter.convertProperty(localProperty, modifiedPropertyClass)).thenReturn(newProperty);

        BaseProperty storedProperty = mock(BaseProperty.class, "stored");
        when(storedProperty.getId()).thenReturn(1L);

        migrateProperty(storedProperty, modifiedPropertyClass, session, localObjects);

        verify(session).evict(storedProperty);
        verify(session, never()).delete(any(BaseProperty.class));
        verify(session, never()).save(any(BaseProperty.class));

        verify(localObject).put("color", newProperty);
    }

    /**
     * This is a utility method used to call the private XWikiHibernateStore#migrateProperty(), which should be moved
     * outside XWikiHibernateStore and we keep it private until then.
     */
    private void migrateProperty(BaseProperty<?> storedProperty, PropertyClass modifiedPropertyClass, Session session,
        Map<Long, BaseObject> localObjects) throws Exception
    {
        Method migrateProperty = store.getClass().getDeclaredMethod("migrateProperty", BaseProperty.class,
            PropertyClass.class, Session.class, Map.class);
        migrateProperty.setAccessible(true);
        migrateProperty.invoke(store, storedProperty, modifiedPropertyClass, session, localObjects);
    }
}
