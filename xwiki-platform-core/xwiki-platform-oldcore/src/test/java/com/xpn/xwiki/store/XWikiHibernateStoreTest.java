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

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.inject.Named;
import javax.inject.Provider;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.dialect.Dialect;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.store.hibernate.HibernateStore;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link XWikiHibernateStore} class.
 * 
 * @version $Id$
 */
@ComponentTest
public class XWikiHibernateStoreTest
{
    /**
     * A special component manager that mocks automatically all the dependencies of the component under test.
     */
    @InjectMockComponents
    private XWikiHibernateStore store;

    @Mock
    private XWikiContext xcontext;

    /**
     * The Hibernate session.
     */
    @Mock
    private Session session;

    /**
     * The Hibernate transaction.
     */
    @Mock
    private Transaction transaction;

    @MockComponent
    private HibernateStore hibernateStore;

    @MockComponent
    private Execution execution;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    @Named("readonly")
    private Provider<XWikiContext> readOnlyContextProvider;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @MockComponent
    @Named("currentmixed")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    @MockComponent
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        // For XWikiHibernateBaseStore#initialize()

        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xcontext);

        when(execution.getContext()).thenReturn(executionContext);

        when(contextProvider.get()).thenReturn(this.xcontext);
        when(readOnlyContextProvider.get()).thenReturn(this.xcontext);

        XWiki wiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(wiki);

        // For XWikiHibernateBaseStore#beginTransaction()

        SessionFactory wrappedSessionFactory = mock(SessionFactory.class);
        when(wrappedSessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);

        // Return null on first get to force the session/transaction creation.
        when(this.hibernateStore.getCurrentSession()).thenReturn(session);
        when(this.hibernateStore.getCurrentTransaction()).thenReturn(transaction);

        // Default is schema mode
        when(this.hibernateStore.isConfiguredInSchemaMode()).thenReturn(true);
    }

    @Test
    void getColumnsForSelectStatement() throws Exception
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
    void createSQLQuery()
    {
        assertEquals("select distinct doc.space, doc.name from XWikiDocument as doc",
            store.createSQLQuery("select distinct doc.space, doc.name", ""));
        assertEquals(
            "select distinct doc.space, doc.name, doc.date from XWikiDocument as doc "
                + "where 1=1 order by doc.date desc",
            store.createSQLQuery("select distinct doc.space, doc.name", "where 1=1 order by doc.date desc"));
    }

    @Test
    void executeDeleteWikiStatementForPostgreSQLWhenInSchemaMode() throws Exception
    {
        Statement statement = mock(Statement.class);
        DatabaseProduct databaseProduct = DatabaseProduct.POSTGRESQL;

        store.executeDeleteWikiStatement(statement, databaseProduct, "schema");

        verify(statement).execute("DROP SCHEMA schema CASCADE");
    }

    @Test
    void executeDeleteWikiStatementForPostgreSQLWhenInDatabaseMode() throws Exception
    {
        when(this.hibernateStore.isConfiguredInSchemaMode()).thenReturn(false);

        Statement statement = mock(Statement.class);
        DatabaseProduct databaseProduct = DatabaseProduct.POSTGRESQL;

        store.executeDeleteWikiStatement(statement, databaseProduct, "schema");

        assertEquals("Subwiki deletion not yet supported in Database mode for PostgreSQL", logCapture.getMessage(0));
        verify(statement, never()).execute(any(String.class));
    }

    @Test
    void locksAreReleasedOnLogout() throws Exception
    {
        // Capture the event listener.
        ArgumentCaptor<EventListener> eventListenerCaptor = ArgumentCaptor.forClass(EventListener.class);
        verify(observationManager).addListener(eventListenerCaptor.capture());
        assertEquals("deleteLocksOnLogoutListener", eventListenerCaptor.getValue().getName());

        Query query = mock(Query.class);
        when(session.createQuery("delete from XWikiLock as lock where lock.userName=:userName")).thenReturn(query);
        when(xcontext.getUserReference()).thenReturn(new DocumentReference("xwiki", "XWiki", "LoggerOutter"));
        when(xcontext.getUser()).thenReturn("XWiki.LoggerOutter");
        when(this.hibernateStore.beginTransaction()).thenReturn(true);

        // Fire the logout event.
        eventListenerCaptor.getValue().onEvent(new ActionExecutingEvent("logout"), null, xcontext);

        verify(query).setParameter("userName", "XWiki.LoggerOutter");
        verify(query).executeUpdate();
        verify(this.hibernateStore).beginTransaction();
        verify(this.hibernateStore).endTransaction(true);
    }

    @Test
    void createHibernateSequenceIfRequiredWhenNotInUpdateCommands() throws Exception
    {
        Session session = mock(Session.class);
        Dialect dialect = mock(Dialect.class);
        when(this.hibernateStore.getDialect()).thenReturn(dialect);
        when(dialect.getNativeIdentifierGeneratorStrategy()).thenReturn("sequence");
        NativeQuery sqlQuery = mock(NativeQuery.class);
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
    void createHibernateSequenceIfRequiredWhenInUpdateCommands() throws Exception
    {
        Session session = mock(Session.class);
        Dialect dialect = mock(Dialect.class);
        when(this.hibernateStore.getDialect()).thenReturn(dialect);
        when(dialect.getNativeIdentifierGeneratorStrategy()).thenReturn("sequence");
        NativeQuery sqlQuery = mock(NativeQuery.class);
        when(session.createSQLQuery("create sequence schema.hibernate_sequence")).thenReturn(sqlQuery);
        when(sqlQuery.executeUpdate()).thenReturn(0);

        this.store.createHibernateSequenceIfRequired(
            new String[] { "whatever", "create sequence schema.hibernate_sequence" }, "schema", session);

        verify(session, never()).createSQLQuery("create sequence schema.hibernate_sequence");
        verify(sqlQuery, never()).executeUpdate();
    }

    /**
     * Save an object that has a property whose type has changed.
     * 
     * @see "XWIKI-9716: Error while migrating SearchSuggestConfig page from 4.1.4 to 5.2.1 with DW"
     */
    @Test
    void saveObjectWithPropertyTypeChange() throws Exception
    {
        // The class must be local.
        DocumentReference classReference = new DocumentReference("myWiki", "mySpace", "myClass");
        when(xcontext.getWikiId()).thenReturn(classReference.getWikiReference().getName());
        BaseObject object = mock(BaseObject.class);
        when(object.getXClassReference()).thenReturn(classReference);

        // Query to check if the object exists already (save versus update).
        when(xcontext.get("hibsession")).thenReturn(session);
        when(session.createQuery("select obj.id from BaseObject as obj where obj.id = :id", Long.class))
            .thenReturn(mock(Query.class));

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

        Query<String> oldClassTypeQuery = mock(Query.class);
        when(session.createQuery(
            "select prop.classType from BaseProperty as prop " + "where prop.id.id = :id and prop.id.name= :name",
            String.class)).thenReturn(oldClassTypeQuery);
        // The old value has a different type (String -> TextArea).
        when(oldClassTypeQuery.uniqueResult()).thenReturn(StringProperty.class.getName());

        // The old property must be loaded from the corresponding table.
        Query oldPropertyQuery = mock(Query.class);
        when(session.createQuery("select prop from " + StringProperty.class.getName()
            + " as prop where prop.id.id = :id and prop.id.name= :name")).thenReturn(oldPropertyQuery);
        BaseProperty oldProperty = mock(BaseProperty.class);
        when(oldPropertyQuery.uniqueResult()).thenReturn(oldProperty);

        store.saveXWikiCollection(object, xcontext, false);

        verify(oldClassTypeQuery).setParameter("id", propertyId);
        verify(oldClassTypeQuery).setParameter("name", propertyName);

        verify(oldPropertyQuery).setParameter("id", propertyId);
        verify(oldPropertyQuery).setParameter("name", propertyName);

        // Delete the old property value and then save the new one.
        verify(session).delete(oldProperty);
        verify(session).save(property);
    }

    @Test
    void existsWithRootLocale() throws Exception
    {
        String fullName = "space.page";
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getLocale()).thenReturn(Locale.ROOT);
        when(doc.getFullName()).thenReturn(fullName);

        Query query = mock(Query.class);
        when(session.createQuery("select doc.fullName from XWikiDocument as doc where doc.fullName=:fullName"))
            .thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(fullName));

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        when(this.wikiDescriptorManager.exists("wiki")).thenReturn(true);

        assertTrue(store.exists(doc, xcontext));

        verify(query).setParameter("fullName", fullName);
    }

    @Test
    void existsWithNonRootLocale() throws Exception
    {
        String fullName = "space.page";
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getLocale()).thenReturn(Locale.ENGLISH);
        when(doc.getFullName()).thenReturn(fullName);

        Query query = mock(Query.class);
        String statement = "select doc.fullName from XWikiDocument as doc where doc.fullName=:fullName"
            + " and doc.language=:language";
        when(session.createQuery(statement)).thenReturn(query);
        when(query.list()).thenReturn(Collections.singletonList(fullName));

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        when(this.wikiDescriptorManager.exists("wiki")).thenReturn(true);

        assertTrue(store.exists(doc, xcontext));

        verify(query).setParameter("fullName", fullName);
        verify(query).setParameter("language", Locale.ENGLISH.toString());
    }

    @Test
    void existsWhenDocumentBelongsToNonExistingWiki() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("notexisting");
        when(this.wikiDescriptorManager.exists("notexisting")).thenReturn(false);

        assertFalse(store.exists(doc, xcontext));
    }

    @Test
    void existsWhenFailureToGetDescriptors() throws Exception
    {
        XWikiDocument doc = mock(XWikiDocument.class);

        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wiki");
        when(this.wikiDescriptorManager.exists("wiki")).thenThrow(new WikiManagerException("error"));

        Throwable exception = assertThrows(XWikiException.class, () -> store.exists(doc, xcontext));
        assertEquals("Error number 3236 in 3: Error while checking for existence of the [wiki] wiki",
            exception.getMessage());
        assertEquals("WikiManagerException: error", ExceptionUtils.getRootCauseMessage(exception));
    }

    @Test
    void getTranslationList() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("chess", Arrays.asList("Path", "To"), "Success");
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getDocumentReference()).thenReturn(documentReference);

        org.xwiki.query.Query query = mock(org.xwiki.query.Query.class);
        List<Object> translationList = Arrays.<Object>asList("fr", "ro");
        when(query.execute()).thenReturn(translationList);

        when(queryManager.createQuery(any(String.class), eq(org.xwiki.query.Query.HQL))).thenReturn(query);
        when(localEntityReferenceSerializer.serialize(documentReference.getParent())).thenReturn("Path.To");

        assertEquals(translationList, store.getTranslationList(doc, xcontext));

        verify(query).setWiki(documentReference.getWikiReference().getName());
        verify(query).bindValue("space", "Path.To");
        verify(query).bindValue("name", documentReference.getName());
    }

    @Test
    void loadBacklinksFromSameWiki() throws Exception
    {
        // in this test we simulate load of backlinks from the same wiki context.
        DocumentReference documentReference = new DocumentReference("xwiki", Arrays.asList("B"), "WebHome");
        // We are currently in the context of "xwiki"
        WikiReference currentWikiReference = new WikiReference("xwiki");
        when(this.xcontext.getWikiReference()).thenReturn(currentWikiReference);
        when(this.compactWikiEntityReferenceSerializer.serialize(documentReference)).thenReturn("B.WebHome");
        List<String> resultList = new ArrayList<>();
        resultList.add("A.WebHome");

        when(this.hibernateStore.beginTransaction()).thenReturn(true);
        Query<String> query = mock(Query.class);
        when(this.session.createQuery(anyString(), same(String.class))).thenReturn(query);
        when(query.list()).thenReturn(resultList);
        DocumentReference expectedBacklink = new DocumentReference("xwiki", Arrays.asList("A"), "WebHome");
        when(this.currentMixedDocumentReferenceResolver.resolve("A.WebHome")).thenReturn(expectedBacklink);

        List<DocumentReference> obtainedReferences = this.store.loadBacklinks(documentReference, true, this.xcontext);
        assertEquals(1, obtainedReferences.size());
        assertEquals(expectedBacklink, obtainedReferences.get(0));
        verify(query).setParameter("backlink", "B.WebHome");
        verify(this.hibernateStore).beginTransaction();
        verify(this.hibernateStore).endTransaction(false);
    }

    @Test
    void loadBacklinksFromSubwiki() throws Exception
    {
        // in this test we simulate load of backlinks from a different wiki context.
        DocumentReference documentReference = new DocumentReference("subwiki", Arrays.asList("B"), "WebHome");
        // We are currently in the context of "xwiki"
        WikiReference currentWikiReference = new WikiReference("xwiki");
        when(this.xcontext.getWikiReference()).thenReturn(currentWikiReference);
        when(this.compactWikiEntityReferenceSerializer.serialize(documentReference)).thenReturn("subwiki:B.WebHome");
        List<String> resultList = new ArrayList<>();
        resultList.add("Foo.WebHome");

        when(this.hibernateStore.beginTransaction()).thenReturn(true);
        Query<String> query = mock(Query.class);
        when(this.session.createQuery(anyString(), same(String.class))).thenReturn(query);
        when(query.list()).thenReturn(resultList);
        DocumentReference expectedBacklink = new DocumentReference("xwiki", Arrays.asList("Foo"), "WebHome");
        when(this.currentMixedDocumentReferenceResolver.resolve("Foo.WebHome")).thenReturn(expectedBacklink);

        List<DocumentReference> obtainedReferences = this.store.loadBacklinks(documentReference, true, this.xcontext);
        assertEquals(1, obtainedReferences.size());
        assertEquals(expectedBacklink, obtainedReferences.get(0));
        verify(query).setParameter("backlink", "subwiki:B.WebHome");
        verify(this.hibernateStore).beginTransaction();
        verify(this.hibernateStore).endTransaction(false);
    }
}
