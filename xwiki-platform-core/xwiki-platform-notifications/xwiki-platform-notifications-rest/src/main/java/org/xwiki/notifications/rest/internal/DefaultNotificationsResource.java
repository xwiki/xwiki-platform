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

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.rest.NotificationsResource;
import org.xwiki.notifications.rest.model.Notification;
import org.xwiki.notifications.rest.model.Notifications;
import org.xwiki.notifications.sources.ParametrizedNotificationManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.rest.XWikiResource;
import org.xwiki.text.StringUtils;

import com.google.common.collect.Sets;

/**
 * Default implementation of {@link NotificationsResource}.
 * @version $Id$
 * @since 10.4RC1
 */
@Component
@Named("org.xwiki.notifications.rest.internal.DefaultNotificationsResource")
public class DefaultNotificationsResource extends XWikiResource implements NotificationsResource
{
    private static final String FIELD_SEPARATOR = ",";

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

    @Override
    public Notifications getNotifications(
            String useUserPreferences,
            String userId,
            String untilDate,
            String blackList,
            String pages,
            String spaces,
            String wikis,
            String filters,
            String count
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
            parameters.endDate = DateFormat.getDateInstance().parse(untilDate);
        }
        if ("true".equals(useUserPreferences)) {
            useUserPreferences(parameters);
        } else {
            dontUseUserPreferences(pages, spaces, wikis, parameters);
        }

        return new Notifications(getAndRenderNotifications(userId, parameters));
    }

    private void dontUseUserPreferences(String pages, String spaces, String wikis, NotificationParameters parameters)
            throws NotificationException, EventStreamException
    {
        parameters.filters = notificationFilterManager.getAllFilters(true);
        parameters.filterPreferences = new ArrayList<>(parameters.filterPreferences);
        enableAllEventTypes(parameters);
        handlePagesParameter(pages, parameters);
        handleSpacesParameter(spaces, parameters);
        handleWikisParameter(wikis, parameters);
    }

    private void useUserPreferences(NotificationParameters parameters) throws NotificationException
    {
        parameters.preferences = notificationPreferenceManager.getPreferences(parameters.user, true,
                parameters.format);
        parameters.filters = notificationFilterManager.getAllFilters(parameters.user);
        parameters.filterPreferences = notificationFilterManager.getFilterPreferences(parameters.user);
    }

    private List<Notification> getAndRenderNotifications(String userId, NotificationParameters parameters)
        throws Exception
    {
        List<CompositeEvent> compositeEvents = newNotificationManager.getEvents(parameters);
        return notificationsRenderer.renderNotifications(compositeEvents, userId);
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
                        = new DefaultNotificationFilterPreference(ScopeNotificationFilter.FILTER_NAME);
                pref.setEnabled(true);
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
