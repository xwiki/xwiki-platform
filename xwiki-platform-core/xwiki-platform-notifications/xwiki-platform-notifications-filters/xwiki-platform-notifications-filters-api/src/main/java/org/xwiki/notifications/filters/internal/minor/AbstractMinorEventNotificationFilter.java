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
package org.xwiki.notifications.filters.internal.minor;

import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.EventProperty;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;
import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.value;

/**
 * Abstract implementation of MinorEventNotificationFilter.
 *
 * @version $Id$
 * @since 10.2
 * @since 9.11.4
 */
public abstract class AbstractMinorEventNotificationFilter implements NotificationFilter, ToggleableNotificationFilter
{
    private static final String UPDATE_TYPE = "update";

    private static final String VERSION_SCHEME = ".1";

    private String filterName;

    private NotificationFormat format;

    /**
     * Construct an AbstractMinorEventNotificationFilter.
     * @param filterName name of the filter
     * @param format format on which the filter applies
     */
    public AbstractMinorEventNotificationFilter(String filterName, NotificationFormat format)
    {
        this.filterName = filterName;
        this.format = format;
    }

    @Override
    public boolean filterEvent(Event event, DocumentReference user, NotificationFormat format)
    {
        return event.getType().equals(UPDATE_TYPE) && !event.getDocumentVersion().endsWith(VERSION_SCHEME);
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
        if (type == NotificationFilterType.EXCLUSIVE && format == this.format) {
            return not(
                    value(EventProperty.TYPE).eq(value(UPDATE_TYPE)).and(
                        not(value(EventProperty.DOCUMENT_VERSION).endsWith(value(VERSION_SCHEME)))
                    )
            );
        }
        return null;
    }

    @Override
    public String getName()
    {
        return filterName;
    }
}
