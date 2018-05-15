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
import java.util.Date;
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

    private boolean isActive;

    private String filterName;

    private String filterPreferenceName;

    private String providerHint;

    private NotificationFilterType filterType;

    private Set<NotificationFormat> notificationFormats;

    private Map<NotificationFilterProperty, List<String>> preferenceProperties;

    private Date startingDate;

    /**
     * Constructs a new {@link DefaultNotificationFilterPreference} using the given filter preference name.
     *
     * @param filterPreferenceName the name of the filter preference
     */
    public DefaultNotificationFilterPreference(String filterPreferenceName)
    {
        this.filterPreferenceName = filterPreferenceName;
    }

    /**
     * Constructs a new {@link DefaultNotificationFilterPreference} as a copy of an other one (Important: fields are
     * copied by references, not by values).
     *
     * @param source the object to copy
     */
    public DefaultNotificationFilterPreference(DefaultNotificationFilterPreference source)
    {
        this.isEnabled = source.isEnabled;
        this.isActive = source.isActive;
        this.filterName = source.filterName;
        this.filterPreferenceName = source.filterPreferenceName;
        this.providerHint = source.providerHint;
        this.filterType = source.filterType;
        this.notificationFormats = source.notificationFormats;
        this.preferenceProperties = source.preferenceProperties;
        this.startingDate = source.startingDate;
    }

    @Override
    public String getFilterPreferenceName()
    {
        return filterPreferenceName;
    }

    /**
     * Set the current filter preference name.
     *
     * @param filterPreferenceName the name of the filter preference
     */
    public void setFilterPreferenceName(String filterPreferenceName)
    {
        this.filterPreferenceName = filterPreferenceName;
    }

    @Override
    public String getFilterName()
    {
        return filterName;
    }

    /**
     * Set the name of the filter that this preference is attached to.
     *
     * @param filterName the name of the filter
     */
    public void setFilterName(String filterName)
    {
        this.filterName = filterName;
    }

    @Override
    public String getProviderHint()
    {
        return providerHint;
    }

    /**
     * Set the hint of the {@link org.xwiki.notifications.filters.NotificationFilterPreferenceProvider} associated
     * with this preference.
     *
     * @param providerHint the name of the provider
     */
    public void setProviderHint(String providerHint)
    {
        this.providerHint = providerHint;
    }

    @Override
    public boolean isEnabled()
    {
        return isEnabled;
    }

    /**
     * Set the enabled status of the current filter preference.
     *
     * @param isEnabled true if the filter preference should be enabled
     */
    public void setEnabled(boolean isEnabled)
    {
        this.isEnabled = isEnabled;
    }

    @Override
    public boolean isActive()
    {
        return isActive;
    }

    /**
     * Set the active status of the current filter preference.
     *
     * @param isActive true if the filter preference should be active, false if the preference should be passive
     */
    public void setActive(boolean isActive)
    {
        this.isActive = isActive;
    }

    @Override
    public List<String> getProperties(NotificationFilterProperty property)
    {
        return preferenceProperties.getOrDefault(property, Collections.EMPTY_LIST);
    }

    /**
     * Set the properties of the current filter preference.
     *
     * @param preferenceProperties a map of lists defining the filter preference properties
     */
    public void setPreferenceProperties(Map<NotificationFilterProperty, List<String>> preferenceProperties)
    {
        this.preferenceProperties = preferenceProperties;
    }

    @Override
    public NotificationFilterType getFilterType()
    {
        return filterType;
    }

    /**
     * Set the filter type of the current filter preference.
     *
     * @param filterType the filter type
     */
    public void setFilterType(NotificationFilterType filterType)
    {
        this.filterType = filterType;
    }

    @Override
    public Set<NotificationFormat> getFilterFormats()
    {
        return notificationFormats;
    }

    @Override
    public Date getStartingDate()
    {
        return startingDate;
    }

    /**
     * @param startingDate the date from which the filter preference is enabled
     */
    public void setStartingDate(Date startingDate)
    {
        this.startingDate = startingDate;
    }

    /**
     * Set the notification formats of the current filter preference.
     *
     * @param notificationFormats the filter formats
     */
    public void setNotificationFormats(Set<NotificationFormat> notificationFormats)
    {
        this.notificationFormats = notificationFormats;
    }
}
