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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.Request;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

/**
 * Copy legacy events to the new event store.
 * 
 * @version $Id$
 * @since 12.6
 */
@Component
@Named(LegacyEventSynchornizationJob.JOBTYPE)
public class LegacyEventSynchornizationJob
    extends AbstractJob<LegacyEventSynchornizationRequest, DefaultJobStatus<LegacyEventSynchornizationRequest>>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "eventstream.legacycopy";

    @Inject
    private EventStreamConfiguration configuration;

    @Inject
    private EventStream eventStream;

    @Inject
    private EventStore eventStore;

    @Inject
    private QueryManager queryManager;

    @Override
    protected LegacyEventSynchornizationRequest castRequest(Request request)
    {
        LegacyEventSynchornizationRequest indexerRequest;
        if (request instanceof LegacyEventSynchornizationRequest) {
            indexerRequest = (LegacyEventSynchornizationRequest) request;
        } else {
            indexerRequest = new LegacyEventSynchornizationRequest(request);
        }

        return indexerRequest;
    }

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        if (this.configuration.isEventStoreEnabled()) {
            this.logger.warn("New event store system is diabled");

            return;
        }

        if (this.configuration.isEventStoreEnabled()) {
            String hint = this.configuration.getEventStore();

            if (StringUtils.isNotEmpty(hint)) {
                try {
                    this.eventStore =
                        this.componentManager.getInstance(EventStore.class, this.configuration.getEventStore());
                } catch (ComponentLookupException e) {
                    this.logger.error("Failed to get the configured store", e);

                    return;
                }
            }
        }

        String queryString;
        if (getRequest().getAfter() != null) {
            queryString = "event.date = :after";
        } else {
            queryString = "";
        }
        Query query = this.queryManager.createQuery(queryString, Query.HQL);
        query.setLimit(100);
        if (getRequest().getAfter() != null) {
            query.bindValue("after", getRequest().getAfter());
        }

        List<Event> events;
        int offset = 0;
        do {
            events = this.eventStream.searchEvents(query);

            // Filter already existing events
            EventSearchResult existingEvents = this.eventStore.search(
                new SimpleEventQuery().in(Event.FIELD_ID,
                    events.stream().map(Event::getId).collect(Collectors.toList())),
                Collections.singleton(Event.FIELD_ID));
            Set<String> existingIds = existingEvents.stream().map(Event::getId).collect(Collectors.toSet());

            // Save missing events
            for (Event event : events) {
                if (!existingIds.contains(event.getId())) {
                    this.eventStore.saveEvent(event);
                }
            }

            // Update the offset
            offset += 100;
            query.setOffset(offset);
        } while (events.size() == 100);
    }
}
