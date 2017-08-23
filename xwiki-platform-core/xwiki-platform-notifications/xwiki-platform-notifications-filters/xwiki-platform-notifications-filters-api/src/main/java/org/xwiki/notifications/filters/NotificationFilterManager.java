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
import org.xwiki.notifications.NotificationFormat;
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
     * Get all notifications filters, from all wikis if the user is global.
     *
     * @param user the user interested in notifications
     * @return a collection of notification filters
     * @throws NotificationException if error happens
     */
    Set<NotificationFilter> getAllFilters(DocumentReference user) throws NotificationException;

    /**
     * Get all the notification filters for the given user that matches the given notification preference.
     *
     * @param user the user to get filters from
     * @param preference the preference that filters should match
     * @return a collection of notification filters
     * @throws NotificationException if error happens
     */
    Set<NotificationFilter> getFilters(DocumentReference user, NotificationPreference preference)
            throws NotificationException;

    /**
     * Get a set of every {@link NotificationFilterPreference} that are available for the current user.
     *
     * @param user the user to get preferences from
     * @return a set of notification filters
     * @throws NotificationException if error happens
     */
    Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException;

    /**
     * Get a set of {@link NotificationFilterPreference} that matches the given filter for the given user.
     *
     * @param user the user to get preferences from
     * @param filter the filter that should match the preferences
     * @return a set of notification filter preferences that corresponds to the given filter and the given user
     * @throws NotificationException if error happens
     */
    Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user, NotificationFilter filter)
            throws NotificationException;

    /**
     * Get a set of {@link NotificationFilterPreference} that matches the given user and also the given filter,
     * filter type and format.
     *
     * @param user the user to get preferences from
     * @param filter the filter that should match the preferences
     * @param filterType the type of the filter
     * @param format the format of the notification that should correspond to the filter
     * @return a set of notification filter preferences
     * @throws NotificationException if error happens
     */
    Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user, NotificationFilter filter,
            NotificationFilterType filterType, NotificationFormat format) throws NotificationException;

    /**
     * Get a set of filters that are toggleable for the given user.
     *
     * @param user the user to get filters from
     * @return a set of toggleable filters
     * @throws NotificationException if error happens
     */
    Set<NotificationFilter> getToggleableFilters(DocumentReference user) throws NotificationException;

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
