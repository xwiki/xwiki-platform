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

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.internal.DefaultEvent;

/**
 * Tests for the {@link EventGroup event group}.
 * 
 * @version $Id$
 */
public class EventGroupTest
{
    EventGroup g;

    @Before
    public void setup()
    {
        this.g = new EventGroup();
    }

    @Test
    public void testConstructors()
    {
        Event e = new DefaultEvent();

        Assert.assertNotNull(this.g.getEvents());
        Assert.assertEquals(0, this.g.getEvents().size());

        this.g = new EventGroup(e);
        Assert.assertEquals(1, this.g.getEvents().size());
        Assert.assertTrue(this.g.getEvents().contains(e));

        List<Event> l = new ArrayList<Event>();
        l.add(e);
        this.g = new EventGroup(l);
        Assert.assertEquals(1, this.g.getEvents().size());
        Assert.assertTrue(this.g.getEvents().contains(e));

        Set<Event> s = new HashSet<Event>();
        s.add(e);
        this.g = new EventGroup(s);
        Assert.assertEquals(1, this.g.getEvents().size());
        Assert.assertTrue(this.g.getEvents().contains(e));
        // Test that the internal set is not the same as the passed set
        s.remove(e);
        Assert.assertEquals(1, this.g.getEvents().size());
        Assert.assertTrue(this.g.getEvents().contains(e));
        s.add(new DefaultEvent());
        s.add(new DefaultEvent());
        Assert.assertEquals(1, this.g.getEvents().size());
        this.g = new EventGroup(s);
        Assert.assertEquals(2, this.g.getEvents().size());
    }

    @Test
    public void testConstructorsWithNull()
    {
        this.g = new EventGroup((List<Event>) null);
        Assert.assertNotNull(this.g.getEvents());
        Assert.assertTrue(this.g.getEvents().isEmpty());

        this.g = new EventGroup((Set<Event>) null);
        Assert.assertNotNull(this.g.getEvents());
        Assert.assertTrue(this.g.getEvents().isEmpty());

        this.g = new EventGroup((Event[]) null);
        Assert.assertNotNull(this.g.getEvents());
        Assert.assertTrue(this.g.getEvents().isEmpty());
    }

    @Test
    public void testConstructorsWithNullElements()
    {
        List<Event> eventsList = new ArrayList<Event>();
        eventsList.add(new DefaultEvent());
        eventsList.add(null);
        eventsList.add(new DefaultEvent());
        eventsList.add(new DefaultEvent());
        this.g = new EventGroup(eventsList);
        Assert.assertEquals(3, this.g.getEvents().size());

        Set<Event> eventsSet = new HashSet<Event>();
        eventsSet.add(new DefaultEvent());
        eventsSet.add(null);
        eventsSet.add(new DefaultEvent());
        eventsSet.add(new DefaultEvent());
        this.g = new EventGroup(eventsSet);
        Assert.assertEquals(3, this.g.getEvents().size());

        this.g = new EventGroup(new DefaultEvent(), null, new DefaultEvent(), null, new DefaultEvent());
        Assert.assertEquals(3, this.g.getEvents().size());
    }

