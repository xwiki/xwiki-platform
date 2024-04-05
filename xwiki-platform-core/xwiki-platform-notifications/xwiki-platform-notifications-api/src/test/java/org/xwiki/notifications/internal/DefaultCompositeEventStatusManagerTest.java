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
package org.xwiki.notifications.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests ofr {@link DefaultCompositeEventStatusManager}
 *
 * @version $Id$
 */
@ComponentTest
public class DefaultCompositeEventStatusManagerTest
{
    @InjectMockComponents
    private DefaultCompositeEventStatusManager eventStatusManager;

    @MockComponent
    private EventStore eventStore;

    @Test
    void getCompositeEventStatusesWhenEmpty() throws Exception
    {
        List<CompositeEventStatus> statuses =
            this.eventStatusManager.getCompositeEventStatuses(Collections.emptyList(), null);
        assertTrue(statuses.isEmpty());
    }

    @Test
    void getCompositeEventStatusesMultipleLoops() throws Exception
    {
        List<CompositeEvent> compositeEvents = new ArrayList<>();

        // We have 55*5 events in total = 275 events
        // With batches of 50 events, we should perform 6 loops
        int numberOfCompositeEvents = 55;
        int numberOfEventsInEach = 5;


        for (int i = 0; i < numberOfCompositeEvents; i++) {
            CompositeEvent compositeEventI = mock(CompositeEvent.class, "compositeEvent" + i);
            List<Event> eventList = new ArrayList<>();
            for (int j = 0; j < numberOfEventsInEach; j++) {
                String eventId = String.format("event_%s_%s", i, j);
                Event eventJ = mock(Event.class, eventId);
                when(eventJ.getId()).thenReturn(eventId);
                eventList.add(eventJ);
            }
            when(compositeEventI.getEvents()).thenReturn(eventList);
            compositeEvents.add(compositeEventI);
        }

        String entityId = "XWiki.Foo";
        when(this.eventStore.getEventStatuses(any(), any())).then(invocationOnMock -> {
            List<Event> eventList = invocationOnMock.getArgument(0);
            List<String> entity = invocationOnMock.getArgument(1);
            assertTrue(eventList.size() == 50 || eventList.size() == 25);
            assertEquals(List.of(entityId), entity);
            EventStatus eventStatus = mock(EventStatus.class);
            when(eventStatus.getEvent()).thenReturn(eventList.get(0));
            return List.of(eventStatus);
        });

        List<CompositeEventStatus> compositeEventStatuses =
            this.eventStatusManager.getCompositeEventStatuses(compositeEvents, entityId);
        assertEquals(55, compositeEventStatuses.size());
        verify(this.eventStore, times(6)).getEventStatuses(any(), any());
    }
}
