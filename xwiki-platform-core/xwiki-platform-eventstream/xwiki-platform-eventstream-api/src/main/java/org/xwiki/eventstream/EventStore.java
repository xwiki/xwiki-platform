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

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.stability.Unstable;

/**
 * Save and access store events.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Role
@Unstable
public interface EventStore
{
    /**
     * Add a new event to the storage.
     * 
     * @param event the event to store
     * @throws EventStreamException when failing to save the event
     */
    void saveEvent(Event event) throws EventStreamException;

    /**
     * Deleted the event matching the passed identifier.
     * 
     * @param eventId the unique identifier of the event
     * @return the deleted event, null if none could be found
     * @throws EventStreamException when failing to delete the event
     */
    Optional<Event> deleteEvent(String eventId) throws EventStreamException;

    /**
     * Deleted the event. Do nothing if the passed event does not exist.
     * 
     * @param event the event to remove from the store
     * @throws EventStreamException when failing to delete the event
     */
    void deleteEvent(Event event) throws EventStreamException;

    /**
     * Get the event matching the passed identifier.
     * 
     * @param eventId the unique identifier of the event
     * @return the event corresponding to the passed id or null of none could be found
     * @throws EventStreamException when failing to get the event
     */
    Optional<Event> getEvent(String eventId) throws EventStreamException;

    /**
     * Save in the storage the given status.
     * 
     * @param status the status to save
     * @throws EventStreamException when failing to save the event status
     */
    void saveEventStatus(EventStatus status) throws EventStreamException;

    /**
     * Delete from the storage the given status.
     * 
     * @param status the status to delete
     * @throws EventStreamException when failing to delete the event status
     */
    void deleteEventStatus(EventStatus status) throws EventStreamException;

    /**
     * Get the event status identifier the passed event identifier and entity reference.
     * 
     * @param eventId the identifier of the event
     * @param entity the identifier of the listening entity
     * @return the event status or null if none could be found
     * @throws EventStreamException when failing to get the event status
     */
    Optional<EventStatus> getEventStatus(String eventId, String entity) throws EventStreamException;

    /**
     * Search for event according to condition provided by the {@link EventQuery}.
     * 
     * @param query the query containing the filtering conditions
     * @return the result of the search
     * @throws EventStreamException when failing to execute the search
     */
    EventSearchResult search(EventQuery query) throws EventStreamException;
}
