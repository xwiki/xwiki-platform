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
package org.xwiki.eventstream;

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

/**
 * The stream of events, allowing to store and retrieve events.
 * 
 * @version $Id$
 * @since 3.0M2
 */
@Role
public interface EventStream
{
    /**
     * Add a new event to the storage.
     * 
     * @param e the event to store
     */
    void addEvent(Event e);

    /**
     * Search stored events. The query will be prefixed with a hardcoded {@code select event from Event as event} or
     * equivalent stub which selects actual events from the storage, so it must start with further {@code from} or
     * {@code where} statements.
     * 
     * @param query a query stub
     * @return the list of events matched by the query
     * @throws QueryException if the query is malformed or cannot be executed
     */
    List<Event> searchEvents(Query query) throws QueryException;

    /**
     * Retrieve the group that a given event is part of.
     * 
     * @param e the event to search for
     * @return the event's group of related events
     */
    EventGroup getRelatedEvents(Event e);

    /**
     * Delete an event from the storage. This method does not perform any rights check, it should be done before calling
     * this method.
     * 
     * @param e the event to delete
     */
    void deleteEvent(Event e);
}
