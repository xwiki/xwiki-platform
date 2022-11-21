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

import javax.inject.Provider;

import org.hibernate.Session;
import org.hibernate.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiHibernateBaseStore.HibernateCallback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link NotificationFilterPreferenceStore}.
 *
 * @version $Id$
 * @since 14.5
 * @since 14.4.1
 * @since 13.10.7
 */
@ComponentTest
class NotificationFilterPreferenceStoreTest
{
    private static final String PREVIOUS_WIKI_ID = "previousWikiId";

    private static final String MAIN_WIKI_ID = "mainWikiId";

    @InjectMockComponents
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private NotificationFilterPreferenceConfiguration filterPreferenceConfiguration;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiHibernateStore hibernateStore;

    @Mock
    private Session session;

    @Mock
    private Query query;

    @BeforeEach
    void setUp() throws XWikiException
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWikiId()).thenReturn(PREVIOUS_WIKI_ID);
        when(this.context.getMainXWiki()).thenReturn(MAIN_WIKI_ID);
        XWiki wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(wiki);
        when(wiki.getHibernateStore()).thenReturn(this.hibernateStore);
        when(this.hibernateStore.executeWrite(same(this.context), any())).thenAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                invocation.<HibernateCallback<Void>>getArgument(1).doInHibernate(session);

                return null;
            }
        });
        when(this.hibernateStore.getSession(this.context)).thenReturn(this.session);
        when(this.session.createQuery(anyString())).thenReturn(this.query);
        when(this.query.setParameter(anyString(), any())).thenReturn(this.query);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteFilterPreference(boolean useMainStore) throws Exception
    {
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        if (useMainStore) {
            when(this.context.getWikiId()).thenReturn(PREVIOUS_WIKI_ID, MAIN_WIKI_ID);
        }

        this.notificationFilterPreferenceStore.deleteFilterPreference(new WikiReference("wikiid"));

        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_ID);
        }
        verify(this.session).createQuery("delete from DefaultNotificationFilterPreference "
            + "where page like :wikiPrefix "
            + "or pageOnly like :wikiPrefix "
            + "or user like :wikiPrefix "
            + "or wiki = :wikiId");
        verify(this.query).setParameter("wikiPrefix", "wikiid:%");
        verify(this.query).setParameter("wikiId", "wikiid");
        verify(this.query).executeUpdate();
        if (useMainStore) {
            verify(this.context).setWikiId(PREVIOUS_WIKI_ID);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteFilterPreferenceHibernateException(boolean useMainStore) throws Exception
    {
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);

        when(this.hibernateStore.executeWrite(same(context), any())).thenThrow(XWikiException.class);
        if (useMainStore) {
            when(this.context.getWikiId()).thenReturn(PREVIOUS_WIKI_ID, MAIN_WIKI_ID);
        }

        NotificationException notificationException = assertThrows(NotificationException.class,
            () -> this.notificationFilterPreferenceStore.deleteFilterPreference(
                new WikiReference("wikiid")));

        assertEquals("Failed to delete the notification preferences for wiki [wikiid]",
            notificationException.getMessage());
        assertEquals(XWikiException.class, notificationException.getCause().getClass());

        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_ID);
        }
        if (useMainStore) {
            verify(this.context).setWikiId(PREVIOUS_WIKI_ID);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteFilterPreferences(boolean useMainStore) throws Exception
    {
        DocumentReference unknownUserDocumentReference = new DocumentReference("xwiki", "XWiki", "UnknownUser");

        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        when(this.entityReferenceSerializer.serialize(unknownUserDocumentReference))
            .thenReturn("xwiki:XWiki.UnknownUser");
        if (useMainStore) {
            when(this.context.getWikiId()).thenReturn(PREVIOUS_WIKI_ID, MAIN_WIKI_ID);
        }

        this.notificationFilterPreferenceStore.deleteFilterPreferences(unknownUserDocumentReference);

        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_ID);
        }
        verify(this.session).createQuery("delete from DefaultNotificationFilterPreference where owner = :user "
            + "or user = :user");
        verify(this.query).setParameter("user", "xwiki:XWiki.UnknownUser");
        verify(this.query).executeUpdate();
        if (useMainStore) {
            verify(this.context).setWikiId(PREVIOUS_WIKI_ID);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteFilterPreferencesHibernateException(boolean useMainStore) throws Exception
    {
        DocumentReference unknownUserDocumentReference = new DocumentReference("xwiki", "XWiki", "UnknownUser");

        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        when(this.entityReferenceSerializer.serialize(unknownUserDocumentReference))
            .thenReturn("xwiki:XWiki.UnknownUser");
        when(this.hibernateStore.executeWrite(same(context), any())).thenThrow(XWikiException.class);
        if (useMainStore) {
            when(this.context.getWikiId()).thenReturn(PREVIOUS_WIKI_ID, MAIN_WIKI_ID);
        }

        NotificationException notificationException = assertThrows(NotificationException.class,
            () -> this.notificationFilterPreferenceStore.deleteFilterPreferences(unknownUserDocumentReference));

        assertEquals("Failed to delete the notification preferences for user [xwiki:XWiki.UnknownUser]",
            notificationException.getMessage());
        assertEquals(XWikiException.class, notificationException.getCause().getClass());

        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_ID);
        }
        if (useMainStore) {
            verify(this.context).setWikiId(PREVIOUS_WIKI_ID);
        }
    }
}
