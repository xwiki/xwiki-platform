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
package org.xwiki.notifications.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.CompositeEventStatus;
import org.xwiki.notifications.CompositeEventStatusManager;

/**
 * Default implementation of {@link CompositeEventStatusManager}.
 *
 * @version $Id$
 * @since 9.4RC1
 */
@Component
@Singleton
public class DefaultCompositeEventStatusManager implements CompositeEventStatusManager
{
    @Inject
    private EventStore eventStore;

    @Override
    public List<CompositeEventStatus> getCompositeEventStatuses(List<CompositeEvent> compositeEvents, String entityId)
        throws Exception
    {
        // Creating a list of all events to avoid multiple calls to getEventStatuses() and so multiple calls to the
        // database.
        List<Event> allEvents = new ArrayList<>();
        // But maintain a mapping between eventId and their composite event status
        Map<String, CompositeEventStatus> map = new HashMap<>();
        for (CompositeEvent compositeEvent : compositeEvents) {
            CompositeEventStatus compositeEventStatus = new CompositeEventStatus(compositeEvent);
            for (Event event : compositeEvent.getEvents()) {
                map.put(event.getId(), compositeEventStatus);
            }
            allEvents.addAll(compositeEvent.getEvents());
        }
        // Put the event statuses into the composite events statuses
        for (EventStatus eventStatus : getEventStatuses(allEvents, entityId)) {
            map.get(eventStatus.getEvent().getId()).add(eventStatus);
        }
        List<CompositeEventStatus> results = new ArrayList<>();
        // Keep the same order than inputs
        for (CompositeEvent event : compositeEvents) {
            results.add(map.get(event.getEvents().get(0).getId()));
        }
        return results;
    }

    private List<EventStatus> getEventStatuses(List<Event> events, String entityId) throws Exception
    {
        return this.eventStore.getEventStatuses(events, Arrays.asList(entityId));
    }
}
