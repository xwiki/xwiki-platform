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
package org.xwiki.notifications.preferences;

import java.util.Date;

import org.xwiki.notifications.NotificationFormat;
import org.xwiki.stability.Unstable;

/**
 * Represent a preference set by a user concerning a given type of event.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Unstable
public class NotificationPreference
{
    private String eventType;

    private boolean isNotificationEnabled;

    private NotificationFormat format;

    private Date startDate;

    /**
     * Construct a NotificationPreference.
     * @param eventType type of an event
     * @param isNotificationEnabled either or not the notification is enabled for the event type or the application
     */
    public NotificationPreference(String eventType, boolean isNotificationEnabled)
    {
        this.eventType = eventType;
        this.isNotificationEnabled = isNotificationEnabled;
        this.format = NotificationFormat.ALERT;
    }

    /**
     * Construct a NotificationPreference.
     * @param eventType type of an event
     * @param isNotificationEnabled either or not the notification is enabled for the event type or the application
     * @param format format of the notification
     *
     * @since 9.5RC1
     */
    public NotificationPreference(String eventType, boolean isNotificationEnabled, NotificationFormat format)
    {
        this.eventType = eventType;
        this.isNotificationEnabled = isNotificationEnabled;
        this.format = format;
    }

    /**
     * Construct a new NotificationPreference.
     * @param eventType type of an event
     * @param isNotificationEnabled either or not the notification is enabled for the event type or the application
     * @param format format of the notification
     * @param startDate the date from which notifications that match this preference should be retrieved
     *
     * @since 9.7RC1
     */
    public NotificationPreference(String eventType, boolean isNotificationEnabled, NotificationFormat format,
            Date startDate)
    {
        this(eventType, isNotificationEnabled, format);
        this.startDate = startDate;
    }

    /**
     * Construct a new NotificationPreference.
     *
     * @param eventType type of an event
     * @param isNotificationEnabled either or not the notification is enabled for the event type or the application
     * @param format format of the notification
     * @param startDate the date from which notifications that match this preference should be retrieved
     *
     * @since 9.7RC1
     */
    public NotificationPreference(String eventType, boolean isNotificationEnabled, String format, Date startDate)
    {
        this(eventType, isNotificationEnabled, NotificationFormat.valueOf(format.toUpperCase()), startDate);
    }

    /**
     * @return the type of the event concerned by the preference
     */
    public String getEventType()
    {
        return eventType;
    }

    /**
     * @return either or not the notification is enabled for the event type or the application
     */
    public boolean isNotificationEnabled()
    {
        return isNotificationEnabled;
    }

    /**
     * @return format of the notification
     *
     * @since 9.5RC1
     */
    public NotificationFormat getFormat()
    {
        return format;
    }

    /**
     * @return the date from which notifications that match this preference should be retrieved
     *
     * @since 9.7RC1
     */
    public Date getStartDate()
    {
        return this.startDate;
    }
}
