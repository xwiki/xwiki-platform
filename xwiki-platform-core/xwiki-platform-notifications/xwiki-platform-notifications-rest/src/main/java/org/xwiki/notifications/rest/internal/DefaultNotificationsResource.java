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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.SystemUserNotificationFilter;
import org.xwiki.notifications.filters.internal.minor.MinorEventAlertNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.status.EventReadAlertFilter;
import org.xwiki.notifications.filters.internal.status.ForUserEventFilter;
import org.xwiki.notifications.filters.internal.user.OwnEventFilter;
import org.xwiki.notifications.notifiers.rss.NotificationRSSManager;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.rest.NotificationsResource;
import org.xwiki.notifications.rest.model.Notifications;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.text.StringUtils;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.rometools.rome.io.SyndFeedOutput;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.XWikiRequest;

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
    private static final String FIELD_SEPARATOR = ",";

    private static final String TRUE = "true";

    @Inject
    private ParametrizedNotificationManager newNotificationManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private InternalNotificationsRenderer notificationsRenderer;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private UsersParameterHandler usersParameterHandler;

    @Inject
    private NotificationRSSManager notificationRSSManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private NotificationEventExecutor executor;

    @Inject
    private NotificationConfiguration configuration;

    @Override
    public Response getNotifications(String useUserPreferences, String userId, String untilDate, String blackList,
        String pages, String spaces, String wikis, String users, String maxCount, String displayOwnEvents,
        String displayMinorEvents, String displaySystemEvents, String displayReadEvents, String displayReadStatus,
        String tags, String currentWiki, String async, String asyncId) throws Exception
    {
        // Build the response
        Response.ResponseBuilder response;
        Object result = getCompositeEvents(useUserPreferences, userId, untilDate, blackList, pages, spaces, wikis,
            users, toMaxCount(maxCount, 21), displayOwnEvents, displayMinorEvents, displaySystemEvents,
            displayReadEvents, tags, currentWiki, async, asyncId, false, false);

        if (result instanceof String) {
            response = Response.status(Status.ACCEPTED);
            response.entity(Collections.singletonMap("asyncId", result));
        } else {
            // Make sure URLs will be rendered like in any other display (by default REST API forces absolute URLs)
            XWikiContext xcontext = getXWikiContext();
            xcontext.setURLFactory(
                xcontext.getWiki().getURLFactoryService().createURLFactory(XWikiContext.MODE_SERVLET, xcontext));

            Notifications notifications = new Notifications(this.notificationsRenderer
                .renderNotifications((List<CompositeEvent>) result, userId, TRUE.equals(displayReadStatus)));

            response = Response.ok(notifications);
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

    private Object getCompositeEvents(String useUserPreferences, String userId, String untilDate, String blackList,
        String pages, String spaces, String wikis, String users, int maxCount, String displayOwnEvents,
        String displayMinorEvents, String displaySystemEvents, String displayReadEvents, String tags,
        String currentWiki, String async, String asyncId, boolean onlyUnread, boolean count) throws Exception
    {
        Object result = null;

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
            String cacheKey = createCacheKey(useUserPreferences, userId, untilDate, blackList, pages, spaces, wikis,
                users, maxCount, displayOwnEvents, displayMinorEvents, displaySystemEvents, displayReadEvents, tags,
                currentWiki, onlyUnread);

            // 3. Search events
            result = this.executor.submit(cacheKey,
                () -> getCompositeEvents(useUserPreferences, userId, untilDate, blackList, pages, spaces, wikis, users,
                    maxCount, displayOwnEvents, displayMinorEvents, displaySystemEvents, displayReadEvents, tags,
                    currentWiki, onlyUnread),
                Boolean.parseBoolean(async), count);
        }

        return result;
    }

    @Override
    public Response getNotificationsCount(String useUserPreferences, String userId, String pages, String spaces,
        String wikis, String users, String maxCount, String displayOwnEvents, String displayMinorEvents,
        String displaySystemEvents, String displayReadEvents, String displayReadStatus, String tags, String currentWiki,
        String async, String asyncId) throws Exception
    {
        // Build the response
        Response.ResponseBuilder response;
        XWikiUser xWikiUser = getXWikiContext().getWiki().getAuthService().checkAuth(getXWikiContext());
        if (xWikiUser == null) {
            response = Response.status(Status.UNAUTHORIZED);
        } else {
            Object result = getCompositeEvents(useUserPreferences, userId, null, null, pages, spaces, wikis, users,
                toMaxCount(maxCount, 21), displayOwnEvents, displayMinorEvents, displaySystemEvents, displayReadEvents,
                tags, currentWiki, async, asyncId, true, true);

            if (result instanceof String) {
                response = Response.status(Status.ACCEPTED);
                response.entity(Collections.singletonMap("asyncId", result));
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
        XWikiUser xWikiUser = getXWikiContext().getWiki().getAuthService().checkAuth(getXWikiContext());
        DocumentReference userIdDoc = this.documentReferenceResolver.resolve(userId);
        if (xWikiUser == null || !userIdDoc.equals(xWikiUser.getUserReference())) {
            getXWikiContext().getResponse().sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        } else {
            List<CompositeEvent> events =
                (List<CompositeEvent>) getCompositeEvents(useUserPreferences, userId, untilDate, blackList, pages,
                    spaces, wikis, users, toMaxCount(maxCount, 10), displayOwnEvents, displayMinorEvents,
                    displaySystemEvents, displayReadEvents, tags, currentWiki, null, null, false, false);

            SyndFeedOutput output = new SyndFeedOutput();
            return output.outputString(notificationRSSManager.renderFeed(events));
        }
    }

    @Override
    public Response postNotifications() throws Exception
    {
        // We should seriously consider to stop using Restlet, because the @FormParam attribute does not work.
        // See: https://github.com/restlet/restlet-framework-java/issues/1120
        // That's why we need to use this workaround: manually getting the POST params in the request object.
        XWikiRequest request = getXWikiContext().getRequest();
        return getNotifications(request.get("useUserPreferences"), request.get("userId"), request.get("untilDate"),
            request.get("blackList"), request.get("pages"), request.get("spaces"), request.get("wikis"),
            request.get("users"), request.get("count"), request.get("displayOwnEvents"),
            request.get("displayMinorEvents"), request.get("displaySystemEvents"), request.get("displayReadEvents"),
            request.get("displayReadStatus"), request.get("tags"), request.get("currentWiki"), request.get("async"),
            request.get("asyncId"));
    }

    private String createCacheKey(String useUserPreferences, String userId, String untilDate, String blackList,
        String pages, String spaces, String wikis, String users, int maxCount, String displayOwnEvents,
        String displayMinorEvents, String displaySystemEvents, String displayReadEvents, String tags,
        String currentWiki, boolean onlyUnread)
    {
        StringBuilder cacheKeyBuilder = new StringBuilder();

        addCacheKeyElement(cacheKeyBuilder, useUserPreferences);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, userId);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, untilDate);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, blackList);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, pages);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, spaces);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, wikis);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, users);
        cacheKeyBuilder.append('/');
        cacheKeyBuilder.append(maxCount);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, displayOwnEvents);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, displayMinorEvents);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, displaySystemEvents);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, displayReadEvents);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, tags);
        cacheKeyBuilder.append('/');
        addCacheKeyElement(cacheKeyBuilder, currentWiki);
        cacheKeyBuilder.append('/');
        cacheKeyBuilder.append(onlyUnread);

        return cacheKeyBuilder.toString();
    }

    private void addCacheKeyElement(StringBuilder cacheKeyBuilder, String value)
    {
        if (value != null) {
            cacheKeyBuilder.append(value.length());
            cacheKeyBuilder.append(value);
        }
    }

    private List<CompositeEvent> getCompositeEvents(String useUserPreferences, String userId, String untilDate,
        String blackList, String pages, String spaces, String wikis, String users, int maxCount,
        String displayOwnEvents, String displayMinorEvents, String displaySystemEvents, String displayReadEvents,
        String tags, String currentWiki, boolean onlyUnread) throws NotificationException, EventStreamException
    {
        NotificationParameters parameters = new NotificationParameters();
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = maxCount;
        parameters.onlyUnread = onlyUnread;

        if (StringUtils.isNotBlank(userId)) {
            parameters.user = documentReferenceResolver.resolve(userId);
        }
        if (StringUtils.isNotBlank(blackList)) {
            parameters.blackList.addAll(Arrays.asList(blackList.split(FIELD_SEPARATOR)));
        }
        if (StringUtils.isNotBlank(untilDate)) {
            parameters.endDate = new Date(Long.parseLong(untilDate));
        }
        if (TRUE.equals(useUserPreferences)) {
            useUserPreferences(parameters);
        } else {
            dontUseUserPreferences(pages, spaces, wikis, users, parameters, displayOwnEvents, displayMinorEvents,
                displaySystemEvents, displayReadEvents, tags, currentWiki);
        }

        return getCompositeEvents(parameters);
    }

    private void dontUseUserPreferences(String pages, String spaces, String wikis, String users,
        NotificationParameters parameters, String displayOwnEvents, String displayMinorEvents,
        String displaySystemEvents, String displayReadEvents, String tags, String currentWiki)
        throws NotificationException, EventStreamException
    {
        List<String> excludedFilters = new ArrayList<>();
        if (TRUE.equals(displayOwnEvents)) {
            excludedFilters.add(OwnEventFilter.FILTER_NAME);
        }
        if (TRUE.equals(displayMinorEvents)) {
            excludedFilters.add(MinorEventAlertNotificationFilter.FILTER_NAME);
        }
        if (TRUE.equals(displaySystemEvents)) {
            excludedFilters.add(SystemUserNotificationFilter.FILTER_NAME);
        }
        if (TRUE.equals(displayReadEvents)) {
            excludedFilters.add(EventReadAlertFilter.FILTER_NAME);
        }
        parameters.filters = notificationFilterManager.getAllFilters(true).stream()
            .filter(filter -> !excludedFilters.contains(filter.getName())).collect(Collectors.toList());

        enableAllEventTypes(parameters);
        handlePagesParameter(pages, parameters);
        handleSpacesParameter(spaces, parameters);
        handleWikisParameter(wikis, parameters, currentWiki);
        usersParameterHandler.handleUsersParameter(users, parameters);

        handleTagsParameter(parameters, tags, currentWiki);
    }

    private void useUserPreferences(NotificationParameters parameters)
        throws NotificationException, EventStreamException
    {
        if (parameters.user != null) {
            // Check if we should pre or post filter events
            if (parameters.format == NotificationFormat.ALERT && this.configuration.isEventPreFilteringEnabled()) {
                enableAllEventTypes(parameters);

                parameters.filters.add(new ForUserEventFilter(NotificationFormat.ALERT, null));
            } else {
                parameters.preferences =
                    notificationPreferenceManager.getPreferences(parameters.user, true, parameters.format);
                parameters.filters = notificationFilterManager.getAllFilters(parameters.user, true);
                parameters.filterPreferences =
                    notificationFilterPreferenceManager.getFilterPreferences(parameters.user);
            }
        }
    }

    private List<CompositeEvent> getCompositeEvents(NotificationParameters parameters) throws NotificationException
    {
        return this.newNotificationManager.getEvents(parameters);
    }

    private void handlePagesParameter(String pages, NotificationParameters parameters)
    {
        handleLocationParameter(pages, parameters, NotificationFilterProperty.PAGE);
    }

    private void handleSpacesParameter(String spaces, NotificationParameters parameters)
    {
        handleLocationParameter(spaces, parameters, NotificationFilterProperty.SPACE);
    }

    private void handleWikisParameter(String wikis, NotificationParameters parameters, String currentWiki)
    {
        handleLocationParameter(wikis, parameters, NotificationFilterProperty.WIKI);

        // When the notifications are displayed in a macro in a subwiki, we assume they should not contain events from
        // other wikis (except if the "wikis" parameter is set).
        // The concept of the subwiki is to restrict a given domain of interest into a given wiki, this is why it does
        // not make sense to show events from other wikis in a "timeline" such as the notifications macro.
        // TODO: add a "handleAllWikis" parameter to disable this behaviour
        // Note that on the main wiki, which is often a "portal" for all the others wikis, we assure it's OK to display
        // events from other wikis.
        if (StringUtils.isBlank(wikis) && !StringUtils.equals(currentWiki, wikiDescriptorManager.getMainWikiId())) {
            handleLocationParameter(currentWiki, parameters, NotificationFilterProperty.WIKI);
        }
    }

    private void handleLocationParameter(String locations, NotificationParameters parameters,
        NotificationFilterProperty property)
    {
        if (StringUtils.isNotBlank(locations)) {
            Set<NotificationFormat> formats = new HashSet<>();
            formats.add(NotificationFormat.ALERT);

            String[] locationArray = locations.split(FIELD_SEPARATOR);
            for (int i = 0; i < locationArray.length; ++i) {
                DefaultNotificationFilterPreference pref = new DefaultNotificationFilterPreference();
                pref.setId(String.format("%s_%s_%s", ScopeNotificationFilter.FILTER_NAME, property, i));
                pref.setEnabled(true);
                pref.setFilterName(ScopeNotificationFilter.FILTER_NAME);
                pref.setFilterType(NotificationFilterType.INCLUSIVE);
                pref.setNotificationFormats(formats);
                pref.setProviderHint("REST");
                switch (property) {
                    case WIKI:
                        pref.setWiki(locationArray[i]);
                        break;
                    case SPACE:
                        pref.setPage(locationArray[i]);
                        break;
                    case PAGE:
                        pref.setPageOnly(locationArray[i]);
                        break;
                    default:
                        break;
                }
                parameters.filterPreferences.add(new ScopeNotificationFilterPreference(pref, entityReferenceResolver));
            }
        }
    }

    private void enableAllEventTypes(NotificationParameters parameters) throws EventStreamException
    {
        parameters.preferences.clear();
        for (RecordableEventDescriptor descriptor : recordableEventDescriptorManager
            .getRecordableEventDescriptors(true)) {
            parameters.preferences.add(new InternalNotificationPreference(descriptor));
        }
    }

    private void handleTagsParameter(NotificationParameters parameters, String tags, String currentWiki)
    {
        if (StringUtils.isNotBlank(tags)) {
            String[] tagArray = tags.split(",");
            for (int i = 0; i < tagArray.length; ++i) {
                parameters.filterPreferences.add(new TagNotificationFilterPreference(tagArray[i], currentWiki));
            }
        }
    }
}
