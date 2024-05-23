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

import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Reference to an entity (a location, a user, etc...) to watch.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public interface WatchedEntityReference
{
    /**
     * Define more precisely the watched status given the event types and the formats.
     *
     * @since 15.5RC1
     */
    @Unstable
    enum WatchedStatus
    {
        /**
         * The entity is watched for all events and all formats: the filter doesn't specify any event or format.
         */
        WATCHED_FOR_ALL_EVENTS_AND_FORMATS(true, false),

        /**
         * The entity is watched for all events and all formats through a filter placed on an ancestor.
         * @since 16.5.0RC1
         */
        @Unstable
        WATCHED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS(true, false),

        /**
         * The entity and its children are watched for all events and all formats.
         * @since 16.5.0RC1
         */
        @Unstable
        WATCHED_WITH_CHILDREN_FOR_ALL_EVENTS_AND_FORMATS(true, false),

        /**
         * The entity does not have a watched status: by default it means it's not watched, but it's not blocked.
         *
         * @since 15.9RC1
         */
        NOT_SET(false, false),

        /**
         * The entity is blocked for all events and formats.
         */
        BLOCKED_FOR_ALL_EVENTS_AND_FORMATS(false, true),

        /**
         * The entity is ignored for all events and all formats through a filter placed on an ancestor.
         * @since 16.5.0RC1
         */
        @Unstable
        BLOCKED_BY_ANCESTOR_FOR_ALL_EVENTS_AND_FORMATS(false, true),

        /**
         * The entity and its children are ignored for all events and all formats.
         * @since 16.5.0RC1
         */
        @Unstable
        BLOCKED_WITH_CHILDREN_FOR_ALL_EVENTS_AND_FORMATS(false, true),

        /**
         * There is a filter for the exact location of the entity but it concerns a subset of event types or formats.
         * @since 16.5.0RC1
         */
        @Unstable
        CUSTOM(false, false);

        private final boolean blocked;
        private final boolean watched;

        /**
         * Default constructor.
         *
         * @param watched {@code true} if the status means the page is watched.
         * @param blocked {@code true} if the status means the page is ignored.
         */
        WatchedStatus(boolean watched, boolean blocked)
        {
            this.blocked = blocked;
            this.watched = watched;
        }

        /**
         * @return {@code true} if the status means the page is watched.
         */
        public boolean isWatched()
        {
            return watched;
        }

        /**
         * @return {@code true} if the status means the page is ignored.
         */
        public boolean isBlocked()
        {
            return blocked;
        }
    }

    /**
     * Retrieve the specific watched status of an entity for the given user.
     *
     * @param userReference the user for whom to check if the entity is watched or not
     * @return the specific watched status of the entity by the given user
     * @throws NotificationException in case of errors
     * @since 16.5.0RC1
     */
    @Unstable
    default WatchedStatus getWatchedStatus(UserReference userReference) throws NotificationException
    {
        return WatchedStatus.CUSTOM;
    }

    /**
     * Try to retrieve the first ancestor of the location which have a status whose status is watched or blocked.
     * This method returns an {@link Optional#empty()} if current watch status is already watched or blocked, and if
     * no ancestor can be found. The first matching ancestor reference is returned along with its computed
     * {@link WatchedStatus}.
     * @param userReference the user for whom to check the watched status
     * @return a pair containing the watched entity reference of the matching ancestor and its watched status or
     * {@link Optional#empty()}.
     * @see WatchedStatus#isWatched()
     * @see WatchedStatus#isBlocked()
     * @throws NotificationException in case of problem for computing the status
     * @since 16.5.0RC1
     */
    @Unstable
    default Optional<Pair<EntityReference, WatchedStatus>> getFirstFilteredAncestor(UserReference userReference)
        throws NotificationException
    {
        return Optional.empty();
    }

    /**
     * @param userReference a user
     * @return {@code true} if the given user watch the current entity reference for any event type or format.
     * @throws NotificationException if an error happens
     * @since 9.9RC1
     * @deprecated use {@link #getWatchedStatus(UserReference)}.
     */
    @Deprecated(since = "16.5.0RC1")
    boolean isWatched(DocumentReference userReference) throws NotificationException;

    /**
     * @param userReference a user
     * @return {@code true} if the given user watch the current entity reference for all events.
     * @throws NotificationException if an error happens
     * @since 12.8RC1
     * @deprecated use {@link #getWatchedStatus(UserReference)}.
     */
    @Deprecated(since = "16.5.0RC1")
    default boolean isWatchedWithAllEventTypes(DocumentReference userReference) throws NotificationException
    {
        return isWatched(userReference);
    }

    /**
     * Check if the given filter preference is an exact match: by "exact" we mean here that the preference concerns
     * this exact entity, for all event types, and all formats.
     * For checking if a preference match this entity, for any event type and any format,
     * use {@link #match(NotificationFilterPreference)}.
     *
     * @param notificationFilterPreference a filter preference
     * @return whether the filter preference concerns this exact entity for all event types and all formats
     * @see #match(NotificationFilterPreference)
     */
    boolean matchExactly(NotificationFilterPreference notificationFilterPreference);

    /**
     * Check if the given filter preference concerns this entity, whatever the event types and formats.
     * For a matching for all event types and all formats use {@link #matchExactly(NotificationFilterPreference)}.
     *
     * @param notificationFilterPreference a filter preference
     * @return whether the filter preference concerns this exact entity
     * @since 15.5RC1
     */
    @Unstable
    default boolean match(NotificationFilterPreference notificationFilterPreference)
    {
        return matchExactly(notificationFilterPreference);
    }

    /**
     * Create a notification filter preference to watch this entity.
     * @return a NotificationFilterPreference to save in order to watch this entity
     * @since 9.9RC1
     */
    NotificationFilterPreference createInclusiveFilterPreference();

    /**
     * Create a notification filter preference to watch this entity.
     * @return a NotificationFilterPreference to save in order to watch this entity
     * @since 9.9RC1
     */
    NotificationFilterPreference createExclusiveFilterPreference();
}
