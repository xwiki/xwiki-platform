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

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.preferences.NotificationPreferenceCategory;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;
import org.xwiki.notifications.preferences.NotificationPreference;

/**
 * Notification filter that handle the generic {@link NotificationPreferenceFilterScope}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named(ScopeNotificationEventTypeFilter.FILTER_NAME)
@Singleton
public class ScopeNotificationEventTypeFilter extends AbstractScopeNotificationFilter
{
    static final String FILTER_NAME = "scopeNotifEventTypeFilter";

    @Override
    protected boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope,
            NotificationPreference preference)
    {
        // We apply the filter only on scopes having the correct eventType
        return (preference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE)
                && scope.getEventTypes().contains(
                        preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE)));
    }

    @Override
    protected boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope, NotificationFormat format,
            Event event)
    {
        return scope.getEventTypes().contains(event.getType());
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return preference.getCategory().equals(NotificationPreferenceCategory.DEFAULT)
                && preference.getProperties().containsKey(NotificationPreferenceProperty.EVENT_TYPE);
    }

    @Override
    public String getProviderHint()
    {
        // By default, those filters are saved in the user profile, we might want to change that at some point.
        return "userProfile";
    }
}
