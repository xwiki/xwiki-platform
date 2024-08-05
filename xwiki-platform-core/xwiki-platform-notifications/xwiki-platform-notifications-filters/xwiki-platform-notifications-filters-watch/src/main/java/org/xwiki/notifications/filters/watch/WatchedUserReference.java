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
package org.xwiki.notifications.filters.watch;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.compress.utils.Sets;
import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterScope;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.user.EventUserFilter;
import org.xwiki.notifications.filters.internal.user.EventUserFilterPreferencesGetter;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;

/**
 * Reference to a user to watch.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public class WatchedUserReference implements WatchedEntityReference
{
    private final String userId;

    private final ComponentManager componentManager;

    private final NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    private final EventUserFilterPreferencesGetter preferencesGetter;
    private final UserReferenceSerializer<DocumentReference> userReferenceSerializer;

    /**
     * Construct a WatchedUserReference.
     * @param userId id of the user to watch.
     * @param componentManager the component manager for loading needed components
     */
    public WatchedUserReference(String userId, ComponentManager componentManager) throws ComponentLookupException
    {
        this.userId = userId;
        this.componentManager = componentManager;
        this.preferencesGetter = this.componentManager.getInstance(EventUserFilterPreferencesGetter.class);
        this.notificationFilterPreferenceManager =
            this.componentManager.getInstance(NotificationFilterPreferenceManager.class);
        this.userReferenceSerializer = componentManager.getInstance(
            new DefaultParameterizedType(null, UserReferenceSerializer.class, DocumentReference.class), "document");
    }

    @Override
    public boolean isWatched(DocumentReference userReference) throws NotificationException
    {
        return preferencesGetter.isUserFollowed(userId,
                notificationFilterPreferenceManager.getFilterPreferences(userReference),
                null);
    }

    @Override
    public WatchedStatus getWatchedStatus(UserReference userReference) throws NotificationException
    {
        DocumentReference userDocReference = this.userReferenceSerializer.serialize(userReference);
        WatchedStatus watchedStatus = WatchedStatus.NOT_SET;
        if (isWatched(userDocReference)) {
            watchedStatus = WatchedStatus.WATCHED_FOR_ALL_EVENTS_AND_FORMATS;
        } else if (preferencesGetter.isUserExcluded(userId,
            notificationFilterPreferenceManager.getFilterPreferences(userDocReference),
            null)) {
            watchedStatus = WatchedStatus.BLOCKED_FOR_ALL_EVENTS_AND_FORMATS;
        }
        return watchedStatus;
    }

    @Override
    public Optional<Pair<EntityReference, WatchedStatus>> getFirstFilteredAncestor(UserReference userReference)
        throws NotificationException
    {
        return Optional.empty();
    }

    @Override
    public boolean matchExactly(NotificationFilterPreference notificationFilterPreference)
    {
        return EventUserFilter.FILTER_NAME.equals(notificationFilterPreference.getFilterName())
                && userId.equals(notificationFilterPreference.getUser())
                && notificationFilterPreference.getNotificationFormats().containsAll(
                        Sets.newHashSet(NotificationFormat.values()))
                && notificationFilterPreference.getEventTypes().isEmpty();
    }

    @Override
    public boolean match(NotificationFilterPreference notificationFilterPreference)
    {
        return EventUserFilter.FILTER_NAME.equals(notificationFilterPreference.getFilterName())
                && userId.equals(notificationFilterPreference.getUser());
    }

    @Override
    public NotificationFilterPreference createInclusiveFilterPreference()
    {
        DefaultNotificationFilterPreference filterPreference = new DefaultNotificationFilterPreference();

        filterPreference.setEnabled(true);
        filterPreference.setFilterType(NotificationFilterType.INCLUSIVE);
        filterPreference.setFilterName(EventUserFilter.FILTER_NAME);
        filterPreference.setNotificationFormats(Sets.newHashSet(NotificationFormat.values()));
        filterPreference.setStartingDate(new Date());
        filterPreference.setEntity(userId);
        filterPreference.setScope(NotificationFilterScope.USER);

        return filterPreference;
    }

    @Override
    public NotificationFilterPreference createExclusiveFilterPreference()
    {
        DefaultNotificationFilterPreference filterPreference = new DefaultNotificationFilterPreference();

        filterPreference.setEnabled(true);
        filterPreference.setFilterType(NotificationFilterType.EXCLUSIVE);
        filterPreference.setFilterName(EventUserFilter.FILTER_NAME);
        filterPreference.setNotificationFormats(Sets.newHashSet(NotificationFormat.values()));
        filterPreference.setStartingDate(new Date());
        filterPreference.setEntity(userId);
        filterPreference.setScope(NotificationFilterScope.USER);

        return filterPreference;
    }
}
