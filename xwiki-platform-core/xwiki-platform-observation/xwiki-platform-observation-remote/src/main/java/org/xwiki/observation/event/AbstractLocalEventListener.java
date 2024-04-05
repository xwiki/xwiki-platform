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
package org.xwiki.observation.event;

import java.util.List;

import javax.inject.Inject;

import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * This is a version of an {@link AbstractEventListener} that skips automatically all events that are remote:
 * only local events will be processed. This abstraction should be preferred over {@link AbstractEventListener} when
 * the remote events should be ignored.
 *
 * @version $Id$
 * @since 14.4RC1
 * @since 13.10.7
 */
public abstract class AbstractLocalEventListener extends AbstractEventListener
{
    @Inject
    protected RemoteObservationManagerContext remoteObservationManagerContext;

    /**
     * @param name the listener's name. It's a free form text identifying this listener instance in a unique manner.
     *            This name is used for some operations in {@link ObservationManager}.
     * @param events the list of events this listener is configured to receive. This listener will be automatically
     *            registered with this list of events against the {@link ObservationManager}. When an event occurs, for
     *            each matching event in this list, the {@link #onEvent(Event, Object, Object)} method will be called.
     */
    public AbstractLocalEventListener(String name, List<? extends Event> events)
    {
        super(name, events);
    }

    /**
     * @param name the listener's name. It's a free form text identifying this listener instance in a unique manner.
     *            This name is used for some operations in {@link ObservationManager}.
     * @param events the list of events this listener is configured to receive. This listener will be automatically
     *            registered with this list of events against the {@link ObservationManager}. When an event occurs, for
     *            each matching event in this list, the {@link #onEvent(Event, Object, Object)} method will be called.
     */
    public AbstractLocalEventListener(String name, Event... events)
    {
        super(name, events);
    }

    /**
     * @param name the listener's name. It's a free form text identifying this listener instance in a unique manner.
     *            This name is used for some operations in {@link ObservationManager}.
     * @param values the list of events this listener is configured to receive (values can be can be Event, Iterable of
     *            Event or Event[]). This listener will be automatically registered with this list of events against the
     *            {@link ObservationManager}. When an event occurs, for each matching event in this list, the
     *            {@link #onEvent(Event, Object, Object)} method will be called.
     */
    public AbstractLocalEventListener(String name, Object... values)
    {
        super(name, values);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!this.remoteObservationManagerContext.isRemoteState()) {
            this.processLocalEvent(event, source, data);
        }
    }

    /**
     * Handle the local events, see {@link #onEvent(Event, Object, Object)} for details.
     *
     * @param event the event triggered. Can be used to differentiate different events if your Object supports several
     *            events for example.
     * @param source the event source i.e. the object for which the event was triggered. For example this would be the
     *            document Object if the event is a document update event.
     * @param data some additional and optional data passed that can be acted on.
     */
    public abstract void processLocalEvent(Event event, Object source, Object data);
}
