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
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.NotificationPreference;

import com.xpn.xwiki.objects.BaseObjectReference;

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
    List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference)
        throws NotificationException;

    /**
     * Set the start date of every NotificationPreference of the given user to the given startDate.
     *
     * @param userReference the document reference of a user
     * @param startDate the date before which we ignore notifications
     * @throws NotificationException if an error occurs
     */
    void setStartDateForUser(DocumentReference userReference, Date startDate) throws NotificationException;

    /**
     * Get all notification preference scope of the given user.
     *
     * @param user user interested in the notifications
     * @param format format on which the preferences apply
     * @return the list of notification preference scopes.
     * @throws NotificationException if error happens
     *
     * @since 9.5RC1
     */
    List<NotificationPreferenceScope> getNotificationPreferenceScopes(DocumentReference user, NotificationFormat format)
            throws NotificationException;

    /**
     * Save an object's property in an hidden document.
     *
     * @param objectReference reference of the object to save
     * @param property the name of the property to set
     * @param value the value of the property to set
     * @throws NotificationException if error happens
     *
     * @since 9.5RC1
     */
    void savePropertyInHiddenDocument(BaseObjectReference objectReference, String property, Object value)
            throws NotificationException;

    /**
     * Return the URL of the given {@link DocumentReference} for the given action.
     *
     * @param documentReference the reference
     * @param action the request action
     * @param parameters the request parameters
     * @return the URL of the given reference
     *
     * @since 9.6RC1
     */
    String getDocumentURL(DocumentReference documentReference, String action, String parameters);

    /**
     * Save the given {@link NotificationPreference} for the given user. If such notification already exists, it will
     * be updated.
     *
     * @param userReference the user we want to work on
     * @param notificationPreference the notification preference to save
     * @throws NotificationException if error happens
     *
     * @since 9.7RC1
     */
    void saveNotificationPreference(DocumentReference userReference, NotificationPreference notificationPreference)
        throws NotificationException;
}
