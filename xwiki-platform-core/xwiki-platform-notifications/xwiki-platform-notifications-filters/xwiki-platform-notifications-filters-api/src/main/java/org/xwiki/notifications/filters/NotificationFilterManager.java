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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.rendering.block.Block;
import org.xwiki.user.UserReference;

/**
 * Provide an interface for interacting with user notification filters.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Role
public interface NotificationFilterManager
{
    /**
     * Get all notifications filters.
     * @param allWikis include filters from all wikis or only the current one
     * @return a collection of notification filters
     * @throws NotificationException if error happens
     * @since 10.4RC1
     */
    Collection<NotificationFilter> getAllFilters(boolean allWikis) throws NotificationException;

    /**
     * Get all notifications filters from the given wikis.
     * @param wikiReference include filters from the given wiki.
     * @return a collection of notification filters
     * @throws NotificationException if error happens
     * @since 13.3RC1
     */
    default Collection<NotificationFilter> getAllFilters(WikiReference wikiReference) throws NotificationException
    {
        return Collections.emptyList();
    }

    /**
     * Get all notifications filters that are enabled to the given user.
     * @param user reference to the user
     * @param onlyEnabled either or not only filters enabled for the user should be collected
     * @return the collection of notification filters enabled to the user.
     * @throws NotificationException if an error happens
     * @since 10.4RC1
     */
    Collection<NotificationFilter> getAllFilters(DocumentReference user, boolean onlyEnabled)
        throws NotificationException;

    /**
     * Get all notifications filters that are enabled to the given user and matching the requested filtering moment.
     *
     * @param user reference to the user
     * @param onlyEnabled either or not only filters enabled for the user should be collected
     * @param filteringPhase when the filter should be used ({@code null} means any phase)
     * @return the collection of notification filters enabled to the user.
     * @throws NotificationException if an error happens
     * @since 12.9RC1
     * @since 12.6.3
     */
    default Collection<NotificationFilter> getAllFilters(DocumentReference user, boolean onlyEnabled,
        NotificationFilter.FilteringPhase filteringPhase)
        throws NotificationException
    {
        return getAllFilters(user, onlyEnabled);
    }

    /**
     * Get from the filters the one that match the given notification preference.
     * @param filters a collection of notification filters
     * @param preference a notification preference
     * @return a stream returning the filters that match the given notification preference.
     * @since 10.4RC1
     */
    Stream<NotificationFilter> getFiltersRelatedToNotificationPreference(Collection<NotificationFilter> filters,
            NotificationPreference preference);

    /**
     * Get from the given filter the one that are toggleable.
     * @param filters a list of notification filters
     * @return a stream returning the toggeable filters that was on the given collection
     * @since 10.4RC1
     */
    Stream<NotificationFilter> getToggleableFilters(Collection<NotificationFilter> filters);

    default List<ToggleableNotificationFilter> getToggleableFilters(UserReference userReference)
        throws NotificationException
    {
        return List.of();
    }

    default List<ToggleableNotificationFilter> getToggleableFilters(WikiReference wikiReference)
        throws NotificationException
    {
        return List.of();
    }

    /**
     * For all toggleable notification filters, get if the filter is enabled regarding the user profile.
     *
     * @param user the user to use
     * @return a map of notification filters with their activation state
     * @throws NotificationException if an error happens
     * @since 10.4RC1
     */
    Map<String, Boolean> getToggeableFilterActivations(DocumentReference user) throws NotificationException;

    /**
     * Goes through every given {@link NotificationFilter}. One of the filters implements
     * {@link ToggleableNotificationFilter}, checks if the given user has disabled this filter. If so, remove the
     * filter from the set.

     * @param filters the filters that should be examined
     * @param filterActivation the map of filters associated to their active status
     * @return a set of filters that are not marked as disabled by the user
     *
     * @since 10.4RC1
     */
    Stream<NotificationFilter> getEnabledFilters(Collection<NotificationFilter> filters,
            Map<String, Boolean> filterActivation);

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
}
