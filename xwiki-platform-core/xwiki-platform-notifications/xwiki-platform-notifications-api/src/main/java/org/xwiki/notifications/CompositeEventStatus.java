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

import java.util.ArrayList;
import java.util.List;

import org.xwiki.eventstream.EventStatus;

/**
 * Combine all event statuses of a composite event.
 *
 * @version $Id$
 * @since 9.4RC1
 */
public class CompositeEventStatus
{
    private CompositeEvent compositeEvent;

    private List<EventStatus> eventStatuses = new ArrayList<>();

    /**
     * Construct a composite event status.
     * @param compositeEvent composite event
     */
    public CompositeEventStatus(CompositeEvent compositeEvent)
    {
        this.compositeEvent = compositeEvent;
    }

    /**
     * Add an event status to the object.
     * The event status must concern an event that compose the composite event.
     * @param eventStatus the event status to add
     */
    public void add(EventStatus eventStatus)
    {
        eventStatuses.add(eventStatus);
    }

    /**
     * @return the composite event
     */
    public CompositeEvent getCompositeEvent()
    {
        return compositeEvent;
    }

    /**
     * @return a global status for all event statuses
     */
    public boolean getStatus()
    {
        boolean result = true;
        // If any of the event is not read, then we consider the composite event as not read.
        for (EventStatus status : eventStatuses) {
            result &= status.isRead();
        }
        return result;
    }
}
