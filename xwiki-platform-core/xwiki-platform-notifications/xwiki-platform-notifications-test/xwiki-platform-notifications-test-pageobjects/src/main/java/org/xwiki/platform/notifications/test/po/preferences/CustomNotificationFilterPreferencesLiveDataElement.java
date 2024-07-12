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

package org.xwiki.platform.notifications.test.po.preferences;

import java.util.List;

import org.xwiki.livedata.test.po.LiveDataElement;
import org.xwiki.platform.notifications.test.po.AbstractNotificationsSettingsPage;
import org.xwiki.platform.notifications.test.po.preferences.filters.CustomNotificationFilterPreference;

/**
 * Page object representing the live data which contains custom filter preferences.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
public class CustomNotificationFilterPreferencesLiveDataElement extends LiveDataElement
{
    private static final String SCOPE_COLUMN = "Scope";
    private static final String LOCATION_COLUMN = "Location";
    private static final String FORMAT_COLUMN = "Formats";
    private static final String EVENT_TYPES_COLUMN = "Events";
    private static final String FILTER_ACTION_COLUMN = "Filter Action";
    private final AbstractNotificationsSettingsPage parentPage;

    /**
     * Default constructor.
     *
     * @param parentPage the page holding the live data.
     */
    public CustomNotificationFilterPreferencesLiveDataElement(AbstractNotificationsSettingsPage parentPage)
    {
        super("notificationCustomFilterPreferencesLiveData");
        this.parentPage = parentPage;
    }

    /**
     * @return the custom filter preferences entries.
     */
    public List<CustomNotificationFilterPreference> getCustomNotificationFilterPreferences()
    {
        return getTableLayout().getRows()
            .stream()
            .map(row -> new CustomNotificationFilterPreference(this.parentPage, row, this.getDriver()))
            .toList();
    }

    /**
     * Filter the scope column.
     * @param scope the value to put in the filter.
     */
    public void filterScope(String scope)
    {
        getTableLayout().filterColumn(SCOPE_COLUMN, scope);
    }

    /**
     * Sort the scope column.
     */
    public void sortScope()
    {
        getTableLayout().sortBy(SCOPE_COLUMN);
    }

    /**
     * Filter the location column.
     * @param location the value to put in the filter.
     */
    public void filterLocation(String location)
    {
        getTableLayout().filterColumn(LOCATION_COLUMN, location);
    }

    /**
     * Sort the location column.
     */
    public void sortLocation()
    {
        getTableLayout().sortBy(LOCATION_COLUMN);
    }

    /**
     * Filter the event type column.
     * @param eventType the value to put in the filter.
     */
    public void filterEventType(String eventType)
    {
        getTableLayout().filterColumn(EVENT_TYPES_COLUMN, eventType);
    }

    /**
     * Sort the event type column.
     */
    public void sortEventType()
    {
        getTableLayout().sortBy(EVENT_TYPES_COLUMN);
    }

    /**
     * Filter the filter action column.
     * @param filterAction the value to put in the filter.
     */
    public void filterFilterAction(String filterAction)
    {
        getTableLayout().filterColumn(FILTER_ACTION_COLUMN, filterAction);
    }

    /**
     * Sort the filter action column.
     */
    public void sortFilterAction()
    {
        getTableLayout().sortBy(FILTER_ACTION_COLUMN);
    }

    /**
     * Filter the format column.
     * @param format the value to put in the filter.
     */
    public void filterFormat(String format)
    {
        getTableLayout().filterColumn(FORMAT_COLUMN, format);
    }

    /**
     * Sort the format column.
     */
    public void sortFormats()
    {
        getTableLayout().sortBy(FORMAT_COLUMN);
    }
}
