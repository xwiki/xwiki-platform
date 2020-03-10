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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.preferences.NotificationPreference;

import static org.xwiki.notifications.filters.expression.generics.ExpressionBuilder.not;

/**
 * Abstract implementation {@link ForUserAlertEventFilter}.
 *
 * @version $Id$
 * @since 12.1RC1
 */
public abstract class AbstractForUserEventFilter implements NotificationFilter, ToggleableNotificationFilter
{
    protected String filterName;

    protected NotificationFormat format;

    protected Boolean read;

    protected boolean not;

    /**
     * Construct an AbstractEventReadFilter.
     * 
     * @param filterName name of the filter
     * @param format format on which the filter applies
     * @param read true if only read status should be included, false for only unread
     * @param not true when a NOT expression should be returned
     */
    public AbstractForUserEventFilter(String filterName, NotificationFormat format, Boolean read, boolean not)
    {
        this.filterName = filterName;
        this.format = format;
        this.read = read;
        this.not = not;
    }

    @Override
    public FilterPolicy filterEvent(Event event, DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences, NotificationFormat format)
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
        if (user != null && type == NotificationFilterType.EXCLUSIVE && format == this.format) {
            ForUserNode node = new ForUserNode(user, this.read);

            return this.not ? not(node) : node;
        }

        return null;
    }

    @Override
    public List<NotificationFormat> getFormats()
    {
        return Collections.singletonList(this.format);
    }

    @Override
    public String getName()
    {
        return this.filterName;
    }

    @Override
    public boolean isEnabledByDefault()
    {
        return false;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractForUserEventFilter that = (AbstractForUserEventFilter) o;

        return new EqualsBuilder()
            .append(not, that.not)
            .append(filterName, that.filterName)
            .append(format, that.format)
            .append(read, that.read)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 37)
            .append(filterName)
            .append(format)
            .append(read)
            .append(not)
            .toHashCode();
    }
}
