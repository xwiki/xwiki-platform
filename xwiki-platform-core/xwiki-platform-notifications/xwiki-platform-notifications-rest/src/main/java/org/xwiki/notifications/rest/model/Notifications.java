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
package org.xwiki.notifications.rest.model;

import java.util.Collection;

/**
 * Represent a serializable version of a notification list, retro-compatible with the old notification services.
 *
 * @version $Id$
 * @since 10.4RC1
 */
public class Notifications
{
    private Collection<Notification> notifications;

    /**
     * Construct a Notifications.
     * @param notifications list of notifications
     */
    public Notifications(Collection<Notification> notifications)
    {
        this.notifications = notifications;
    }

    /**
     * @return the list of the notifications
     */
    public Collection<Notification> getNotifications()
    {
        return notifications;
    }
}
