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
package org.xwiki.eventstream;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A group of related events, all happening as a consequence of the same action.
 * 
 * @version $Id$
 * @since 3.0M2
 */
public class EventGroup
{
    /** The events inside this group. */
    private Set<Event> events = new LinkedHashSet<Event>();

    /** Default constructor that creates an empty event group. */
    public EventGroup()
    {
        // Nothing to do
    }

    /**
     * Constructor that creates a group containing the passed events.
     * 
     * @param events the list of initial events to put in the group
     */
    public EventGroup(List<Event> events)
    {
        if (events != null) {
            this.events.addAll(events);
            this.events.remove(null);
        }
    }

    /**
     * Constructor that creates a group containing the passed events.
     * 
     * @param events the list of initial events to put in the group
     */
    public EventGroup(Set<Event> events)
    {
        if (events != null) {
            this.events.addAll(events);
            this.events.remove(null);
        }
    }

    /**
     * Constructor that creates a group containing the passed events.
     * 
     * @param events the list of initial events to put in the group
     */
    public EventGroup(Event... events)
    {
        if (events != null) {
            for (Event e : events) {
                if (e != null) {
                    this.events.add(e);
                }
            }
        }
    }

    /**
     * List the events that are part of this group.
     * 
     * @return a read only snapshot of the events in this group; future changes in this group will not be reflected in
     *         the returned snapshot
     */
    public Set<Event> getEvents()
    {
        Set<Event> clone = new LinkedHashSet<Event>(this.events.size(), 1f);
        clone.addAll(this.events);
        return Collections.unmodifiableSet(clone);
    }

    /**
     * Add more events to this group. Duplicate events are added only once, since this is a {@link Set}.
     * 
     * @param events the new events to add
     */
    public void addEvents(Event... events)
    {
        for (Event e : events) {
            if (e != null) {
                this.events.add(e);
            }
        }
    }

    /** Remove all the events from this group. */
    public void clearEvents()
    {
        this.events.clear();
    }

    /**
     * Get the most important event in this group. The "importance" is given by the {@link Event#getImportance()
     * importance} property of events. If more events have the same maximal importance, the first one found in the group
     * is returned. Usually this corresponds to the order the events were created, but this is not a guaranteed
     * property.
     * 
     * @return the most important event found in this group, {@code null} if the group is empty.
     */
    public Event getMainEvent()
    {
        Event result = null;
        for (Event e : this.events) {
            if (result == null || e.getImportance().ordinal() > result.getImportance().ordinal()) {
                result = e;
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.events.toString();
    }
}
