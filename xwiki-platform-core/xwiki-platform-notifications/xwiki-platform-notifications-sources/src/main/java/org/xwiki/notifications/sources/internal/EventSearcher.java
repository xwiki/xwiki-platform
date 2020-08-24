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
package org.xwiki.notifications.sources.internal;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStream;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.EventStreamConfiguration;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.notifications.sources.NotificationParameters;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;

/**
 * Search {@link Event}.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
@Component(roles = EventSearcher.class)
@Singleton
public class EventSearcher
{
    @Inject
    private EventStream eventStream;

    @Inject
    private EventStore eventStore;

    @Inject
    private QueryGenerator queryGenerator;

    @Inject
    private EventQueryGenerator eventQueryGenerator;

    @Inject
    private EventStreamConfiguration configuration;

    @Inject
    private Logger logger;

    /**
     * @param offset the index where to start returning events
     * @param limit the maximum number of events to return
     * @param parameters parameters to use
     * @return the found events
     * @throws QueryException when to search the events
     * @throws EventStreamException when to search the events
     */
    public List<Event> searchEvents(int offset, int limit, NotificationParameters parameters)
        throws QueryException, EventStreamException
    {
        // Try event store if enabled
        if (this.configuration.isEventStoreEnabled()) {
            try {
                return searchStoreEvents(offset, limit, parameters);
            } catch (EventStreamException e) {
                this.logger.debug("Failed to get events from the EventStore. Trying on the legacy store", e);
            }
        }

        // Fallback on legacy event stream
        return searchStreamEvents(offset, limit, parameters);
    }

    /**
     * @param offset the index where to start returning events
     * @param limit the maximum number of events to return
     * @param parameters parameters to use
     * @return the found events
     * @throws QueryException when to search the events
     * @throws EventStreamException when to search the events
     */
    public List<Event> searchStoreEvents(int offset, int limit, NotificationParameters parameters)
        throws EventStreamException
    {
        // Create the query
        SimpleEventQuery query = this.eventQueryGenerator.generateQuery(parameters);

        query.setLimit(limit).setOffset(offset);

        // Get a batch of events
        EventSearchResult result = this.eventStore.search(query);

        return result.stream().collect(Collectors.toList());
    }

    /**
     * @param offset the index where to start returning events
     * @param limit the maximum number of events to return
     * @param parameters parameters to use
     * @return the found events
     * @throws QueryException when to search the events
     * @throws EventStreamException when to search the events
     */
    public List<Event> searchStreamEvents(int offset, int limit, NotificationParameters parameters)
        throws QueryException, EventStreamException
    {
        // Create the query
        Query query = this.queryGenerator.generateQuery(parameters);
        if (query == null) {
            return Collections.emptyList();
        }
        query.setLimit(limit).setOffset(offset);

        // Get a batch of events
        return this.eventStream.searchEvents(query);
    }
}
