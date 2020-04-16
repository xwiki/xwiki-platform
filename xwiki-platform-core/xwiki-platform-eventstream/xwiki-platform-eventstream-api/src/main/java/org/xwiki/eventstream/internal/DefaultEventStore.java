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

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStatusManager;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.eventstream.events.EventStreamDeletedEvent;
import org.xwiki.observation.ObservationManager;
import org.xwiki.query.QueryException;

/**
 * The default implementation of {@link EventStore} dispatching the event in the various enabled stores.
 * 
 * @version $Id$
 * @since 12.3RC1
 */
@Component
public class DefaultEventStore implements EventStore, Initializable
{
    @Inject
    private EventStreamConfiguration configuration;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ObservationManager observationManager;

    private EventStream eventStream;

    private EventStatusManager eventStatusManager;

    private EventStore store;

    @Override
    public void initialize() throws InitializationException
    {
        if (this.configuration.isEventStoreEnabled()) {
            String hint = this.configuration.getEventStore();

            if (StringUtils.isNotEmpty(hint)) {
                try {
                    this.store =
                        this.componentManager.getInstance(EventStore.class, this.configuration.getEventStore());
                } catch (ComponentLookupException e) {
                    throw new InitializationException("Failed to get the configured store", e);
                }
            }
        }

        // retro compatibility: make sure to synchronize the old storage until the new store covers everything we
        // want it to cover
        if (this.componentManager.hasComponent(EventStream.class)) {
            try {
                this.eventStream = this.componentManager.getInstance(EventStream.class);
            } catch (ComponentLookupException e) {
                throw new InitializationException("Failed to get the legacy event stream", e);
            }
        }
        if (this.componentManager.hasComponent(EventStatusManager.class)) {
            try {
                this.eventStatusManager = this.componentManager.getInstance(EventStatusManager.class);
            } catch (ComponentLookupException e) {
                throw new InitializationException("Failed to get the legacy event status store", e);
            }
        }
    }

    @Override
    public void saveEvent(Event event) throws EventStreamException
    {
        if (this.store != null) {
            this.store.saveEvent(event);
        }

        if (this.eventStream != null) {
            this.eventStream.addEvent(event);
        }

        // Notify about this change
        this.observationManager.notify(new EventStreamAddedEvent(), event);
    }

    @Override
    public Event deleteEvent(String eventId) throws EventStreamException
    {
        Event event;
        if (this.store != null) {
            event = this.store.deleteEvent(eventId);
        } else {
            event = getEvent(eventId);
        }

        if (event != null) {
            if (this.eventStream != null) {
                this.eventStream.deleteEvent(event);
            }

            // Notify about this change
            this.observationManager.notify(new EventStreamDeletedEvent(), event);
        }

        return event;
    }

    @Override
    public void deleteEvent(Event event) throws EventStreamException
    {
        if (this.store != null) {
            this.store.deleteEvent(event);
        }

        if (this.eventStream != null) {
            this.eventStream.deleteEvent(event);
        }

        if (this.store != null || this.eventStream != null) {
            // Notify about this change
            this.observationManager.notify(new EventStreamDeletedEvent(), event);
        }
    }

    @Override
    public Event getEvent(String eventId) throws EventStreamException
    {
        Event event = null;

        // Try the new store
        if (this.store != null) {
            event = this.store.getEvent(eventId);
        }

        // Try the old store
        if (this.eventStream != null && event == null) {
            try {
                event = this.eventStream.getEvent(eventId);
            } catch (QueryException e) {
                throw new EventStreamException("Failed to get event from the old store", e);
            }
        }

        return event;
    }

    @Override
    public void saveEventStatus(EventStatus status) throws EventStreamException
    {
        if (this.store != null) {
            this.store.saveEventStatus(status);
        }

        if (this.eventStatusManager != null) {
            try {
                this.eventStatusManager.saveEventStatus(status);
            } catch (Exception e) {
                throw new EventStreamException("Failed to save the status in the old event store", e);
            }
        }
    }

    @Override
    public void deleteEventStatus(EventStatus status) throws EventStreamException
    {
        this.store.deleteEventStatus(status);
    }

    @Override
    public EventStatus getEventStatus(String eventId, String entity) throws EventStreamException
    {
        return this.store.getEventStatus(eventId, entity);
    }

    @Override
    public EventSearchResult search(EventQuery query) throws EventStreamException
    {
        return this.store.search(query);
    }
}
