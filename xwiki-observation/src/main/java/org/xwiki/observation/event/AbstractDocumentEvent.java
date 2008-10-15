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
 *
 */
package org.xwiki.observation.event;

import org.xwiki.observation.event.filter.AlwaysMatchingEventFilter;
import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.FixedNameEventFilter;

/**
 * Base class for all document {@link Event events}.
 * 
 * @version $Id$
 */
public abstract class AbstractDocumentEvent implements Event
{
    /** A filter for comparing document names, used in {@link #matches(Object)}. */
    private EventFilter eventFilter;

    /**
     * Constructor initializing the event filter with an {@link AlwaysMatchingEventFilter}, meaning that this event will
     * match any other document event of the same type.
     */
    public AbstractDocumentEvent()
    {
        this.eventFilter = new AlwaysMatchingEventFilter();
    }

    /**
     * Constructor initializing the event filter with a {@link FixedNameEventFilter}, meaning that this event will match
     * only events of the same type affecting the same document.
     * 
     * @param documentName the name of the document related to this event
     */
    public AbstractDocumentEvent(String documentName)
    {
        this.eventFilter = new FixedNameEventFilter(documentName);
    }

    /**
     * Constructor using a custom {@link EventFilter}.
     * 
     * @param eventFilter the filter to use for matching events
     */
    public AbstractDocumentEvent(EventFilter eventFilter)
    {
        this.eventFilter = eventFilter;
    }

    /**
     * Retrieves the filter used to match this event agains other events, used in {@link #matches(Object)}.
     * 
     * @return the event's {@link #eventFilter filter}.
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
            isMatching = getEventFilter().matches(((AbstractDocumentEvent) otherEvent).getEventFilter());
        }
        return isMatching;
    }
}
