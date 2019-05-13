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
package org.xwiki.notifications.filters.internal.migrators;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.store.XWikiHibernateStore;
import org.hibernate.Query;
import org.hibernate.Session;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.eventstream.store.internal.LegacyEventStreamStoreConfiguration;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.filters.internal.CachedModelBridge;
import org.xwiki.notifications.filters.internal.DocumentMovedListener;
import org.xwiki.notifications.filters.internal.ModelBridge;
import org.xwiki.refactoring.event.DocumentRenamedEvent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import javax.inject.Provider;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since
 */
public class DocumentMovedListenerTest
{
    @Rule
    public final MockitoComponentMockingRule<DocumentMovedListener> mocker =
            new MockitoComponentMockingRule<>(DocumentMovedListener.class);

    private Provider<XWikiContext> contextProvider;
    private WikiDescriptorManager wikiDescriptorManager;
    private XWikiContext xwikicontext;
    private XWiki xwiki;

    private EntityReferenceSerializer<String> serializer;
    private LegacyEventStreamStoreConfiguration legacyEventStreamStoreConfiguration;
    private NamespaceContextExecutor namespaceContextExecutor;
    private CachedModelBridge cachedModelBridge;

    @Before
    public void setUp() throws Exception
    {
        contextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xwikicontext = mock(XWikiContext.class);
        when(contextProvider.get()).thenReturn(xwikicontext);
        xwiki = mock(XWiki.class);
        when(xwikicontext.getWiki()).thenReturn(xwiki);
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        serializer = mocker.getInstance(EntityReferenceSerializer.TYPE_STRING);

        legacyEventStreamStoreConfiguration = mocker.getInstance(LegacyEventStreamStoreConfiguration.class);
        namespaceContextExecutor = mocker.getInstance(NamespaceContextExecutor.class);
        cachedModelBridge = mock(CachedModelBridge.class);
        mocker.registerComponent(ModelBridge.class, "cached", cachedModelBridge);
    }

    @Test
    public void onEventWhenNonTerminalDocumentOnMainWiki() throws Exception
    {
        DocumentReference source = new DocumentReference("xwiki", "PageA", "WebHome");
        DocumentReference target = new DocumentReference("xwiki", "PageB", "WebHome");
        when(serializer.serialize(new SpaceReference("PageA", new WikiReference("xwiki")))).thenReturn("xwiki:PageA");
        when(serializer.serialize(new SpaceReference("PageB", new WikiReference("xwiki")))).thenReturn("xwiki:PageB");
        when(serializer.serialize(source)).thenReturn("xwiki:PageA.WebHome");
        when(serializer.serialize(target)).thenReturn("xwiki:PageB.WebHome");

        // Mock
        when(legacyEventStreamStoreConfiguration.useLocalStore()).thenReturn(true);
        when(legacyEventStreamStoreConfiguration.useMainStore()).thenReturn(true);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainWiki");
        when(wikiDescriptorManager.isMainWiki("mainWiki")).thenReturn(true);

        XWikiHibernateStore hibernateStore = mock(XWikiHibernateStore.class);
        when(xwiki.getHibernateStore()).thenReturn(hibernateStore);
        Session session = mock(Session.class);
        when(hibernateStore.getSession(eq(xwikicontext))).thenReturn(session);
        Query query = mock(Query.class);
        when(session.createQuery("update DefaultNotificationFilterPreference p set p.page = :newPage " +
                "where p.page = :oldPage")).thenReturn(query);
        when(query.setString(anyString(), anyString())).thenReturn(query);
        Query query2 = mock(Query.class);
        when(session.createQuery("update DefaultNotificationFilterPreference p set p.pageOnly = :newPage " +
                "where p.pageOnly = :oldPage")).thenReturn(query2);
        when(query2.setString(anyString(), anyString())).thenReturn(query2);

        // Test
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);
        mocker.getComponentUnderTest().onEvent(event, null, null);

