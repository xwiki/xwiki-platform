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

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;

/**
 * Handle the statuses for the events.
 *
 * @version $Id$
 * @since 9.2RC1
 * @deprecated use {@link EventStore} instead
 */
@Role
@Deprecated(since = "14.6-rc-1")
public interface EventStatusManager
{
    /**
     * Get the list of statuses concerning the given events and the given entities.
     *
     * @param events a list of events
     * @param entityIds a list of ids of entities (users and groups)
     * @return the list of statuses corresponding to each pair or event/entity
     * @throws Exception if an error occurs
     * @deprecated use {@link EventStore#getEventStatuses(java.util.Collection, java.util.Collection)} instead
     */
    @Deprecated(since = "14.6-rc-1")
    List<EventStatus> getEventStatus(List<Event> events, List<String> entityIds) throws Exception;

    /**
     * Save in the storage the given status.
     * 
     * @param eventStatus the status to save
     * @throws Exception if an error occurs
     * @deprecated since 12.3RC1, use {@link EventStore#saveEventStatus(EventStatus)} instead
     */
    @Deprecated
    void saveEventStatus(EventStatus eventStatus) throws Exception;

    /**
     * Delete from the storage the given status.
     * 
     * @param eventStatus the status to save
     * @throws Exception if an error occurs
     * @since 12.5RC1
     * @deprecated since 12.5RC1, use {@link EventStore#deleteEventStatus(EventStatus)} instead
     */
    @Deprecated
    default void deleteEventStatus(EventStatus eventStatus) throws Exception
    {

    }

    /**
     * @param startDate date before which to remove event status
     * @param entityId the id of the entity concerned by the status
     * @throws Exception if an error occurs
     * @since 12.1RC1
     * @deprecated since 12.6, use {@link EventStore#deleteEventStatuses(String, Date)} instead
     */
    @Deprecated
    default void deleteAllForEntity(Date startDate, String entityId) throws Exception
    {
    }
}
