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

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.observation.event.Event;

/**
 * The main orchestrator for event notification. To receive events create a component implementing the
 * {@link EventListener} interface. Your component will be automatically registered when this Observation
 * Manager component is loaded. To send events to all registered listeners, call one of the 
 * {@link #notify} methods.
 * 
 * @version $Id$
 */
@ComponentRole
public interface ObservationManager
{
    /**
     * Manually add a listener. Components implementing the {@link EventListener} interfaces are only loaded
     * when the Observation Manager component is created. Thus if you need to add a new listener while the
     * system is running you'll need to call this method.
     * 
     * Also note that contrary to other components it's not possible for the Observation Manager to watch
     * Component Manager events since the Component Manager is itself using the Observation Manager to send
     * its events (chicken and egg problem). Thus if any new Event Listener is created dynamically it needs to
     * be added using this method manually.
     * 
     * @param eventListener the listener to register
     */
    void addListener(EventListener eventListener);

    /**
     * Remove a listener from the list of registered listeners. The removed listener will no longer receive events.
     * 
     * @param listenerName the name of the listener to remove (must match {@link EventListener#getName()}
     */
    void removeListener(String listenerName);

    /**
     * Adds an Event to an already registered listener.
     * 
     * @param listenerName the name of the listener to which the event must be added 
     *        (must match {@link EventListener#getName()}
     * @param event the event to add to the matching listener
     */
    void addEvent(String listenerName, Event event);

    /**
     * Removes an Event to an already registered listener.
     * 
     * @param listenerName the name of the listener to which the event must be removed 
     *        (must match {@link EventListener#getName()}
     * @param event the event to remove to the matching listener
     */
    void removeEvent(String listenerName, Event event);
    
    /**
     * @param listenerName the name of the listener
     * @return the registered listener's instance or null if no listener is registered under that name 
     */
    EventListener getListener(String listenerName);
    
    /**
     * Call the registered listeners matching the passed Event. 
     * The definition of <em>source</em> and <em>data</em> is purely up to the communicating classes.
     * 
     * @param event the event to pass to the registered listeners
     * @param source the source of the event (or <code>null</code>)
     * @param data the additional data related to the event (or <code>null</code>)
     */
    void notify(Event event, Object source, Object data);

    /**
     * Convenience front-end where the additional data parameter is <code>null</code>.
     * 
     * @param event the event to pass to the registered listeners
     * @param source the source of the event (or <code>null</code>)
     * @see #notify(org.xwiki.observation.event.Event, Object, Object)
     */
    void notify(Event event, Object source);
}
