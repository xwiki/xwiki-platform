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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;

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
    private EntityReferenceResolver<String> entityReferenceResolver;

    /**
     * Get all "ScopeNotificationFilterPreference(s)" and transform them to LocationFilter(s).
     * This method has a specific handling of the {@code eventType} parameter based on {@code onlyGivenType} boolean
     * value. If this value is {@code false}, then {@code eventType} is only used for filters matching specific types:
     * filters matching all types are always considered. Moreover in that case if {@code eventType} is {@code null} all
     * filters are considered. Now if {@code onlyGivenType} is {@code false} and {@code eventType} is not null, only the
     * filters using the specified type are considered, filters matching all types won't be considered. On the contrary,
     * if {@code onlyGivenType} is {@code false} and {@code eventType} is null, then only the filters matching all types
     * are considered.
     *
     * @param filterPreferences the collection of all preferences to take into account
     * @param eventType the type of event to take into account: {@code null} is accepted but have different meaning
     *                  depending on {@code onlyGivenType}.
     * @param format the notification format (can be {@code null} in which cases all formats are accepted)
     * @param onlyGivenType if {@code true} only the given {@code evenType} is taken into account. If {@code false}, the
     *                      given type and filters matching all types are taken into account.
     * @param onlyGivenFormat if {@code true} the filter preference should match exactly the format requested even if
     *                        it's {@code null}.
     * @return a hierarchy of scope notification filter preferences
     */
    public ScopeNotificationFilterPreferencesHierarchy getScopeFilterPreferences(
        Collection<NotificationFilterPreference> filterPreferences, String eventType, NotificationFormat format,
        boolean onlyGivenType, boolean onlyGivenFormat)
    {
        List<ScopeNotificationFilterPreference> results = new ArrayList<>();

        // Filter them according to the event type and the filter name
        Stream<NotificationFilterPreference> filterPreferenceStream = filterPreferences.stream().filter(
            pref -> matchAllCriteria(pref, eventType, format, onlyGivenType, onlyGivenFormat)
        );

        Iterator<NotificationFilterPreference> iterator = filterPreferenceStream.iterator();
        while (iterator.hasNext()) {
            results.add(new ScopeNotificationFilterPreference(iterator.next(), entityReferenceResolver));
        }

        return new ScopeNotificationFilterPreferencesHierarchy(results);
    }

    private boolean matchAllCriteria(NotificationFilterPreference pref, String eventType, NotificationFormat format,
        boolean onlyGivenType, boolean onlyGivenFormat)
    {
        if (!matchFilter(pref) || !matchFormat(pref, format, onlyGivenFormat)) {
            return false;
        } else if (!onlyGivenType && !matchAllEvents(pref) && !matchEventType(pref, eventType)) {
            return false;
        } else if (onlyGivenType && eventType == null && !matchAllEvents(pref)) {
            return false;
        } else if (onlyGivenType && eventType != null && !matchEventType(pref, eventType)) {
            return false;
        }
        return true;
    }

    private boolean matchFilter(NotificationFilterPreference pref)
    {
        return pref.isEnabled() && ScopeNotificationFilter.FILTER_NAME.equals(pref.getFilterName());
    }

    private boolean matchFormat(NotificationFilterPreference filterPreference, NotificationFormat format,
                                boolean onlyGivenFormat)
    {
        if (!onlyGivenFormat) {
            return format == null || filterPreference.getNotificationFormats().contains(format);
        } else if (format == null) {
            return filterPreference.getNotificationFormats().isEmpty()
                    || filterPreference.getNotificationFormats().equals(Set.of(NotificationFormat.values()));
        } else {
            return filterPreference.getNotificationFormats().equals(Set.of(format));
        }
    }

    /**
     * @param filterPreference a filter preference
     * @return either or not the preference concern all event types
     */
    private boolean matchAllEvents(NotificationFilterPreference filterPreference)
    {
        // When the list of event types concerned by the filter is empty, we consider that the filter concerns
        // all events.
        return filterPreference.getEventTypes().isEmpty();
    }

    /**
     * @param filterPreference a filter preference
     * @param eventType the event type
     * @return if the filter preference concerns the event of the notification preference
     */
    private boolean matchEventType(NotificationFilterPreference filterPreference, String eventType)
    {
        return eventType == null || filterPreference.getEventTypes().contains(eventType);
    }
}
