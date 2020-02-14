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
package org.xwiki.notifications.notifiers.internal;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.observation.event.Event;

/**
 * Event triggered for for each user associated with a {@link org.xwiki.eventstream.Event}.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the event as a {@link org.xwiki.eventstream.Event}</li>
 * </ul>
 * 
 * @version $Id$
 * @since 12.1RC1
 */
public class UserNotificationEvent implements Event
{
    private DocumentReference userReference;

    private NotificationFormat format;

    /**
     * Matches all users.
     */
    public UserNotificationEvent()
    {

    }

    /**
     * @param userReference the reference of the user listening the event
     */
    public UserNotificationEvent(DocumentReference userReference)
    {
        this.userReference = userReference;
    }

    /**
     * @param userReference the reference of the user listening the event
     * @param format the format of the notification
     */
    public UserNotificationEvent(DocumentReference userReference, NotificationFormat format)
    {
        this.userReference = userReference;
        this.format = format;
    }

    /**
     * @param format the format of the notification
     */
    public UserNotificationEvent(NotificationFormat format)
    {
        this.format = format;
    }

    /**
     * @return the user the reference of the user listening the event
     */
    public DocumentReference getUserReference()
    {
        return this.userReference;
    }

    /**
     * @return the format the format of the notification
     */
    public NotificationFormat getFormat()
    {
        return format;
    }

    @Override
    public boolean matches(Object o)
    {
        if (o instanceof UserNotificationEvent) {
            UserNotificationEvent notificationEvent = (UserNotificationEvent) o;

            return (this.userReference == null || this.userReference.equals(notificationEvent.getUserReference())
                && (this.format == null || this.format == notificationEvent.getFormat()));
        }

        return false;
    }
}
