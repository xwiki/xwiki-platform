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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.SystemUserNotificationFilter;
import org.xwiki.notifications.filters.internal.minor.MinorEventAlertNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.status.EventReadAlertFilter;
import org.xwiki.notifications.filters.internal.user.OwnEventFilter;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.rest.NotificationsResource;
import org.xwiki.notifications.rest.model.Notification;
import org.xwiki.notifications.rest.model.Notifications;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.rest.XWikiResource;
import org.xwiki.text.StringUtils;

import com.google.common.collect.Sets;

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
    private InternalNotificationsRenderer notificationsRenderer;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private UsersParameterHandler usersParameterHandler;

    @Override
    public Notifications getNotifications(
            String useUserPreferences,
            String userId,
            String untilDate,
            String blackList,
            String pages,
            String spaces,
            String wikis,
            String users,
            String count,
            String displayOwnEvents,
            String displayMinorEvents,
            String displaySystemEvents,
            String displayReadEvents,
            String displayReadStatus
    ) throws Exception
    {
        NotificationParameters parameters = new NotificationParameters();
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = 10;

        if (StringUtils.isNotBlank(count)) {
            parameters.expectedCount = Integer.parseInt(count);
        }
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
                    displaySystemEvents, displayReadEvents);
        }

        return new Notifications(getAndRenderNotifications(userId, parameters, TRUE.equals(displayReadStatus)));
    }

    private void dontUseUserPreferences(String pages, String spaces, String wikis, String users,
            NotificationParameters parameters, String displayOwnEvents, String displayMinorEvents,
            String displaySystemEvents, String displayReadEvents)
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
        parameters.filters = notificationFilterManager.getAllFilters(true).stream().filter(
                filter -> !excludedFilters.contains(filter.getName())
        ).collect(Collectors.toList());

        parameters.filterPreferences = new ArrayList<>(parameters.filterPreferences);
        enableAllEventTypes(parameters);
        handlePagesParameter(pages, parameters);
        handleSpacesParameter(spaces, parameters);
        handleWikisParameter(wikis, parameters);
        usersParameterHandler.handleUsersParameter(users, parameters);
    }

    private void useUserPreferences(NotificationParameters parameters) throws NotificationException
    {
        parameters.preferences = notificationPreferenceManager.getPreferences(parameters.user, true,
                parameters.format);
        parameters.filters = notificationFilterManager.getAllFilters(parameters.user, true);
        parameters.filterPreferences = notificationFilterManager.getFilterPreferences(parameters.user);
    }

    private List<Notification> getAndRenderNotifications(String userId, NotificationParameters
        parameters, boolean showReadStatus) throws Exception
    {
        List<CompositeEvent> compositeEvents = newNotificationManager.getEvents(parameters);
        return notificationsRenderer.renderNotifications(compositeEvents, userId, showReadStatus);
    }

    private void handlePagesParameter(String pages, NotificationParameters parameters)
    {
        handleLocationParameter(pages, parameters, NotificationFilterProperty.PAGE);
    }

    private void handleSpacesParameter(String spaces, NotificationParameters parameters)
    {
        handleLocationParameter(spaces, parameters, NotificationFilterProperty.SPACE);
    }

    private void handleWikisParameter(String wikis, NotificationParameters parameters)
    {
        handleLocationParameter(wikis, parameters, NotificationFilterProperty.WIKI);
    }

    private void handleLocationParameter(String locations, NotificationParameters parameters,
            NotificationFilterProperty property)
    {
        if (StringUtils.isNotBlank(locations)) {
            String[] locationArray = locations.split(FIELD_SEPARATOR);
            for (int i = 0; i < locationArray.length; ++i) {
                DefaultNotificationFilterPreference pref
                        = new DefaultNotificationFilterPreference(String.format("%s_%s_%s",
                        ScopeNotificationFilter.FILTER_NAME, property, i));
                pref.setEnabled(true);
                pref.setFilterName(ScopeNotificationFilter.FILTER_NAME);
                pref.setFilterType(NotificationFilterType.INCLUSIVE);
                pref.setNotificationFormats(Sets.newHashSet(NotificationFormat.ALERT));
                Map<NotificationFilterProperty, List<String>> preferenceProperties = new HashMap<>();
                List<String> locationList = new ArrayList<>();
                locationList.add(locationArray[i].trim());
                preferenceProperties.put(property, locationList);
                pref.setPreferenceProperties(preferenceProperties);
                parameters.filterPreferences.add(
                        new ScopeNotificationFilterPreference(pref, entityReferenceResolver));
            }
        }
    }

    private void enableAllEventTypes(NotificationParameters parameters) throws EventStreamException
    {
        parameters.preferences = new ArrayList<>();
        for (RecordableEventDescriptor descriptor
                : recordableEventDescriptorManager.getRecordableEventDescriptors(true)) {
            parameters.preferences.add(new InternalNotificationPreference(descriptor));
        }
    }
}
