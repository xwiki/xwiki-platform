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
package org.xwiki.eventstream.internal;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventQuery;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * The default implementation of {@link EventStore} dispatching the event in the various enabled stores.
 * 
 * @version $Id$
 * @since 12.4RC1
 */
@Component
@Singleton
public class DefaultEventStore implements EventStore, Initializable
{
    /**
     * Key used to store the request ID in the context.
     */
    private static final String GROUP_ID_CONTEXT_KEY = "activitystream_requestid";

    private static final String NO_STORE = "No event store available";

    @Inject
    private EventStreamConfiguration configuration;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Execution execution;

    @Inject
    private Logger logger;

    private EventStore legacyStore;

    private EventStore store;

    @Override
    public void initialize() throws InitializationException
    {
        if (this.configuration.isEventStoreEnabled()) {
            String hint = this.configuration.getEventStore();

            // Check if the configured event store exist
            if (this.componentManager.hasComponent(EventStore.class, hint)) {
                try {
                    this.store = this.componentManager.getInstance(EventStore.class, hint);
                } catch (ComponentLookupException e) {
                    throw new InitializationException(
                        String.format("Failed to get the configured event store [%s]", hint), e);
                }
            } else {
                if (this.configuration.isEventStoreSet()) {
                    // An event store was explicitly configured
                    throw new InitializationException(String
                        .format("Could not find the configured implementation of event store with hint [%s]", hint));
                }

                // Just warn, no event will be stored (except in the legacy one if available)
                this.logger.warn("No default implementation of EventStore could be found."
                    + " No event will be stored (except in the legacy store if available).");
            }
        }

        // Retro compatibility: make sure to synchronize the old storage until the new store covers everything we
        // want it to cover
        String legacyHint = this.store != null ? "legacy" : "legacy/verbose";
        if (this.componentManager.hasComponent(EventStore.class, legacyHint)) {
            try {
                this.legacyStore = this.componentManager.getInstance(EventStore.class, legacyHint);
            } catch (ComponentLookupException e) {
                throw new InitializationException(
                    String.format("Failed to get the legacy event stream [%s]", legacyHint), e);
            }
        }
    }

    @Override
    public CompletableFuture<Event> saveEvent(Event event)
    {
        prepareEvent(event);

        CompletableFuture<Event> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.saveEvent(event);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.saveEvent(event);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public CompletableFuture<Event> prefilterEvent(Event event)
    {
        prepareEvent(event);

        CompletableFuture<Event> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.prefilterEvent(event);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.prefilterEvent(event);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public CompletableFuture<Optional<Event>> deleteEvent(String eventId)
    {
        CompletableFuture<Optional<Event>> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.deleteEvent(eventId);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.deleteEvent(eventId);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public CompletableFuture<Optional<Event>> deleteEvent(Event event)
    {
        CompletableFuture<Optional<Event>> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.deleteEvent(event);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.deleteEvent(event);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public CompletableFuture<EventStatus> saveEventStatus(EventStatus status)
    {
        CompletableFuture<EventStatus> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.saveEventStatus(status);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.saveEventStatus(status);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public CompletableFuture<EventStatus> saveMailEntityEvent(EntityEvent event)
    {
        CompletableFuture<EventStatus> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.saveMailEntityEvent(event);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.saveMailEntityEvent(event);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public CompletableFuture<Optional<EventStatus>> deleteEventStatus(EventStatus status)
    {
        CompletableFuture<Optional<EventStatus>> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.deleteEventStatus(status);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.deleteEventStatus(status);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public CompletableFuture<Void> deleteEventStatuses(String entityId, Date date)
    {
        CompletableFuture<Void> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.deleteEventStatuses(entityId, date);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.deleteEventStatuses(entityId, date);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public CompletableFuture<Optional<EventStatus>> deleteMailEntityEvent(EntityEvent event)
    {
        CompletableFuture<Optional<EventStatus>> future = null;

        if (this.legacyStore != null) {
            future = this.legacyStore.deleteMailEntityEvent(event);
        }

        if (this.store != null) {
            // Forget about legacy store result if new store is enabled
            future = this.store.deleteMailEntityEvent(event);
        }

        if (future == null) {
            future = new CompletableFuture<>();
            future.completeExceptionally(new EventStreamException(NO_STORE));
        }

        return future;
    }

    @Override
    public Optional<Event> getEvent(String eventId) throws EventStreamException
    {
        Optional<Event> event = Optional.empty();

        // Try the new store
        if (this.store != null) {
            event = this.store.getEvent(eventId);
        } else if (this.legacyStore != null) {
            event = this.legacyStore.getEvent(eventId);
        }

        return event;
    }

    @Override
    public EventSearchResult search(EventQuery query) throws EventStreamException
    {
        if (this.store != null) {
            return this.store.search(query);
        }

        if (this.legacyStore != null) {
            return this.legacyStore.search(query);
        }

        return EventSearchResult.EMPTY;
    }

    @Override
    public EventSearchResult search(EventQuery query, Set<String> fields) throws EventStreamException
    {
        if (this.store != null) {
            return this.store.search(query, fields);
        }

        if (this.legacyStore != null) {
            return this.legacyStore.search(query, fields);
        }

        return EventSearchResult.EMPTY;
    }

    /**
     * Generate event ID for the given ID. Note that this method does not perform the set of the ID in the event object.
     *
     * @param event event to generate the ID for
     * @param context the XWiki context
     * @return the generated ID
     */
    private String generateEventId(Event event, ExecutionContext context)
    {
        final String key = String.format("%s-%s-%s-%s", event.getStream(), event.getApplication(),
            serializer.serialize(event.getDocument()), event.getType());
        long hash = key.hashCode();
        if (hash < 0) {
            hash = -hash;
        }

        final String id =
            String.format("%d-%d-%s", hash, event.getDate().getTime(), RandomStringUtils.secure().nextAlphanumeric(8));
        if (context != null && context.getProperty(GROUP_ID_CONTEXT_KEY) == null) {
            context.setProperty(GROUP_ID_CONTEXT_KEY, id);
        }

        return id;
    }

    /**
     * Set fields in the given event object.
     *
     * @param event the event to prepare
     */
    private void prepareEvent(Event event)
    {
        ExecutionContext context = this.execution.getContext();

        if (event.getUser() == null) {
            event.setUser(this.documentAccessBridge.getCurrentUserReference());
        }

        if (event.getWiki() == null) {
            String wikiId = this.wikiDescriptorManager.getCurrentWikiId();
            if (wikiId != null) {
                event.setWiki(new WikiReference(wikiId));
            }
        }

        if (event.getApplication() == null) {
            event.setApplication("xwiki");
        }

        if (event.getDate() == null) {
            event.setDate(new Date());
        }

        if (event.getId() == null) {
            event.setId(generateEventId(event, context));
        }

        if (event.getGroupId() == null && context != null) {
            event.setGroupId((String) context.getProperty(GROUP_ID_CONTEXT_KEY));
        }
    }

    @Override
    public List<EventStatus> getEventStatuses(Collection<Event> events, Collection<String> entityIds) throws Exception
    {
        if (this.store != null) {
            return this.store.getEventStatuses(events, entityIds);
        }

        if (this.legacyStore != null) {
            return this.legacyStore.getEventStatuses(events, entityIds);
        }

        return List.of();
    }
}
