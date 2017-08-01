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
import java.util.List;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;

/**
 * Provide an interface for interacting with user notification preferences.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public interface NotificationPreferenceManager
{
    /**
     * Get a list of registered {@link NotificationPreference} for the given user.
     *
     * @param user the user to use
     * @return every {@link NotificationPreference} linked to this user
     * @throws NotificationException if an error occurs
     */
    List<NotificationPreference> getNotificationsPreferences(DocumentReference user) throws NotificationException;

    /**
     * Update the start date for every notification preference that the user has.
     *
     * @param user the user to use
     * @param startDate the new start date
     * @throws NotificationException if an error occurs
     */
    void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException;

    /**
     * Save the given {@link NotificationPreference} for the given user. If such notification already exists, it will
     * be updated.
     *
     * @param userReference the user we want to work on
     * @param notificationPreferences the list of notification preference to save
     * @throws NotificationException if error happens
     *
     * @since 9.7RC1
     */
    void saveNotificationsPreferences(DocumentReference userReference,
            List<NotificationPreference> notificationPreferences) throws NotificationException;
}
