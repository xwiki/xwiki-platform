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
package org.xwiki.notifications.filters.internal.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * Helper to get user preferences for the {@link EventUserFilter}.
 *
 * @version $Id$
 * @since 9.10RC1
 */
@Component(roles = EventUserFilterPreferencesGetter.class)
@Singleton
public class EventUserFilterPreferencesGetter
{
    @Inject
    private Logger logger;

    /**
     * @param testUser user to test
     * @param filterPreferences the collection of all preferences to take into account
     * @param format the notification format (could be null)
     * @return either or not the user to test is part of the excluded users of the given user
     */
    public boolean isUserExcluded(String testUser, Collection<NotificationFilterPreference> filterPreferences,
            NotificationFormat format)
    {
        return getPreferences(filterPreferences, format, NotificationFilterType.EXCLUSIVE).anyMatch(
            pref -> pref.getProperties(NotificationFilterProperty.USER).contains(testUser)
        );
    }

    /**
     * @param testUser user to test
     * @param filterPreferences the collection of all preferences to take into account
     * @param format the notification format (could be null)
     * @return either or not the user to test is part of the followed users of the given user
     * @since 10.3RC1
     * @since 9.11.5
     */
    public boolean isUsedFollowed(String testUser, Collection<NotificationFilterPreference> filterPreferences,
            NotificationFormat format)
    {
        return getPreferences(filterPreferences, format, NotificationFilterType.INCLUSIVE).anyMatch(
            pref -> pref.getProperties(NotificationFilterProperty.USER).contains(testUser)
        );
    }

    /**
     * @param filterPreferences the collection of all preferences to take into account
     * @param format the notification format (could be null)
     * @return the collection of users followed by the given user
     * @since 10.3RC1
     * @since 9.11.5
     */
    public Collection<String> getFollowedUsers(Collection<NotificationFilterPreference> filterPreferences,
            NotificationFormat format)
    {
        return collect(getPreferences(filterPreferences, format, NotificationFilterType.INCLUSIVE));
    }

    /**
     * @param filterPreferences the collection of all preferences to take into account
     * @param format the notification format (could be null)
     * @return the collection of excluded users by the given user
     */
    public Collection<String> getExcludedUsers(Collection<NotificationFilterPreference> filterPreferences,
            NotificationFormat format)
    {
        return collect(getPreferences(filterPreferences, format, NotificationFilterType.EXCLUSIVE));
    }

    private Collection<String> collect(Stream<NotificationFilterPreference> stream)
    {
        return stream.map(fp -> fp.getProperties(NotificationFilterProperty.USER))
            .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
    }

    private Stream<NotificationFilterPreference> getPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format,
            NotificationFilterType filterType)
    {
        try {
            return filterPreferences.stream().filter(
                pref -> matchFilter(pref)
                    && matchFormat(pref, format)
                    && matchFilterType(pref, filterType)
                    && matchAllEvents(pref)
            );
        } catch (Exception e) {
            logger.warn("Failed to get the list of UserFilter notification preferences.", e);
            return Stream.empty();
        }
    }

    private boolean matchFormat(NotificationFilterPreference filterPreference, NotificationFormat format)
    {
        return format == null || filterPreference.getFilterFormats().contains(format);
    }

    private boolean matchFilter(NotificationFilterPreference pref)
    {
        return pref.isEnabled() && EventUserFilter.FILTER_NAME.equals(pref.getFilterName());
    }

    private boolean matchFilterType(NotificationFilterPreference pref, NotificationFilterType filterType)
    {
        return pref.getFilterType() == filterType;
    }

    /**
     * @param filterPreference a filter preference
     * @return either or not the preference concern all event types
     */
    private boolean matchAllEvents(NotificationFilterPreference filterPreference)
    {
        // When the list of event types concerned by the filter i
        // s empty, we consider that the filter concerns
        // all events.
        return filterPreference.getProperties(NotificationFilterProperty.EVENT_TYPE).isEmpty();
    }
}
