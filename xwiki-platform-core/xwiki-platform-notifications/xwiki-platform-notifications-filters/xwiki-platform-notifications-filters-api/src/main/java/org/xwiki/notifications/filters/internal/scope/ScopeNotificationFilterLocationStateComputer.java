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
import java.util.Optional;

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

    // TODO: This should be probably cached info.
    public WatchedLocationState isLocationWatchedWithAllTypesAndFormats(Collection<NotificationFilterPreference>
        filterPreferences, EntityReference location)
    {
        ScopeNotificationFilterPreferencesHierarchy preferencesAllFormatsAndAllEventsTypes = preferencesGetter
            .getScopeFilterPreferences(filterPreferences, null, null, true, true);
        ScopeNotificationFilterPreferencesHierarchy preferencesAnyFormatAnyEventTypes = preferencesGetter
            .getScopeFilterPreferences(filterPreferences, null, null, false, false);

        WatchedLocationState result = new WatchedLocationState();

        if (!filterPreferences.isEmpty()) {
            Optional<WatchedLocationState> optionalWatchedLocationState =
                handleExclusiveFilters(location, preferencesAllFormatsAndAllEventsTypes, true);

            if (optionalWatchedLocationState.isPresent()) {
                result = optionalWatchedLocationState.get();
            } else {
                optionalWatchedLocationState =
                    handleExclusiveFilters(location, preferencesAnyFormatAnyEventTypes, false);
                if (optionalWatchedLocationState.isPresent()) {
                    result = optionalWatchedLocationState.get();
                }
            }

            optionalWatchedLocationState =
                handleInclusiveFilters(location, preferencesAllFormatsAndAllEventsTypes, true);
            if (result.getState() == WatchedLocationState.WatchedState.NOT_SET
                && optionalWatchedLocationState.isPresent()) {
                result = optionalWatchedLocationState.get();
            } else if (result.getState() == WatchedLocationState.WatchedState.NOT_SET) {
                optionalWatchedLocationState =
                    handleInclusiveFilters(location, preferencesAnyFormatAnyEventTypes, false);
                if (optionalWatchedLocationState.isPresent()) {
                    result = optionalWatchedLocationState.get();
                }
            }
        }
        return result;
    }

    public WatchedLocationState isLocationWatched(Collection<NotificationFilterPreference>
        filterPreferences, EntityReference location, String eventType, NotificationFormat format,
        boolean handleInclusiveFilters)
    {
        ScopeNotificationFilterPreferencesHierarchy preferences = preferencesGetter
            .getScopeFilterPreferences(filterPreferences, eventType, format, true, true);

        WatchedLocationState result = new WatchedLocationState();

        if (!filterPreferences.isEmpty()) {
            Optional<WatchedLocationState> optionalWatchedLocationState =
                handleExclusiveFilters(location, preferences, true);

            if (optionalWatchedLocationState.isPresent()) {
                result = optionalWatchedLocationState.get();
            } else if (handleInclusiveFilters) {
                optionalWatchedLocationState =
                    handleInclusiveFilters(location, preferences, true);
                if (optionalWatchedLocationState.isPresent()) {
                    result = optionalWatchedLocationState.get();
                }
            }
        }
        return result;
    }

    private Optional<WatchedLocationState> handleInclusiveFilters(EntityReference location,
        ScopeNotificationFilterPreferencesHierarchy preferencesHierarchy,
        boolean allEventsAllFormats)
    {
        Optional<WatchedLocationState> result = Optional.empty();
        boolean match = false;
        Date startingDate = null;
        Iterator<ScopeNotificationFilterPreference> inclusiveFiltersIterator =
            preferencesHierarchy.getInclusiveFiltersThatHasNoParents();

        while (inclusiveFiltersIterator.hasNext()) {
            ScopeNotificationFilterPreference pref = inclusiveFiltersIterator.next();

            // If the inclusive filter match the event location...
            if (match(pref, location)) {
                // Then it means we watch this location
                match = true;
                if (startingDate == null
                    || (pref.getStartingDate() != null && startingDate.after(pref.getStartingDate())))
                {
                    startingDate = pref.getStartingDate();
                }
            }
        }
        if (match) {
            WatchedLocationState.WatchedState watchedState =
                (allEventsAllFormats) ? WatchedLocationState.WatchedState.WATCHED :
                    WatchedLocationState.WatchedState.CUSTOM;
            result = Optional.of(new WatchedLocationState(watchedState, startingDate));
        }
        return result;
    }
