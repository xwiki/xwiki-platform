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

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.xwiki.component.annotation.Role;

/**
 * Save and access store events.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Role
public interface EventStore
{
    /**
     * Asynchronously save in the store the given event.
     * 
     * @param event the event to save
     * @return the new {@link CompletableFuture} providing the added {@link Event}
     * @since 12.5RC1
     * @see #deleteEvent(Event)
     */
    CompletableFuture<Event> saveEvent(Event event);

    /**
     * Asynchronously save in the storage the given status.
     * 
     * @param status the status to save
     * @return the new {@link CompletableFuture} providing the added {@link EventStatus}
     * @since 12.5RC1
     * @see #deleteEventStatus(EventStatus)
     */
    CompletableFuture<EventStatus> saveEventStatus(EventStatus status);

    /**
     * Asynchronously save in the storage the given mail status.
     * 
     * @param event the status to save
     * @return the new {@link CompletableFuture} providing the added {@link EntityEvent}
     * @since 12.6
     * @see #deleteMailEntityEvent(EntityEvent)
     */
    CompletableFuture<EventStatus> saveMailEntityEvent(EntityEvent event);

    /**
     * Asynchronously deleted the event matching the passed identifier and all associated statuses.
     * 
     * @param eventId the unique identifier of the event
     * @return the new {@link CompletableFuture} providing the deleted {@link Event} or empty if none could be found
     * @since 12.5RC1
     * @see #saveEvent(Event)
     */
    CompletableFuture<Optional<Event>> deleteEvent(String eventId);

    /**
     * Asynchronously deleted the event. Do nothing if the passed event does not exist.
     * 
     * @param event the event to remove from the store
     * @return the new {@link CompletableFuture} providing the deleted {@link Event} or empty if none could be found
     * @since 12.5RC1
     * @see #saveEvent(Event)
     */
    CompletableFuture<Optional<Event>> deleteEvent(Event event);

    /**
     * Asynchronously delete from the store the given status.
     * 
     * @param status the status to delete
     * @return the new {@link CompletableFuture} providing the deleted {@link EventStatus} or empty if none could be
     *         found
     * @since 12.5RC1
     * @see #saveEventStatus(EventStatus)
     */
    CompletableFuture<Optional<EventStatus>> deleteEventStatus(EventStatus status);

    /**
     * Asynchronously delete from the store all the status associated with the passed entity and before the passed date.
     * 
     * @param entityId the id of the entity for which to remove the statuses
     * @param date the date before which to remove the statuses
     * @return the new {@link CompletableFuture} providing the deleted {@link EventStatus} or empty if none could be
     *         found
     * @since 12.6
     */
    CompletableFuture<Void> deleteEventStatuses(String entityId, Date date);

    /**
     * Asynchronously delete from the store the given mail status.
     * 
     * @param event the status to delete
     * @return the new {@link CompletableFuture} providing the deleted {@link EntityEvent} or empty if none could be
     *         found
     * @since 12.5RC1
     * @see #saveMailEntityEvent(EntityEvent)
     */
    CompletableFuture<Optional<EventStatus>> deleteMailEntityEvent(EntityEvent event);

    /**
     * Asynchronously update the event to indicate that it's been pre filtered.
     * 
     * @param event the event to update
     * @return the new {@link CompletableFuture} providing the updated {@link Event}
     * @since 12.6
     */
    CompletableFuture<Event> prefilterEvent(Event event);

    // Read

    /**
     * Get the event matching the passed identifier.
     * 
     * @param eventId the unique identifier of the event
     * @return the event corresponding to the passed id or null of none could be found
     * @throws EventStreamException when failing to get the event
     */
    Optional<Event> getEvent(String eventId) throws EventStreamException;

    /**
     * Search for event according to condition provided by the {@link EventQuery}.
     * 
     * @param query the query containing the filtering conditions
     * @return the result of the search
     * @throws EventStreamException when failing to execute the search
     */
    EventSearchResult search(EventQuery query) throws EventStreamException;

    /**
     * Search for event according to condition provided by the {@link EventQuery}.
     * 
     * @param query the query containing the filtering conditions
     * @param fields the fields included in the result, null or empty means all fields
     * @return the result of the search
     * @throws EventStreamException when failing to execute the search
     * @since 12.6
     */
    EventSearchResult search(EventQuery query, Set<String> fields) throws EventStreamException;

    /**
     * Get the list of statuses concerning the given events and the given entities.
     *
     * @param events a list of events
     * @param entityIds a list of ids of entities (users and groups)
     * @return the list of statuses corresponding to each pair or event/entity
     * @throws Exception if an error occurs
     * @since 14.6RC1
     */
    default List<EventStatus> getEventStatuses(Collection<Event> events, Collection<String> entityIds) throws Exception
    {
        return List.of();
    }
}
