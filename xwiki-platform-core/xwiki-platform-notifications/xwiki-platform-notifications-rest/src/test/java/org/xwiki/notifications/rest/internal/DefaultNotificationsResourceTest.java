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
package org.xwiki.notifications.rest.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.notifiers.internal.DefaultNotificationCacheManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.internal.DefaultNotificationParametersFactory;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.XWikiResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultNotificationsResource}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultNotificationsResourceTest
{
    @InjectMockComponents
    private DefaultNotificationsResource notificationsResource;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private DefaultNotificationParametersFactory notificationParametersFactory;

    @MockComponent
    private DefaultNotificationCacheManager cacheManager;

    @MockComponent
    private NotificationEventExecutor executor;

    @MockComponent
    private RSSFeedRenderer rssFeedRenderer;

    private XWikiContext context;
    private XWiki wiki;
    private XWikiResponse response;

    @BeforeComponent
    void beforeComponent()
    {
        // We need this before component because XWikiResource is calling the context in Initialize call.
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);
    }

    @BeforeEach
    void setup()
    {
        this.wiki = mock(XWiki.class);
        when(this.context.getWiki()).thenReturn(this.wiki);
        this.response = mock(XWikiResponse.class);
        when(this.context.getResponse()).thenReturn(this.response);
    }

    @Test
    void getNotificationsRSSGuestUserId() throws Exception
    {
        String userId = "XWiki.Admin";
        when(this.wiki.checkAuth(this.context)).thenReturn(null);
        assertNull(this.notificationsResource.getNotificationsRSS(null, userId, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null));
        verify(this.wiki).checkAuth(this.context);
        verify(this.response).sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    void getNotificationsRSSUserId() throws Exception
    {
        String userId = "XWiki.Admin";
        XWikiUser wikiUser = mock(XWikiUser.class);
        when(this.wiki.checkAuth(this.context)).thenReturn(wikiUser);
        DocumentReference userIdDocReference = mock(DocumentReference.class);
        when(this.documentReferenceResolver.resolve(userId)).thenReturn(userIdDocReference);

        when(wikiUser.getUserReference()).thenReturn(mock(DocumentReference.class));
        assertNull(this.notificationsResource.getNotificationsRSS(null, userId, null, null, null, null, null, null,
            null, null, null, null, null, null, null, null));
        verify(this.wiki).checkAuth(this.context);
        verify(this.response).sendError(HttpServletResponse.SC_UNAUTHORIZED);

        when(wikiUser.getUserReference()).thenReturn(userIdDocReference);

        String useUserPreferences = "false";
        String untilDate = "0";
        String blackList = "";
        String pages = "Main.WebHome";
        String spaces = "";
        String wikis = "";
        String users = "";
        String displayOwnEvents = "false";
        String displayMinorEvents = "false";
        String displaySystemEvents = "false";
        String displayReadEvents = "false";
        String tags = "foo";
        String currentWiki = "xwiki";

        Map<DefaultNotificationParametersFactory.ParametersKey, String> parametersMap = new HashMap<>();
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.USE_USER_PREFERENCES, useUserPreferences);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.USER_ID, userId);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.UNTIL_DATE, untilDate);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.UNTIL_DATE_INCLUDED, String.valueOf(true));
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.BLACKLIST, blackList);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.PAGES, pages);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.SPACES, spaces);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.WIKIS, wikis);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.USERS, users);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.MAX_COUNT, String.valueOf(10));
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.DISPLAY_OWN_EVENTS, displayOwnEvents);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.DISPLAY_MINOR_EVENTS, displayMinorEvents);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.DISPLAY_SYSTEM_EVENTS,
            displaySystemEvents);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.DISPLAY_READ_EVENTS, displayReadEvents);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.TAGS, tags);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.CURRENT_WIKI, currentWiki);
        parametersMap.put(DefaultNotificationParametersFactory.ParametersKey.ONLY_UNREAD, String.valueOf(false));

        NotificationParameters notificationParameters = mock(NotificationParameters.class);
        when(this.notificationParametersFactory.createNotificationParameters(any()))
            .then(invocationOnMock -> {
                assertEquals(parametersMap, invocationOnMock.getArgument(0));
                return notificationParameters;
            });

        String expectedCacheKey = "cacheKey";
        when(this.cacheManager.createCacheKey(notificationParameters)).thenReturn(expectedCacheKey);

        List<CompositeEvent> eventList = Collections.singletonList(mock(CompositeEvent.class));
        when(this.executor.submit(eq(expectedCacheKey), any(), eq(false), eq(false), eq(true))).thenReturn(eventList);

        String expectedResult = "some results";
        when(this.rssFeedRenderer.render(eventList)).thenReturn(expectedResult);

        assertEquals(expectedResult, this.notificationsResource
            .getNotificationsRSS(useUserPreferences, userId, untilDate, blackList, pages, spaces, wikis, users, "",
                displayOwnEvents, displayMinorEvents, displaySystemEvents, displayReadEvents, "", tags, currentWiki));
    }
}