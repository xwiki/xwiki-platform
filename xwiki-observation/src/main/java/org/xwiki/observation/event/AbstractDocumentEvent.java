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

import org.xwiki.observation.event.filter.EventFilter;
import org.xwiki.observation.event.filter.FixedNameEventFilter;

public abstract class AbstractDocumentEvent implements Event
{
    private EventFilter eventFilter;
    
    public AbstractDocumentEvent(String documentName)
    {
        this.eventFilter = new FixedNameEventFilter(documentName);
    }

    public AbstractDocumentEvent(EventFilter eventFilter)
    {
        this.eventFilter = eventFilter;
    }

    public EventFilter getEventFilter()
    {
        return this.eventFilter;
    }

    public boolean matches(Object otherEvent)
    {
        boolean isMatching = false;
        if (AbstractDocumentEvent.class.isAssignableFrom(otherEvent.getClass())) {
            AbstractDocumentEvent event = (AbstractDocumentEvent) otherEvent;
            isMatching = getEventFilter().matches(event.getEventFilter());
        }
        return isMatching;
    }
}
