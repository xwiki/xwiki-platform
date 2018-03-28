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
package org.xwiki.notifications.filters.internal.status;

import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;

/**
 * Abstract implementation of EventReadFilter.
 *
 * @version $Id$
 * @since 10.1RC1
 */
public abstract class AbstractEventReadFilter implements NotificationFilter, ToggleableNotificationFilter
{
    private String filterName;

    private NotificationFormat format;

    /**
     * Construct an AbstractEventReadFilter.
     * @param filterName name of the filter
     * @param format format on which the filter applies
     */
    public AbstractEventReadFilter(String filterName, NotificationFormat format)
    {
        this.filterName = filterName;
        this.format = format;
    }

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user, NotificationFormat format)
    {
        // We only handle it at the expression level to avoid too much accesses to the database
        return FilterPolicy.NO_EFFECT;
    }

    @Override
    public boolean matchesPreference(NotificationPreference preference)
    {
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
        if (type == NotificationFilterType.EXCLUSIVE && format == this.format) {
            return not(new InListOfReadEventsNode(user));
        }
        return null;
    }

    @Override
    public String getName()
    {
        return filterName;
    }

    @Override
    public boolean isEnabledByDefault()
    {
        return false;
    }
}
