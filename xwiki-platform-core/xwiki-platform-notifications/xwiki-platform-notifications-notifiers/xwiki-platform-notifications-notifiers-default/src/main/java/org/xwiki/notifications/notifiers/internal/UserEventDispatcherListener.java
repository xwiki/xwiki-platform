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
package org.xwiki.notifications.notifiers.internal;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;

/**
 * Receive events and dispatch them for each user.
 * 
 * @version $Id$
 * @since 12.1RC1
 */
@Component
@Singleton
@Named(UserEventDispatcherListener.NAME)
public class UserEventDispatcherListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.notifications.notifiers.internal.UserEventDispatcherListener";

    @Inject
    private UserEventDispatcher dispatcher;

    @Inject
    private NotificationConfiguration notificationConfiguration;

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private RemoteObservationManagerContext remoteState;

    @Inject
    private EventStore store;

    @Inject
    private Logger logger;

    /**
     * Configure the listener.
     */
    public UserEventDispatcherListener()
    {
        super(NAME, new EventStreamAddedEvent(), new ApplicationReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Don't do anything if notifications in general or pre filtering are disabled
        if (this.notificationConfiguration.isEnabled() && this.notificationConfiguration.isEventPrefilteringEnabled()) {
            if (event instanceof EventStreamAddedEvent) {
                // Find out the users to associate with the event
                if (!this.remoteState.isRemoteState()) {
                    prefilterEvent((org.xwiki.eventstream.Event) source);
                }
            } else if (event instanceof ApplicationReadyEvent) {
                // Load all the events for which pre filtering was not finished and restart the process for them
                // Don't block the event thread
                CompletableFuture.runAsync(this::prefilterMissingEvents);
            }
        }
    }

    private void prefilterMissingEvents()
    {
        SimpleEventQuery query = new SimpleEventQuery();
        query.eq(org.xwiki.eventstream.Event.FIELD_PREFILTERED, false);

        try {
            EventSearchResult result = this.store.search(query);

            result.stream().forEach(this::prefilterEvent);
        } catch (EventStreamException e) {
            this.logger.error("Failed to search events for which pre filtering was missed", e);
        }
    }

    private void prefilterEvent(org.xwiki.eventstream.Event eventStreamEvent)
    {
        try {
            // We can’t directly store a list of RecordableEventDescriptors as some of them can be
            // dynamically defined at runtime.
            List<RecordableEventDescriptor> descriptorList =
                this.recordableEventDescriptorManager.getRecordableEventDescriptors(true);

            // Try to match one of the given descriptors with the current event.
            for (RecordableEventDescriptor descriptor : descriptorList) {
                // Find a descriptor that corresponds to the given event
                if (descriptor.getEventType().equals(eventStreamEvent.getType())) {
                    // Add the event to the queue
                    this.dispatcher.addEvent(eventStreamEvent);

                    break;
                }
            }
        } catch (EventStreamException e) {
            this.logger.warn("Unable to retrieve a full list of RecordableEventDescriptor.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            this.logger.warn("Failed to add the event [{}] in the user event dispatcher", eventStreamEvent.getId(), e);
        }
    }
}
