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
package org.xwiki.notifications.sources.internal;

import java.util.Collection;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

/**
 * Filter that make sure we don't record events that have happened before the starting date of the corresponding
 * preference (the query dot not guarantee that).
 *
 * @version $Id$
 * @since 10.8RC1
 * @since 9.11.8
 */
@Component(roles = PreferenceDateNotificationFilter.class)
@Singleton
public class PreferenceDateNotificationFilter
{
    /**
     * @param event an event to test
     * @param preferences all the preferences that are handled
     * @return either or not the event should be filtered (ie not recorded)
     */
    public boolean shouldFilter(Event event, Collection<NotificationPreference> preferences)
    {
        for (NotificationPreference preference : preferences) {
            Object preferenceEventType = preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE);
            if (preferenceEventType != null && event.getType().equals(preferenceEventType)
                    && preference.getStartDate().after(event.getDate())) {
                return true;
            }
        }

        return false;
    }
}
