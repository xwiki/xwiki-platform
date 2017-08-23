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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * This is the default implementation of {@link NotificationFilterPreference}.
 *
 * @version $Id$
 * @since 9.8RC1
 */
public class DefaultNotificationFilterPreference implements NotificationFilterPreference
{
    private boolean isEnabled;

    private String filterName;

    private String filterPreferenceName;

    private NotificationFilterType filterType;

    private Set<NotificationFormat> notificationFormats;

    private Map<NotificationFilterProperty, List<String>> preferenceProperties;

    /**
     * Constructs a new {@link DefaultNotificationFilterPreference}.
     *
     * @param filterPreferenceName the name of the filter preference
     * @param filterName the name of the filter corresponding to the given filter
     * @param isEnabled determine if the current filter preference is enabled
     * @param filterType the type of the filter
     * @param notificationFormats a set of formats used by the filter
     * @param preferenceProperties the properties of the filter preference
     */
    public DefaultNotificationFilterPreference(String filterPreferenceName, String filterName, boolean isEnabled,
            NotificationFilterType filterType, Set<NotificationFormat> notificationFormats,
            Map<NotificationFilterProperty, List<String>> preferenceProperties)
    {
        this.filterPreferenceName = filterPreferenceName;
        this.filterName = filterName;
        this.isEnabled = isEnabled;
        this.filterType = filterType;
        this.notificationFormats = notificationFormats;
        this.preferenceProperties = preferenceProperties;
    }

    @Override
    public String getFilterPreferenceName()
    {
        return filterPreferenceName;
    }

    @Override
    public String getFilterName()
    {
        return filterName;
    }

    @Override
    public boolean isEnabled()
    {
        return isEnabled;
    }

    @Override
    public List<String> getProperties(NotificationFilterProperty property)
    {
        return preferenceProperties.getOrDefault(property, Collections.EMPTY_LIST);
    }

    @Override
    public NotificationFilterType getFilterType()
    {
        return filterType;
    }

    @Override
    public Set<NotificationFormat> getFilterFormats()
    {
        return notificationFormats;
    }
}
