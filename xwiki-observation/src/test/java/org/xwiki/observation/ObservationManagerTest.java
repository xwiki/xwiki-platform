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
package org.xwiki.observation;

import java.util.Arrays;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.internal.DefaultObservationManager;

/**
 * Unit tests for {@link ObservationManager}.
 * 
 * @version $Id$
 */
public class ObservationManagerTest
{
    private ObservationManager manager;

    private Mockery context = new Mockery();

    @Before
    public void setUp()
    {
        this.manager = new DefaultObservationManager();
    }

    @After
    public void tearDown()
    {
        this.context.assertIsSatisfied();
    }
    
    @Test
    public void testNotifyWhenMatching()
    {
        final EventListener listener = this.context.mock(EventListener.class);
        final Event event = this.context.mock(Event.class);
        
        this.context.checking(new Expectations() {{
            allowing(listener).getName(); will(returnValue("mylistener"));
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            oneOf(listener).onEvent(event, "some source", "some data");
            oneOf(event).matches(event); will(returnValue(true));
        }});
        
        this.manager.addListener(listener);
        Assert.assertSame(listener, this.manager.getListener("mylistener"));
        this.manager.notify(event, "some source", "some data");
    }

    @Test
    public void testRemoveListener() 
    {
        final EventListener listener = this.context.mock(EventListener.class);
        final Event event = this.context.mock(Event.class);
        
        this.context.checking(new Expectations() {{
            allowing(listener).getName(); will(returnValue("mylistener"));
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(event)));
            never(listener).onEvent(with(any(Event.class)), with(any(Object.class)), with(any(Object.class)));
        }});
        
        this.manager.addListener(listener);
        this.manager.removeListener("mylistener");
        this.manager.notify(event, null);
    }

    @Test
    public void testAddEvent() throws Exception
    {
        final EventListener listener = this.context.mock(EventListener.class);
        final Event initialEvent = this.context.mock(Event.class, "initial");
        final Event afterEvent = this.context.mock(Event.class, "after");
        final Event notifyEvent = this.context.mock(Event.class, "notify");

        this.context.checking(new Expectations() {{
            allowing(listener).getName(); will(returnValue("mylistener"));
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(initialEvent)));
            oneOf(listener).onEvent(with(any(Event.class)), with(any(Object.class)), with(any(Object.class)));
            
            // Since the observation returns the first matching event, return false from initialEvent so that 
            // afterEvent is called.
            oneOf(initialEvent).matches(with(same(notifyEvent))); will(returnValue(false));
            oneOf(afterEvent).matches(with(same(notifyEvent))); will(returnValue(true));
        }});
        
        this.manager.addListener(listener);
        this.manager.addEvent("mylistener", afterEvent);
        this.manager.notify(notifyEvent, null);
    }

    @Test
    public void testRemoveEvent()
    {
        final EventListener listener = this.context.mock(EventListener.class);
        final Event initialEvent = this.context.mock(Event.class, "initial");
        final Event afterEvent = this.context.mock(Event.class, "after");
        final Event notifyEvent = this.context.mock(Event.class, "notify");

        this.context.checking(new Expectations() {{
            allowing(listener).getName(); will(returnValue("mylistener"));
            allowing(listener).getEvents(); will(returnValue(Arrays.asList(initialEvent)));
            
            // Since the observation returns the first matching event, return false from initialEvent so that 
            // the second event can be called (if there's a second event - in our case it'll be removed but
            // we still want the test to fail if that doesn't work).
            oneOf(initialEvent).matches(with(same(notifyEvent))); will(returnValue(false));
            
            // Ensure that the afterEvent is never called since we're adding it and removing it
            never(afterEvent);
        }});
        
        this.manager.addListener(listener);
        this.manager.addEvent("mylistener", afterEvent);
        this.manager.removeEvent("mylistener", afterEvent);
        this.manager.notify(notifyEvent, null);
    }
}
