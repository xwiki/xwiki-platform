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

import java.util.Collections;
import java.util.Date;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.stability.Unstable;

/**
 * Provide {@link NotificationFilterPreference} from multiple sources.
 *
 * @version $Id$
 * @since 9.8RC1
 * @deprecated this interface is not used anywhere anymore in XWiki Standard.
 */
@Role
@Deprecated(since = "16.5.0RC1")
public interface NotificationFilterPreferenceProvider
{
    /**
     * Get every registered {@link NotificationFilterPreference} for the given user.
     *
     * @param user the user for which to retrieve the notification preferences
     * @return a list of notification filter preferences
     * @throws NotificationException if an error happened
     */
    Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException;

    /**
     * Get every registered {@link NotificationFilterPreference} for the given user.
     *
     * @param wikiReference the wiki for which to retrieve the notification preferences
     * @return a list of notification filter preferences
     * @throws NotificationException if an error happened
     * @since 13.3RC1
     */
    default Set<NotificationFilterPreference> getFilterPreferences(WikiReference wikiReference)
        throws NotificationException
    {
        return Collections.emptySet();
    }

    /**
     * Save a given set of filter preferences.
     *
     * @param user the user for which to save the notification preferences
     * @param filterPreferences the {@link NotificationFilterPreference} to save
     * @throws NotificationException if an error occurred
     *
     * @since 10.11RC1
     * @since 10.8.3
     * @since 9.11.9
     */
    void saveFilterPreferences(DocumentReference user, Set<NotificationFilterPreference> filterPreferences)
        throws NotificationException;

    /**
     * Delete a filter preference.
     * @param user the user for which to delete the notification preferences
     * @param filterPreferenceId id of the filter preference
     * @throws NotificationException if an error happens
     *
     * @since 10.11RC1
     * @since 10.8.3
     * @since 9.11.9
     */
    void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException;

    /**
     * Delete a set of filter preferences.
     * @param user the user for which to delete the notification preferences
     * @param filterPreferenceIds ids of the filter preferences
     * @throws NotificationException if an error happens
     *
     * @since 15.10.2
     * @since 16.0.0RC1
     */
    @Unstable
    default void deleteFilterPreferences(DocumentReference user, Set<String> filterPreferenceIds)
        throws NotificationException
    {
        // Do nothing
    }

    /**
     * Delete a filter preference.
     * @param wikiReference the wiki for which to delete the notification preferences
     * @param filterPreferenceId id of the filter preference
     * @throws NotificationException if an error happens
     *
     * @since 13.3RC1
     */
    default void deleteFilterPreference(WikiReference wikiReference, String filterPreferenceId)
        throws NotificationException
    {
        // do nothing
    }

    /**
     * Enable or disable a filter preference.
     * @param user the user for which to save the notification preferences
     * @param filterPreferenceId id of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     *
     * @since 10.11RC1
     * @since 10.8.3
     * @since 9.11.9
     */
    void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceId, boolean enabled)
        throws NotificationException;

    /**
     * Enable or disable a filter preference.
     * @param wikiReference the wiki for which to save the notification preferences
     * @param filterPreferenceId id of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     *
     * @since 13.3RC1
     */
    default void setFilterPreferenceEnabled(WikiReference wikiReference, String filterPreferenceId, boolean enabled)
        throws NotificationException
    {
        // do nothing
    }

    /**
     * Update the start date for every notification preference that the user has.
     *
     * @param user the user to use
     * @param startDate the new start date
     * @throws NotificationException if an error occurs
     *
     * @since 10.5RC1
     * @since 10.4
     * @since 9.11.5
     */
    void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException;
}