//
//    /**
//     * This method has a specific handling of the {@code eventType} parameter based on {@code onlyGivenType} boolean
//     * value. If this value is {@code false}, then {@code eventType} is only used for filters matching specific types:
//     * filters matching all types are always considered. Moreover in that case if {@code eventType} is {@code null} all
//     * filters are considered. Now if {@code onlyGivenType} is {@code false} and {@code eventType} is not null, only the
//     * filters using the specified type are considered, filters matching all types won't be considered. On the contrary,
//     * if {@code onlyGivenType} is {@code false} and {@code eventType} is null, then only the filters matching all types
//     * are considered.
//     *
//     * @param filterPreferences the collection of all preferences to take into account
//     * @param eventType the type of event to take into account: {@code null} is accepted but have different meaning
//     *                  depending on {@code onlyGivenType}.
//     * @param format the notification format (can be {@code null} in which cases all formats are accepted)
//     * @param onlyGivenType if {@code true} only the given {@code evenType} is taken into account. If {@code false}, the
//     *                      given type and filters matching all types are taken into account.
//     * @param onlyGivenFormat if {@code true} the filter preference should match exactly the format requested even if
//     *      *                 it's {@code null}.
//     * @param location the location to check
//     * @param checkInclusiveFilters if {@code true} both inclusive and exclusive filters are considered.
//     *                              if {@code false} only exclusive filters are considered.
//     * @return if the location is watched by the user, for the given event type and format
//     */
//    public WatchedLocationState isLocationWatched(Collection<NotificationFilterPreference> filterPreferences,
//        EntityReference location, String eventType, NotificationFormat format, boolean onlyGivenType,
//        boolean checkInclusiveFilters, boolean onlyGivenFormat)
//    {
//        ScopeNotificationFilterPreferencesHierarchy preferences = preferencesGetter
//                .getScopeFilterPreferences(filterPreferences, eventType, format, onlyGivenType, onlyGivenFormat);
//
//        if (preferences.isEmpty()) {
//            return new WatchedLocationState();
//        }
//
//        WatchedLocationState state = handleExclusiveFilters(location, preferences);
//        if (state != null) {
//            return state;
//        }
//
//        Iterator<ScopeNotificationFilterPreference> it = preferences.getInclusiveFiltersThatHasNoParents();
//        if (!checkInclusiveFilters || !it.hasNext()) {
//            // No inclusive filters == we get nothing, so it's not watched.
//            return new WatchedLocationState(false);
//        }
//
//        boolean match = false;
//        Date startingDate = null;
//        while (it.hasNext()) {
//            ScopeNotificationFilterPreference pref = it.next();
//
//            // If the inclusive filter match the event location...
//            if (match(pref, location)) {
//                // Then it means we watch this location
//                match = true;
//                if (startingDate == null
//                        || (pref.getStartingDate() != null && startingDate.after(pref.getStartingDate()))) {
//                    startingDate = pref.getStartingDate();
//                }
//            }
//        }
//
//        // If we are here, we have filter preferences but no one is matching the current event location,
//        // so we don't watch this location
//        return new WatchedLocationState(match, startingDate);
//    }

    private Optional<WatchedLocationState> handleExclusiveFilters(EntityReference location,
        ScopeNotificationFilterPreferencesHierarchy preferences, boolean allTypesAndEvents)
    {
        WatchedLocationState.WatchedState state = null;
        int deepestLevel = 0;
        Date startingDate = null;

        Iterator<ScopeNotificationFilterPreference> it = preferences.getExclusiveFiltersThatHasNoParents();
        while (it.hasNext()) {
            ScopeNotificationFilterPreference pref = it.next();

            int deepLevel = pref.getScopeReference().size();

            // If the exclusive filter match the event location...
            if (match(pref, location) && deepLevel > deepestLevel) {
                if (allTypesAndEvents) {
                    state = WatchedLocationState.WatchedState.BLOCKED;
                } else {
                    state = WatchedLocationState.WatchedState.CUSTOM;
                }
                startingDate = pref.getStartingDate();
                deepestLevel = deepLevel;

                // then we watch the location if there is at least an inclusive filter child matching it
                for (ScopeNotificationFilterPreference child : pref.getChildren()) {
                    int childDeepLevel = child.getScopeReference().size();
                    if (match(child, location) && childDeepLevel > deepestLevel) {
                        if (allTypesAndEvents) {
                            state = WatchedLocationState.WatchedState.WATCHED;
                        } else {
                            state = WatchedLocationState.WatchedState.CUSTOM;
                        }
                        deepestLevel = childDeepLevel;
                        startingDate = child.getStartingDate();
                    }
                }
            }
        }

        if (state != null) {
            return Optional.of(new WatchedLocationState(state, startingDate));
        } else {
            return Optional.empty();
        }
    }

    private boolean match(ScopeNotificationFilterPreference pref, EntityReference location)
    {
        return location.equals(pref.getScopeReference()) || location.hasParent(pref.getScopeReference());
    }
}
