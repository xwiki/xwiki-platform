/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package org.xwiki.observation.internal;

import org.xwiki.observation.event.Event;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.EventListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DefaultObservationManager implements ObservationManager
{
    /**
     * Registered listeners.
     * The Map key is the Event class name and the values is a List with (Event, Listener) pairs.
     */
    private Map listeners = new HashMap();

    /**
     * {@inheritDoc}
     * @see ObservationManager#addListener(Event,org.xwiki.observation.EventListener)
     */
    public void addListener(Event event, EventListener eventListener)
    {
        // Check if this is a new Event not already registered
        List eventListeners = (List) this.listeners.get(event.getClass().getName());
        if (eventListeners == null) {
            eventListeners = new ArrayList();
            this.listeners.put(event.getClass().getName(),  eventListeners);
        }

        // Check to see if the event/listener pair is already there
        for (Iterator it = eventListeners.iterator(); it.hasNext(); ) {
            WeakReference reference = (WeakReference) it.next();
            Object[] pairs = (Object[]) reference.get();
            if (pairs[1] == eventListener) {
                return;
            }
        }

        // Add the event/listener pair
        eventListeners.add(new WeakReference(new Object[] {event, eventListener}));
    }

    /**
     * {@inheritDoc}
     * @see ObservationManager#removeListener(org.xwiki.observation.event.Event, EventListener)
     */
    public void removeListener(Event event, EventListener eventListener)
    {
        if (event == null) {
            removeListener(eventListener);
            return;
        }

        // Find the event key for the specified event
        List eventListeners = (List) this.listeners.get(event.getClass().getName());
        if (eventListeners == null) {
            return;
        }

        // Remove the listener
        for (Iterator it = eventListeners.iterator(); it.hasNext();) {
            WeakReference reference = (WeakReference) it.next();
            Object[] pairs = (Object[]) reference.get();
            if (pairs[1] == eventListener) {
                it.remove();
                break;
            }
        }

        // Remove the event as such if this was the last listener for this event
        if (eventListeners.size() == 0) {
            this.listeners.remove(event);
        }
    }

    /**
     * {@inheritDoc}
     * @see ObservationManager#removeListener(EventListener)
     */
    public void removeListener(EventListener eventListener)
    {
        // Loop over all registered events and remove the specified listener
        // Loop over a copy in case the removeListener() call wants to
        // remove the entire event from the map.
        List listeners = new ArrayList(this.listeners.keySet());
        for (Iterator it = listeners.iterator(); it.hasNext();) {
            Object[] pairs = (Object[]) it.next();
            if (pairs[1] == eventListener) {
                removeListener((Event) pairs[0], eventListener);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see ObservationManager#notify(org.xwiki.observation.event.Event, Object, Object)
     */
    public void notify(Event event, Object source, Object data)
    {
        // Find all event/listeners pairs for this event
        List eventListeners = (List) this.listeners.get(event.getClass().getName());
        if (eventListeners == null) {
            return;
        }

        // Loop over a copy of the list in case it is altered by a listener
        List copy = new ArrayList(eventListeners);
        for (Iterator it = copy.iterator(); it.hasNext();) {
            WeakReference reference = (WeakReference) it.next();
            Object[] pairs = (Object[]) reference.get();
            if (pairs[1] == null) {
                it.remove();
            } else {
                Event storedEvent = (Event) pairs[0];
                if (storedEvent.matches(event)) {
                    EventListener eventListener = (EventListener) pairs[1];
                    eventListener.onEvent(event, source, data);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see ObservationManager#notify(org.xwiki.observation.event.Event, Object)
     */
    public void notify(Event event, Object source)
    {
        notify(event, source, null);
    }
}
