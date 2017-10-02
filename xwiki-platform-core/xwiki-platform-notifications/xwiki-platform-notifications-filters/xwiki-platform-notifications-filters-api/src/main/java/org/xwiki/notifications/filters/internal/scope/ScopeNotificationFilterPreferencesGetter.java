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
package org.xwiki.notifications.filters.internal.scope;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;

/**
 * Helper to get all ScopeNotificationFilterPreferencesGetter for a given user.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component(roles = ScopeNotificationFilterPreferencesGetter.class)
@Singleton
public class ScopeNotificationFilterPreferencesGetter
{
    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private EntityReferenceResolver<String> entityReferenceResolver;

    @Inject
    private Logger logger;

    /**
     * Get all "ScopeNotificationFilterPreference(s)" and transform them to LocationFilter(s).
     * @param user the user for who we compute the notifications
     * @param eventType the event type (could be null)
     * @param format the notification format (could be null)
     * @return a hierarchy of scope notification filter preferences
     */
    public ScopeNotificationFilterPreferencesHierarchy getScopeFilterPreferences(DocumentReference user,
            String eventType, NotificationFormat format)
    {
        List<ScopeNotificationFilterPreference> results = new ArrayList<>();

        try {
            // Get every filterPreference linked to the current user
            Set<NotificationFilterPreference> filterPreferences = notificationFilterManager.getFilterPreferences(user);

            // Filter them according to the event type and the filter name
            Stream<NotificationFilterPreference> filterPreferenceStream = filterPreferences.stream().filter(
                pref -> ScopeNotificationFilter.FILTER_NAME.equals(pref.getFilterName())
                        && matchFormat(pref, format)
                        && (matchAllEvents(pref) || matchEventType(pref, eventType))
            );

            Iterator<NotificationFilterPreference> iterator = filterPreferenceStream.iterator();
            while (iterator.hasNext()) {
                results.add(new ScopeNotificationFilterPreference(iterator.next(), entityReferenceResolver));
            }

        } catch (NotificationException e) {
            logger.warn("Failed to compute the list of location filters", e);
        }

        return new ScopeNotificationFilterPreferencesHierarchy(results);
    }

    private boolean matchFormat(NotificationFilterPreference filterPreference, NotificationFormat format)
    {
        return format == null || filterPreference.getFilterFormats().contains(format);
    }

    /**
     * @param filterPreference a filter preference
     * @return either or not the preference concern all event types
     */
    private boolean matchAllEvents(NotificationFilterPreference filterPreference)
    {
        // When the list of event types concerned by the filter is empty, we consider that the filter concerns
        // all events.
        return filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE).isEmpty();
    }

    /**
     * @param filterPreference a filter preference
     * @param eventType the event type
     * @return if the filter preference concerns the event of the notification preference
     */
    private boolean matchEventType(NotificationFilterPreference filterPreference, String eventType)
    {
        return eventType == null
                || filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE).contains(eventType);
    }
}
