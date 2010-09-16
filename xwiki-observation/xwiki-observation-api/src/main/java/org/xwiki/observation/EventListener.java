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
 *
 */
package org.xwiki.observation;

import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.observation.event.Event;

/**
 * Components wanting to receive Observation {@link Event events} must implement this interface.
 * 
 * @version $Id$
 */
@ComponentRole
public interface EventListener
{
    /**
     * @return the listener's name. It's a free form text identifying this listener instance in a unique manner.
     *         This name is used for some operations in {@link ObservationManager}.
     */
    String getName();
    
    /**
     * @return the list of events this listener is configured to receive. This listener will be automatically
     *         registered with this list of events against the {@link ObservationManager}. When an event occurs, 
     *         for each matching event in this list, the {@link #onEvent(Event, Object, Object)} method will be
     *         called. 
     */
    List<Event> getEvents();
    
    /**
     * The {@link org.xwiki.observation.ObservationManager} calls this method when an event matches one of the events
     * for which this listener is registered (see {@link #getEvents()}.
     * 
     * @param event the event triggered. Can be used to differentiate different events if your Object supports several
     *            events for example.
     * @param source the event source i.e. the object for which the event was triggered. For example this would be the
     *            document Object if the event is a document update event.
     * @param data some additional and optional data passed that can be acted on.
     */
    void onEvent(Event event, Object source, Object data);
}
