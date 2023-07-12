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
package org.xwiki.notifications.notifiers.internal.email.live;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.concurrent.ExecutionContextRunnable;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationConfiguration;

/**
 * Dispatch events to users with live email notifications enabled.
 * 
 * @version $Id$
 * @since 12.6
 */
@Component(roles = PrefilteringLiveNotificationEmailDispatcher.class)
@Singleton
public class PrefilteringLiveNotificationEmailDispatcher implements Initializable, Disposable
{
    private static class QueueEntry
    {
        private final Event event;

        private final Set<DocumentReference> entities = ConcurrentHashMap.newKeySet();

        QueueEntry(Event event, DocumentReference userReference)
        {
            this.event = event;

            this.entities.add(userReference);
        }
    }

    @Inject
    private PrefilteringLiveNotificationEmailSender sender;

    @Inject
    private NotificationConfiguration notificationConfiguration;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    private ScheduledExecutorService processingService;

    private final BlockingQueue<QueueEntry> queue = new LinkedBlockingQueue<>();

    private long grace;

    @Override
    public void initialize() throws InitializationException
    {
        // Load the grace time present in the wiki configuration
        this.grace = 60000L * this.notificationConfiguration.liveNotificationsGraceTime();

        // Start the processing service
        this.processingService = Executors.newSingleThreadScheduledExecutor();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.processingService.shutdownNow();
    }

    /**
     * @param event the event to dispatch
     * @param userDocumentReference the reference of the user associated with this event
     */
    public void addEvent(Event event, DocumentReference userDocumentReference)
    {
        // Optimize a bit the queue to group users associated with the same event
        synchronized (this.queue) {
            for (QueueEntry entry : this.queue) {
                if (entry.event.getId().equals(event.getId())) {
                    entry.entities.add(userDocumentReference);

                    return;
                }
            }
        }

        QueueEntry entry = new QueueEntry(event, userDocumentReference);
        this.queue.add(entry);

        Instant instant = event.getDate().toInstant().plusMillis(this.grace);
        Duration duration = Duration.between(Instant.now(), instant);

        this.processingService.schedule(new ExecutionContextRunnable(this::dispatch, this.componentManager),
            duration.getSeconds(), TimeUnit.SECONDS);
    }

    private void dispatch()
    {
        // Remove the entry from the queue
        QueueEntry currentEntry;
        synchronized (this.queue) {
            currentEntry = this.queue.remove();
        }

        if (currentEntry.entities.isEmpty()) {
            return;
        }

        // Prepare a map of events to send per entity
        Map<DocumentReference, List<Event>> eventsToSend = new HashMap<>();
        currentEntry.entities.forEach(entity -> eventsToSend.put(entity, new ArrayList<>(List.of(currentEntry.event))));
        // Consume following events targeting same users
        this.queue.forEach(entry -> prepare(entry, eventsToSend));

        // Send mails
        if (!eventsToSend.isEmpty()) {
            this.sender.sendMails(eventsToSend);
        }
    }

    private void prepare(QueueEntry entry, Map<DocumentReference, List<Event>> eventsToSend)
    {
        entry.entities.forEach(entity -> {
            if (eventsToSend.containsKey(entity)) {
                List<Event> events = eventsToSend.get(entity);
                events.add(entry.event);
                entry.entities.remove(entity);
            }
        });
    }
}
