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

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.stability.Unstable;

/**
 * Provide {@link NotificationFilterPreference} from multiple sources.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Role
@Unstable
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
     * Save a given set of filter preferences.
     *
     * @param filterPreferences the {@link NotificationFilterPreference} to save
     * @throws NotificationException if an error occurred
     */
    void saveFilterPreferences(Set<NotificationFilterPreference> filterPreferences) throws NotificationException;

    /**
     * Delete a filter preference.
     * @param filterPreferenceName name of the filter preference
     * @throws NotificationException if an error happens
     */
    void deleteFilterPreference(String filterPreferenceName) throws NotificationException;

    /**
     * Enable or disable a filter preference.
     * @param filterPreferenceName name of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    void setFilterPreferenceEnabled(String filterPreferenceName, boolean enabled) throws NotificationException;

    /**
     * Get the priority that the preferences given by this provider should have.
     * This is useful when different notification preferences are in conflict. The higher is the number, the higher
     * is the priority.
     *
     * @return the priority of the provider
     *
     * @since 10.3RC1
     * @since 9.11.5
     */
    default int getProviderPriority() {
        return 1000;
    }
}
