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

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.user.UserReference;

/**
 * Manage the watched entities.
 * We call `Watched Entities` the locations or the users for that we receive all events.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Role
public interface WatchedEntitiesManager
{
    /**
     * Add a filter to watch the specified entity.
     *
     * @param entity the entity to watch
     * @param user user that will watch the entity
     * @throws NotificationException if an error happens
     */
    @Deprecated
    void watchEntity(WatchedEntityReference entity, DocumentReference user) throws NotificationException;

    boolean watch(WatchedEntityReference entity, UserReference user) throws NotificationException;
    boolean removeWatchFilter(WatchedEntityReference entity, UserReference user) throws NotificationException;
    boolean block(WatchedEntityReference entity, UserReference user) throws NotificationException;

    /**
     * Remove a filter to stop watching the specified entity.
     *
     * @param entity the entity to watch
     * @param user user that will watch the entity
     * @throws NotificationException if an error happens
     */
    @Deprecated
    void unwatchEntity(WatchedEntityReference entity, DocumentReference user) throws NotificationException;

    /**
     * @param user user for who we want to know the watched users
     * @return the users watched by the given user
     * @throws NotificationException if an error happens
     * @since 10.4RC1
     */
    Collection<String> getWatchedUsers(DocumentReference user) throws NotificationException;
}
