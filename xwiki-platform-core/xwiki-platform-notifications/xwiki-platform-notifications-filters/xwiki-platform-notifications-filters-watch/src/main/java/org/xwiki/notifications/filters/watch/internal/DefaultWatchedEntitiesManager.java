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
package org.xwiki.notifications.filters.watch.internal;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.compress.utils.Sets;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.user.EventUserFilter;
import org.xwiki.notifications.filters.watch.WatchedEntitiesManager;
import org.xwiki.notifications.filters.watch.WatchedEntityReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Default implementation of {@link WatchedEntitiesManager}.
 *
 * @version $Id$
 * @since 9.8R1
 */
@Component
@Singleton
public class DefaultWatchedEntitiesManager implements WatchedEntitiesManager
{
    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @Override
    public void watchEntity(WatchedEntityReference entity, DocumentReference user) throws NotificationException
    {
        UserReference userReference = this.userReferenceResolver.resolve(user);
        handleEntity(entity, userReference, true);
    }

    @Override
    public boolean watch(WatchedEntityReference entity, UserReference user) throws NotificationException
    {
        return handleEntity(entity, user, true);
    }

    @Override
    public boolean removeWatchFilter(WatchedEntityReference entity, UserReference user) throws NotificationException
    {
        DocumentReference userDocRef = this.userReferenceSerializer.serialize(user);
        Set<String> filterPreferencesIds = new HashSet<>();
        for (NotificationFilterPreference filterPreference
            : notificationFilterPreferenceManager.getFilterPreferences(userDocRef)) {
            if (entity.matchExactly(filterPreference)) {
                filterPreferencesIds.add(filterPreference.getId());
            }
        }

        boolean result = false;
        if (!filterPreferencesIds.isEmpty()) {
            this.notificationFilterPreferenceManager.deleteFilterPreferences(userDocRef, filterPreferencesIds);
            result = true;
        }

        return result;
    }

    @Override
    public boolean block(WatchedEntityReference entity, UserReference user) throws NotificationException
    {
        return handleEntity(entity, user, false);
    }

    @Override
    public void unwatchEntity(WatchedEntityReference entity, DocumentReference user)
            throws NotificationException
    {
        UserReference userReference = this.userReferenceResolver.resolve(user);
        handleEntity(entity, userReference, false);
    }

    @Override
    public Collection<String> getWatchedUsers(DocumentReference user) throws NotificationException
    {
        Collection<NotificationFilterPreference> filterPreferences
            = notificationFilterPreferenceManager.getFilterPreferences(user);

        Set<String> results = new HashSet<>();
        Iterator<NotificationFilterPreference> iterator =
            filterPreferences.stream().filter(
                pref -> pref.isEnabled() && pref.getFilterName().equals(EventUserFilter.FILTER_NAME)
                        && pref.getFilterType() == NotificationFilterType.INCLUSIVE
            ).iterator();
        while (iterator.hasNext()) {
            NotificationFilterPreference preference = iterator.next();
            results.add(preference.getUser());
        }
        return results;
    }

    private boolean handleEntity(WatchedEntityReference entity, UserReference user, boolean shouldBeWatched)
            throws NotificationException
    {
        boolean result = false;
        if (entityIsAlreadyInDesiredState(entity, user, shouldBeWatched)) {
            return result;
        }

        DocumentReference userDocRef = this.userReferenceSerializer.serialize(user);
        Iterator<NotificationFilterPreference> filterPreferences =
                notificationFilterPreferenceManager.getFilterPreferences(userDocRef).iterator();

        boolean matchFound = false;
        // Look if an existing filter match the entity
        while (filterPreferences.hasNext()) {
            NotificationFilterPreference notificationFilterPreference = filterPreferences.next();
            if (entity.matchExactly(notificationFilterPreference)) {
                matchFound = true;
                if (notificationFilterPreference.getFilterType() == NotificationFilterType.INCLUSIVE
                        && notificationFilterPreference.isEnabled() != shouldBeWatched) {
                    enableOrDeleteFilter(shouldBeWatched, notificationFilterPreference,
                        userDocRef);
                } else if (notificationFilterPreference.getFilterType() == NotificationFilterType.EXCLUSIVE
                        && notificationFilterPreference.isEnabled() == shouldBeWatched) {
                    enableOrDeleteFilter(!shouldBeWatched, notificationFilterPreference,
                        userDocRef);
                }
            } else if (shouldDisableFilter(entity, notificationFilterPreference, shouldBeWatched)) {
                // Disable custom filters that might be contradictory.
                notificationFilterPreferenceManager.setFilterPreferenceEnabled(userDocRef,
                        notificationFilterPreference.getId(),
                        false);
            }
        }
        if (!matchFound || !entityIsAlreadyInDesiredState(entity, user, shouldBeWatched)) {
            notificationFilterPreferenceManager.saveFilterPreferences(userDocRef,
                Sets.newHashSet(createFilterPreference(entity, shouldBeWatched)));
            result = true;
        }
        return result;
    }

    private boolean shouldDisableFilter(WatchedEntityReference entity, NotificationFilterPreference filterPreference,
                                        boolean shouldBeWatched)
    {
        if (entity.match(filterPreference) && filterPreference.isEnabled()) {
            if (shouldBeWatched) {
                return filterPreference.getFilterType() == NotificationFilterType.EXCLUSIVE;
            } else {
                return filterPreference.getFilterType() == NotificationFilterType.INCLUSIVE;
            }
        }
        return false;
    }

    private boolean entityIsAlreadyInDesiredState(WatchedEntityReference entity, UserReference user,
            boolean desiredState) throws NotificationException
    {
        // If the notifications are enabled and the entity is already in the desired state, then we have nothing to do
        WatchedEntityReference.WatchedStatus watchedStatus = entity.getWatchedStatus(user);
        return (desiredState && watchedStatus.isWatched()) || (!desiredState && watchedStatus.isBlocked());
    }

    private void enableOrDeleteFilter(boolean enable, NotificationFilterPreference notificationFilterPreference,
        DocumentReference user) throws NotificationException
    {
        if (enable) {
            this.notificationFilterPreferenceManager.setFilterPreferenceEnabled(user,
                notificationFilterPreference.getId(),
                true);
        } else {
            // Delete this filter instead of just disabling it, because we don't want to let remaining
            // filters
            this.notificationFilterPreferenceManager.deleteFilterPreference(user, notificationFilterPreference.getId());
        }
    }

    private NotificationFilterPreference createFilterPreference(WatchedEntityReference entity, boolean shouldBeWatched)
    {
        return shouldBeWatched ? entity.createInclusiveFilterPreference() : entity.createExclusiveFilterPreference();
    }
}
