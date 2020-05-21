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
package org.xwiki.eventstream.internal;

import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.EventStatusManager;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.query.QueryException;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link DefaultEventStore}.
 * 
 * @version $Id$
 */
@ComponentTest
public class DefaultEventStoreTest
{
    private static final DefaultEvent EVENT = new DefaultEvent();

    private static final DefaultEventStatus EVENTSTATUS = new DefaultEventStatus(EVENT, "entity", true);

    @MockComponent
    private EventStreamConfiguration configuration;

    @MockComponent
    private EventStream eventStream;

    @MockComponent
    private EventStatusManager eventStatusManager;

    @MockComponent
    @Named("test")
    private EventStore store;

    @InjectMockComponents
    private DefaultEventStore defaultStore;

    @AfterComponent
    public void afterComponent()
    {
        when(this.configuration.isEventStoreEnabled()).thenReturn(true);
        when(this.configuration.getEventStore()).thenReturn("test");
    }

    @Test
    void saveEvent() throws EventStreamException
    {
        this.defaultStore.saveEvent(EVENT);

        verify(this.store).saveEvent(EVENT);
        verify(this.eventStream).addEvent(EVENT);
    }

    @Test
    void saveEventStatus() throws Exception
    {
        this.defaultStore.saveEventStatus(EVENTSTATUS);

        verify(this.store).saveEventStatus(EVENTSTATUS);
        verify(this.eventStatusManager).saveEventStatus(EVENTSTATUS);
    }

    @Test
    void deleteEventByInstance() throws EventStreamException
    {
        this.defaultStore.deleteEvent(EVENT);

        verify(this.store).deleteEvent(EVENT);
        verify(this.eventStream).deleteEvent(EVENT);
    }

    @Test
    void deleteEventstatus() throws EventStreamException
    {
        this.defaultStore.deleteEventStatus(EVENTSTATUS);

        verify(this.store).deleteEventStatus(EVENTSTATUS);
    }

    @Test
    void deleteEventById() throws EventStreamException
    {
        assertFalse(this.defaultStore.deleteEvent("id").isPresent());

        verify(this.store).deleteEvent("id");

        when(this.store.deleteEvent("id")).thenReturn(Optional.of(EVENT));

        assertSame(EVENT, this.defaultStore.deleteEvent("id").get());

        verify(this.store, times(2)).deleteEvent("id");
        verify(this.eventStream).deleteEvent(EVENT);
    }

    @Test
    void getEvent() throws EventStreamException, QueryException
    {
        assertFalse(this.defaultStore.getEvent("id").isPresent());

        when(this.store.getEvent("id")).thenReturn(Optional.of(EVENT));

        assertSame(EVENT, this.defaultStore.getEvent("id").get());

        when(this.store.getEvent("id")).thenReturn(Optional.empty());

        assertFalse(this.defaultStore.getEvent("id").isPresent());

        when(this.eventStream.getEvent("id")).thenReturn(EVENT);

        assertSame(EVENT, this.defaultStore.getEvent("id").get());
    }

    @Test
    void getEventStatus() throws EventStreamException, QueryException
    {
        assertFalse(this.defaultStore.getEventStatus("id", "entity").isPresent());

        when(this.store.getEventStatus("id", "entity")).thenReturn(Optional.of(EVENTSTATUS));

        assertSame(EVENTSTATUS, this.defaultStore.getEventStatus("id", "entity").get());
    }
}
