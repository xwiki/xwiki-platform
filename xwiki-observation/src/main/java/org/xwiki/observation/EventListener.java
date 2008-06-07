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

import org.xwiki.observation.event.Event;

/**
 * Any Java object wanting to receive {@link Event events} must implement this interface. For example, if you want to be
 * notified when a document is updated you'll implement this interface and you'll register the object implementing it
 * against the {@link ObservationManager}.
 * 
 * @version $Id$
 */
public interface EventListener
{
    /**
     * The {@link org.xwiki.observation.ObservationManager} calls this methods when an event matches the event passed
     * when this object was registered against the Observation Manager.
     * 
     * @param event the event triggered. Can be used to differentiate different events if your Object supports several
     *            events for example.
     * @param source the event source i.e. the object for which the event was triggered. For example this would be the
     *            document Object if the event is a document update event.
     * @param data some additional and optional data passed that can be acted on.
     */
    void onEvent(Event event, Object source, Object data);
}
