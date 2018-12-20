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
import java.util.Map;

import org.xwiki.notifications.NotificationFormat;

/**
 * Represent a preference set by a user concerning a notification.
 * As we cannot define a Notification by itself as an object, {@link #getProperties()} can be used to recover a set
 * of attributes that describe the kind of notification on which this {@link NotificationPreference} should apply.
 *
 * Note that a {@link NotificationPreference} should be self-sufficient, therefore, it should contain every
 * information needed about what it does.
 * A preference is defined for a category of event, since a specific date and in a given format.
 *
 * @version $Id$
 * @since 9.2RC1
 */
public interface NotificationPreference
{
    /**
     * @return either or not the notification is enabled for the event type or the application
     */
    boolean isNotificationEnabled();

    /**
     * @return format of the notification
     *
     * @since 9.5RC1
     */
    NotificationFormat getFormat();

    /**
     * @return the date from which notifications that match this preference should be retrieved
     *
     * @since 9.7RC1
     */
    Date getStartDate();

    /**
     * @return the properties of the given notification preference
     *
     * @since 9.7RC1
     */
    Map<NotificationPreferenceProperty, Object> getProperties();

    /**
     * The provider linked to a {@link NotificationPreference} is helpful as it permits the
     * {@link NotificationPreferenceManager} to know where to save a given preference.
     *
     * @return the hint of the provider that the preference comes from. If no provider is defined, returns null.
     *
     * @since 9.7RC1
     */
    String getProviderHint();

    /**
     * @return the category of the {@link NotificationPreference}
     */
    NotificationPreferenceCategory getCategory();
}
