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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.internal.DefaultEvent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link EventGroup event group}.
 * 
 * @version $Id$
 */
public class EventGroupTest
{
    EventGroup g;

    DefaultEvent event1 = new DefaultEvent();

    DefaultEvent event2 = new DefaultEvent();

    DefaultEvent event3 = new DefaultEvent();

    @BeforeEach
    public void beforeEach()
    {
        this.g = new EventGroup();

        this.event1.setId("id1");
        this.event2.setId("id2");
        this.event3.setId("id3");
    }

    @Test
    public void testConstructors()
    {
        assertNotNull(this.g.getEvents());
        assertEquals(0, this.g.getEvents().size());

        this.g = new EventGroup(this.event1);
        assertEquals(1, this.g.getEvents().size());
        assertTrue(this.g.getEvents().contains(this.event1));

        List<Event> l = new ArrayList<Event>();
        l.add(this.event1);
        this.g = new EventGroup(l);
        assertEquals(1, this.g.getEvents().size());
        assertTrue(this.g.getEvents().contains(this.event1));

        Set<Event> s = new HashSet<Event>();
        s.add(this.event1);
        this.g = new EventGroup(s);
        assertEquals(1, this.g.getEvents().size());
        assertTrue(this.g.getEvents().contains(this.event1));
        // Test that the internal set is not the same as the passed set
        s.remove(this.event1);
        assertEquals(1, this.g.getEvents().size());
        assertTrue(this.g.getEvents().contains(this.event1));
        s.add(this.event2);
        s.add(this.event3);
        assertEquals(1, this.g.getEvents().size());
        this.g = new EventGroup(s);
        assertEquals(2, this.g.getEvents().size());
    }

    @Test
    public void testConstructorsWithNull()
    {
        this.g = new EventGroup((List<Event>) null);
        assertNotNull(this.g.getEvents());
        assertTrue(this.g.getEvents().isEmpty());

        this.g = new EventGroup((Set<Event>) null);
        assertNotNull(this.g.getEvents());
        assertTrue(this.g.getEvents().isEmpty());

        this.g = new EventGroup((Event[]) null);
        assertNotNull(this.g.getEvents());
        assertTrue(this.g.getEvents().isEmpty());
    }

    @Test
    public void testConstructorsWithNullElements()
    {
        List<Event> eventsList = new ArrayList<Event>();
        eventsList.add(this.event1);
        eventsList.add(null);
        eventsList.add(this.event2);
        eventsList.add(this.event3);
        this.g = new EventGroup(eventsList);
        assertEquals(3, this.g.getEvents().size());

        Set<Event> eventsSet = new HashSet<Event>();
        eventsSet.add(this.event1);
        eventsSet.add(null);
        eventsSet.add(this.event2);
        eventsSet.add(this.event3);
        this.g = new EventGroup(eventsSet);
        assertEquals(3, this.g.getEvents().size());

        this.g = new EventGroup(this.event1, null, this.event2, null, this.event3);
        assertEquals(3, this.g.getEvents().size());
    }

    @Test
    public void testGetMainEvent()
    {
        this.event1.setImportance(Importance.BACKGROUND);
        this.event2.setImportance(Importance.MAJOR);
        this.event3.setImportance(Importance.MAJOR);

        assertNull(this.g.getMainEvent());

        this.g.addEvents(this.event1);
        assertEquals(this.event1, this.g.getMainEvent());
        this.g.addEvents(this.event2);
        assertEquals(this.event2, this.g.getMainEvent());
        this.g.addEvents(this.event3);
        assertEquals(this.event2, this.g.getMainEvent());

        this.g.clearEvents();
        assertNull(this.g.getMainEvent());

        this.g.clearEvents();
        this.g.addEvents(this.event3);
        assertEquals(this.event3, this.g.getMainEvent());
        this.g.addEvents(this.event2);
        assertEquals(this.event3, this.g.getMainEvent());
        this.g.addEvents(this.event1);
        assertEquals(this.event3, this.g.getMainEvent());

        this.g.clearEvents();
        this.g.addEvents(this.event2);
        assertEquals(this.event2, this.g.getMainEvent());
        this.g.addEvents(this.event3);
        assertEquals(this.event2, this.g.getMainEvent());
        this.g.addEvents(this.event1);
        assertEquals(this.event2, this.g.getMainEvent());

        this.g.clearEvents();
        this.g.addEvents(this.event3);
        assertEquals(this.event3, this.g.getMainEvent());
        this.g.addEvents(this.event3);
        assertEquals(this.event3, this.g.getMainEvent());
        this.g.addEvents(this.event2);
        assertEquals(this.event3, this.g.getMainEvent());
    }

    @Test
    public void testAddEvents()
    {
        assertTrue(this.g.getEvents().isEmpty());
        assertTrue(this.g.getEvents().isEmpty());
        this.g.addEvents(this.event1);
        assertFalse(this.g.getEvents().isEmpty());
        assertEquals(1, this.g.getEvents().size());
        this.g.addEvents(this.event1);
        assertEquals(1, this.g.getEvents().size());
        this.g.addEvents(this.event2, this.event2, this.event2);
        assertEquals(2, this.g.getEvents().size());
        this.g.addEvents(this.event1, this.event2);
        assertEquals(2, this.g.getEvents().size());
        this.g.addEvents(this.event1, this.event2, this.event3);
        assertEquals(3, this.g.getEvents().size());
        this.g.clearEvents();
        assertEquals(0, this.g.getEvents().size());
        assertTrue(this.g.getEvents().isEmpty());
        this.g.addEvents(this.event1, this.event2, this.event3);
        assertEquals(3, this.g.getEvents().size());
    }

    @Test
    public void testAddEventsWithNull()
    {
        this.g.addEvents((Event) null);
        assertTrue(this.g.getEvents().isEmpty());

        this.g.addEvents(this.event1);
        assertEquals(1, this.g.getEvents().size());
        this.g.addEvents((Event) null);
        assertEquals(1, this.g.getEvents().size());
    }

    @Test
    public void testClearEvents()
    {
        assertTrue(this.g.getEvents().isEmpty());
        this.g.addEvents(this.event1);
        this.g.addEvents(this.event2);
        this.g.addEvents(this.event3);
        assertFalse(this.g.getEvents().isEmpty());
        this.g.clearEvents();
        assertTrue(this.g.getEvents().isEmpty());
        this.g.clearEvents();
        assertTrue(this.g.getEvents().isEmpty());
        this.g.addEvents(this.event1);
        assertFalse(this.g.getEvents().isEmpty());
        this.g.clearEvents();
        assertTrue(this.g.getEvents().isEmpty());
    }

    @Test
    public void testGetEventsIsReadonly()
    {
        this.g.addEvents(this.event1, this.event2);

        assertThrows(UnsupportedOperationException.class, () -> this.g.getEvents().clear());
    }

    @Test
    public void testGetEventsIsNotLive()
    {
        this.g.addEvents(this.event1, this.event2);
        Set<Event> view = this.g.getEvents();
        assertEquals(2, view.size());
        this.g.addEvents(this.event3);
        assertEquals(2, view.size());
    }
}
