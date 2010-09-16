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
package org.xwiki.observation.event;

import org.xwiki.observation.event.filter.AlwaysMatchingEventFilter;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.FixedNameEventFilter;

import java.io.Serializable;

/**
 * A generic Event implementation to extend for all Events that want to support {@link EventFilter}s.
 * 
 * @version $Id$
 * @since 2.4M2
 */
public abstract class AbstractFilterableEvent implements FilterableEvent, Serializable
{
    /**
     * The version identifier for this Serializable class. Increment only if the <i>serialized</i> form of the class
     * changes.
     */
    private static final long serialVersionUID = 1L;

    /**
     * A filter for comparing document names, used in {@link #matches(Object)}.
     */
    private EventFilter eventFilter;

    /**
     * Constructor initializing the event filter with an
     * {@link org.xwiki.observation.event.filter.AlwaysMatchingEventFilter}, meaning that this event will
     * match any other event of the same type.
     */
    public AbstractFilterableEvent()
    {
        this.eventFilter = new AlwaysMatchingEventFilter();
    }

    /**
     * Constructor initializing the event filter with a {@link org.xwiki.observation.event.filter.FixedNameEventFilter},
     * meaning that this event will match only events of the same type affecting the same passed name.
     *
     * @param name a generic name that uniquely identifies an event type
     */
    public AbstractFilterableEvent(String name)
    {
        this.eventFilter = new FixedNameEventFilter(name);
    }

    /**
     * Constructor using a custom {@link EventFilter}.
     *
     * @param eventFilter the filter to use for matching events
     */
    public AbstractFilterableEvent(EventFilter eventFilter)
    {
        this.eventFilter = eventFilter;
    }

    /**
     * {@inheritDoc}
     * @see FilterableEvent#getEventFilter()
     */
    public EventFilter getEventFilter()
    {
        return this.eventFilter;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This type of events match only events of the same type, and only if the internal {@link #eventFilter}s also
     * {@link EventFilter#matches(EventFilter) match}.
     * </p>
     *
     * @see Event#matches(Object)
     * @see EventFilter#matches(EventFilter)
     */
    public boolean matches(Object otherEvent)
    {
        boolean isMatching = false;
        if (this.getClass().isAssignableFrom(otherEvent.getClass())) {
            isMatching = getEventFilter().matches(((AbstractFilterableEvent) otherEvent).getEventFilter());
        }
        return isMatching;
    }
}
