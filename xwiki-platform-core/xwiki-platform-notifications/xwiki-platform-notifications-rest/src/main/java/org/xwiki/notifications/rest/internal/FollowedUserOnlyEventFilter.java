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

import java.util.Collection;

import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Notification filter that remove all events that have not been triggered by one of the given users.
 *
 * @version $Id$
 * @since 10.4RC1
 */
public class FollowedUserOnlyEventFilter implements NotificationFilter
{
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Collection<String> followedUsers;

    /**
     * Construct a FollowedUserOnlyEventFilter.
     * @param entityReferenceSerializer the default entity reference serializer
     * @param followedUsers the collection of users to include
     */
    public FollowedUserOnlyEventFilter(
            EntityReferenceSerializer<String> entityReferenceSerializer,
            Collection<String> followedUsers)
    {
        this.entityReferenceSerializer = entityReferenceSerializer;
        this.followedUsers = followedUsers;
    }

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
    {
        return followedUsers.contains(entityReferenceSerializer.serialize(event.getUser())) ? FilterPolicy.NO_EFFECT
            : FilterPolicy.FILTER;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        return false;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationPreference preference)
    {
        return null;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilterType type,
            NotificationFormat format)
    {
        if (type == NotificationFilterType.EXCLUSIVE) {
            return value(EventProperty.USER).inStrings(followedUsers);
        }
        return null;
    }

    @Override
    public String getName()
    {
        return "FollowedUserOnlyEventFilter";
    }

    @Override
    public int getPriority() {
        return 3000;
    }

}
