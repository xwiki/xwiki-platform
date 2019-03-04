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
package org.xwiki.eventstream.store.internal;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.eventstream.events.AbstractEventStreamEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.AllEvent;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * Store the recordable event inside the event stream (except events that are already handled by the Activity Stream
 * implementation).
 *
 * @version $Id$
 * @since 11.1RC1
 */
@Component
@Singleton
@Named("RecordableEventListener")
public class RecordableEventListener extends AbstractEventListener
{
    private static final BeginFoldEvent IGNORED_EVENTS = otherEvent ->  otherEvent instanceof BeginFoldEvent;

    @Inject
    private EventStream eventStream;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private RecordableEventConverter defaultConverter;

    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @Inject
    private ObservationContext observationContext;

    @Inject
    private Logger logger;

    @Inject
    private Execution execution;

    /**
     * Construct a NotificationEventListener.
     */
    public RecordableEventListener()
    {
        super("RecordableEventListener", AllEvent.ALLEVENT);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (!(event instanceof RecordableEvent) || remoteObservationManagerContext.isRemoteState()
            || observationContext.isIn(IGNORED_EVENTS)) {
            return;
        }

        if (!this.execution.getContext().hasProperty(AbstractEventStreamEvent.EVENT_LOOP_CONTEXT_LOCK_PROPERTY)) {

            // Don't handle events that are already saved by the Activity Stream implementation.
            for (Event ignoredEvent : DocumentEventListener.LISTENER_EVENTS) {
                if (ignoredEvent.matches(event)) {
                    return;
                }
            }

            try {
                this.execution.getContext()
                        .setProperty(AbstractEventStreamEvent.EVENT_LOOP_CONTEXT_LOCK_PROPERTY, true);

                // Save the event in the event stream
                eventStream.addEvent(convertEvent(event, source, data));

            } catch (Exception e) {
                logger.warn("Failed to save the event [{}].", event.getClass().getCanonicalName(), e);
            } finally {
                this.execution.getContext()
                        .removeProperty(AbstractEventStreamEvent.EVENT_LOOP_CONTEXT_LOCK_PROPERTY);
            }
        }
    }

    /**
     * Convert an Event from the Observation module to an Event from the EventStream module, ready to be saved.
     */
    private org.xwiki.eventstream.Event convertEvent(Event event, Object source, Object data) throws Exception
    {
        for (RecordableEventConverter converter
                : componentManager.<RecordableEventConverter>getInstanceList(RecordableEventConverter.class)) {
            if (converter == defaultConverter) {
                continue;
            }
            for (RecordableEvent ev : converter.getSupportedEvents()) {
                if (ev.matches(event)) {
                    // Convert the event
                    return converter.convert((RecordableEvent) event, (String) source, data);
                }
            }
        }

        // Use the default notification converter if no other converter match the current event
        return defaultConverter.convert((RecordableEvent) event, (String) source, data);
    }
}