        // Verify
        verify(cachedModelBridge).clearCache();
        verify(query).setString("newPage", "xwiki:PageB");
        verify(query).setString("oldPage", "xwiki:PageA");
        verify(query).executeUpdate();
        verify(query2).setString("newPage", "xwiki:PageB.WebHome");
        verify(query2).setString("oldPage", "xwiki:PageA.WebHome");
        verify(query2).executeUpdate();
        verifyZeroInteractions(namespaceContextExecutor);
    }

    @Test
    public void onEventWhenNonTerminalDocumentOnSubWiki() throws Exception
    {
        DocumentReference source = new DocumentReference("xwiki", "PageA", "WebHome");
        DocumentReference target = new DocumentReference("xwiki", "PageB", "WebHome");
        when(serializer.serialize(new SpaceReference("PageA", new WikiReference("xwiki")))).thenReturn("xwiki:PageA");
        when(serializer.serialize(new SpaceReference("PageB", new WikiReference("xwiki")))).thenReturn("xwiki:PageB");
        when(serializer.serialize(source)).thenReturn("xwiki:PageA.WebHome");
        when(serializer.serialize(target)).thenReturn("xwiki:PageB.WebHome");

        // Mock
        when(legacyEventStreamStoreConfiguration.useLocalStore()).thenReturn(true);
        when(legacyEventStreamStoreConfiguration.useMainStore()).thenReturn(true);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("subwiki");
        when(wikiDescriptorManager.getMainWikiId()).thenReturn("mainWiki");
        when(wikiDescriptorManager.isMainWiki("subwiki")).thenReturn(false);

        XWikiHibernateStore hibernateStore = mock(XWikiHibernateStore.class);
        when(xwiki.getHibernateStore()).thenReturn(hibernateStore);
        Session session = mock(Session.class);
        when(hibernateStore.getSession(eq(xwikicontext))).thenReturn(session);
        Query query = mock(Query.class);
        when(session.createQuery("update DefaultNotificationFilterPreference p set p.page = :newPage " +
                "where p.page = :oldPage")).thenReturn(query);
        when(query.setString(anyString(), anyString())).thenReturn(query);
        Query query2 = mock(Query.class);
        when(session.createQuery("update DefaultNotificationFilterPreference p set p.pageOnly = :newPage " +
                "where p.pageOnly = :oldPage")).thenReturn(query2);
        when(query2.setString(anyString(), anyString())).thenReturn(query2);

        // Test
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);
        mocker.getComponentUnderTest().onEvent(event, null, null);

        // Verify
        verify(cachedModelBridge).clearCache();
        verify(query).setString("newPage", "xwiki:PageB");
        verify(query).setString("oldPage", "xwiki:PageA");
        verify(query).executeUpdate();
        verify(query2).setString("newPage", "xwiki:PageB.WebHome");
        verify(query2).setString("oldPage", "xwiki:PageA.WebHome");
        verify(query2).executeUpdate();
        verify(namespaceContextExecutor).execute(eq(new WikiNamespace("mainWiki")), any());
    }

    @Test
    public void onEventNonTerminalDocumentOnMainWiki() throws Exception
    {
        DocumentReference source = new DocumentReference("xwiki", "PageA", "Terminal");
        DocumentReference target = new DocumentReference("xwiki", "PageB", "Terminal");
        when(serializer.serialize(source)).thenReturn("xwiki:PageA.Terminal");
        when(serializer.serialize(target)).thenReturn("xwiki:PageB.Terminal");

        // Mock
        when(legacyEventStreamStoreConfiguration.useLocalStore()).thenReturn(true);
        when(legacyEventStreamStoreConfiguration.useMainStore()).thenReturn(true);
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("mainWiki");
        when(wikiDescriptorManager.isMainWiki("mainWiki")).thenReturn(true);

        XWikiHibernateStore hibernateStore = mock(XWikiHibernateStore.class);
        when(xwiki.getHibernateStore()).thenReturn(hibernateStore);
        Session session = mock(Session.class);
        when(hibernateStore.getSession(eq(xwikicontext))).thenReturn(session);
        Query query = mock(Query.class);
        when(session.createQuery("update DefaultNotificationFilterPreference p set p.pageOnly = :newPage " +
                "where p.pageOnly = :oldPage")).thenReturn(query);
        when(query.setString(anyString(), anyString())).thenReturn(query);

        // Test
        DocumentRenamedEvent event = new DocumentRenamedEvent(source, target);
        mocker.getComponentUnderTest().onEvent(event, null, null);

        // Verify
        verify(cachedModelBridge).clearCache();
        verify(query).setString("newPage", "xwiki:PageB.Terminal");
        verify(query).setString("oldPage", "xwiki:PageA.Terminal");
        verify(query).executeUpdate();
        verifyZeroInteractions(namespaceContextExecutor);
    }
}
