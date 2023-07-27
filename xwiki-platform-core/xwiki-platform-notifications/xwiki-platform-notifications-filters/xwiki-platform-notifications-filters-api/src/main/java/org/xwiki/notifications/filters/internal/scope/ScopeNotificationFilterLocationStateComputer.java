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

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;

/**
 * Helper to know if a given location is "watched" according to the ScopeNotificationFilter.
 *
 * @version $Id$
 * @since 9.9RC1
 */
@Component(roles = ScopeNotificationFilterLocationStateComputer.class)
@Singleton
public class ScopeNotificationFilterLocationStateComputer
{
    @Inject
    private ScopeNotificationFilterPreferencesGetter preferencesGetter;

    private enum WatchedState
    {
        WATCHED,
        NOT_WATCHED,
        UNKNOWN
    }

    /**
     * @param filterPreferences the collection of all preferences to take into account
     * @param location a location
     * @return {@code true} if the location is watched by the user, for any kind of event types or format.
     */
    public boolean isLocationWatched(Collection<NotificationFilterPreference> filterPreferences,
        EntityReference location)
    {
        return isLocationWatched(filterPreferences, location, null, null, false, true, false).isWatched();
    }

    /**
     * @param filterPreferences the collection of all preferences to take into account
     * @param location a location
     * @return {@code true} if the location is watched by the user, for all formats and for all events only.
     */
    public boolean isLocationWatchedWithAllEventTypes(Collection<NotificationFilterPreference> filterPreferences,
        EntityReference location)
    {
        return isLocationWatched(filterPreferences, location, null, null, true, true, true).isWatched();
    }

    /**
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
     *      *                 it's {@code null}.
     * @param location the location to check
     * @param checkInclusiveFilters if {@code true} both inclusive and exclusive filters are considered.
     *                              if {@code false} only exclusive filters are considered.
     * @return if the location is watched by the user, for the given event type and format
     */
    public WatchedLocationState isLocationWatched(Collection<NotificationFilterPreference> filterPreferences,
        EntityReference location, String eventType, NotificationFormat format, boolean onlyGivenType,
        boolean checkInclusiveFilters, boolean onlyGivenFormat)
    {
        // TODO: write a unit test with a complex set of preferences

        ScopeNotificationFilterPreferencesHierarchy preferences = preferencesGetter
                .getScopeFilterPreferences(filterPreferences, eventType, format, onlyGivenType, onlyGivenFormat);

        if (preferences.isEmpty()) {
            // If there is no filter preference, then nothing is watched.
            return new WatchedLocationState(false);
        }

        WatchedLocationState state = handleExclusiveFilters(location, preferences);
        if (state != null) {
            return state;
        }

        Iterator<ScopeNotificationFilterPreference> it = preferences.getInclusiveFiltersThatHasNoParents();
        if (!checkInclusiveFilters || !it.hasNext()) {
            // No inclusive filters == we get nothing, so it's not watched.
            return new WatchedLocationState(false);
        }

        boolean match = false;
        Date startingDate = null;
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            // If the inclusive filter match the event location...
            if (match(pref, location)) {
                // Then it means we watch this location
                match = true;
                if (startingDate == null
                        || (pref.getStartingDate() != null && startingDate.after(pref.getStartingDate()))) {
                    startingDate = pref.getStartingDate();
                }
            }
        }

        // If we are here, we have filter preferences but no one is matching the current event location,
        // so we don't watch this location
        return new WatchedLocationState(match, startingDate);
    }

    private WatchedLocationState handleExclusiveFilters(EntityReference location,
        ScopeNotificationFilterPreferencesHierarchy preferences)
    {
        WatchedState state = WatchedState.UNKNOWN;
        int deepestLevel = 0;
        Date startingDate = null;

        Iterator<ScopeNotificationFilterPreference> it = preferences.getExclusiveFiltersThatHasNoParents();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            int deepLevel = pref.getScopeReference().size();

            // If the exclusive filter match the event location...
            if (match(pref, location) && deepLevel > deepestLevel) {
                state = WatchedState.NOT_WATCHED;
                startingDate = pref.getStartingDate();
                deepestLevel = deepLevel;

                // then we watch the location if there is at least an inclusive filter child matching it
                for (ScopeNotificationFilterPreference child : pref.getChildren()) {
                    int childDeepLevel = child.getScopeReference().size();
                    if (match(child, location) && childDeepLevel > deepestLevel) {
                        state = WatchedState.WATCHED;
                        deepestLevel = childDeepLevel;
                        startingDate = child.getStartingDate();
                    }
                }
            }
        }

        return state == WatchedState.UNKNOWN ? null
            : new WatchedLocationState(state == WatchedState.WATCHED, startingDate);
    }

    private boolean match(ScopeNotificationFilterPreference pref, EntityReference location)
    {
        return location.equals(pref.getScopeReference()) || location.hasParent(pref.getScopeReference());
    }
}
