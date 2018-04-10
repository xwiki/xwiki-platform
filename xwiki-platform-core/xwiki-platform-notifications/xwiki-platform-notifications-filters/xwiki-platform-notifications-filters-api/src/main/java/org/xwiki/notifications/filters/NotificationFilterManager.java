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
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.rendering.block.Block;
import org.xwiki.stability.Unstable;

/**
 * Provide an interface for interacting with user notification filters.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Role
@Unstable
public interface NotificationFilterManager
{
    /**
     * Get all notifications filters.
     *
     * @param allWikis include filters from all wikis or only the current one
     * @return a collection of notification filters
     * @throws NotificationException if error happens
     * @since 10.3RC1
     */
    Collection<NotificationFilter> getAllFilters(boolean allWikis) throws NotificationException;

    Collection<NotificationFilter> getAllFilters(DocumentReference user) throws NotificationException;

    Stream<NotificationFilter> getFiltersRelatedToNotificationPreference(Collection<NotificationFilter> filters,
            NotificationPreference preference);

    Collection<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException;

    Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter);

    Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter,
            NotificationFilterType filterType);

    Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter,
            NotificationFilterType filterType, NotificationFormat format);

    Stream<NotificationFilter> getToggleableFilters(Collection<NotificationFilter> filters);

    /**
     * For all toggeable notification filters, get if the filter is enabled regarding the user profile.
     *
     * @param user the user to use
     * @return a map of notification filters with their activation state
     * @throws NotificationException if an error happens
     * @since 10.3RC1
     */
    Map<String, Boolean> getToggeableFilterActivations(DocumentReference user) throws NotificationException;

    /**
     * Goes through every given {@link NotificationFilter}. One of the filters implements
     * {@link ToggleableNotificationFilter}, checks if the given user has disabled this filter. If so, remove the
     * filter from the set.

     * @param filters the filters that should be examined
     * @return a set of filters that are not marked as disabled by the user
     *
     * @since 10.3RC1
     */
    Stream<NotificationFilter> getEnabledFilters(Collection<NotificationFilter> filters,
            Map<String, Boolean> filterActivation);

    /**
     * Save the given set of {@link NotificationFilterPreference} against their respective
     * {@link NotificationFilterPreferenceProvider}.
     *
     * @param notificationFilterPreferences a set of {@link NotificationFilterPreference} to save
     */
    void saveFilterPreferences(Set<NotificationFilterPreference> notificationFilterPreferences);

    /**
     * Render a {@link NotificationFilter} using an associated {@link NotificationFilterPreference}.
     *
     * @param filter the filter to use
     * @param preference the notification preference to use
     * @return a rendered form of the notification filter
     * @throws NotificationException if an error happens
     */
    Block displayFilter(NotificationFilter filter, NotificationFilterPreference preference)
            throws NotificationException;

    /**
     * Delete a filter preference.
     * @param filterPreferenceName name of the filter preference
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
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
}
