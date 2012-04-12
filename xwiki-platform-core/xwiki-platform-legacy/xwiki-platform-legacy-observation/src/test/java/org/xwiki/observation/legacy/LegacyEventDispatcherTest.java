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
package org.xwiki.observation.legacy;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.FilterableEvent;
import org.xwiki.test.AbstractComponentTestCase;

@Deprecated
public class LegacyEventDispatcherTest extends AbstractComponentTestCase
{
    private Event receivedEvent;

    private ObservationManager om;

    @Override
    @Before
    public void setUp() throws Exception
    {
        this.om = getComponentManager().getInstance(ObservationManager.class);
    }

    @Test
    public void testLegacyDocumentDeleteEventGetsDispatched() throws Exception
    {
        this.registerListenerWithLegacyEvent(new DocumentDeleteEvent());
        this.om.notify(new DocumentDeletedEvent(new DocumentReference("wiki", "space", "name")), null);
        
        // The notification is synchronous, so the following assertion will only be tested
        // once all matching event listeners have been notified.
        Assert.assertNotNull("Should have been notified by legacy event dispatcher", this.receivedEvent);
        Assert.assertEquals("Wrong event filter", "wiki:space.name", ((FilterableEvent)this.receivedEvent).getEventFilter().getFilter());
    }

    @Test
    public void testLegacyDocumentSaveEventGetsDispatched() throws Exception
    {
        this.registerListenerWithLegacyEvent(new DocumentSaveEvent());
        this.om.notify(new DocumentCreatedEvent(new DocumentReference("wiki", "space", "name")), null);
        
        // The notification is synchronous, so the following assertion will only be tested
        // once all matching event listeners have been notified.
        Assert.assertNotNull("Should have been notified by legacy event dispatcher", this.receivedEvent);
        Assert.assertEquals("Wrong event filter", "wiki:space.name", ((FilterableEvent)this.receivedEvent).getEventFilter().getFilter());
    }

    @Test
    public void testLegacyDocumentUpdateEventGetsDispatched() throws Exception
    {
        this.registerListenerWithLegacyEvent(new DocumentUpdateEvent());
        this.om.notify(new DocumentUpdatedEvent(new DocumentReference("wiki", "space", "name")), null);

        // The notification is synchronous, so the following assertion will only be tested
        // once all matching event listeners have been notified.
        Assert.assertNotNull("Should have been notified by legacy event dispatcher", this.receivedEvent);
        Assert.assertEquals("Wrong event filter", "wiki:space.name", ((FilterableEvent)this.receivedEvent).getEventFilter().getFilter());
    }

    private void registerListenerWithLegacyEvent(final Event event)
    {
        this.om.addListener(new EventListener()
        {
            @Override
            public String getName()
            {
                return "testLegacyEventDispatchEventListener";
            }

            @Override
            public List<Event> getEvents()
            {
                return Collections.<Event> singletonList(event);
            }

            @Override
            public void onEvent(Event event, Object source, Object data)
            {
                receivedEvent = event;
            }
        });
    }
}
