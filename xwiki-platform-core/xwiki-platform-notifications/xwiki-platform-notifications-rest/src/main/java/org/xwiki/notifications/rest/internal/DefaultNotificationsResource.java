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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.CompositeEventStatusManager;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.scope.ScopeNotificationFilterPreference;
import org.xwiki.notifications.notifiers.NotificationRenderer;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceManager;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.rest.NotificationsResource;
import org.xwiki.notifications.rest.model.Notification;
import org.xwiki.notifications.rest.model.Notifications;
import org.xwiki.notifications.sources.NewNotificationManager;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rest.XWikiResource;
import org.xwiki.text.StringUtils;

import com.google.common.collect.Sets;

/**
 * @version $Id$
 * @since
 */
@Component
@Named("org.xwiki.notifications.rest.internal.DefaultNotificationsResource")
public class DefaultNotificationsResource extends XWikiResource implements NotificationsResource
{
    @Inject
    private NewNotificationManager newNotificationManager;

    @Inject
    private CompositeEventStatusManager compositeEventStatusManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private NotificationPreferenceManager notificationPreferenceManager;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private NotificationRenderer notificationRenderer;

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
            String filters
        ) throws Exception
    {
        List<Notification> notifications = new ArrayList<>();

        NotificationParameters parameters = new NotificationParameters();
        parameters.format = NotificationFormat.ALERT;
        parameters.expectedCount = 10;

        if (StringUtils.isNotBlank(userId)) {
            parameters.user = documentReferenceResolver.resolve(userId);
        }
        if (StringUtils.isNotBlank(blackList)) {
            parameters.blackList.addAll(Arrays.asList(blackList.split(",")));
        }
        if (StringUtils.isNotBlank(untilDate)) {
            parameters.endDate = DateFormat.getDateInstance().parse(untilDate);
        }
        if ("true".equals(useUserPreferences)) {
            parameters.preferences = notificationPreferenceManager.getPreferences(parameters.user, true,
                    parameters.format);
            parameters.filters = notificationFilterManager.getAllFilters(parameters.user);
            parameters.filterPreferences = notificationFilterManager.getFilterPreferences(parameters.user);
        } else {
            parameters.filters = notificationFilterManager.getAllFilters(true);
            parameters.filterPreferences = new ArrayList<>(parameters.filterPreferences);
            parameters.preferences = new ArrayList<>();
            for (RecordableEventDescriptor descriptor
                    : recordableEventDescriptorManager.getRecordableEventDescriptors(true)) {
                parameters.preferences.add(new NotificationPreference() {
                    @Override public boolean isNotificationEnabled()
                    {
                        return true;
                    }

                    @Override public NotificationFormat getFormat()
                    {
                        return NotificationFormat.ALERT;
                    }

                    @Override public Date getStartDate()
                    {
                        return new Date(0);
                    }

                    @Override public Map<NotificationPreferenceProperty, Object> getProperties()
                    {
                        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
                        properties.put(NotificationPreferenceProperty.EVENT_TYPE, descriptor.getEventType());
                        return properties;
                    }

                    @Override public String getProviderHint()
                    {
                        return "rest";
                    }

                    @Override public NotificationPreferenceCategory getCategory()
                    {
                        return NotificationPreferenceCategory.DEFAULT;
                    }
                });
            }

            if (StringUtils.isNotBlank(pages)) {
                String[] pagesArray = pages.split(",");

                for (int i = 0; i < pagesArray.length; ++i) {
                    DefaultNotificationFilterPreference pref
                            = new DefaultNotificationFilterPreference(ScopeNotificationFilter.FILTER_NAME);
                    pref.setEnabled(true);
                    pref.setFilterType(NotificationFilterType.INCLUSIVE);
                    pref.setNotificationFormats(Sets.newHashSet(NotificationFormat.ALERT));
                    Map<NotificationFilterProperty, List<String>> preferenceProperties = new HashMap<>();
                    List<String> pagesList = new ArrayList<>();
                    pagesList.add(pagesArray[i].trim());
                    preferenceProperties.put(NotificationFilterProperty.PAGE, pagesList);
                    pref.setPreferenceProperties(preferenceProperties);
                    parameters.filterPreferences.add(
                            new ScopeNotificationFilterPreference(pref, entityReferenceResolver));
                }
            }

        }

        List<CompositeEvent> compositeEvents = newNotificationManager.getEvents(parameters);
        for (CompositeEventStatus status
                : compositeEventStatusManager.getCompositeEventStatuses(compositeEvents, userId)) {
            String html = null;
            String exception = null;
            try {
                html = render(status.getCompositeEvent());
            } catch (Exception e) {
                exception = e.toString();
            }

            notifications.add(new Notification(status.getCompositeEvent(), status.getStatus(),
                    html, exception, entityReferenceSerializer));
        }

        return new Notifications(notifications);
    }

    private String render(CompositeEvent compositeEvent) throws Exception
    {
        WikiPrinter printer = new DefaultWikiPrinter();
        BlockRenderer renderer = componentManager.getInstance(BlockRenderer.class, "html/5.0");
        renderer.render(notificationRenderer.render(compositeEvent), printer);
        return printer.toString();
    }
}
