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

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Stream;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.stability.Unstable;

/**
 * Provide an interface for interacting with user notification filters preferences.
 *
 * @version $Id$
 * @since 10.9
 */
@Role
@Unstable
public interface NotificationFilterPreferenceManager
{
    /**
     * Get the notification filter preferences of the given user.
     * @param user a reference of the user
     * @return the list of the notification filter preferences of the given user.
     * @throws NotificationException if an error occurs
     */
    Collection<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException;

    /**
     * Get from the given filter preferences the ones that match the given filter.
     * @param filterPreferences a list of filter preferences
     * @param filter a notification filter
     * @return a stream returning the filter preferences that match the given filter
     */
    Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter);

    /**
     * Get from the given filter preferences the ones that match the given filter and the given filter type.
     * @param filterPreferences a list of filter preferences
     * @param filter a notification filter
     * @param filterType a filter type
     * @return a stream returning the filter preferences that match the given filter and filter type
     */
    Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter,
            NotificationFilterType filterType);

    /**
     * Get from the given filter preferences the ones that match the given filter, filter type and format.
     * @param filterPreferences a list of filter preferences
     * @param filter a notification filter
     * @param filterType a filter type
     * @param format a notification format
     * @return a stream returning the filter preferences that match the given filter, filter type and format
     */
    Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter,
            NotificationFilterType filterType, NotificationFormat format);

    /**
     * Save the given set of {@link NotificationFilterPreference} against their respective
     * {@link NotificationFilterPreferenceProvider}.
     *
     * @param notificationFilterPreferences a set of {@link NotificationFilterPreference} to save
     */
    void saveFilterPreferences(Set<NotificationFilterPreference> notificationFilterPreferences);

    /**
     * Delete a filter preference.
     * @param filterPreferenceId id of the filter preference
     * @throws NotificationException if an error happens
     */
    void deleteFilterPreference(String filterPreferenceId) throws NotificationException;

    /**
     * Enable or disable a filter preference.
     * @param filterPreferenceId id of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     */
    void setFilterPreferenceEnabled(String filterPreferenceId, boolean enabled) throws NotificationException;

    /**
     * Update the start date for every filter preference that the user has.
     *
     * @param user the user to use
     * @param startDate the new start date
     * @throws NotificationException if an error occurs
     */
    void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException;
}
