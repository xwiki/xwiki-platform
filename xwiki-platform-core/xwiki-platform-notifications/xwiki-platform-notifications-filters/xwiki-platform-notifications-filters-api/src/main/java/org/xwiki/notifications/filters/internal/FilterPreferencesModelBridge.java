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
package org.xwiki.notifications.filters.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * Internal role that make requests to the model and avoid a direct dependency to oldcore.
 *
 * @version $Id$
 * @since 9.2RC1
 */
@Role
public interface FilterPreferencesModelBridge
{
    /**
     * Get all the notification preferences that corresponds to the given user.
     *
     * @param user the user from which we need to extract the preference
     * @return a set of available filter preferences
     * @throws NotificationException if an error happens
     */
    Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException;

    /**
     * Get all the notification preferences that corresponds to the given user.
     *
     * @param wikiReference the wiki from which we need to extract the preference
     * @return a set of available filter preferences
     * @throws NotificationException if an error happens
     * @since 13.3RC1
     */
    default Set<NotificationFilterPreference> getFilterPreferences(WikiReference wikiReference)
        throws NotificationException
    {
        return Collections.emptySet();
    }

    /**
     * For all toggeable notification filters, get if the filter is enabled regarding the user profile.
     *
     * @param user the user to use
     * @return a map of notification filters with their activation state
     * @throws NotificationException if an error happens
     * @since 10.1RC1
     */
    Map<String, ToggleableNotificationFilterActivation> getToggleableFilterActivations(DocumentReference user)
        throws NotificationException;

    /**
     * Delete a filter preference.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferenceId name of the filter preference
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException;

    /**
     * Delete a set of filter preferences.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferenceIds the names of the filter preferences
     * @throws NotificationException if an error happens
     *
     * @since 15.10.2
     * @since 16.0.0RC1
     */
    void deleteFilterPreferences(DocumentReference user, Set<String> filterPreferenceIds) throws NotificationException;

    /**
     * Delete all the filter preferences related to a given user.
     *
     * @param user the document reference of a wiki user
     * @throws NotificationException in case of error when deleting the filter preferences
     * @since 14.5
     * @since 14.4.1
     * @since 13.10.7
     */
    default void deleteFilterPreferences(DocumentReference user) throws NotificationException
    {
        throw new UnsupportedOperationException(
            "ModelBridge.deleteFilterPreferences(DocumentReference) Not implemented");
    }

    /**
     * Delete a filter preference.
     * @param wikiReference reference of the wiki concerned by the filter preference
     * @param filterPreferenceId name of the filter preference
     * @throws NotificationException if an error happens
     *
     * @since 13.3RC1
     */
    void deleteFilterPreference(WikiReference wikiReference, String filterPreferenceId) throws NotificationException;

    /**
     * Delete all the filter preferences of a wiki.
     *
     * @param wikiReference a wiki reference
     * @throws NotificationException in case of error while deleting the preferences
     * @since 14.5
     * @since 14.4.1
     * @since 13.10.7
     */
    default void deleteFilterPreferences(WikiReference wikiReference) throws NotificationException
    {
        throw new UnsupportedOperationException("ModelBridge.deleteFilterPreference(WikiReference) Not implemented");
    }

    /**
     * Enable or disable a filter preference.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferenceId name of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceId, boolean enabled)
            throws NotificationException;

    /**
     * Enable or disable a filter preference.
     * @param wikiReference reference of the wiki concerned by the filter preference
     * @param filterPreferenceId name of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     *
     * @since 13.3RC1
     */
    void setFilterPreferenceEnabled(WikiReference wikiReference, String filterPreferenceId, boolean enabled)
        throws NotificationException;

    /**
     * Save a collection of NotificationFilterPreferences.
     * @param user reference of the user concerned by the filter preference
     * @param filterPreferences a list of NotificationFilterPreference
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    void saveFilterPreferences(DocumentReference user, Collection<NotificationFilterPreference> filterPreferences)
        throws NotificationException;

    /**
     * Save a collection of NotificationFilterPreferences.
     * @param wikiReference reference of the wiki concerned by the filter preference
     * @param filterPreferences a list of NotificationFilterPreference
     * @throws NotificationException if an error happens
     *
     * @since 13.3RC1
     */
    void saveFilterPreferences(WikiReference wikiReference, Collection<NotificationFilterPreference> filterPreferences)
        throws NotificationException;

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

    /**
     * Create a scope notification filter preference for the current user.
     *
     * @param user the user for who the preference will be created
     * @param type type of the filter preference to create
     * @param formats formats concerned by the preference
     * @param eventTypes the event types concerned by the preference
     * @param reference the reference of the wiki, the space or the page concerned by the preference
     * @throws NotificationException if an error occurs
     *
     * @since 10.8RC1
     * @since 9.11.8
     */
    void createScopeFilterPreference(DocumentReference user, NotificationFilterType type,
            Set<NotificationFormat> formats, List<String> eventTypes, EntityReference reference)
            throws NotificationException;

    /**
     * Create a scope notification filter preference for the current user.
     *
     * @param wikiReference the wiki for which the preference will be created
     * @param type type of the filter preference to create
     * @param formats formats concerned by the preference
     * @param eventTypes the event types concerned by the preference
     * @param reference the reference of the wiki, the space or the page concerned by the preference
     * @throws NotificationException if an error occurs
     *
     * @since 13.3RC1
     */
    void createScopeFilterPreference(WikiReference wikiReference, NotificationFilterType type,
        Set<NotificationFormat> formats, List<String> eventTypes, EntityReference reference)
        throws NotificationException;
}
