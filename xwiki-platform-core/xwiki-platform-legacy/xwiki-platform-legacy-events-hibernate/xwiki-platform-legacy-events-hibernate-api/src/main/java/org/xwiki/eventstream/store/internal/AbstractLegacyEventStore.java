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
package org.xwiki.eventstream.store.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStatusManager;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.AbstractAsynchronousEventStore;
import org.xwiki.eventstream.internal.EmptyEventSearchResult;
import org.xwiki.query.QueryException;

/**
 * The base implementation of {@link EventStore} for {@link LegacyEvent}.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public abstract class AbstractLegacyEventStore extends AbstractAsynchronousEventStore
{
    @Inject
    private EventStream eventStream;

    @Inject
    private EventStatusManager eventStatusManager;

    @Override
    protected Event syncSaveEvent(Event event) throws EventStreamException
    {
        this.eventStream.addEvent(event);

        return event;
    }

    @Override
    protected Optional<Event> syncDeleteEvent(String eventId) throws EventStreamException
    {
        Optional<Event> existingEvent = getEvent(eventId);

        if (existingEvent.isPresent()) {
            this.eventStream.deleteEvent(existingEvent.get());
        }

        return existingEvent;
    }

    @Override
    protected Optional<Event> syncDeleteEvent(Event event) throws EventStreamException
    {
        Optional<Event> existingEvent = getEvent(event.getId());

        if (existingEvent.isPresent()) {
            this.eventStream.deleteEvent(event);
        }

        return existingEvent;
    }

    @Override
    protected EventStatus syncSaveEventStatus(EventStatus status) throws EventStreamException
    {
        try {
            this.eventStatusManager.saveEventStatus(status);
        } catch (Exception e) {
            throw new EventStreamException("Failed to save the status in the old event store", e);
        }

        return status;
    }

    @Override
    protected EntityEvent syncSaveMailEntityEvent(EntityEvent event) throws EventStreamException
    {
        // Unsupported
        return event;
    }

    @Override
    protected Optional<EventStatus> syncDeleteEventStatus(EventStatus status) throws EventStreamException
    {
        try {
            this.eventStatusManager.deleteEventStatus(status);
        } catch (Exception e) {
            throw new EventStreamException("Failed to delete the status from the old event store", e);
        }

        return Optional.of(status);
    }

    @Override
    protected Void syncDeleteEventStatuses(String entityId, Date date) throws EventStreamException
    {
        try {
            this.eventStatusManager.deleteAllForEntity(date, entityId);
        } catch (Exception e) {
            throw new EventStreamException("Failed to delete the statuses from the old event store", e);
        }

        return null;
    }

    @Override
    public List<EventStatus> getEventStatuses(Collection<Event> events, Collection<String> entityIds) throws Exception
    {
        return this.eventStatusManager.getEventStatus(
            events instanceof List ? (List<Event>) events : new ArrayList<>(events),
            events instanceof List ? (List<String>) entityIds : new ArrayList<>(entityIds));
    }

    @Override
    protected Optional<EntityEvent> syncDeleteMailEntityEvent(EntityEvent event) throws EventStreamException
    {
        // Unsupported
        return Optional.empty();
    }

    @Override
    protected Event syncPrefilterEvent(Event event) throws EventStreamException
    {
        // Unsupported
        return event;
    }

    @Override
    public Optional<Event> getEvent(String eventId) throws EventStreamException
    {
        try {
            return Optional.ofNullable(this.eventStream.getEvent(eventId));
        } catch (QueryException e) {
            throw new EventStreamException("Failed to get event from the old store", e);
        }
    }

    @Override
    public EventSearchResult search(EventQuery query) throws EventStreamException
    {
        // TODO
        return EmptyEventSearchResult.INSTANCE;
    }

    @Override
    public EventSearchResult search(EventQuery query, Set<String> fields) throws EventStreamException
    {
        // TODO
        return EmptyEventSearchResult.INSTANCE;
    }
}
