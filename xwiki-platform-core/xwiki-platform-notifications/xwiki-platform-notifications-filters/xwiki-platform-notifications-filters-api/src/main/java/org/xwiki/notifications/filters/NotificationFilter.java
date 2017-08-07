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
import org.xwiki.notifications.filters.expression.generics.AbstractNode;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.stability.Unstable;

/**
 * Enable or disable notifications from the event stream (for customization purpose).
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Role
@Unstable
public interface NotificationFilter
{
    /**
     * Enable or disable an event in the notification list (post-filter).
     *
     * @param event an event
     * @param user the user interested in the notification
     * @param format format of the notification
     * @return true if the event should be dismiss
     */
    boolean filterEvent(Event event, DocumentReference user, NotificationFormat format);

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
     * @return a filtering expression
     *
     * @since 9.7RC1
     */
    AbstractNode filterExpression(DocumentReference user, NotificationPreference preference);
}
