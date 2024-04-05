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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.internal.DefaultEntityEvent;
import org.xwiki.eventstream.internal.DefaultEventStatus;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.eventstream.query.SortableEventQuery.SortClause.Order;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.internal.DeletedDocumentCleanUpFilterProcessingQueue;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.user.UserException;
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
public class UserEventDispatcher
{
    private static final long BATCH_SIZE = 100;

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
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private RemoteObservationManagerConfiguration remoteObservation;

    @Inject
    private DeletedDocumentCleanUpFilterProcessingQueue cleanUpFilterProcessingQueue;

    @Inject
    private Logger logger;

    private Set<String> getSupportedEventTypes() throws EventStreamException
    {
        List<RecordableEventDescriptor> descriptorList =
            this.recordableEventDescriptorManager.getRecordableEventDescriptors(true);

        return descriptorList.stream().map(RecordableEventDescriptor::getEventType).collect(Collectors.toSet());
    }

    /**
     * Pre-filter any waiting event.
     * 
     * @throws Exception when failing
     */
    public void flush() throws Exception
    {
        // Get supported events types
        Set<String> types = getSupportedEventTypes();

        // Create a search request for events produced by the current instance which haven't been pre-filtered yet
        SimpleEventQuery query = new SimpleEventQuery();
        // Only events which haven't been pre-filtered already
        query.eq(Event.FIELD_PREFILTERED, false);
        query.open();
        // Events produced by this instance
        query.eq(Event.FIELD_REMOTE_OBSERVATION_ID, this.remoteObservation.getId());
        query.or();
        // Or some old events not containing the remote observation id (or some other reason causing it to be null)
        query.eq(Event.FIELD_REMOTE_OBSERVATION_ID, null);
        query.close();
        // Start by oldest events
        query.addSort(Event.FIELD_DATE, Order.ASC);
        // Limit the result to BATCH_SIZE events to not impact the memory too much
        query.setLimit(BATCH_SIZE);

        // Events to ignore
        List<String> failedEvents = new ArrayList<>();
        query.not().in(Event.FIELD_ID, failedEvents);

        // Keep getting the BATCH_SIZE oldest not pre-filtered events (except the handled ones) until we cannot find any
        // left
        do {
            try (EventSearchResult result = this.events.search(query)) {
                if (result.getSize() == 0) {
                    break;
                }

                // Pre-filter all the found events
                Iterable<Event> it = () -> result.stream().iterator();
                for (Event event : it) {
                    try {
                        CompletableFuture<?> completableFuture = prefilterEvent(event, types);
                        completableFuture.join();
                    } catch (Exception e) {
                        this.logger.warn("Failed to pre filter event with id [{}]: {}", event.getId(),
                            ExceptionUtils.getRootCauseMessage(e));
                        // Remember the failed event to not query it again
                        failedEvents.add(event.getId());
                    }
                }
            }
        } while (true);
    }

    private CompletableFuture<?> prefilterEvent(Event event, Set<String> types) throws EventStreamException
    {
        if (types.contains(event.getType())) {
            return dispatch(event);
        } else {
            // Remember this event does not need to be pre-filtered
            return this.events.prefilterEvent(event);
        }
    }

    /**
     * Associate an event with the users located in the event's wiki and in the main wiki or with explicitly targeted
     * users.
     * 
     * @param event the event to associate with the user
     * @throws EventStreamException when failing to pre filter the event
     */
    private CompletableFuture<?> dispatch(Event event) throws EventStreamException
    {
        // Keeping the same ExecutionContext forever can lead to memory leak and cache problems since
        // most of the code expect it to be short lived
        try {
            this.ecm.pushContext(new ExecutionContext(), false);
        } catch (ExecutionContextException e) {
            throw new EventStreamException("Failed to push a new execution context", e);
        }

        try {
            return dispatchInContext(event);
        } finally {
            // Get rid of current context
            this.ecm.popContext();
        }
    }

