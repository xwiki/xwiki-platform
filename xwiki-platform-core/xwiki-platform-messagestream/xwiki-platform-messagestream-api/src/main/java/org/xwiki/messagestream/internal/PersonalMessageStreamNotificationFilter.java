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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.messagestream.PersonalMessageDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.internal.user.EventUserFilterPreferencesGetter;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.NotificationPreferenceProperty;

/**
 * Filter that make sure a message from the message stream is visible by the current user.
 *
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
@Component
@Singleton
@Named("PersonalMessageStreamNotificationFilter")
public class PersonalMessageStreamNotificationFilter implements NotificationFilter
{
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private EventUserFilterPreferencesGetter preferencesGetter;

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
    {
        String sender = serializer.serialize(event.getUser());
        return preferencesGetter.isUsedFollowed(sender, filterPreferences, format) ? FilterPolicy.KEEP
                : FilterPolicy.FILTER;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return PersonalMessageDescriptor.EVENT_TYPE.equals(getEventType(preference));
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationPreference preference)
    {
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
        return "Personal Message Stream Notification Filter";
    }
}
