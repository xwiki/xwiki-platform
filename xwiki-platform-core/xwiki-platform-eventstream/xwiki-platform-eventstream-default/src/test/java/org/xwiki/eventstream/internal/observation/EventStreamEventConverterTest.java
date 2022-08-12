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
package org.xwiki.eventstream.internal.observation;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.eventstream.events.MailEntityAddedEvent;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validate {@link EventStreamEventConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
class EventStreamEventConverterTest
{
    @MockComponent
    EventStore store;

    @InjectMockComponents(role = LocalEventConverter.class)
    EventStreamEventConverter converter;

    @Test
    void convertNoThing()
    {
        assertFalse(this.converter.toRemote(new LocalEventData(new LogEvent(), null, null), new RemoteEventData()));
        assertFalse(this.converter.fromRemote(new RemoteEventData(new LogEvent(), null, null), new LocalEventData()));
    }

    @Test
    void convertEmpty()
    {
        RemoteEventData remoteEvent = new RemoteEventData();

        assertTrue(this.converter.toRemote(new LocalEventData(new EventStreamAddedEvent(), null, null), remoteEvent));

        LocalEventData localEvent = new LocalEventData();

        assertTrue(this.converter.fromRemote(remoteEvent, localEvent));

        assertSame(EventStreamAddedEvent.class, localEvent.getEvent().getClass());
    }

    @Test
    void convertWithEvent() throws EventStreamException
    {
        DefaultEvent initialEvent = new DefaultEvent();
        initialEvent.setId("id");

        RemoteEventData remoteEvent = new RemoteEventData();

        assertTrue(
            this.converter.toRemote(new LocalEventData(new EventStreamAddedEvent(), initialEvent, null), remoteEvent));

        LocalEventData localEvent = new LocalEventData();

        DefaultEvent loadedEvent = new DefaultEvent();
        when(this.store.getEvent("id")).thenReturn(Optional.of(loadedEvent));

        assertTrue(this.converter.fromRemote(remoteEvent, localEvent));

        assertSame(EventStreamAddedEvent.class, localEvent.getEvent().getClass());
        assertNotNull(localEvent.getSource());
        assertSame(loadedEvent, localEvent.getSource());
    }

    @Test
    void convertWrongEventStreamEvent()
    {
        assertFalse(
            this.converter.toRemote(new LocalEventData(new MailEntityAddedEvent(), null, null), new RemoteEventData()));
        assertFalse(this.converter.fromRemote(new RemoteEventData(new MailEntityAddedEvent(), null, null),
            new LocalEventData()));

    }
}
