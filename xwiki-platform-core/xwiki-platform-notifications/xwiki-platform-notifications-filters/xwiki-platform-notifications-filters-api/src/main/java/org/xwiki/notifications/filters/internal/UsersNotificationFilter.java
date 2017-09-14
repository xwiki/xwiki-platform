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

import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Define a notification filter based on a list of users.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Component
@Named(UsersNotificationFilter.FILTER_NAME)
@Singleton
public class UsersNotificationFilter extends AbstractScopeOrUserNotificationFilter<NotificationFilterPreference>
{
    /**
     * Name of the filter.
     */
    public static final String FILTER_NAME = "usersNotificationFilter";

    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Default constructor.
     */
    public UsersNotificationFilter()
    {
        super(FILTER_NAME);
    }

    @Override
    protected NotificationFilterPreference convertPreferences(NotificationFilterPreference preference)
    {
        return preference;
    }

    @Override
    protected boolean matchRestriction(Event event, NotificationFilterPreference preference)
            throws NotificationException
    {
        List<String> watchedUsers = preference.getProperties(NotificationFilterProperty.USER);
        return watchedUsers.contains(serializer.serialize(event.getUser()));
    }

    @Override
    protected Predicate<NotificationFilterPreference> filter()
    {
        return preference -> FILTER_NAME.equals(preference.getFilterName())
                && !preference.getProperties(NotificationFilterProperty.USER).isEmpty();
    }

    @Override
    protected AbstractOperatorNode generateNode(NotificationFilterPreference filterPreference)
    {
        Iterator<String> userIterator = filterPreference.getProperties(NotificationFilterProperty.USER).iterator();
        AbstractOperatorNode node = value(EventProperty.USER).eq(value(userIterator.next()));

        // In case the user list contains more than 1 user...
        while (userIterator.hasNext()) {
            node = node.or(value(EventProperty.USER).eq(value(userIterator.next())));
        }

        return node;
    }
}
