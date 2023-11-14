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
import org.xwiki.notifications.filters.internal.event.NotificationFilterPreferenceDeletedEvent;
import org.xwiki.observation.ObservationManager;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private static final WikiReference CURRENT_WIKI_REFERENCE = new WikiReference("current");
    private static final WikiReference MAIN_WIKI_REFERENCE = new WikiReference("main");

    @InjectMockComponents
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @MockComponent
    private NotificationFilterPreferenceConfiguration filterPreferenceConfiguration;

    @MockComponent
    private ObservationManager observationManager;

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
        when(this.context.getWikiReference()).thenReturn(CURRENT_WIKI_REFERENCE);
        when(this.context.getMainXWiki()).thenReturn(MAIN_WIKI_REFERENCE.getName());
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
    void deleteWikiFilterPreferences(boolean useMainStore) throws Exception
    {
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        when(this.context.getWikiReference()).thenReturn(CURRENT_WIKI_REFERENCE);

        WikiReference wikiReference = new WikiReference("wikiid");
        this.notificationFilterPreferenceStore.deleteFilterPreference(wikiReference);

        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_REFERENCE.getName());
        } else {
            verify(this.context).setWikiReference(wikiReference);
        }
        verify(this.session).createQuery("delete from DefaultNotificationFilterPreference "
            + "where page like :wikiPrefix "
            + "or pageOnly like :wikiPrefix "
            + "or user like :wikiPrefix "
            + "or wiki = :wikiId");
        verify(this.query).setParameter("wikiPrefix", "wikiid:%");
        verify(this.query).setParameter("wikiId", "wikiid");
        verify(this.query).executeUpdate();
        verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteWikiFilterPreferenceHibernateException(boolean useMainStore) throws Exception
    {
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        when(this.context.getWikiReference()).thenReturn(CURRENT_WIKI_REFERENCE);

        when(this.hibernateStore.executeWrite(same(context), any())).thenThrow(XWikiException.class);

        WikiReference wikiReference = new WikiReference("wikiid");
        NotificationException notificationException = assertThrows(NotificationException.class,
            () -> this.notificationFilterPreferenceStore.deleteFilterPreference(
                wikiReference));

        assertEquals("Failed to delete the notification preferences for wiki [wikiid]",
            notificationException.getMessage());
        assertEquals(XWikiException.class, notificationException.getCause().getClass());

        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_REFERENCE.getName());
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        } else {
            verify(this.context).setWikiReference(wikiReference);
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteUserFilterPreferences(boolean useMainStore) throws Exception
    {
        DocumentReference unknownUserDocumentReference = new DocumentReference("xwiki", "XWiki", "UnknownUser");
        when(this.context.getWikiReference()).thenReturn(CURRENT_WIKI_REFERENCE);
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        when(this.entityReferenceSerializer.serialize(unknownUserDocumentReference))
            .thenReturn("xwiki:XWiki.UnknownUser");

        this.notificationFilterPreferenceStore.deleteFilterPreferences(unknownUserDocumentReference);

        verify(this.session).createQuery("delete from DefaultNotificationFilterPreference where owner = :user "
            + "or user = :user");
        verify(this.query).setParameter("user", "xwiki:XWiki.UnknownUser");
        verify(this.query).executeUpdate();
        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_REFERENCE.getName());
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        } else {
            verify(this.context).setWikiReference(new WikiReference("xwiki"));
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteUserFilterPreferencesHibernateException(boolean useMainStore) throws Exception
    {
        DocumentReference unknownUserDocumentReference = new DocumentReference("xwiki", "XWiki", "UnknownUser");
        when(this.context.getWikiReference()).thenReturn(CURRENT_WIKI_REFERENCE);
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        when(this.entityReferenceSerializer.serialize(unknownUserDocumentReference))
            .thenReturn("xwiki:XWiki.UnknownUser");
        when(this.hibernateStore.executeWrite(same(context), any())).thenThrow(XWikiException.class);

        NotificationException notificationException = assertThrows(NotificationException.class,
            () -> this.notificationFilterPreferenceStore.deleteFilterPreferences(unknownUserDocumentReference));

        assertEquals("Failed to delete the notification preferences for user [xwiki:XWiki.UnknownUser]",
            notificationException.getMessage());
        assertEquals(XWikiException.class, notificationException.getCause().getClass());

        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_REFERENCE.getName());
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        } else {
            verify(this.context).setWikiReference(new WikiReference("xwiki"));
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteUserFilterPreference(boolean useMainStore) throws NotificationException
    {
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Foo");
        long internalId = 84523;
        String filterId = "NFP_" + internalId;
        when(this.context.getWikiReference()).thenReturn(CURRENT_WIKI_REFERENCE);
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        this.notificationFilterPreferenceStore.deleteFilterPreference(userReference, filterId);
        verify(this.session).createQuery(
            "delete from DefaultNotificationFilterPreference where internalId = :id");
        verify(this.query).setParameter("id", internalId);
        verify(this.query).executeUpdate();
        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_REFERENCE.getName());
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        } else {
            verify(this.context).setWikiReference(new WikiReference("xwiki"));
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        }
        verify(this.observationManager).notify(
            any(NotificationFilterPreferenceDeletedEvent.class), eq(userReference), eq(filterId));
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void deleteWikiFilterPreference(boolean useMainStore) throws NotificationException
    {
        WikiReference wikiReference = new WikiReference("foo");
        long internalId = 74;
        String filterId = "NFP_" + internalId;
        when(this.context.getWikiReference()).thenReturn(CURRENT_WIKI_REFERENCE);
        when(this.filterPreferenceConfiguration.useMainStore()).thenReturn(useMainStore);
        this.notificationFilterPreferenceStore.deleteFilterPreference(wikiReference, filterId);
        verify(this.session).createQuery(
            "delete from DefaultNotificationFilterPreference where internalId = :id");
        verify(this.query).setParameter("id", internalId);
        verify(this.query).executeUpdate();
        if (useMainStore) {
            verify(this.context).setWikiId(MAIN_WIKI_REFERENCE.getName());
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        } else {
            verify(this.context).setWikiReference(wikiReference);
            verify(this.context).setWikiReference(CURRENT_WIKI_REFERENCE);
        }
        verify(this.observationManager).notify(
            any(NotificationFilterPreferenceDeletedEvent.class), eq(wikiReference), eq(filterId));
    }
}
