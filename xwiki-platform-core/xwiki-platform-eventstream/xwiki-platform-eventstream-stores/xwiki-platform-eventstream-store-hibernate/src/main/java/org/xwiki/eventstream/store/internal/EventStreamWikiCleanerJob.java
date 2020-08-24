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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStream;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;

/**
 * Job that delete the events related to a given wiki.
 *
 * @since 11.3RC1
 * @since 10.11.4
 * @since 10.8.4
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@Named(EventStreamWikiCleanerJob.JOB_TYPE)
public class EventStreamWikiCleanerJob
    extends AbstractJob<EventStreamWikiCleanerJobRequest, DefaultJobStatus<EventStreamWikiCleanerJobRequest>>
{
    /**
     * Type of the job.
     */
    public static final String JOB_TYPE = "EventStreamWikiCleanerJob";

    @Inject
    // TODO: don't use this anymore
    private EventStream eventStream;

    @Inject
    private QueryManager queryManager;

    @Override
    protected void runInternal() throws Exception
    {
        String wikiId = request.getWikiId();
        List<Event> eventsToDelete = null;
        do {
            try {
                Query query = queryManager.createQuery("where event.wiki = :wiki", Query.HQL);
                query.bindValue("wiki", wikiId);
                // This limit is arbitrary
                query.setLimit(1024);
                // I would prefer to perform a more efficient DELETE query on the store, but then
                // EventStreamDeletedEvent would not be triggered for each deleted event.
                eventsToDelete = eventStream.searchEvents(query);
                for (org.xwiki.eventstream.Event toDelete : eventsToDelete) {
                    eventStream.deleteEvent(toDelete);
                }
            } catch (Exception e) {
                logger.error("Failed to delete events related to the deleted wiki [{}] in the event stream.", wikiId,
                    e);
            }
        } while (eventsToDelete != null && !eventsToDelete.isEmpty());
    }

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }
}
