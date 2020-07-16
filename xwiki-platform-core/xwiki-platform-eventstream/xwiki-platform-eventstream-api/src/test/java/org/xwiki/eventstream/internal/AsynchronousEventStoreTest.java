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

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link AbstractAsynchronousEventStore}.
 * 
 * @version $Id$
 */
@ComponentTest
public class AsynchronousEventStoreTest
{
    public static class TestAbstractAsynchronousEventStore extends AbstractAsynchronousEventStore
    {
        class EventEntry
        {
            DefaultEvent event;

            Map<String, EventStatus> statuses = new ConcurrentHashMap<>();

            Map<String, EntityEvent> mailstatuses = new ConcurrentHashMap<>();
        }

        Map<String, EventEntry> events = new ConcurrentHashMap<>();

        EventEntry getEventEntry(String eventId, boolean create)
        {
            EventEntry entry = this.events.get(eventId);

            if (entry == null && create) {
                entry = new EventEntry();
                this.events.put(eventId, entry);
            }

            return entry;
        }

        @Override
        public Optional<Event> getEvent(String eventId) throws EventStreamException
        {
            EventEntry entry = getEventEntry(eventId, false);

            return entry != null ? Optional.ofNullable(entry.event) : Optional.empty();
        }

        @Override
        public EventSearchResult search(EventQuery query) throws EventStreamException
        {
            // Not needed for the test
            return null;
        }

        @Override
        public void initialize() throws InitializationException
        {
            initialize(2, true, false);
        }

        @Override
        protected EventStatus syncSaveEventStatus(EventStatus status) throws EventStreamException
        {
            getEventEntry(status.getEvent().getId(), true).statuses.put(status.getEntityId(), status);

            return status;
        }

        @Override
        protected EntityEvent syncSaveMailEntityEvent(EntityEvent event) throws EventStreamException
        {
            getEventEntry(event.getEvent().getId(), true).mailstatuses.put(event.getEntityId(), event);

            return event;
        }

        @Override
        protected Event syncSaveEvent(Event event) throws EventStreamException
        {
            getEventEntry(event.getId(), true).event = (DefaultEvent) event;

            return event;
        }

        @Override
        protected Event syncPrefilterEvent(Event event) throws EventStreamException
        {
            DefaultEvent storeEvent = getEventEntry(event.getId(), true).event;

            storeEvent.setPrefiltered(true);

            return storeEvent;
        }

        @Override
        protected Optional<EventStatus> syncDeleteEventStatus(EventStatus status) throws EventStreamException
        {
            EventEntry entry = getEventEntry(status.getEvent().getId(), false);

            if (entry != null) {
                return Optional.ofNullable(entry.statuses.remove(status.getEntityId()));
            }

            return Optional.empty();
        }

        @Override
        protected Void syncDeleteEventStatuses(String entityId, Date date) throws EventStreamException
        {
            this.events.forEach((eventId, entry) -> {
                if (entry.event.getDate() != null && !entry.event.getDate().after(date)) {
                    entry.statuses.remove(entityId);
                }
            });

            return null;
        }

        @Override
        protected Optional<EntityEvent> syncDeleteMailEntityEvent(EntityEvent event) throws EventStreamException
        {
            EventEntry entry = getEventEntry(event.getEvent().getId(), false);

            if (entry != null) {
                return Optional.ofNullable(entry.mailstatuses.remove(event.getEntityId()));
            }

            return Optional.empty();
        }

        @Override
        protected Optional<Event> syncDeleteEvent(String eventId) throws EventStreamException
        {
            Optional<Event> event = getEvent(eventId);

            this.events.remove(eventId);

            return event;
        }

        @Override
        protected Optional<Event> syncDeleteEvent(Event event) throws EventStreamException
        {
            return syncDeleteEvent(event.getId());
        }
    }

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @InjectMockComponents
    private TestAbstractAsynchronousEventStore store;

    private DefaultEvent event(String id)
    {
        DefaultEvent event = new DefaultEvent();

        event.setId(id);

        return event;
    }

    private DefaultEventStatus eventStatus(DefaultEvent event, String entityId, boolean read)
    {
        return new DefaultEventStatus(event, entityId, read);
    }

    private DefaultEntityEvent entityEvent(DefaultEvent event, String entityId)
    {
        return new DefaultEntityEvent(event, entityId);
    }

    // Tests

    @Test
    void event() throws InterruptedException, ExecutionException, EventStreamException
    {
        DefaultEvent event1 = event("id1");
        DefaultEvent event2 = event("id2");

        this.store.saveEvent(event1);
        // Only wait for the completion of the last order since all the other are supposed to be completed before
        this.store.saveEvent(event2).get();

        assertSame(event1, this.store.getEvent(event1.getId()).get());
        assertSame(event2, this.store.getEvent(event2.getId()).get());

        this.store.deleteEvent(event1).get();

        assertFalse(this.store.getEvent(event1.getId()).isPresent());
        assertSame(event2, this.store.getEvent(event2.getId()).get());

        this.store.deleteEvent(event2.getId()).get();

        assertFalse(this.store.getEvent(event1.getId()).isPresent());
        assertFalse(this.store.getEvent(event2.getId()).isPresent());
    }

