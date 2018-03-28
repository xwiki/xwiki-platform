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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.preferences.NotificationPreference;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Define notification filters that are activated by default on every user and that filter the notifications
 * coming from the system user.
 *
 * This filter is not bound to any {@link NotificationPreference} and should be applied globally.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component
@Singleton
@Named(SystemUserNotificationFilter.FILTER_NAME)
public class SystemUserNotificationFilter implements NotificationFilter, ToggleableNotificationFilter
{
    /**
     * The name of the filter.
     */
    public static final String FILTER_NAME = "systemUserNotificationFilter";

    /**
     * Local document reference to the system user.
     */
    private static final LocalDocumentReference SYSTEM_USER =
            new LocalDocumentReference("XWiki", "superadmin");

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> serializer;

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user, NotificationFormat format)
    {
        return event.getUser().getLocalDocumentReference().equals(SYSTEM_USER)
                ? FilterPolicy.FILTER : FilterPolicy.NO_EFFECT;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
        // As the filter is applied globally, it’s not bound to any preference
        return false;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user, NotificationPreference preference)
    {
        return null;
    }

    @Override
    public ExpressionNode filterExpression(DocumentReference user, NotificationFilterType type,
            NotificationFormat format)
    {
        if (type == NotificationFilterType.EXCLUSIVE) {
            return value(EventProperty.USER).notEq(value(serializer.serialize(SYSTEM_USER)));
        }
        return null;
    }

    @Override
    public String getName()
    {
        return FILTER_NAME;
    }
}
