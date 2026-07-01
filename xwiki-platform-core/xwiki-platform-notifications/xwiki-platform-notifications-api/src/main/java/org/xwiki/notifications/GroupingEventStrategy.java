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

/**
 * Components implementing this interface defines how the events should be grouped to form composite events.
 * Grouping of events should be based on the type of events and other metadata like the documents events are referring
 * to. Different grouping strategy can be used for different targets (e.g. email vs alert).
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Role
public interface GroupingEventStrategy
{
    /**
     * Groups the given list of events to form composite events.
     * @param eventList the list of events to group
     * @return a list of {@link CompositeEvent} that should contain all events given as input
     * @throws NotificationException in case of problem when performing the grouping
     */
    List<CompositeEvent> group(List<Event> eventList) throws NotificationException;

    /**
     * Groups the new events to add them in the provided list of composite events.
     * The method doesn't return anything but update the provided list of composite events.
     *
     * @param compositeEvents a list of already existing composite events (can be empty)
     * @param newEvents the new events to group
     * @throws NotificationException in case of problem when performing the grouping
     */
    void group(List<CompositeEvent> compositeEvents, List<Event> newEvents) throws NotificationException;
}
