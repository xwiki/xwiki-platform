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
package org.xwiki.notifications.filters.internal.user;

import java.util.Collection;
import java.util.Iterator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.generics.AbstractOperatorNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.text.StringUtils;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Handle a black list of users not to watch.
 *
 * @version $Id$
 * @since 9.10RC1
 */
@Component
@Singleton
@Named(EventUserFilter.FILTER_NAME)
public class EventUserFilter implements NotificationFilter
{
    /**
     * Name of the filter.
     */
    public static final String FILTER_NAME = "eventUserNotificationFilter";

    @Inject
    private EventUserFilterPreferencesGetter preferencesGetter;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
    {
        final String eventUserId = serializer.serialize(event.getUser());
        if (preferencesGetter.isUserExcluded(eventUserId, filterPreferences, format)) {
            return FilterPolicy.FILTER;
        }
        if (preferencesGetter.isUsedFollowed(eventUserId, filterPreferences, format)) {
            return FilterPolicy.KEEP;
        }
        return FilterPolicy.NO_EFFECT;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        // As the filter is applied globally, it’s not bound to any preference
        return false;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences, NotificationPreference preference)
    {
        // We don't handle this use-case
        return null;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences,
            NotificationFilterType type, NotificationFormat format)
    {
        if (type == NotificationFilterType.EXCLUSIVE) {
            Collection<String> users = preferencesGetter.getExcludedUsers(filterPreferences, format);
            if (!users.isEmpty()) {
                return not(value(EventProperty.USER).inStrings(users));
            }
        } else {
            Iterator<NotificationFilterPreference>
                    iterator = preferencesGetter.getFollowedUsersPreferences(filterPreferences, format).iterator();
            AbstractOperatorNode node = null;
            while (iterator.hasNext()) {
                NotificationFilterPreference pref = iterator.next();
                if (StringUtils.isNotBlank(pref.getUser())) {
                    AbstractOperatorNode thisNode = value(EventProperty.USER).eq(value(pref.getUser())).and(
                            value(EventProperty.DATE).greaterThan(value(pref.getStartingDate())));
                    if (node == null) {
                        node = thisNode;
                    } else {
                        node = node.or(thisNode);
                    }
                }
            }
            if (node != null) {
                return node;
            }
        }

        return null;
    }

    @Override
    public String getName()
    {
        return FILTER_NAME;
    }

    @Override
    public int getPriority()
    {
        // Since we want to force some event to be kept when the user is followed, we need a high priority
        // (default is 1000).
        return 2000;
    }
}
