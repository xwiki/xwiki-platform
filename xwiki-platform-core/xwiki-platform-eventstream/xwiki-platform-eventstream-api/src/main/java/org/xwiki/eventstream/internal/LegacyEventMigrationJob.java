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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

/**
 * Copy legacy events to the new event store.
 * 
 * @version $Id$
 * @since 12.6
 */
@Component
@Named(LegacyEventMigrationJob.JOBTYPE)
public class LegacyEventMigrationJob
    extends AbstractJob<LegacyEventMigrationRequest, DefaultJobStatus<LegacyEventMigrationRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "eventstream.legacycopy";

    private static final int BATCH_SIZE = 100;

    @Inject
    private EventStreamConfiguration configuration;

    @Inject
    private EventStream eventStream;

    private EventStore eventStore;

    @Inject
    private QueryManager queryManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        if (!this.configuration.isEventStoreEnabled() || StringUtils.isEmpty(this.configuration.getEventStore())) {
            this.logger.warn("New event store system is disabled");

            return;
        }

        try {
            this.eventStore = this.componentManager.getInstance(EventStore.class, this.configuration.getEventStore());
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to get the configured event store [{}]", this.configuration.getEventStore(), e);

            return;
        }

        long legacyEventCount = this.eventStream.countEvents();
        int stepCount = (int) (legacyEventCount / BATCH_SIZE);

        if (legacyEventCount % BATCH_SIZE != 0) {
            stepCount++;
        }

        this.progressManager.pushLevelProgress(stepCount, this);

        try {
            Query query = prepareQuery();

            List<Event> events;
            int offset = 0;
            do {
                this.progressManager.startStep(this);

                events = this.eventStream.searchEvents(query);

                if (getRequest().isVerbose()) {
                    this.logger.info("Synchronizing legacy events from index {} to {}", offset, offset + events.size());
                }

                if (!events.isEmpty()) {
                    migrate(events);

                    // Update the offset
                    offset += BATCH_SIZE;
                    query.setOffset(offset);
                }

                this.progressManager.endStep(this);
            } while (events.size() == BATCH_SIZE);
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private void migrate(List<Event> events) throws EventStreamException, InterruptedException, ExecutionException
    {
        // Filter already existing events
        List<Event> eventsToSave = getEventsToSave(events);

        // Save events
        CompletableFuture<?> future = null;
        for (Iterator<Event> it = eventsToSave.iterator(); it.hasNext();) {
            future = migrate(it.next());
        }

        // Wait until the last event of the batch is saved
        if (future != null) {
            future.get();
        }

        if (getRequest().isVerbose()) {
            this.logger.info("{} events were saved in the new store because they did not already exist",
                eventsToSave.size());
        }
    }

    private CompletableFuture<?> migrate(Event event)
    {
        CompletableFuture<?> future = this.eventStore.saveEvent(event);

        List<String> entities;
        try {
            entities = getReadEntities(event.getId());

            for (String entity : entities) {
                future = this.eventStore.saveEventStatus(new DefaultEventStatus(event, entity, true));
            }
        } catch (QueryException e) {
            this.logger.error("Failed to get read entities associated with event [{}]", event.getId(), e);
        }

        return future;
    }

    private List<String> getReadEntities(String eventId) throws QueryException
    {
        Query query = this.queryManager.createQuery("select eventStatus.entityId from LegacyEventStatus eventStatus "
            + "where eventStatus.activityEvent.id = :eventId", Query.HQL);
        query.bindValue("eventId", eventId);

        return query.execute();
    }

    private Query prepareQuery() throws QueryException
    {
        StringBuilder queryString = new StringBuilder();

        // Take into account the provided minimum date
        if (getRequest().getSince() != null) {
            queryString.append("WHERE event.date >= :since ");
        }

        // Speed up the feeling of completeness by handling most recent events first
        queryString.append("ORDER BY event.date desc");

        // Create the query
        Query query = this.queryManager.createQuery(queryString.toString(), Query.HQL);
        query.setLimit(100);
        if (getRequest().getSince() != null) {
            query.bindValue("since", getRequest().getSince());
        }

        return query;
    }

    private List<Event> getEventsToSave(List<Event> events) throws EventStreamException
    {
        // TODO: find out what's wrong with the IN clause (it return less results than it should right now)
        // EventSearchResult existingEvents = this.eventStore.search(
        // new SimpleEventQuery().in(Event.FIELD_ID, events.stream().
        // map(Event::getId).collect(Collectors.toList())),
        // Collections.singleton(Event.FIELD_ID));
        // Set<String> existingIds = existingEvents.stream().map(Event::getId).collect(Collectors.toSet());

        List<Event> eventsToSave = new ArrayList<>(events.size());
        for (Event event : events) {
            // TODO: optimize this a bit but there seems to be a problem with the IN clause, see previous commented code
            try {
                if (!this.eventStore.getEvent(event.getId()).isPresent()
                    // Ensure that the event concerns a wiki that still exist.
                    && (event.getWiki() == null || this.wikiDescriptorManager.exists(event.getWiki().getName()))) {
                    eventsToSave.add(event);
                }
            } catch (WikiManagerException e) {
                logger.warn(
                    "Error while checking if the wiki [{}] exists. The event (id: [{}]) referencing this wiki "
                        + "won't be migrated. Root cause: [{}].",
                    event.getWiki().getName(), event.getId(), ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return eventsToSave;
    }
}
