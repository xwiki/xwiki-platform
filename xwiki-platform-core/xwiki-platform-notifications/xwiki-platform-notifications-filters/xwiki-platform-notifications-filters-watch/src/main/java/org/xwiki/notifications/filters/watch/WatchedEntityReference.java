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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.stability.Unstable;

/**
 * Reference to an entity (a location, a user, etc...) to watch.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Unstable
public interface WatchedEntityReference
{
    /**
     * @param userReference a user
     * @return if the given user watch the current entity reference
     * @since 9.9RC1
     */
    boolean isWatched(DocumentReference userReference) throws NotificationException;

    /**
     * @param notificationFilterPreference a filter preference
     * @return either or not the filter preference concerns this exact entity
     */
    boolean matchExactly(NotificationFilterPreference notificationFilterPreference);

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
