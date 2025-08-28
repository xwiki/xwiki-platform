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
package org.xwiki.notifications;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.eventstream.Event;
import org.xwiki.user.UserReference;

/**
 * Component responsible to perform the grouping based on the available {@link GroupingEventStrategy}, on the targeted
 * output, and on the preference of the users.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Role
public interface GroupingEventManager
{
    /**
     * Retrieve the {@link GroupingEventStrategy} based on the given information, or use a default strategy and apply
     * it to group the given events.
     *
     * @param events the list of events to group
     * @param userReference the user for whom the grouping is performed (or {@code null} if it's not for a specific
     *                      user)
     * @param target the output target (e.g. email or alert)
     * @return a list of composite events as computed by {@link GroupingEventStrategy#group(List)}
     * @throws NotificationException in case of problem when performing the grouping
     */
    List<CompositeEvent> getCompositeEvents(List<Event> events, UserReference userReference, String target)
        throws NotificationException;

    /**
     * Add new events to an already existing list of composite events, using the {@link GroupingEventStrategy}
     * retrieved from the given information.
     * This method does not return anything but update the given list of composite events.
     *
     * @param compositeEvents a list of composite events (might be empty)
     * @param newEvents the new events to group along with the given list of composite events
     * @param userReference the user for whom the grouping is performed (or {@code null} if it's not for a specific
     *                      user)
     * @param target the output target (e.g. email or alert)
     * @throws NotificationException in case of problem when performing the grouping
     */
    void augmentCompositeEvents(List<CompositeEvent> compositeEvents, List<Event> newEvents,
        UserReference userReference, String target) throws NotificationException;
}
