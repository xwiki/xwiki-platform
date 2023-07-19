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
package org.xwiki.notifications.preferences.internal;

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreference;

/**
 * Internal role that make requests to the model and avoid a direct dependency to oldcore.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
public interface NotificationPreferenceModelBridge
{
    /**
     * Return the notifications preferences that are stored as XObjects in the user profile.
     *
     * @param userReference the document reference of a user
     * @return the list of preferences
     *
     * @throws NotificationException if an error occurs
     */
    List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference)
        throws NotificationException;

    /**
     * Return the notifications preferences that are stored as XObjects in the wiki administration.
     *
     * @param wikiReference the wiki where to load data (null means default preferences)
     * @return the list of preferences
     *
     * @throws NotificationException if an error occurs
     */
    List<NotificationPreference> getNotificationsPreferences(WikiReference wikiReference)
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

    /**
     * Retrieve the defined grouping strategy in user preferences for the given target.
     *
     * @param userDocReference the user for which to retrieve the preference
     * @param target the target output for which the grouping strategy will be used (e.g. mail or alert)
     * @return the hint of the {@link org.xwiki.notifications.GroupingEventStrategy} component to use
     * @throws NotificationException in case of problem for loading the preferences
     * @since 15.5RC1
     */
    default String getEventGroupingStrategyHint(DocumentReference userDocReference, String target)
        throws NotificationException
    {
        return "default";
    }
}
