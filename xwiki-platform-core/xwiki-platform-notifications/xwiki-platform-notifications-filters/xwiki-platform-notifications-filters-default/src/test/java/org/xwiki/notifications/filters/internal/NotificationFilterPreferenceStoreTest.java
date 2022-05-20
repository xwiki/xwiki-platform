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
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link NotificationFilterPreferenceStore}.
 *
 * @version $Id$
 * @since 14.5RC1
 * @since 13.10.6
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

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiHibernateStore hibernateStore;

    @Mock
    private Session session;

    @Mock
    private Query query;

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWikiId()).thenReturn(PREVIOUS_WIKI_ID);
        when(this.context.getMainXWiki()).thenReturn(MAIN_WIKI_ID);
        XWiki wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(wiki);
        when(wiki.getHibernateStore()).thenReturn(this.hibernateStore);
        when(this.hibernateStore.getSession(this.context)).thenReturn(this.session);
        when(this.session.createQuery(anyString())).thenReturn(this.query);
        when(this.query.setParameter(anyString(), any())).thenReturn(this.query);
    }

    @Test
    void deleteFilterPreference() throws Exception
    {
        this.notificationFilterPreferenceStore.deleteFilterPreference(new WikiReference("wikiid"));
        verify(this.context).setWikiId(MAIN_WIKI_ID);
        verify(this.hibernateStore).beginTransaction(this.context);
        verify(this.session).createQuery("delete from DefaultNotificationFilterPreference "
            + "where page like :wikiPrefix "
            + "or pageOnly like :wikiPrefix "
            + "or user like :wikiPrefix "
            + "or wiki = :wikiId");
        verify(this.query).setParameter("wikiPrefix", "wikiid:%");
        verify(this.query).setParameter("wikiId", "wikiid");
        verify(this.query).executeUpdate();
        verify(this.hibernateStore).endTransaction(this.context, true);
        verify(this.context).setWikiId(PREVIOUS_WIKI_ID);
    }

    @Test
    void deleteFilterPreferenceHibernateException() throws Exception
    {
        WikiReference wikiReference = new WikiReference("wikiid");

        when(this.hibernateStore.beginTransaction(this.context)).thenThrow(XWikiException.class);

        NotificationException notificationException = assertThrows(NotificationException.class,
            () -> this.notificationFilterPreferenceStore.deleteFilterPreference(
                wikiReference));

        assertEquals("Failed to delete the notification preferences for wiki [wikiid]",
            notificationException.getMessage());
        assertEquals(XWikiException.class, notificationException.getCause().getClass());

        verify(this.context).setWikiId(MAIN_WIKI_ID);
        verify(this.hibernateStore).beginTransaction(this.context);
        verify(this.hibernateStore).endTransaction(this.context, false);
        verify(this.context).setWikiId(PREVIOUS_WIKI_ID);
    }
}
