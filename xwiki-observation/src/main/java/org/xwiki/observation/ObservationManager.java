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
 * The main orchestrator for event notification. You register {@link EventListener}s against this manager, create
 * {@link Event}s and pass them to this manager, and it notifies the right event listeners of the events they registered
 * for.
 * 
 * @version $Id$
 */
public interface ObservationManager
{
    /**
     * Add a listener.
     * 
     * @param event the event to register the listener against; acts as a template that filters out only specific events
     *            the istener is interested in
     * @param eventListener the listener to register
     */
    void addListener(Event event, EventListener eventListener);

    /**
     * Remove a listener from a specific event.
     * 
     * @param event the event to remove the listener from.
     * @param eventListener the listener to remove.
     */
    void removeListener(Event event, EventListener eventListener);

    /**
     * Remove a listener from all events it is registered by. Convenient way of cleaning up an listener object being
     * destroyed.
     * 
     * @param eventListener the listener to remove.
     */
    void removeListener(EventListener eventListener);

    /**
     * Call the registered listeners. The definition of <em>source</em> and <em>data</em> is purely up to the
     * communicating classes.
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
