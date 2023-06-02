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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.GroupingEventStrategy;
import org.xwiki.notifications.NotificationException;

/**
 * Implementation of {@link GroupingEventStrategy} that only grouped together event of the same type, regardless of
 * other information.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Named("applicationtype")
@Singleton
public class ApplicationTypeGroupingStrategy implements GroupingEventStrategy
{
    @Override
    public List<CompositeEvent> group(List<Event> eventList) throws NotificationException
    {
        Map<String, CompositeEvent> compositeEventMap = new HashMap<>();

        for (Event event : eventList) {
            String eventType = event.getType();
            if (compositeEventMap.containsKey(eventType)) {
                CompositeEvent compositeEvent = compositeEventMap.get(eventType);
                compositeEvent.add(event);
            } else {
                CompositeEvent compositeEvent = new CompositeEvent(event);
                compositeEventMap.put(eventType, compositeEvent);
            }
        }

        return new ArrayList<>(compositeEventMap.values());
    }

    @Override
    public void group(List<CompositeEvent> compositeEvents, List<Event> newEvents) throws NotificationException
    {
        Map<String, CompositeEvent> compositeEventMap = new HashMap<>();
        for (CompositeEvent compositeEvent : compositeEvents) {
            compositeEventMap.put(compositeEvent.getType(), compositeEvent);
        }
        for (Event event : newEvents) {
            String eventType = event.getType();
            if (compositeEventMap.containsKey(eventType)) {
                CompositeEvent compositeEvent = compositeEventMap.get(eventType);
                compositeEvent.add(event);
            } else {
                CompositeEvent compositeEvent = new CompositeEvent(event);
                compositeEventMap.put(eventType, compositeEvent);
                compositeEvents.add(compositeEvent);
            }
        }
    }
}
