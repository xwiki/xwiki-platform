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
package org.xwiki.notifications.rest.internal;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

/**
 * An internal implementation of {@link NotificationPreference} used to enable all event types when the user does not
 * specify her own preferences.
 *
 * @version $Id$
 * @since 10.4RC1
 */
public class InternalNotificationPreference implements NotificationPreference
{
    private RecordableEventDescriptor descriptor;

    /**
     * Construct an InternalNotificationPreference.
     * @param descriptor descriptor of a recordable event type
     */
    public InternalNotificationPreference(RecordableEventDescriptor descriptor)
    {
        this.descriptor = descriptor;
    }

    @Override
    public boolean isNotificationEnabled()
    {
        return true;
    }

    @Override
    public NotificationFormat getFormat()
    {
        return NotificationFormat.ALERT;
    }

    @Override
    public Date getStartDate()
    {
        return new Date(0);
    }

    @Override
    public Map<NotificationPreferenceProperty, Object> getProperties()
    {
        Map<NotificationPreferenceProperty, Object> properties = new HashMap<>();
        properties.put(NotificationPreferenceProperty.EVENT_TYPE, descriptor.getEventType());
        return properties;
    }

    @Override
    public String getProviderHint()
    {
        return "rest";
    }

    @Override
    public NotificationPreferenceCategory getCategory()
    {
        return NotificationPreferenceCategory.DEFAULT;
    }
}
