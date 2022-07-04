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
package org.xwiki.notifications.script.internal;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.CompositeEventStatusManager;
import org.xwiki.notifications.script.NotificationScriptService;

/**
 * Helper for event related operations on the {@link NotificationScriptService}.
 * This helper limits the number of imports in {@link NotificationScriptService}.
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Component(roles = NotificationScriptEventHelper.class)
@Singleton
public class NotificationScriptEventHelper
{
    @Inject
    private EventStore eventStore;

    @Inject
    private CompositeEventStatusManager compositeEventStatusManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Just as {@link NotificationScriptService#getEventStatuses(List)}, get the list of statuses concerning the given
     * events and the current user.
     *
     * @param events a list of events
     * @return the list of statuses corresponding to each pair or event/entity
     *
     * @throws Exception if an error occurs
     */
    public List<EventStatus> getEventStatuses(List<Event> events) throws Exception
    {
        return this.eventStore.getEventStatuses(events,
            Arrays.asList(entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference())));
    }

    /**
     * Get the list of statuses concerning the given composite events and the current user.
     * See {@link NotificationScriptService#getCompositeEventStatuses(List)} for more details.
     *
     * @param compositeEvents a list of composite events
     * @return the list of statuses corresponding to each pair or event/entity
     *
     * @throws Exception if an error occurs
     */
    public List<CompositeEventStatus> getCompositeEventStatuses(List<CompositeEvent> compositeEvents) throws Exception
    {
        return compositeEventStatusManager.getCompositeEventStatuses(compositeEvents,
                entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference()));
    }

    /**
     * Save a status for the current user.
     * See {@link NotificationScriptService#saveEventStatus(String, boolean)} for more details.
     *
     * @param eventId id of the event
     * @param isRead either or not the current user has read the given event
     * @throws Exception if an error occurs
     */
    public void saveEventStatus(String eventId, boolean isRead) throws Exception
    {
        DefaultEvent event = new DefaultEvent();
        event.setId(eventId);
        String userId = entityReferenceSerializer.serialize(documentAccessBridge.getCurrentUserReference());
        this.eventStore.saveEventStatus(new DefaultEventStatus(event, userId, isRead));
    }

    /**
     * Remove all event status entries associated with current user.
     *
     * @param startDate date before which to remove event status
     * @throws Exception if an error occurs
     * @since 12.1RC1
     */
    public void clearAllStatus(Date startDate) throws Exception
    {
        String userId = this.entityReferenceSerializer.serialize(this.documentAccessBridge.getCurrentUserReference());
        this.eventStore.deleteEventStatuses(userId, startDate);
    }
}
