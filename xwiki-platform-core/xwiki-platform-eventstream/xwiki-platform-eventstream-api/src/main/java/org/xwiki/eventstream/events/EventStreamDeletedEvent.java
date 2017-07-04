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
package org.xwiki.eventstream.events;

import org.xwiki.eventstream.Event;

/**
 * Event triggered when an event is deleted from the {@link org.xwiki.eventstream.EventStream}.
 *
 * @since 9.6RC1
 * @version $Id$
 */
public class EventStreamDeletedEvent extends AbstractEventStreamEvent
{
    /**
     * Constructs a new {@link EventStreamDeletedEvent}.
     */
    public EventStreamDeletedEvent()
    {
        super(null);
    }

    /**
     * Constructs a new {@link EventStreamDeletedEvent}.
     *
     * @param event the event stream event related to this particular event
     */
    public EventStreamDeletedEvent(Event event)
    {
        super(event);
    }

    @Override
    public boolean matches(Object o)
    {
        return (o instanceof EventStreamDeletedEvent && ((EventStreamDeletedEvent) o).getEvent().equals(this.event));
    }
}
