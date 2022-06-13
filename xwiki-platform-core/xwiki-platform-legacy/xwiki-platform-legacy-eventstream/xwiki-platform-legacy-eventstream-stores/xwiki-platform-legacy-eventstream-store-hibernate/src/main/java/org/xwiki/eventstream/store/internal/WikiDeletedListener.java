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

import org.slf4j.Logger;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.JobExecutor;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Arrays;

/**
 * Listener that start a EventStreamWikiCleanerJob when a wiki is deleted. The idea is to clean the event stream when
 * a wiki is deleted.
 *
 * @since 11.3RC1
 * @since 10.11.4
 * @since 10.8.4
 * @version $Id$
 */
@Component
@Named(WikiDeletedListener.NAME)
@Singleton
public class WikiDeletedListener extends AbstractEventListener
{
    /**
     * Name of the listener.
     */
    public static final String NAME = "EventStreamWikiDeletedEventListener";

    @Inject
    private JobExecutor jobExecutor;

    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public WikiDeletedListener()
    {
        super(NAME, new WikiDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        WikiDeletedEvent wikiDeletedEvent = (WikiDeletedEvent) event;

        try {
            EventStreamWikiCleanerJobRequest request = new EventStreamWikiCleanerJobRequest(
                    wikiDeletedEvent.getWikiId());
            request.setId(Arrays.asList(EventStreamWikiCleanerJob.JOB_TYPE, wikiDeletedEvent.getWikiId()));
            jobExecutor.execute(EventStreamWikiCleanerJob.JOB_TYPE, request);
        } catch (Exception e) {
            logger.error("Failed to start a job [EventStreamWikiCleanerJob] for the deleted wiki [{}]",
                    wikiDeletedEvent.getWikiId(), e);
        }
    }
}
