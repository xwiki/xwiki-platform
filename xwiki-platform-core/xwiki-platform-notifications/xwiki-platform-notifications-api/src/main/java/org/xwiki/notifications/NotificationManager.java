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
package org.xwiki.notifications;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.Event;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.stability.Unstable;

/**
 * Retrieve notifications for users.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
@Unstable
public interface NotificationManager
{
    /**
     * @return a list of notifications concerning the current user
     * @throws NotificationException if error happens
     */
    List<Event> getEvents(int offset, int limit) throws NotificationException;

    /**
     * @param userId id of the user
     * @return a list of notifications concerning a given user
     * @throws NotificationException if error happens
     */
    List<Event> getEvents(String userId, int offset, int limit) throws NotificationException;

    /**
     * @return the list of notifications preferences for the current user
     * @throws NotificationException
     */
    List<NotificationPreference> getPreferences() throws NotificationException;

    /**
     * @param userId id of the user
     * @return the list of notifications preferences for a given user
     * @throws NotificationException
     */
    List<NotificationPreference> getPreferences(String userId) throws NotificationException;

    XDOM render(Event event) throws NotificationException;
}
