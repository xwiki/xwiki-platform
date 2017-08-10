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
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.stability.Unstable;

/**
 * Role used to provide {@link NotificationFilter} to the {@link NotificationFilterManager}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Role
@Unstable
public interface NotificationFilterProvider
{
    /**
     * Get the priority that the preferences given by this provider should have.
     * This is useful when different notification preferences are in conflict.
     *
     * @return the priority of the provider
     */
    int getProviderPriority();

    /**
     * Get every registered {@link NotificationFilter} for the given user.
     *
     * @param user the user for which to retrieve the notification preferences
     * @return a list of notification filters
     * @throws NotificationException if an error happened
     */
    Set<NotificationFilter> getAllFilters(DocumentReference user) throws NotificationException;

    /**
     * Get every registered {@link NotificationFilter} for the given user and corresponding with the given
     * {@link NotificationPreference}.
     *
     * @param user the user for which to retrieve the notification filters
     * @param preference the preference to use
     * @return a list of notification filters
     * @throws NotificationException if an error happened
     */
    Set<NotificationFilter> getFilters(DocumentReference user, NotificationPreference preference)
            throws NotificationException;

    /**
     * Save a given list of notification filters.
     *
     * @param filters the {@link NotificationFilter} to save
     * @throws NotificationException if an error occurred
     */
    void saveFilters(Collection<NotificationFilter> filters) throws NotificationException;
}
