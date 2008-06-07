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

import junit.framework.TestCase;

import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.filter.RegexEventFilter;
import org.xwiki.observation.internal.DefaultObservationManager;

public class ObservationManagerTest extends TestCase
{
    private ObservationManager manager;

    private TestListener listener;

    public class TestListener implements EventListener
    {
        public boolean hasListenerBeenCalled = false;

        public int listenerCalls = 0;

        public void onEvent(Event event, Object source, Object data)
        {
            assertEquals(DocumentSaveEvent.class.getName(), event.getClass().getName());
            assertEquals("some source", (String) source);
            assertEquals("some data", (String) data);
            this.hasListenerBeenCalled = true;
            this.listenerCalls++;
        }
    }

    @Override
    protected void setUp()
    {
        this.manager = new DefaultObservationManager();
        this.listener = new TestListener();
    }

    public void testNotifyWhenUsingDocumentSaveEvent()
    {
        this.manager.addListener(new DocumentSaveEvent("SomeDocument"), this.listener);
        this.manager.notify(new DocumentSaveEvent("SomeDocument"), "some source", "some data");
        assertTrue("Listener has not been called", this.listener.hasListenerBeenCalled);
    }

    public void testNotifyWhenUsingDocumentSaveEventWithRegexFilter()
    {
        this.manager.addListener(new DocumentSaveEvent(new RegexEventFilter(".*Doc.*")), this.listener);

        this.manager.notify(new DocumentSaveEvent("SomeDocument"), "some source", "some data");
        assertTrue("Listener has not been called", this.listener.hasListenerBeenCalled);
    }

    public void testRemoveListener()
    {
        this.manager.addListener(new DocumentUpdateEvent(), this.listener);
        this.manager.addListener(new DocumentSaveEvent("Some.Document"), this.listener);
        this.manager.addListener(new DocumentSaveEvent("Another.Document"), this.listener);
        this.manager.removeListener(this.listener);
        this.manager.notify(new DocumentSaveEvent("Some.Document"), "some source", "some data");
        assertFalse("Listener was not removed", this.listener.hasListenerBeenCalled);
        this.manager.addListener(new DocumentSaveEvent("Some.Document"), this.listener);
        this.manager.removeListener(new DocumentSaveEvent("Another.Document"), this.listener);
        this.manager.notify(new DocumentSaveEvent("Some.Document"), "some source", "some data");
        assertTrue("Listener was wrongly removed", this.listener.hasListenerBeenCalled);
    }

    public void testDoubleAdditionPrevented()
    {
        this.manager.addListener(new DocumentSaveEvent("Some.Document"), this.listener);
        this.manager.addListener(new DocumentSaveEvent("Some.Document"), this.listener);
        this.manager.addListener(new DocumentSaveEvent(), this.listener);
        this.manager.notify(new DocumentSaveEvent("Some.Document"), "some source", "some data");
        assertTrue("Listener was not called", this.listener.hasListenerBeenCalled);
        assertEquals("Listener was called too many times", 1, this.listener.listenerCalls);
    }

    public void testAlwaysMatchingEvents()
    {
        this.manager.addListener(new DocumentSaveEvent(), this.listener);
        this.manager.notify(new DocumentUpdateEvent("Some.Document"), "some source", "some data");
        assertFalse("Listener was wrongly called", this.listener.hasListenerBeenCalled);
        this.manager.notify(new DocumentSaveEvent("Some.Document"), "some source", "some data");
        assertTrue("Listener was not called", this.listener.hasListenerBeenCalled);
    }
}
