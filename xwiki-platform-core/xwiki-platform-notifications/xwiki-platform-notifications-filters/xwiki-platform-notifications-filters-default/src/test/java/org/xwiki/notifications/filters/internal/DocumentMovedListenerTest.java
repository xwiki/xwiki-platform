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
package org.xwiki.notifications.filters.internal;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Provider;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;
import com.xpn.xwiki.store.XWikiHibernateStore;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DocumentMovedListener}.
 * 
 * @version $Id$
 */
@ComponentTest
class DocumentMovedListenerTest
{
    @InjectComponentManager
    protected MockitoComponentManager componentManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private NamespaceContextExecutor namespaceContextExecutor;

    @InjectMockComponents
    private DocumentMovedListener listener;

    private CachedFilterPreferencesModelBridge cachedModelBridge;

    @Mock
    private XWikiContext xwikicontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiHibernateStore hibernateStore;

    private Session session;

    @AfterComponent
    private void afterComponent() throws Exception
    {
        this.cachedModelBridge = mock(CachedFilterPreferencesModelBridge.class);
        this.componentManager.registerComponent(FilterPreferencesModelBridge.class, "cached", this.cachedModelBridge);
    }

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.xwikicontext);
        when(this.xwikicontext.getWiki()).thenReturn(this.xwiki);
        when(this.xwiki.getHibernateStore()).thenReturn(this.hibernateStore);
        when(this.hibernateStore.executeWrite(same(this.xwikicontext), any())).then(invocationOnMock -> {
            HibernateCallback callback = invocationOnMock.getArgument(1);
            callback.doInHibernate(session);
            return null;
        });
        when(this.namespaceContextExecutor.execute(any(), any())).then(invocationOnMock -> {
            Callable callable = invocationOnMock.getArgument(1);
            callable.call();
            return null;
        });
    }

    @Test
    void onEventWhenNonTerminalDocumentOnMainWiki() throws Exception
    {
        DocumentReference source = new DocumentReference("xwiki", "PageA", "WebHome");
        DocumentReference target = new DocumentReference("xwiki", "PageB", "WebHome");
        when(serializer.serialize(new SpaceReference("PageA", new WikiReference("xwiki")))).thenReturn("xwiki:PageA");
        when(serializer.serialize(new SpaceReference("PageB", new WikiReference("xwiki")))).thenReturn("xwiki:PageB");
        when(serializer.serialize(source)).thenReturn("xwiki:PageA.WebHome");
        when(serializer.serialize(target)).thenReturn("xwiki:PageB.WebHome");

        // Mock
        when(wikiDescriptorManager.getAllIds()).thenReturn(List.of("mainWiki"));

        session = mock(Session.class);
        when(hibernateStore.getSession(eq(xwikicontext))).thenReturn(session);
        Query query = mock(Query.class);
        when(session.createQuery(
            "update DefaultNotificationFilterPreference p set p.page = :newPage " + "where p.page = :oldPage"))
                .thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        Query query2 = mock(Query.class);
        when(session.createQuery(
            "update DefaultNotificationFilterPreference p set p.pageOnly = :newPage " + "where p.pageOnly = :oldPage"))
                .thenReturn(query2);
        when(query2.setParameter(any(String.class), any())).thenReturn(query2);

        // Test
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);
        this.listener.onEvent(event, null, null);

        // Verify
        verify(cachedModelBridge).clearCache();
        verify(query).setParameter("newPage", "xwiki:PageB");
        verify(query).setParameter("oldPage", "xwiki:PageA");
        verify(query).executeUpdate();
        verify(query2).setParameter("newPage", "xwiki:PageB.WebHome");
        verify(query2).setParameter("oldPage", "xwiki:PageA.WebHome");
        verify(query2).executeUpdate();
        verify(namespaceContextExecutor).execute(eq(new WikiNamespace("mainWiki")), any());
    }

    @Test
    void onEventWhenNonTerminalDocumentOnMainWikiLocalStoreOnly() throws Exception
    {
        DocumentReference source = new DocumentReference("xwiki", "PageA", "WebHome");
        DocumentReference target = new DocumentReference("xwiki", "PageB", "WebHome");
        when(serializer.serialize(new SpaceReference("PageA", new WikiReference("xwiki")))).thenReturn("xwiki:PageA");
        when(serializer.serialize(new SpaceReference("PageB", new WikiReference("xwiki")))).thenReturn("xwiki:PageB");
        when(serializer.serialize(source)).thenReturn("xwiki:PageA.WebHome");
        when(serializer.serialize(target)).thenReturn("xwiki:PageB.WebHome");

        // Mock
        when(wikiDescriptorManager.getAllIds()).thenReturn(List.of("foo", "bar", "mainWiki"));

        session = mock(Session.class);
        when(hibernateStore.getSession(eq(xwikicontext))).thenReturn(session);
        Query query = mock(Query.class);
        when(session.createQuery(
            "update DefaultNotificationFilterPreference p set p.page = :newPage " + "where p.page = :oldPage"))
            .thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        Query query2 = mock(Query.class);
        when(session.createQuery(
            "update DefaultNotificationFilterPreference p set p.pageOnly = :newPage " + "where p.pageOnly = :oldPage"))
            .thenReturn(query2);
        when(query2.setParameter(any(String.class), any())).thenReturn(query2);

        // Test
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);
        this.listener.onEvent(event, null, null);

        // Verify
        verify(cachedModelBridge).clearCache();
        verify(query, times(3)).setParameter("newPage", "xwiki:PageB");
        verify(query, times(3)).setParameter("oldPage", "xwiki:PageA");
        verify(query, times(3)).executeUpdate();
        verify(query2, times(3)).setParameter("newPage", "xwiki:PageB.WebHome");
        verify(query2, times(3)).setParameter("oldPage", "xwiki:PageA.WebHome");
        verify(query2, times(3)).executeUpdate();
        verify(namespaceContextExecutor).execute(eq(new WikiNamespace("mainWiki")), any());
        verify(namespaceContextExecutor).execute(eq(new WikiNamespace("foo")), any());
        verify(namespaceContextExecutor).execute(eq(new WikiNamespace("bar")), any());
    }


    @Test
    void onEventWhenNonTerminalDocumentOnSubWiki() throws Exception
    {
        DocumentReference source = new DocumentReference("xwiki", "PageA", "WebHome");
        DocumentReference target = new DocumentReference("xwiki", "PageB", "WebHome");
        when(serializer.serialize(new SpaceReference("PageA", new WikiReference("xwiki")))).thenReturn("xwiki:PageA");
        when(serializer.serialize(new SpaceReference("PageB", new WikiReference("xwiki")))).thenReturn("xwiki:PageB");
        when(serializer.serialize(source)).thenReturn("xwiki:PageA.WebHome");
        when(serializer.serialize(target)).thenReturn("xwiki:PageB.WebHome");

        // Mock
        when(wikiDescriptorManager.getAllIds()).thenReturn(List.of("mainWiki"));

        session = mock(Session.class);
        when(hibernateStore.getSession(eq(xwikicontext))).thenReturn(session);
        Query query = mock(Query.class);
        when(session.createQuery(
            "update DefaultNotificationFilterPreference p set p.page = :newPage " + "where p.page = :oldPage"))
                .thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        Query query2 = mock(Query.class);
        when(session.createQuery(
            "update DefaultNotificationFilterPreference p set p.pageOnly = :newPage " + "where p.pageOnly = :oldPage"))
                .thenReturn(query2);
        when(query2.setParameter(any(String.class), any())).thenReturn(query2);

        // Test
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);
        this.listener.onEvent(event, null, null);

        // Verify
        verify(cachedModelBridge).clearCache();
        verify(query).setParameter("newPage", "xwiki:PageB");
        verify(query).setParameter("oldPage", "xwiki:PageA");
        verify(query).executeUpdate();
        verify(query2).setParameter("newPage", "xwiki:PageB.WebHome");
        verify(query2).setParameter("oldPage", "xwiki:PageA.WebHome");
        verify(query2).executeUpdate();
        verify(namespaceContextExecutor).execute(eq(new WikiNamespace("mainWiki")), any());
    }

    @Test
    void onEventNonTerminalDocumentOnMainWiki() throws Exception
    {
        DocumentReference source = new DocumentReference("xwiki", "PageA", "Terminal");
        DocumentReference target = new DocumentReference("xwiki", "PageB", "Terminal");
        when(serializer.serialize(source)).thenReturn("xwiki:PageA.Terminal");
        when(serializer.serialize(target)).thenReturn("xwiki:PageB.Terminal");

        // Mock
        when(wikiDescriptorManager.getAllIds()).thenReturn(List.of("mainWiki"));

        session = mock(Session.class);
        when(hibernateStore.getSession(eq(xwikicontext))).thenReturn(session);
        Query query = mock(Query.class);
        when(session.createQuery(
            "update DefaultNotificationFilterPreference p set p.pageOnly = :newPage " + "where p.pageOnly = :oldPage"))
                .thenReturn(query);
        when(query.setParameter(anyString(), anyString())).thenReturn(query);

        // Test
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);
        this.listener.onEvent(event, null, null);

        // Verify
        verify(cachedModelBridge).clearCache();
        verify(query).setParameter("newPage", "xwiki:PageB.Terminal");
        verify(query).setParameter("oldPage", "xwiki:PageA.Terminal");
        verify(query).executeUpdate();
        verify(namespaceContextExecutor).execute(eq(new WikiNamespace("mainWiki")), any());
    }
}
