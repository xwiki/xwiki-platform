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

import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationFormat;

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
        UNKNOW
    }

    /**
     * @param user user for who we display notifications
     * @param location a location
     * @return if the location is watched by the user, for the given event type and format
     */
    public boolean isLocationWatched(DocumentReference user, EntityReference location)
    {
        return isLocationWatched(user, location, null, null);
    }

    /**
     * @param user user for who we display notifications
     * @param location a location
     * @param eventType an event type (could be null)
     * @param format the notification format (could be null)
     * @return if the location is watched by the user, for the given event type and format
     */
    public boolean isLocationWatched(DocumentReference user, EntityReference location,
            String eventType, NotificationFormat format)
    {
        // TODO: write a unit test with a complex set of preferences

        ScopeNotificationFilterPreferencesHierarchy preferences
                = preferencesGetter.getScopeFilterPreferences(user, eventType, format);

        if (preferences.isEmpty()) {
            // If there is no filter preference, then the location is watched (no filter = we get everything)
            return true;
        }

        WatchedState state = handleExclusiveFilters(location, preferences);
        if (state != WatchedState.UNKNOW) {
            return state == WatchedState.WATCHED;
        }

        Iterator<ScopeNotificationFilterPreference> it = preferences.getInclusiveFiltersThatHasNoParents();
        if (!it.hasNext()) {
            // No inclusive filters ==  we get everything, so the location is watched
            return true;
        }

        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            // If the inclusive filter match the event location...
            if (location.equals(pref.getScopeReference()) || location.hasParent(pref.getScopeReference())) {
                // Then it means we watch this location
                return true;
            }
        }

        // If we are here, we have filter preferences but no one is matching the current event location,
        // so we don't watch this location
        return false;
    }

    private WatchedState handleExclusiveFilters(EntityReference location,
            ScopeNotificationFilterPreferencesHierarchy preferences)
    {
        WatchedState state = WatchedState.UNKNOW;
        int maxDeepLevel = 0;

        Iterator<ScopeNotificationFilterPreference> it = preferences.getExclusiveFiltersThatHasNoParents();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            int deepLevel = pref.getScopeReference().size();

            // If the exclusive filter match the event location...
            if ((location.equals(pref.getScopeReference()) || location.hasParent(pref.getScopeReference()))
                    && deepLevel > maxDeepLevel) {
                state = WatchedState.NOT_WATCHED;
                maxDeepLevel = deepLevel;

                // then we watch the location if there is at least an inclusive filter child matching it
                for (ScopeNotificationFilterPreference child : pref.getChildren()) {
                    int childDeepLevel = child.getScopeReference().size();
                    if ((location.equals(child.getScopeReference()) || location.hasParent(child.getScopeReference()))
                            && childDeepLevel > maxDeepLevel)
                    {
                        state = WatchedState.WATCHED;
                        maxDeepLevel = childDeepLevel;
                    }
                }
            }
        }

        return state;
    }
}
