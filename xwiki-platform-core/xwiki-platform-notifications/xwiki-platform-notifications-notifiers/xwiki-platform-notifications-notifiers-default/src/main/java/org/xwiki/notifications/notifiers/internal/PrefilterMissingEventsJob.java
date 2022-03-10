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
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.RecordableEventDescriptor;
import org.xwiki.eventstream.RecordableEventDescriptorManager;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobStatus;

/**
 * Find in the store unfiltered events and pre filter them.
 * 
 * @version $Id$
 * @since 12.6.3
 * @since 12.9RC1
 */
@Component
@Named(PrefilterMissingEventsJob.JOBTYPE)
public class PrefilterMissingEventsJob extends AbstractJob<Request, JobStatus>
{
    /**
     * The id of the job.
     */
    public static final String JOBTYPE = "event.indexer";

    private static final long BATCH_SIZE = 100;

    @Inject
    private EventStore eventStore;

    @Inject
    private RecordableEventDescriptorManager recordableEventDescriptorManager;

    @Inject
    private UserEventDispatcher dispatcher;

    @Override
    public String getType()
    {
        return JOBTYPE;
    }

    @Override
    protected void runInternal() throws Exception
    {
        // Get supported events types
        List<RecordableEventDescriptor> descriptorList =
            this.recordableEventDescriptorManager.getRecordableEventDescriptors(true);
        Set<String> types =
            descriptorList.stream().map(RecordableEventDescriptor::getEventType).collect(Collectors.toSet());

        // Create a search request for events which haven't been prefiltered yet
        SimpleEventQuery query = new SimpleEventQuery();
        query.eq(org.xwiki.eventstream.Event.FIELD_PREFILTERED, false);
        query.setLimit(BATCH_SIZE);

        try {
            do {
                EventSearchResult result = this.eventStore.search(query);

                result.stream().forEach(event -> prefilterEvent(event, types));

                if (result.getSize() < BATCH_SIZE) {
                    break;
                }

                query.setOffset(result.getOffset() + BATCH_SIZE);
            } while (true);
        } catch (EventStreamException e) {
            this.logger.error("Failed to search events for which pre filtering was missed", e);
        }
    }

    private void prefilterEvent(Event eventStreamEvent, Set<String> types)
    {
        if (types.contains(eventStreamEvent.getType())) {
            this.dispatcher.dispatch(eventStreamEvent);
        }
    }
}
