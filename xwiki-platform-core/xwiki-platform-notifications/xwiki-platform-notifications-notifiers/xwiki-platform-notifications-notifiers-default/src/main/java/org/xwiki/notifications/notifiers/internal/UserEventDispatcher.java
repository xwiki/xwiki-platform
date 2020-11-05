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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEntityEvent;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.user.UserManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.group.GroupException;
import org.xwiki.user.group.GroupManager;
import org.xwiki.user.internal.group.UsersCache;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Dispatch generated event to each user.
 * 
 * @version $Id$
 * @since 12.1RC1
 */
@Component(roles = UserEventDispatcher.class)
@Singleton
public class UserEventDispatcher implements Runnable, Disposable, Initializable
{
    private static final Event WAKEUP_EVENT = new DefaultEvent();

    private static final long LIVE_EVENT_INTERVAL = 5L * 60L * 1000L;

    protected boolean disposed;

    protected BlockingQueue<Event> priorityQueue;

    protected BlockingQueue<String> secondaryQueue;

    @Inject
    private UsersCache userCache;

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private UserEventManager userEventManager;

    @Inject
    private NotificationConfiguration notificationConfiguration;

    @Inject
    private ExecutionContextManager ecm;

    @Inject
    private DocumentReferenceResolver<String> resolver;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private GroupManager groupManager;

    @Inject
    private UserManager userManager;

    @Inject
    @Named("document")
    private UserReferenceResolver<DocumentReference> documentReferenceUserReferenceResolver;

    @Inject
    private EventStore events;

    @Inject
    private Logger logger;

    @Override
    public void initialize() throws InitializationException
    {
        // Live events (event which just been produced) are put in the priority queue to be dealt with faster and we put
        // events coming from some migration or restart in a larger queue but containing only the id to not flood the
        // live events queue. If the priority queue is full (very very active wiki or some mistake in a script) events
        // will fallback on the large queue. If both queue are full we just ignore the event and it will be picked up by
        // the next missing pre filtered event run.
        this.priorityQueue = new LinkedBlockingQueue<>(1000);
        this.secondaryQueue = new LinkedBlockingQueue<>(100000);

        // Start a background thread to filter and dispatch users events
        // Not making it a daemon thread because we don't want to loose events
        Thread thread = new Thread(this);
        thread.setName("User event dispatcher thread");
        thread.setPriority(Thread.NORM_PRIORITY - 1);
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // Mark the component as disposed
        this.disposed = true;

        // Make sure to wake the thread up to stop it
        wakeup();
    }

    private void wakeup()
    {
        if (this.priorityQueue != null && this.priorityQueue.isEmpty()) {
            // We don't need to wait since we just want to make sure that the queue is not empty
            this.priorityQueue.offer(WAKEUP_EVENT);
        }
    }

    /**
     * @param event the event to dispatch
     * @return true if the event was added to the queue, false if it was ignored (in which case it will generally
     *         retried later by {@link PrefilterMissingEventsJob})
     * @throws InterruptedException if interrupted while waiting
     */
    public boolean addEvent(Event event) throws InterruptedException
    {
        if (!this.disposed) {
            // Put in the priority queue only live events (event which just been produced)
            if (event.getDate() == null
                || (System.currentTimeMillis() - event.getDate().getTime()) >= LIVE_EVENT_INTERVAL
                || !this.priorityQueue.offer(event)) {
                return addEvent(event.getId(), false);
            }

            return true;
        }

        return false;
    }

    /**
     * @param eventId the identifier of the event to add in the queue
     * @param wait true if the event should always be added to the queue (and wait when the queue is full)
     * @return true if the event was added to the queue, false if it was ignored (in which case it will generally
     *         retried later by {@link PrefilterMissingEventsJob})
     * @throws InterruptedException if interrupted while waiting
     * @since 12.9RC1
     * @since 12.6.3
     */
    public boolean addEvent(String eventId, boolean wait) throws InterruptedException
    {
        boolean added;

        if (wait) {
            this.secondaryQueue.put(eventId);
            added = true;
        } else {
            added = this.secondaryQueue.offer(eventId);
        }

        if (added) {
            // Make sure to wake the thread up
            wakeup();
        }

        return added;
    }

