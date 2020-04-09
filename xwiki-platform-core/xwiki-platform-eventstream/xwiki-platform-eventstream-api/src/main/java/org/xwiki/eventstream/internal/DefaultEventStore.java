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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;

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

    private EventStore store;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            this.store = this.componentManager.getInstance(EventStore.class, this.configuration.getStore());
        } catch (ComponentLookupException e) {
            throw new InitializationException("Failed to get the configured store", e);
        }
    }

    @Override
    public void saveEvent(Event event) throws EventStreamException
    {
        this.store.saveEvent(event);
    }

    @Override
    public void deleteEvent(String eventId) throws EventStreamException
    {
        this.store.deleteEvent(eventId);
    }

    @Override
    public Event getEvent(String eventId) throws EventStreamException
    {
        return this.store.getEvent(eventId);
    }

    @Override
    public void saveEventStatus(EventStatus status) throws EventStreamException
    {
        this.store.saveEventStatus(status);
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
}
