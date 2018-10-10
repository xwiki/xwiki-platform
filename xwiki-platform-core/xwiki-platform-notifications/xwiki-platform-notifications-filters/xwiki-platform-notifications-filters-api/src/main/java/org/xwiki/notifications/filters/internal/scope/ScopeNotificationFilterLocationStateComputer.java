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

    private enum WatchedState {
        WATCHED,
        NOT_WATCHED,
        UNKNOWN
    }

    /**
     * @param filterPreferences the collection of all preferences to take into account
     * @param location a location
     * @return if the location is watched by the user, for the given event type and format
     */
    public boolean isLocationWatched(Collection<NotificationFilterPreference> filterPreferences,
            EntityReference location)
    {
        return isLocationWatched(filterPreferences, location, null, null).isWatched();
    }

    /**
     * @param filterPreferences the collection of all preferences to take into account
     * @param location a location
     * @param eventType an event type (could be null)
     * @param format the notification format (could be null)
     * @return if the location is watched by the user, for the given event type and format
     */
    public WatchedLocationState isLocationWatched(Collection<NotificationFilterPreference> filterPreferences,
            EntityReference location, String eventType, NotificationFormat format)
    {
        // TODO: write a unit test with a complex set of preferences

        ScopeNotificationFilterPreferencesHierarchy preferences
                = preferencesGetter.getScopeFilterPreferences(filterPreferences, eventType, format);

        if (preferences.isEmpty()) {
            // If there is no filter preference, then the location is watched (no filter = we get everything)
            return new WatchedLocationState(true);
        }

        WatchedState state = handleExclusiveFilters(location, preferences);
        if (state != WatchedState.UNKNOWN) {
            return new WatchedLocationState(state == WatchedState.WATCHED);
        }

        Iterator<ScopeNotificationFilterPreference> it = preferences.getInclusiveFiltersThatHasNoParents();
        if (!it.hasNext()) {
            // No inclusive filters ==  we get everything, so the location is watched
            return new WatchedLocationState(true);
        }

        boolean match = false;
        Date startingDate = null;
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            // If the inclusive filter match the event location...
            if (match(pref, location)) {
                // Then it means we watch this location
                match = true;
                if (startingDate == null || startingDate.after(pref.getStartingDate())) {
                    startingDate = pref.getStartingDate();
                }
            }
        }

        // If we are here, we have filter preferences but no one is matching the current event location,
        // so we don't watch this location
        return new WatchedLocationState(match, startingDate);
    }

    private WatchedState handleExclusiveFilters(EntityReference location,
            ScopeNotificationFilterPreferencesHierarchy preferences)
    {
        WatchedState state = WatchedState.UNKNOWN;
        int deepestLevel = 0;

        Iterator<ScopeNotificationFilterPreference> it = preferences.getExclusiveFiltersThatHasNoParents();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            int deepLevel = pref.getScopeReference().size();

            // If the exclusive filter match the event location...
            if (match(pref, location) && deepLevel > deepestLevel) {
                state = WatchedState.NOT_WATCHED;
                deepestLevel = deepLevel;

                // then we watch the location if there is at least an inclusive filter child matching it
                for (ScopeNotificationFilterPreference child : pref.getChildren()) {
                    int childDeepLevel = child.getScopeReference().size();
                    if (match(child, location) && childDeepLevel > deepestLevel) {
                        state = WatchedState.WATCHED;
                        deepestLevel = childDeepLevel;
                    }
                }
            }
        }

        return state;
    }

    private boolean match(ScopeNotificationFilterPreference pref, EntityReference location)
    {
        return location.equals(pref.getScopeReference()) || location.hasParent(pref.getScopeReference());
    }
}
