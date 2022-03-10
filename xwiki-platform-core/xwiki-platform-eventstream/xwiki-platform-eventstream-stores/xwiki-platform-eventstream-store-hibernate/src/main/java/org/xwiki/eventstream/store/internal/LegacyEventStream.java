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

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventGroup;
import org.xwiki.eventstream.EventStream;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

/**
 * Default implementation for the {@link EventStream} that uses the old Activity Stream storage thanks to
 * {@link LegacyEvent} and {@link LegacyEventConverter}. The idea is to be backward compatible with the old Activity
 * Stream.
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Singleton
public class LegacyEventStream implements EventStream
{
    @Inject
    private LegacyEventLoader legacyEventLoader;

    @Inject
    private LegacyEventSaver legacyEventSaver;

    @Inject
    private LegacyEventDeleter legacyEventDeleter;

    @Override
    public void addEvent(Event event)
    {
        legacyEventSaver.saveEvent(event);
    }

    @Override
    public void deleteEvent(Event event)
    {
        legacyEventDeleter.deleteEvent(event);
    }

    @Override
    public EventGroup getRelatedEvents(Event event)
    {
        return legacyEventLoader.getRelatedEvents(event);
    }

    @Override
    public List<Event> searchEvents(Query query) throws QueryException
    {
        return legacyEventLoader.searchEvents(query);
    }

    @Override
    public Event getEvent(String eventId) throws QueryException
    {
        return legacyEventLoader.getEvent(eventId);
    }

    @Override
    public long countEvents() throws QueryException
    {
        return this.legacyEventLoader.countEvents();
    }
}
