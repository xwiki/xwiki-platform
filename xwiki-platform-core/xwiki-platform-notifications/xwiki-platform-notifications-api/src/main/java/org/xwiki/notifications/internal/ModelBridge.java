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
package org.xwiki.notifications.internal;

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationPreference;
import org.xwiki.notifications.page.PageNotificationEventDescriptor;

/**
 * Internal role that make requests to the model and avoid a direct dependency to oldcore.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
public interface ModelBridge
{
    /**
     * Return the notifications preferences of the given user.
     *
     * @param userReference the document reference of a user
     * @return the list of preferences
     *
     * @throws NotificationException if an error occurs
     */
    List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference) throws
            NotificationException;

    /**
     * @param userReference the document reference of a user
     * @return the date before which we ignore notifications
     * @throws NotificationException if an error occurs
     */
    Date getUserStartDate(DocumentReference userReference) throws NotificationException;

    /**
     * @param userReference the document reference of a user
     * @param startDate the date before which we ignore notifications
     * @throws NotificationException if an error occurs
     */
    void setStartDateForUser(DocumentReference userReference, Date startDate) throws NotificationException;

    /**
     * Return the notification event of a given document.
     *
     * @param documentReference the document reference
     * @return the list of applied notifications events
     * @throws NotificationException if an error occurs
     */
    PageNotificationEventDescriptor getPageNotificationEventDescriptor(DocumentReference documentReference)
            throws NotificationException;
}
