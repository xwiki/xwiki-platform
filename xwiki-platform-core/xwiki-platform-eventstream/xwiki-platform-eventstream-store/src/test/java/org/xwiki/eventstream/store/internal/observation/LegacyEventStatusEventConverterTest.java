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
package org.xwiki.eventstream.store.internal.observation;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.internal.events.EventStatusAddOrUpdatedEvent;
import org.xwiki.eventstream.store.internal.LegacyEvent;
import org.xwiki.eventstream.store.internal.LegacyEventLoader;
import org.xwiki.eventstream.store.internal.LegacyEventStatus;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.observation.remote.LocalEventData;
import org.xwiki.observation.remote.RemoteEventData;
import org.xwiki.observation.remote.converter.LocalEventConverter;
import org.xwiki.query.QueryException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Validate {@link LegacyEventStatusEventConverter}.
 * 
 * @version $Id$
 */
@ComponentTest
public class LegacyEventStatusEventConverterTest
{
    @MockComponent
    LegacyEventLoader loader;

    @InjectMockComponents(role = LocalEventConverter.class)
    LegacyEventStatusEventConverter converter;

    @Test
    public void convertNoThing()
    {
        assertFalse(this.converter.toRemote(new LocalEventData(new LogEvent(), null, null), new RemoteEventData()));
        assertFalse(this.converter.fromRemote(new RemoteEventData(new LogEvent(), null, null), new LocalEventData()));
    }

    @Test
    public void convertEmpty()
    {
        RemoteEventData remoteEvent = new RemoteEventData();

        assertTrue(
            this.converter.toRemote(new LocalEventData(new EventStatusAddOrUpdatedEvent(), null, null), remoteEvent));

        LocalEventData localEvent = new LocalEventData();

        assertTrue(this.converter.fromRemote(remoteEvent, localEvent));

        assertSame(EventStatusAddOrUpdatedEvent.class, localEvent.getEvent().getClass());
    }

    @Test
    public void convertWithEvent() throws QueryException
    {
        LegacyEvent initialEvent = new LegacyEvent();
        initialEvent.setEventId("id");
        LegacyEventStatus status = new LegacyEventStatus();
        status.setActivityEvent(initialEvent);
        status.setEntityId("entity");
        status.setRead(true);

        RemoteEventData remoteEvent = new RemoteEventData();

        assertTrue(
            this.converter.toRemote(new LocalEventData(new EventStatusAddOrUpdatedEvent(), status, null), remoteEvent));

        LocalEventData localEvent = new LocalEventData();

        LegacyEvent loadedEvent = new LegacyEvent();
        when(this.loader.getLegacyEvent("id")).thenReturn(loadedEvent);

        assertTrue(this.converter.fromRemote(remoteEvent, localEvent));

        assertSame(EventStatusAddOrUpdatedEvent.class, localEvent.getEvent().getClass());
        assertNotNull(localEvent.getSource());
        assertSame(loadedEvent, ((LegacyEventStatus) localEvent.getSource()).getActivityEvent());
        assertEquals("entity", ((LegacyEventStatus) localEvent.getSource()).getEntityId());
        assertTrue(((LegacyEventStatus) localEvent.getSource()).isRead());
    }
}
