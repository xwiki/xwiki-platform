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
 * Abstract class for every event that is related to the {@link org.xwiki.eventstream.EventStream}.
 *
 * @since 9.6RC1
 * @version $Id$
 */
public abstract class AbstractEventStreamEvent implements org.xwiki.observation.event.Event
{
    protected Event event;

    /**
     * Constructs a new {@link AbstractEventStreamEvent}.
     *
     * @param event the event stream event that is related to this particular event.
     */
    public AbstractEventStreamEvent(Event event)
    {
        this.event = event;
    }

    /**
     * @return the event related to the event stream.
     */
    public Event getEvent()
    {
        return this.event;
    }

    @Override
    public boolean matches(Object o)
    {
        return (this.getClass().isInstance(o) && ((AbstractEventStreamEvent) o).getEvent().equals(this.event));
    }
}
