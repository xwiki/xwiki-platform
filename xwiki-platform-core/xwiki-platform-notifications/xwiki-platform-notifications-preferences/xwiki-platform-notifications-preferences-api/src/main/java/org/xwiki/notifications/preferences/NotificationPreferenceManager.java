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

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.stability.Unstable;
import org.xwiki.user.UserReference;

/**
 * Provide an interface for interacting with user notification preferences.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Role
public interface NotificationPreferenceManager
{
    /**
     * Get a list of registered {@link NotificationPreference} for the given user.
     *
     * @param user the user to use
     * @return every {@link NotificationPreference} linked to this user
     * @throws NotificationException if an error occurs
     */
    List<NotificationPreference> getAllPreferences(DocumentReference user) throws NotificationException;

    /**
     * Get a list of registered and/or inherited  {@link NotificationPreference} for the given wiki.
     *
     * @param wiki the wiki to use
     * @return every {@link NotificationPreference} linked to this wiki
     * @throws NotificationException if an error occurs
     *
     * @since 9.11.4
     * @since 10.2
     */
    List<NotificationPreference> getAllPreferences(WikiReference wiki) throws NotificationException;

    /**
     * Get a list of registered {@link NotificationPreference} for the given user.
     *
     * @param user the user to use
     * @param isEnabled should the preference be enabled ?
     * @param format the format of notification described in the preference
     * @return a list of {@link NotificationPreference}
     * @throws NotificationException if an error occurs
     */
    List<NotificationPreference> getPreferences(DocumentReference user, boolean isEnabled,
            NotificationFormat format) throws NotificationException;

    /**
     * Update the start date for every notification preference that the user has.
     *
     * @param user the user to use
     * @param startDate the new start date
     * @throws NotificationException if an error occurs
     */
    void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException;

    /**
     * Save the given {@link NotificationPreference}. If such notification already exists, it will
     * be updated.
     *
     * @param preferences the list of notification preference to save
     * @throws NotificationException if error happens
     *
     * @since 9.7RC1
     */
    void savePreferences(List<NotificationPreference> preferences)
            throws NotificationException;

    /**
     * Retrieve the defined grouping strategy in user preferences for the given target.
     *
     * @param userReference the user for which to retrieve the preference
     * @param target the target output for which the grouping strategy will be used (e.g. mail or alert)
     * @return the hint of the {@link org.xwiki.notifications.GroupingEventStrategy} component to use
     * @throws NotificationException in case of problem for loading the preferences
     * @since 15.4RC1
     */
    @Unstable
    default String getNotificationGroupingStrategy(UserReference userReference, String target)
        throws NotificationException
    {
        return "default";
    }
}
