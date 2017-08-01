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

import java.util.List;
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.stability.Unstable;

/**
 * Enable or disable notifications from the event stream (for customization purpose).
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
@Unstable
public interface NotificationFilter
{
    /**
     * Enable or disable an event in the notification list (post-filter).
     *
     * @param event an event
     * @param user the user interested in the notification
     * @param format format of the notification
     * @return true if the event should be dismiss
     */
    boolean filterEvent(Event event, DocumentReference user, NotificationFormat format);

    /**
     * HQL code to inject in the query to fetch notifications from the event stream, inside an "OR" statement (cannot
     * dismiss everything).
     *
     * @param user the user interested in the notifications
     * @param format format of the notification
     * @param properties the properties of the notification to filter
     * @return the HQL code to inject
     */
    String queryFilterOR(DocumentReference user, NotificationFormat format,
            Map<NotificationProperty, String> properties);

    /**
     * HQL code to inject in the query to fetch notifications from the event stream, inside an "AND" statement (can
     * dismiss everything).
     *
     * @param user the user interested in the notifications
     * @param format format of the notification
     * @param properties the properties of the notification to filter
     * @return the HQL code to inject
     */
    String queryFilterAND(DocumentReference user, NotificationFormat format,
            Map<NotificationProperty, String> properties);

    /**
     * Parameters to add to the query using bindValue().
     *
     * @param user the user interested in the notifications
     * @param format format of the notification
     * @param propertiesList a list of the properties to filter
     * @return the values to bind to the query, mapped by value's name
     */
    Map<String, Object> queryFilterParams(DocumentReference user, NotificationFormat format,
            List<Map<NotificationProperty, String>> propertiesList);
}
