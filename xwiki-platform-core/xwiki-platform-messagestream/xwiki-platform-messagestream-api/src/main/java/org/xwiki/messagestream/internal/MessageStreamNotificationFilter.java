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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.inject.Named;
import javax.inject.Singleton;

import org.springframework.stereotype.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.messagestream.DirectMessageDescriptor;
import org.xwiki.messagestream.PersonalMessageDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Filter that make sure a message from the message stream is visible by the current user.
 *
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
@Component
@Singleton
@Named("MessageStreamNotificationFilter")
public class MessageStreamNotificationFilter implements NotificationFilter
{
    private static final List<String> SUPPORTED_EVENT_TYPES = Arrays.asList(DirectMessageDescriptor.EVENT_TYPE,
            PersonalMessageDescriptor.EVENT_TYPE);

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
    {
        if (DirectMessageDescriptor.EVENT_TYPE.equals(event.getType()) && !user.equals(event.getUser())) {
            return FilterPolicy.FILTER;
        }
        // TODO
        // Messages to the followers.
        // Case 1: the sender of the message is followed by the current user
        // Case 2: the notifications macro is used to displayed all events of the sender of this message

        return FilterPolicy.NO_EFFECT;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return SUPPORTED_EVENT_TYPES.contains(getEventType(preference));
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationPreference preference)
    {
        String eventType = getEventType(preference);
        if (DirectMessageDescriptor.EVENT_TYPE.equals(eventType)) {
            return value(EventProperty.STREAM).eq(value(user));
        }
        return null;
    }

    private String getEventType(NotificationPreference preference)
    {
        return (String) preference.getProperties().get(NotificationPreferenceProperty.EVENT_TYPE);
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilterType type,
            NotificationFormat format)
    {
        return null;
    }

    @Override
    public String getName()
    {
        return "Message Stream Notification Filter";
    }
}
