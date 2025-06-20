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

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.internal.DefaultNotificationCacheManager;
import org.xwiki.notifications.rest.NotificationsResource;
import org.xwiki.notifications.rest.model.Notifications;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.notifications.sources.internal.DefaultNotificationParametersFactory;
import org.xwiki.notifications.sources.internal.DefaultNotificationParametersFactory.ParametersKey;
import org.xwiki.rest.XWikiResource;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiUser;

/**
 * Default implementation of {@link NotificationsResource}.
 *
 * @version $Id$
 * @since 10.4RC1
 */
@Component
@Named("org.xwiki.notifications.rest.internal.DefaultNotificationsResource")
public class DefaultNotificationsResource extends XWikiResource implements NotificationsResource
{
    private static final String TRUE = "true";
    private static final String ASYNC_ID = "asyncId";

    @Inject
    private ParametrizedNotificationManager newNotificationManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private InternalNotificationsRenderer notificationsRenderer;

    @Inject
    private DefaultNotificationCacheManager cacheManager;

    @Inject
    private NotificationEventExecutor executor;

    @Inject
    private DefaultNotificationParametersFactory notificationParametersFactory;

    @Inject
    private RSSFeedRenderer rssFeedRenderer;

    @Override
    public Response getNotifications(String useUserPreferences, String userId, String untilDate,
        boolean untilDateIncluded, String blackList, String pages, String spaces, String wikis, String users,
        String maxCount, String displayOwnEvents, String displayMinorEvents, String displaySystemEvents,
        String displayReadEvents, String displayReadStatus, String tags, String currentWiki, String async,
        String asyncId, String target) throws Exception
    {
        // Build the response
        Response.ResponseBuilder response;
        Object result = getCompositeEvents(useUserPreferences, userId, untilDate, untilDateIncluded, blackList, pages,
            spaces, wikis, users, toMaxCount(maxCount, 21), displayOwnEvents, displayMinorEvents, displaySystemEvents,
            displayReadEvents, tags, currentWiki, async, asyncId, false, false);

        if (result instanceof String) {
            response = Response.status(Status.ACCEPTED);
            response.entity(Collections.singletonMap(ASYNC_ID, result));
        } else {
            // Make sure URLs will be rendered like in any other display (by default REST API forces absolute URLs)
            XWikiContext xcontext = getXWikiContext();
            xcontext.setURLFactory(
                xcontext.getWiki().getURLFactoryService().createURLFactory(XWikiContext.MODE_SERVLET, xcontext));

            // Make sure to rendering the notifications in the right wiki
            String currentOriginalWiki = xcontext.getOriginalWikiId();
            try {
                if (currentWiki != null) {
                    xcontext.setOriginalWikiId(currentWiki);
                }

                Notifications notifications = new Notifications(this.notificationsRenderer
                    .renderNotifications((List<CompositeEvent>) result, userId, TRUE.equals(displayReadStatus)));

                response = Response.ok(notifications);
            } finally {
                xcontext.setOriginalWikiId(currentOriginalWiki);
            }
        }

        // Add the "cache control" header.
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoCache(true);
        response.cacheControl(cacheControl);

        return response.build();
    }

    private int toMaxCount(String maxCount, int defaultMaxCount)
    {
        return NumberUtils.toInt(maxCount, defaultMaxCount);
    }

    private Object getCompositeEvents(String useUserPreferences, String userId, String untilDate,
        boolean untilDateIncluded, String blackList, String pages, String spaces, String wikis, String users,
        int maxCount, String displayOwnEvents, String displayMinorEvents, String displaySystemEvents,
        String displayReadEvents, String tags, String currentWiki, String async, String asyncId, boolean onlyUnread,
        boolean count) throws Exception
    {
        Object result = null;
        NotificationParameters notificationParameters = getNotificationParameters(useUserPreferences, userId, untilDate,
            untilDateIncluded, blackList, pages, spaces, wikis, users, maxCount, displayOwnEvents, displayMinorEvents,
            displaySystemEvents, displayReadEvents, tags, currentWiki, onlyUnread);

        // 1. Check current asynchronous execution
        if (asyncId != null) {
            result = this.executor.popAsync(asyncId);

            if (result == null) {
                // Another round
                result = asyncId;
            }
        }

        if (result == null) {
            // 2. Generate the cache key
            String cacheKey = this.cacheManager.createCacheKey(notificationParameters);

            // 3. Search events
            result = this.executor.submit(cacheKey,
                () -> getCompositeEvents(notificationParameters),
                Boolean.parseBoolean(async), count, true);
        }

        return result;
    }

    private List<CompositeEvent> getCompositeEvents(NotificationParameters notificationParameters)
        throws NotificationException
    {
        return this.newNotificationManager.getEvents(notificationParameters);
    }

