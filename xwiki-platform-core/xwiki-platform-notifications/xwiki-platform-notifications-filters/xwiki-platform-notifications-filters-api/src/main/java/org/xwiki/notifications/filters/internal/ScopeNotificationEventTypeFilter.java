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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationProperty;

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

    private static final String PREFIX_FORMAT = "scopeNotifEventTypeFilter_%s_%d";

    protected String generateQueryRestriction(String suffix) {
        return String.format("event.type = :type_%s", suffix);
    }

    @Override
    protected Map<String, Object> generateQueryRestrictionParams(String suffix)
    {
        // TODO
        return null;
    }

    @Override
    protected String generateParameterSuffix(NotificationPreferenceScopeFilterType filterType, int parameterNumber)
    {
        return String.format(PREFIX_FORMAT, filterType.name(), parameterNumber);
    }

    @Override
    protected boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope, NotificationFormat format,
            Map<NotificationProperty, String> properties)  {
        // We apply the filter only on scopes having the correct eventType
        return (properties.containsKey(NotificationProperty.EVENT_TYPE)
                && scope.getEventType().equals(properties.get(NotificationProperty.EVENT_TYPE)));
    }

    @Override
    protected boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope, NotificationFormat format,
            List<Map<NotificationProperty, String>> propertyList)  {
        // Get every eventType contained in the given properties
        List<String> eventTypes = new ArrayList<>();
        for (Map<NotificationProperty, String> properties : propertyList) {
            if (properties.containsKey(NotificationProperty.EVENT_TYPE)) {
                eventTypes.add(properties.get(NotificationProperty.EVENT_TYPE));
            }
        }

        return eventTypes.contains(scope.getEventType());
    }

    @Override
    protected boolean scopeMatchesFilteringContext(NotificationPreferenceFilterScope scope, NotificationFormat format,
            Event event)
    {
        return scope.getEventType().equals(event.getType());
    }
}
