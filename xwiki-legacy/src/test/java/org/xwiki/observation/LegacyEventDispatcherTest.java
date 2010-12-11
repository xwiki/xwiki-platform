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
package org.xwiki.observation;

import java.util.Collections;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.test.AbstractComponentTestCase;

public class LegacyEventDispatcherTest extends AbstractComponentTestCase
{

    private boolean hasBeenNotified;

    private ObservationManager om;

    @Before
    public void setUp() throws Exception
    {
        this.hasBeenNotified = false;
        this.om = this.getComponentManager().lookup(ObservationManager.class);
    }

    @Test
    public void testLegacyDocumentDeleteEventGetsDispatched() throws Exception
    {
        this.registerListenerWithLegacyEvent(new DocumentDeleteEvent());
        om.notify(new DocumentDeletedEvent(), new Object());
        // The notification is synchronous, so the following assertion will only be tested
        // once all matching event listeners have been notified.
        Assert.assertTrue("Should have been notified by legacy event dispatcher", this.hasBeenNotified);
    }

    @Test
    public void testLegacyDocumentSaveEventGetsDispatched() throws Exception
    {
        this.registerListenerWithLegacyEvent(new DocumentSaveEvent());
        om.notify(new DocumentCreatedEvent(), new Object());
        // The notification is synchronous, so the following assertion will only be tested
        // once all matching event listeners have been notified.
        Assert.assertTrue("Should have been notified by legacy event dispatcher", this.hasBeenNotified);
    }

    @Test
    public void testLegacyDocumentUpdateEventGetsDispatched() throws Exception
    {
        this.registerListenerWithLegacyEvent(new DocumentUpdateEvent());
        om.notify(new DocumentUpdatedEvent(), new Object());
        // The notification is synchronous, so the following assertion will only be tested
        // once all matching event listeners have been notified.
        Assert.assertTrue("Should have been notified by legacy event dispatcher", this.hasBeenNotified);
    }

    private void registerListenerWithLegacyEvent(final Event event)
    {
        this.om.addListener(new EventListener()
        {
            public String getName()
            {
                return "testLegacyEventDispatchEventListener";
            }

            public List<Event> getEvents()
            {
                return Collections.<Event> singletonList(event);
            }

            public void onEvent(Event event, Object source, Object data)
            {
                setNotified();
            }

        });
    }

    private void setNotified()
    {
        this.hasBeenNotified = true;
    }
}
