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
package org.xwiki.refactoring.internal.listener;

import javax.inject.Inject;

import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * Listeners for document events should only process local events to avoid processing twice the events.
 *
 * @version $Id$
 * @since 11.9RC1
 */
public abstract class AbstractDocumentEventListener extends AbstractEventListener
{
    @Inject
    private RemoteObservationManagerContext observationManagerContext;

    /**
     * Default constructor, see {@link AbstractEventListener}.
     *
     * @param name name of the listener.
     * @param events the events to listen to.
     */
    public AbstractDocumentEventListener(String name, Event... events)
    {
        super(name, events);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Only take into account local events
        if (!this.observationManagerContext.isRemoteState()) {
            processLocalEvent(event, source, data);
        }
    }

    /**
     * The method called when the listener is triggered on local event.
     * For more information see {@link org.xwiki.observation.EventListener#onEvent(Event, Object, Object)}.
     *
     * @param event the local event.
     * @param source the source of the event.
     * @param data the data of the event.
     */
    public abstract void processLocalEvent(Event event, Object source, Object data);
}
