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
package org.xwiki.notifications.preferences.internal;

import java.util.Date;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.text.StringUtils;

/**
 * Abstract implementation of {@link NotificationPreference}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
public abstract class AbstractNotificationPreference implements NotificationPreference
{
    protected boolean isNotificationEnabled;

    protected NotificationFormat format;

    protected Date startDate;

    protected Map<NotificationPreferenceProperty, Object> properties;

    protected String providerHint;

    protected NotificationPreferenceCategory category;

    /**
     * Construct a new NotificationPreference.
     * @param isNotificationEnabled either or not the notification is enabled for the event type or the application
     * @param format format of the notification
     * @param category the category of the notification preference
     * @param startDate the date from which notifications that match this preference should be retrieved
     * @param providerHint the hint of the provider component used to save the preference
     * @param properties a map of the preference properties
     *
     * @since 9.7RC1
     */
    public AbstractNotificationPreference(boolean isNotificationEnabled, NotificationFormat format,
            NotificationPreferenceCategory category, Date startDate, String providerHint,
            Map<NotificationPreferenceProperty, Object> properties)
    {
        this.isNotificationEnabled = isNotificationEnabled;
        this.format = format;
        this.category = category;
        this.startDate = startDate;
        this.providerHint = providerHint;
        this.properties = properties;
    }

    protected AbstractNotificationPreference()
    {

    }

    @Override
    public boolean isNotificationEnabled()
    {
        return isNotificationEnabled;
    }

    @Override
    public NotificationFormat getFormat()
    {
        return format;
    }

    @Override
    public Date getStartDate()
    {
        return startDate;
    }

    @Override
    public Map<NotificationPreferenceProperty, Object> getProperties()
    {
        return this.properties;
    }

    @Override
    public String getProviderHint()
    {
        return (providerHint == null || StringUtils.isBlank(providerHint)) ? null : providerHint;
    }

    @Override
    public NotificationPreferenceCategory getCategory()
    {
        return category;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        // Here, we only compute a subset of the properties, because we want to say equals() == true if the other
        // preference is about the same event type, etc...
        AbstractNotificationPreference that = (AbstractNotificationPreference) o;
        EqualsBuilder equalsBuilder = new EqualsBuilder();
        equalsBuilder.append(that.format, format);
        equalsBuilder.append(that.properties, properties);
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode()
    {
        // Here, we only compute a subset of the properties, because we want to have the same hashcode if the other
        // preference is about the same event type, etc...
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder();
        hashCodeBuilder.append(format);
        hashCodeBuilder.append(properties);
        return hashCodeBuilder.hashCode();
    }
}