    @Override
    public void run()
    {
        try {
            while (!this.disposed) {
                // Wait for available event
                Event event = this.priorityQueue.take();

                runEvents(event);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            this.logger.warn("User notification dispatched thread has been interrupted: {}",
                ExceptionUtils.getRootCauseMessage(e));
        }
    }

    private void runEvents(Event firstEvent)
    {
        Event currentEvent = firstEvent;

        while (!this.disposed && currentEvent != null) {
            if (currentEvent != WAKEUP_EVENT && !currentEvent.isPrefiltered()) {
                // Dispatch event
                dispatch(currentEvent);
            }

            // Get next priority event
            currentEvent = this.priorityQueue.poll();

            // If there is no next event in the priority queue try the large queue
            if (currentEvent == null) {
                String eventId = this.secondaryQueue.poll();
                if (!this.disposed && eventId != null) {
                    try {
                        currentEvent = this.events.getEvent(eventId).orElse(null);
                    } catch (EventStreamException e) {
                        this.logger.error("Failed to load event with id [{}]", eventId, e);
                    }
                }
            }
        }
    }

    /**
     * Associated an event with the users located in the event's wiki and in the main wiki or with explicitly targeted
     * users.
     * 
     * @param event the event to associate with the user
     * @since 12.9RC1
     * @since 12.6.3
     */
    public void dispatch(Event event)
    {
        // Keeping the same ExecutionContext forever can lead to memory leak and cache problems since
        // most
        // of the code expect it to be short lived
        try {
            this.ecm.pushContext(new ExecutionContext(), false);
        } catch (ExecutionContextException e) {
            this.logger.error("Failed to push a new execution context for event [{}]", event.getId(), e);

            return;
        }

        try {
            dispatchInContext(event);
        } catch (Exception e) {
            this.logger.error("Unexpected exception has been raised while dispatching event [{}]", event.getId(), e);
        } finally {
            // Get rid of current context
            this.ecm.popContext();
        }
    }

    private void dispatchInContext(Event event)
    {
        WikiReference eventWiki = event.getWiki();

        if (CollectionUtils.isNotEmpty(event.getTarget())) {
            // The event explicitly indicate with which entities to associated it

            boolean mailEnabled = this.notificationConfiguration.areEmailsEnabled();
            event.getTarget().forEach(entity -> {
                DocumentReference entityReference = this.resolver.resolve(entity, event.getWiki());
                UserReference userReference = this.documentReferenceUserReferenceResolver.resolve(entityReference);

                if (this.userManager.exists(userReference)) {
                    dispatch(event, entityReference, mailEnabled);
                } else {
                    // Also recursively associate the members of the entity if it's a group
                    try {
                        this.groupManager.getMembers(entityReference, true)
                            .forEach(userDocumentReference -> dispatch(event, userDocumentReference, mailEnabled));
                    } catch (GroupException e) {
                        this.logger.warn("Failed to get the member of the entity [{}]: {}", entity,
                            ExceptionUtils.getRootCauseMessage(e));
                    }
                }
                // Remember we are done pre filtering this event
                this.events.prefilterEvent(event);
            });
        } else {
            // Try to find users listening to this event

            // Associated event with event's wiki users
            dispatch(event, this.userCache.getUsers(eventWiki, true));

            // Also take into account global users (main wiki users) if the event is on a subwiki
            if (!this.wikiManager.isMainWiki(eventWiki.getName())) {
                dispatch(event, this.userCache.getUsers(new WikiReference(this.wikiManager.getMainWikiId()), true));
            }
        }
    }

    private void dispatch(Event event, DocumentReference user, boolean mailEnabled)
    {
        // Make sure the user asked to be alerted about this event
        if (this.userEventManager.isListening(event, user, NotificationFormat.ALERT)) {
            // Associate the event with the user
            String userId = this.entityReferenceSerializer.serialize(user);
            saveEventStatus(event, userId);
        }

        // Make sure the notification module is allowed to send mails
        // Make sure the user asked to receive mails about this event
        if (mailEnabled && this.userEventManager.isListening(event, user, NotificationFormat.EMAIL)) {
            // Associate the event with the user
            String userId = this.entityReferenceSerializer.serialize(user);
            saveMailEntityEvent(event, userId);
        }
    }

    private void dispatch(Event event, List<DocumentReference> users)
    {
        boolean mailEnabled = this.notificationConfiguration.areEmailsEnabled();

        for (DocumentReference user : users) {
            dispatch(event, user, mailEnabled);
        }

        // Remember we are done pre filtering this event
        this.events.prefilterEvent(event);
    }

    private void saveEventStatus(Event event, String entityId)
    {
        this.events.saveEventStatus(new DefaultEventStatus(event, entityId, false));
    }

    private void saveMailEntityEvent(Event event, String entityId)
    {
        this.events.saveMailEntityEvent(new DefaultEntityEvent(event, entityId));
    }
}