    private CompletableFuture<?> dispatchInContext(Event event)
    {
        CompletableFuture<?> result = new CompletableFuture<>();
        WikiReference eventWiki = event.getWiki();

        if (CollectionUtils.isNotEmpty(event.getTarget())) {
            // The event explicitly indicate with which entities the event is associated with

            boolean mailEnabled = this.notificationConfiguration.areEmailsEnabled();
            for (String entity : event.getTarget()) {
                DocumentReference entityReference = this.resolver.resolve(entity, event.getWiki());
                UserReference userReference = this.documentReferenceUserReferenceResolver.resolve(entityReference);

                try {
                    if (this.userManager.exists(userReference)) {
                        dispatch(event, entityReference, mailEnabled);
                    } else {
                        // Also recursively associate the members of the entity if it's a group
                        this.groupManager.getMembers(entityReference, true)
                            .forEach(userDocumentReference -> dispatch(event, userDocumentReference, mailEnabled));
                    }
                } catch (UserException e) {
                    this.logger.warn("Failed to verify if user [{}] exists. Cause: [{}]", userReference,
                        ExceptionUtils.getRootCauseMessage(e));
                } catch (GroupException e) {
                    this.logger.warn("Failed to get the member of the entity [{}]: {}", entity,
                        ExceptionUtils.getRootCauseMessage(e));
                }
            }

            // Remember we are done pre filtering this event
            result = this.events.prefilterEvent(event);
        } else {
            // Try to find users listening to this event

            // Associated event with event's wiki users
            result = dispatch(event, this.userCache.getUsers(eventWiki, true));

            // Also take into account global users (main wiki users) if the event is on a subwiki
            if (!this.wikiManager.isMainWiki(eventWiki.getName())) {
                List<DocumentReference> userList =
                    this.userCache.getUsers(new WikiReference(this.wikiManager.getMainWikiId()), true);
                result = dispatch(event, userList);
            }
        }
        return result;
    }

    private CompletableFuture<?> dispatch(Event event, DocumentReference user, boolean mailEnabled)
    {
        // Get the entity id
        String entityId = this.entityReferenceSerializer.serialize(user);
        CompletableFuture<?> result = new CompletableFuture<>();

        // Make sure the event is not already pre filtered
        // Make sure the user asked to be alerted about this event
        if (!isStatusPrefiltered(event, entityId)
            && this.userEventManager.isListening(event, user, NotificationFormat.ALERT)) {
            // Associate the event with the user
            result = saveEventStatus(event, entityId);
        }

        // Make sure the notification module is allowed to send mails
        // Make sure the event is not already pre filtered
        // Make sure the user asked to receive mails about this event
        if (mailEnabled && !isMailPrefiltered(event, entityId)
            && this.userEventManager.isListening(event, user, NotificationFormat.EMAIL)) {
            // Associate the event with the user
            result = saveMailEntityEvent(event, entityId);
        }

        // FIXME: reuse constant from EventType once it's moved (see https://jira.xwiki.org/browse/XWIKI-21669)
        if (StringUtils.equals(event.getType(), "delete")) {
            this.cleanUpFilterProcessingQueue.addCleanUpTask(user, event.getDocument());
        }

        return result;
    }

    private boolean isStatusPrefiltered(Event event, String entityId)
    {
        return isPrefiltered(event, entityId, false);
    }

    private boolean isMailPrefiltered(Event event, String entityId)
    {
        return isPrefiltered(event, entityId, true);
    }

    private boolean isPrefiltered(Event event, String entityId, boolean mail)
    {
        SimpleEventQuery eventQuery = new SimpleEventQuery(0, 0);

        eventQuery.eq(Event.FIELD_ID, event.getId());

        if (mail) {
            eventQuery.withMail(entityId);
        } else {
            eventQuery.withStatus(entityId);
        }

        try (EventSearchResult result = this.events.search(eventQuery)) {
            return result.getTotalHits() > 0;
        } catch (Exception e) {
            this.logger.error("Failed to check status for event [{}] and entity [{}]", event.getId(), entityId, e);

            return false;
        }
    }

    private CompletableFuture<?> dispatch(Event event, List<DocumentReference> users)
    {
        boolean mailEnabled = this.notificationConfiguration.areEmailsEnabled();

        for (DocumentReference user : users) {
            dispatch(event, user, mailEnabled);
        }

        // Remember we are done pre filtering this event
        return this.events.prefilterEvent(event);
    }

    private CompletableFuture<?> saveEventStatus(Event event, String entityId)
    {
        return this.events.saveEventStatus(new DefaultEventStatus(event, entityId, false));
    }

    private CompletableFuture<?> saveMailEntityEvent(Event event, String entityId)
    {
        return this.events.saveMailEntityEvent(new DefaultEntityEvent(event, entityId));
    }
}
