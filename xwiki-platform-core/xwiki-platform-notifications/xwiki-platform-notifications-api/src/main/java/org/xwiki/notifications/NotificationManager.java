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

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.Event;
import org.xwiki.stability.Unstable;

/**
 * Get notifications for users.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
@Unstable
public interface NotificationManager
{
    /**
     * @param offset the offset
     * @param limit the number of events to get
     * @return a list of events concerning the current user and to display as notifications
     * @throws NotificationException if error happens
     */
    List<Event> getEvents(int offset, int limit) throws NotificationException;

    /**
     * @param userId id of the user
     * @param offset the offset
     * @param limit the number of events to get
     * @return a list of events concerning a given user and to display as notifications
     * @throws NotificationException if error happens
     */
    List<Event> getEvents(String userId, int offset, int limit) throws NotificationException;

    /**
     * @return the list of notifications preferences for the current user
     * @throws NotificationException if an error happens
     */
    List<NotificationPreference> getPreferences() throws NotificationException;

    /**
     * @param userId id of the user
     * @return the list of notifications preferences for a given user
     * @throws NotificationException if an error happens
     */
    List<NotificationPreference> getPreferences(String userId) throws NotificationException;

    /**
     * Return the number of events to display as notifications concerning the current user.
     *
     * @param onlyUnread either if only unread events should be counted or all events
     * @return the list of events to display as notifications
     * @throws NotificationException if an error happens
     */
    long getEventsCount(boolean onlyUnread) throws NotificationException;

    /**
     * * Return the number of events to display as notifications concerning a given user.
     *
     * @param onlyUnread either if only unread events should be counted or all events
     * @param userId if a user
     * @return the list of events to display as notifications
     * @throws NotificationException if an error happens
     */
    long getEventsCount(boolean onlyUnread, String userId) throws NotificationException;

    /**
     * Set the start date for the given user.
     *
     * @param userId the id of the user
     * @param startDate the date before which we ignore notifications
     * @throws NotificationException if an error happens
     */
    void setStartDate(String userId, Date startDate) throws NotificationException;
}