    @Test
    public void testGetMainEvent()
    {
        Event e1 = new DefaultEvent();
        e1.setImportance(Importance.BACKGROUND);
        Event e2 = new DefaultEvent();
        e2.setImportance(Importance.MAJOR);
        Event e3 = new DefaultEvent();
        e3.setImportance(Importance.MAJOR);

        Assert.assertNull(this.g.getMainEvent());

        this.g.addEvents(e1);
        Assert.assertEquals(e1, this.g.getMainEvent());
        this.g.addEvents(e2);
        Assert.assertEquals(e2, this.g.getMainEvent());
        this.g.addEvents(e3);
        Assert.assertEquals(e2, this.g.getMainEvent());

        this.g.clearEvents();
        Assert.assertNull(this.g.getMainEvent());

        this.g.clearEvents();
        this.g.addEvents(e3);
        Assert.assertEquals(e3, this.g.getMainEvent());
        this.g.addEvents(e2);
        Assert.assertEquals(e3, this.g.getMainEvent());
        this.g.addEvents(e1);
        Assert.assertEquals(e3, this.g.getMainEvent());

        this.g.clearEvents();
        this.g.addEvents(e2);
        Assert.assertEquals(e2, this.g.getMainEvent());
        this.g.addEvents(e3);
        Assert.assertEquals(e2, this.g.getMainEvent());
        this.g.addEvents(e1);
        Assert.assertEquals(e2, this.g.getMainEvent());

        this.g.clearEvents();
        this.g.addEvents(e3);
        Assert.assertEquals(e3, this.g.getMainEvent());
        this.g.addEvents(e3);
        Assert.assertEquals(e3, this.g.getMainEvent());
        this.g.addEvents(e2);
        Assert.assertEquals(e3, this.g.getMainEvent());
    }

    @Test
    public void testAddEvents()
    {
        Assert.assertTrue(this.g.getEvents().isEmpty());
        Event e1 = new DefaultEvent();
        Assert.assertTrue(this.g.getEvents().isEmpty());
        this.g.addEvents(e1);
        Assert.assertFalse(this.g.getEvents().isEmpty());
        Assert.assertEquals(1, this.g.getEvents().size());
        this.g.addEvents(e1);
        Assert.assertEquals(1, this.g.getEvents().size());
        Event e2 = new DefaultEvent();
        this.g.addEvents(e2, e2, e2);
        Assert.assertEquals(2, this.g.getEvents().size());
        this.g.addEvents(e1, e2);
        Assert.assertEquals(2, this.g.getEvents().size());
        Event e3 = new DefaultEvent();
        this.g.addEvents(e1, e2, e3);
        Assert.assertEquals(3, this.g.getEvents().size());
        this.g.clearEvents();
        Assert.assertEquals(0, this.g.getEvents().size());
        Assert.assertTrue(this.g.getEvents().isEmpty());
        this.g.addEvents(e1, e2, e3);
        Assert.assertEquals(3, this.g.getEvents().size());
    }

    @Test
    public void testAddEventsWithNull()
    {
        this.g.addEvents((Event) null);
        Assert.assertTrue(this.g.getEvents().isEmpty());

        this.g.addEvents(new DefaultEvent());
        Assert.assertEquals(1, this.g.getEvents().size());
        this.g.addEvents((Event) null);
        Assert.assertEquals(1, this.g.getEvents().size());
    }

    @Test
    public void testClearEvents()
    {
        Assert.assertTrue(this.g.getEvents().isEmpty());
        this.g.addEvents(new DefaultEvent());
        this.g.addEvents(new DefaultEvent());
        this.g.addEvents(new DefaultEvent());
        Assert.assertFalse(this.g.getEvents().isEmpty());
        this.g.clearEvents();
        Assert.assertTrue(this.g.getEvents().isEmpty());
        this.g.clearEvents();
        Assert.assertTrue(this.g.getEvents().isEmpty());
        this.g.addEvents(new DefaultEvent());
        Assert.assertFalse(this.g.getEvents().isEmpty());
        this.g.clearEvents();
        Assert.assertTrue(this.g.getEvents().isEmpty());
    }

    @Test(expected = java.lang.UnsupportedOperationException.class)
    public void testGetEventsIsReadonly()
    {
        this.g.addEvents(new DefaultEvent(), new DefaultEvent());
        this.g.getEvents().clear();
        Assert.fail("No exception thrown");
    }

    @Test
    public void testGetEventsIsNotLive()
    {
        this.g.addEvents(new DefaultEvent(), new DefaultEvent());
        Set<Event> view = this.g.getEvents();
        Assert.assertEquals(2, view.size());
        this.g.addEvents(new DefaultEvent());
        Assert.assertEquals(2, view.size());
    }
}
