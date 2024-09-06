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
import org.xwiki.messagestream.DirectMessageDescriptor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;

/**
 * Filter that make sure a direct message (to someone) from the message stream is visible by the current user.
 *
 * @version $Id$
 * @since 10.5RC1
 * @since 9.11.6
 */
@Component
@Singleton
@Named("DirectMessageStreamNotificationFilter")
public class DirectMessageStreamNotificationFilter extends AbstractMessageStreamNotificationFilter
{
    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
    {
        // Don't handle events that are not direct messages!
        if (!getEventType().equals(event.getType())) {
            return FilterPolicy.NO_EFFECT;
        }

        if (user != null) {
            String userId = this.serializer.serialize(user);
            // Here we don't use FilterPolicy.KEEP in case the message is addressed to the given user, because the
            // sender might be blacklisted by an other filter (the sender might be an harasser).
            // So we just make sure the message is filtered if the current user is not the recipient, but nothing more.
            return userId.equals(event.getStream()) ? FilterPolicy.NO_EFFECT : FilterPolicy.FILTER;
        } else {
            return FilterPolicy.FILTER;
        }
    }

    @Override
    public String getName()
    {
        return "Direct Message Stream Notification Filter";
    }

    @Override
    public int getPriority()
    {
        // The priority must be very high, because for "security" reason, a message that is not sent to the current user
        // must not been seen by other users.
        // In particular, the priority must be higher than org.xwiki.notifications.filters.internal.user.EventUserFilter
        // otherwise users will receive direct messages addressed to others if they follow the sender.
        return Integer.MAX_VALUE;
    }

    @Override
    String getEventType()
    {
        return DirectMessageDescriptor.EVENT_TYPE;
    }
}
