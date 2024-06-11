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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.notifications.NotificationException;

/**
 * Helper to create watched entity references.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Role
public interface WatchedEntityFactory
{
    /**
     * Create a watched location reference.
     * @param location the reference of the location to watch
     * @return the watched location reference
     */
    WatchedLocationReference createWatchedLocationReference(EntityReference location) throws NotificationException;

    /**
     * Create a watched user reference.
     * @param userId the ID of the user to watch
     * @return the watched user reference
     */
    WatchedUserReference createWatchedUserReference(String userId) throws NotificationException;
}
