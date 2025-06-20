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
package org.xwiki.messagestream.internal;

import java.util.Collection;
import java.util.Objects;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

/**
 * Base class for the message notification filters.
 *
 * @version $Id$
 * @since 12.10
 */
public abstract class AbstractMessageStreamNotificationFilter implements NotificationFilter
{
    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return Objects.equals(getEventType(), getEventType(preference));
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences, NotificationFilterType type,
        NotificationFormat format)
    {
        return null;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences, NotificationPreference preference)
    {
        return null;
    }

    /**
     * @return the event type handled by the filter
     */
    abstract String getEventType();

    private String getEventType(NotificationPreference preference)
    {
        return (String) preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE);
    }
}