    @Test
    void eventstatus() throws InterruptedException, ExecutionException, EventStreamException
    {
        DefaultEvent event1 = event("id1");
        DefaultEvent event2 = event("id2");
        DefaultEventStatus status11 = eventStatus(event1, "entity1", true);
        DefaultEventStatus status12 = eventStatus(event1, "entity2", true);
        DefaultEventStatus status13 = eventStatus(event1, "entity3", true);
        DefaultEventStatus status21 = eventStatus(event2, "entity1", true);
        DefaultEventStatus status22 = eventStatus(event2, "entity2", true);
        DefaultEventStatus status23 = eventStatus(event2, "entity3", true);

        this.store.saveEvent(event1);
        this.store.saveEvent(event2);
        this.store.saveEventStatus(status11);
        this.store.saveEventStatus(status12);
        this.store.saveEventStatus(status13);
        this.store.saveEventStatus(status21);
        this.store.saveEventStatus(status22);
        // Only wait for the completion of the last order since all the other are supposed to be completed before
        this.store.saveEventStatus(status23).get();

        assertSame(status11, this.store.events.get(event1.getId()).statuses.get(status11.getEntityId()));
        assertSame(status12, this.store.events.get(event1.getId()).statuses.get(status12.getEntityId()));
        assertSame(status13, this.store.events.get(event1.getId()).statuses.get(status13.getEntityId()));
        assertSame(status21, this.store.events.get(event2.getId()).statuses.get(status21.getEntityId()));
        assertSame(status22, this.store.events.get(event2.getId()).statuses.get(status22.getEntityId()));
        assertSame(status23, this.store.events.get(event2.getId()).statuses.get(status23.getEntityId()));

        this.store.deleteEventStatus(status11).get();

        assertNull(this.store.events.get(event1.getId()).statuses.get(status11.getEntityId()));
        assertSame(status12, this.store.events.get(event1.getId()).statuses.get(status12.getEntityId()));
        assertSame(status13, this.store.events.get(event1.getId()).statuses.get(status13.getEntityId()));
        assertSame(status21, this.store.events.get(event2.getId()).statuses.get(status21.getEntityId()));
        assertSame(status22, this.store.events.get(event2.getId()).statuses.get(status22.getEntityId()));
        assertSame(status23, this.store.events.get(event2.getId()).statuses.get(status23.getEntityId()));
    }

    @Test
    void mailentity() throws InterruptedException, ExecutionException, EventStreamException
    {
        DefaultEvent event1 = event("id1");
        DefaultEvent event2 = event("id2");
        DefaultEntityEvent status11 = entityEvent(event1, "entity1");
        DefaultEntityEvent status13 = entityEvent(event1, "entity3");
        DefaultEntityEvent status21 = entityEvent(event2, "entity1");
        DefaultEntityEvent status24 = entityEvent(event2, "entity4");

        this.store.saveEvent(event1);
        this.store.saveEvent(event2);
        this.store.saveMailEntityEvent(status11);
        this.store.saveMailEntityEvent(status13);
        this.store.saveMailEntityEvent(status21);
        this.store.saveMailEntityEvent(status24).get();

        assertSame(status11, this.store.events.get(event1.getId()).mailstatuses.get(status11.getEntityId()));
        assertSame(status13, this.store.events.get(event1.getId()).mailstatuses.get(status13.getEntityId()));
        assertSame(status21, this.store.events.get(event2.getId()).mailstatuses.get(status21.getEntityId()));
        assertSame(status24, this.store.events.get(event2.getId()).mailstatuses.get(status24.getEntityId()));

        this.store.deleteMailEntityEvent(status11).get();

        assertNull(this.store.events.get(event1.getId()).mailstatuses.get(status11.getEntityId()));
        assertSame(status13, this.store.events.get(event1.getId()).mailstatuses.get(status13.getEntityId()));
        assertSame(status21, this.store.events.get(event2.getId()).mailstatuses.get(status21.getEntityId()));
        assertSame(status24, this.store.events.get(event2.getId()).mailstatuses.get(status24.getEntityId()));
    }

    @Test
    void prefilter() throws InterruptedException, ExecutionException, EventStreamException
    {
        DefaultEvent event1 = event("id1");
        DefaultEvent event2 = event("id2");

        this.store.saveEvent(event1);
        this.store.saveEvent(event2).get();

        assertFalse(this.store.getEvent(event1.getId()).get().isPrefiltered());
        assertFalse(this.store.getEvent(event2.getId()).get().isPrefiltered());

        this.store.prefilterEvent(event1).get();

        assertTrue(this.store.getEvent(event1.getId()).get().isPrefiltered());
        assertFalse(this.store.getEvent(event2.getId()).get().isPrefiltered());
    }
}
