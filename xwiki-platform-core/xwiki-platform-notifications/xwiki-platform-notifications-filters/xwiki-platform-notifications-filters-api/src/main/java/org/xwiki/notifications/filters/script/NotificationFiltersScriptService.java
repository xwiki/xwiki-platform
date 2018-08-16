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
package org.xwiki.notifications.filters.script;

import java.util.Collection;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.rendering.block.Block;
import org.xwiki.script.service.ScriptService;

/**
 * Script service for the notification filters.
 *
 * @since 9.7RC1
 * @version $Id$
 */
@Component
@Named("notification.filters")
@Singleton
public class NotificationFiltersScriptService implements ScriptService
{
    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Get a set of notification filters that can be toggled by the current user.
     *
     * @return a set of notification filters that are toggleable
     * @throws NotificationException if an error occurs
     */
    public Set<NotificationFilter> getToggleableNotificationFilters() throws NotificationException
    {
        return notificationFilterManager.getToggleableFilters(
                notificationFilterManager.getAllFilters(documentAccessBridge.getCurrentUserReference(), false)).collect(
                Collectors.toSet());
    }

    /**
     * @return a collection of every {@link NotificationFilter} available to the current user.
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    public Collection<NotificationFilter> getFilters() throws NotificationException
    {
        return notificationFilterManager.getAllFilters(documentAccessBridge.getCurrentUserReference(), false);
    }

    /**
     * Get a collection of notification filters preferences that are available for the current user and that corresponds
     * to the given filter.
     *
     * @param filter the filter associated to the preferences
     * @return a set of {@link NotificationFilterPreference}
     * @throws NotificationException if an error occurs
     *
     * @since 9.8RC1
     */
    public Set<NotificationFilterPreference> getFilterPreferences(NotificationFilter filter)
            throws NotificationException
    {
        return notificationFilterManager.getFilterPreferences(
                notificationFilterManager.getFilterPreferences(documentAccessBridge.getCurrentUserReference()),
                filter
        ).collect(Collectors.toSet());
    }

    /**
     * Get a displayable form of the given {@link NotificationFilterPreference}.
     *
     * @param filter the filter bound to the given preference
     * @param preference the filter preference to display
     * @return a {@link Block} that can be used to display the given notification filter
     * @throws NotificationException if an error occurs
     *
     * @since 9.8RC1
     */
    public Block displayFilterPreference(NotificationFilter filter, NotificationFilterPreference preference)
            throws NotificationException
    {
        return notificationFilterManager.displayFilter(filter, preference);
    }

    /**
     * Delete a filter preference.
     * @param filterPreferenceId name of the filter preference
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    public void deleteFilterPreference(String filterPreferenceId) throws NotificationException
    {

        notificationFilterManager.deleteFilterPreference(filterPreferenceId);
    }

    /**
     * Enable or disable a filter preference.
     * @param filterPreferenceId id of the filter preference
     * @param enabled either or not the filter preference should be enabled
     * @throws NotificationException if an error happens
     *
     * @since 9.8RC1
     */
    public void setFilterPreferenceEnabled(String filterPreferenceId, boolean enabled) throws NotificationException
    {
        notificationFilterManager.setFilterPreferenceEnabled(filterPreferenceId, enabled);
    }

    /**
     * Update the start date for every filter preference that current user has.
     *
     * @param startDate the new start date
     * @throws NotificationException if an error occurs
     *
     * @since 10.5RC1
     * @since 10.4
     * @since 9.11.5
     */
    public void setStartDate(Date startDate) throws NotificationException
    {
        notificationFilterManager.setStartDateForUser(documentAccessBridge.getCurrentUserReference(), startDate);
    }
}