    @Override
    public Response getNotificationsCount(String useUserPreferences, String userId, String pages, String spaces,
        String wikis, String users, String maxCount, String displayOwnEvents, String displayMinorEvents,
        String displaySystemEvents, String displayReadEvents, String displayReadStatus, String tags, String currentWiki,
        String async, String asyncId) throws Exception
    {
        // Build the response
        Response.ResponseBuilder response;
        XWikiUser xWikiUser = getXWikiContext().getWiki().checkAuth(getXWikiContext());
        if (!StringUtils.isEmpty(userId) && xWikiUser == null) {
            response = Response.status(Status.UNAUTHORIZED);
        } else {
            Object result = getCompositeEvents(useUserPreferences, userId, null, true, null, pages, spaces, wikis,
                users, toMaxCount(maxCount, 21), displayOwnEvents, displayMinorEvents, displaySystemEvents,
                displayReadEvents, tags, currentWiki, async, asyncId, true, true);

            if (result instanceof String) {
                response = Response.status(Status.ACCEPTED);
                response.entity(Collections.singletonMap(ASYNC_ID, result));
            } else {
                response = Response.ok(Collections.singletonMap("unread", result));
            }

            // Add the "cache control" header.
            CacheControl cacheControl = new CacheControl();
            cacheControl.setNoCache(true);
            response.cacheControl(cacheControl);
        }
        return response.build();
    }

    @Override
    public String getNotificationsRSS(String useUserPreferences, String userId, String untilDate, String blackList,
        String pages, String spaces, String wikis, String users, String maxCount, String displayOwnEvents,
        String displayMinorEvents, String displaySystemEvents, String displayReadEvents, String displayReadStatus,
        String tags, String currentWiki) throws Exception
    {
        // Build the response
        XWikiUser xWikiUser = getXWikiContext().getWiki().checkAuth(getXWikiContext());
        DocumentReference userIdDoc = this.documentReferenceResolver.resolve(userId);
        if ((xWikiUser == null && !StringUtils.isEmpty(userId))
            || (xWikiUser != null && !userIdDoc.equals(xWikiUser.getUserReference()))) {
            getXWikiContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } else {
            List<CompositeEvent> events =
                (List<CompositeEvent>) getCompositeEvents(useUserPreferences, userId, untilDate, true, blackList, pages,
                    spaces, wikis, users, toMaxCount(maxCount, 10), displayOwnEvents, displayMinorEvents,
                    displaySystemEvents, displayReadEvents, tags, currentWiki, null, null, false, false);

            return this.rssFeedRenderer.render(events);
        }
    }

    @Override
    public Response postNotifications(String useUserPreferences, String userId, String untilDate,
        boolean untilDateIncluded, String blackList, String pages, String spaces, String wikis, String users,
        String count, String displayOwnEvents, String displayMinorEvents, String displaySystemEvents,
        String displayReadEvents, String displayReadStatus, String tags, String currentWiki, String async,
        String asyncId, String target) throws Exception
    {
        return getNotifications(useUserPreferences, userId, untilDate, untilDateIncluded, blackList, pages, spaces,
            wikis, users, count, displayOwnEvents, displayMinorEvents, displaySystemEvents, displayReadEvents,
            displayReadStatus, tags, currentWiki, async, asyncId, "alert");
    }

    private NotificationParameters getNotificationParameters(String useUserPreferences, String userId, String untilDate,
        boolean untilDateIncluded, String blackList, String pages, String spaces, String wikis, String users,
        int maxCount, String displayOwnEvents, String displayMinorEvents, String displaySystemEvents,
        String displayReadEvents, String tags, String currentWiki, boolean onlyUnread) throws NotificationException
    {
        Map<ParametersKey, String> parametersMap = new HashMap<>();
        parametersMap.put(ParametersKey.USE_USER_PREFERENCES, useUserPreferences);
        parametersMap.put(ParametersKey.USER_ID, userId);
        parametersMap.put(ParametersKey.UNTIL_DATE, untilDate);
        parametersMap.put(ParametersKey.UNTIL_DATE_INCLUDED, String.valueOf(untilDateIncluded));
        parametersMap.put(ParametersKey.BLACKLIST, blackList);
        parametersMap.put(ParametersKey.PAGES, pages);
        parametersMap.put(ParametersKey.SPACES, spaces);
        parametersMap.put(ParametersKey.WIKIS, wikis);
        parametersMap.put(ParametersKey.USERS, users);
        parametersMap.put(ParametersKey.MAX_COUNT, String.valueOf(maxCount));
        parametersMap.put(ParametersKey.DISPLAY_OWN_EVENTS, displayOwnEvents);
        parametersMap.put(ParametersKey.DISPLAY_MINOR_EVENTS, displayMinorEvents);
        parametersMap.put(ParametersKey.DISPLAY_SYSTEM_EVENTS, displaySystemEvents);
        parametersMap.put(ParametersKey.DISPLAY_READ_EVENTS, displayReadEvents);
        parametersMap.put(ParametersKey.TAGS, tags);
        parametersMap.put(ParametersKey.CURRENT_WIKI, currentWiki);
        parametersMap.put(ParametersKey.ONLY_UNREAD, String.valueOf(onlyUnread));

        return this.notificationParametersFactory.createNotificationParameters(parametersMap);
    }
}
