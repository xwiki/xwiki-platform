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
package org.xwiki.notifications.filters;

/**
 * Define a list of properties supported by {@link org.xwiki.notifications.filters.expression.PropertyValueNode}.
 * Those properties can be used in order to create specific filters on notifications.
 *
 * @since 9.7RC1
 * @version $Id$
 */
public enum NotificationFilterProperty
{
    /**
     * The notification application.
     */
    APPLICATION,

    /**
     * The notification body.
     */
    BODY,

    /**
     * The type of event that triggered the notification.
     */
    EVENT_TYPE,

    /**
     * Is the document focused by the notification hidden ?
     */
    HIDDEN,

    /**
     * Page targeted by the notification.
     */
    PAGE,

    /**
     * Priority of the event that triggered the notification.
     */
    PRIORITY,

    /**
     * The space targeted by the notification.
     */
    SPACE,

    /**
     * The title of the notification.
     */
    TITLE,

    /**
     * The user targeted by the notification.
     */
    USER,

    /**
     * The wiki in which the notification occurred.
     */
    WIKI
}
