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
package org.xwiki.notifications.filters;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.expression.ExpressionNode;
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.stability.Unstable;

/**
 * Enable or disable notifications from the event stream (for customization purpose).
 * A {@link NotificationFilter} has two goals :
 * <ul>
 *     <li>Pre-Filtering : Generate a query (made with {@link AbstractNode}) that will be used to retrieve a specific
 *     subset of notifications from a provider.</li>
 *     <li>Post-Filtering : given an {@link Event}, determine if this event should be filtered or not.</li>
 * </ul>
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
@Unstable
public interface NotificationFilter extends Comparable
{
    /**
     * The different behaviours a filter could have regarding an event.
     * @since 9.11.5
     * @since 10.3RC1
     */
    enum FilterPolicy
    {
        /**
         * Value used when the event must be not returned.
         */
        FILTER,

        /**
         * Value used when the event must be kept.
         */
        KEEP,

        /**
         * Value used when the filter has no impact in the given event.
         */
        NO_EFFECT
    }

    /**
     * Enable or disable an event in the notification list (post-filter).
     *
     * @param event an event
     * @param user the user interested in the notification
     * @param format format of the notification
     * @return true if the event should be dismiss
     * @since 9.11.5
     * @since 10.3RC1
     */
    FilterPolicy filterEvent(Event event, DocumentReference user, NotificationFormat format);

    /**
     * Determine if the current filter can be applied to the given preference.
     * In order to do so, the {@link NotificationFilter} can rely on the different parameters of the
     * {@link NotificationPreference}.
     *
     * @param preference the preference to use
     * @return true if the filter is compatible with the preference
     *
     * @since 9.7RC1
     */
    boolean matchesPreference(NotificationPreference preference);

    /**
     * Filtering expression to use when retrieving notifications.
     *
     * @param user the user interested in the notifications
     * @param preference the notification preference associated with the filter
     * @return the updated query
     *
     * @since 9.7RC1
     */
    ExpressionNode filterExpression(DocumentReference user, NotificationPreference preference);

    /**
     * Filtering expression to use when retrieving notifications.
     * Note that this filtering expression will not be bound to any notification preference.

     * @param user the user interested in the notifications
     * @param type of the expected notification filter
     * @param format format of the notification
     * @return a filtering expression or null
     *
     * @since 9.10RC1
     */
    ExpressionNode filterExpression(DocumentReference user, NotificationFilterType type, NotificationFormat format);

    /**
     * Get the name of the filter. This is useful as {@link NotificationFilterPreference} will be able to be linked to
     * this filter using its name. If the {@link NotificationFilter} is used as a component, consider defining
     * the hint of this component as the name of the filter.
     *
     * @return the name of the filter
     *
     * @since 9.7RC1
     */
    String getName();

    /**
     * @return the priority of the filter. The higher it is, the more important the result of
     * {@link NotificationFilter#filterEvent(Event event, DocumentReference user, NotificationFormat format) is}.
     *
     * @since 9.11.5
     * @since 10.3RC1
     */
    default int getPriority() {
        return 1000;
    }

    @Override
    default int compareTo(Object o)
    {
        if (o instanceof NotificationFilter) {
            NotificationFilter other = (NotificationFilter) o;
            return other.getPriority() - this.getPriority();
        }
        return 0;
    }
}
