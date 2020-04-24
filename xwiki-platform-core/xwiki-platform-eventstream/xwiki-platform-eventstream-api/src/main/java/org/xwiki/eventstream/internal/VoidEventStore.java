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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;

/**
 * Implementation of {@link EventStore}doing nothing.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Named(VoidEventStore.HINT)
@Singleton
public class VoidEventStore implements EventStore
{
    /**
     * The hint of the component.
     */
    public static final String HINT = "void";

    @Override
    public void saveEvent(Event event) throws EventStreamException
    {
        // Do nothing
    }

    @Override
    public Optional<Event> deleteEvent(String eventId) throws EventStreamException
    {
        // Do nothing
        return null;
    }

    @Override
    public void deleteEvent(Event event) throws EventStreamException
    {
        // Do nothing
    }

    @Override
    public Optional<Event> getEvent(String eventId) throws EventStreamException
    {
        // Do nothing
        return null;
    }

    @Override
    public void saveEventStatus(EventStatus status) throws EventStreamException
    {
        // Do nothing
    }

    @Override
    public void deleteEventStatus(EventStatus status) throws EventStreamException
    {
        // Do nothing
    }

    @Override
    public Optional<EventStatus> getEventStatus(String eventId, String entity) throws EventStreamException
    {
        // Do nothing
        return null;
    }

    @Override
    public EventSearchResult search(EventQuery query) throws EventStreamException
    {
        return EmptyEventSearchResult.EMPTY;
    }
}
