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
package org.xwiki.extension.cluster.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobManager;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

/**
 * Receive remote extension install/uninstall request events and apply them on current instance.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Named(RemoteListener.LISTENERID)
@Singleton
public class RemoteListener implements EventListener
{
    /**
     * The unique identifier of the event listener.
     */
    public static final String LISTENERID = "extension.cluster";

    /**
     * The events supported by this listener.
     */
    private static final List<Event> EVENTS = Arrays.<Event> asList(new RemoteJobStartedEvent());

    /**
     * Used to start new job.
     */
    @Inject
    private JobManager jobManager;

    /**
     * Used to log job related error.
     */
    @Inject
    private Logger logger;

    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    @Override
    public String getName()
    {
        return LISTENERID;
    }

    @Override
    public void onEvent(Event event, Object data, Object source)
    {
        RemoteJobStartedEvent jobEvent = (RemoteJobStartedEvent) event;

        try {
            Job job = this.jobManager.addJob(jobEvent.getJobType(), jobEvent.getRequest());

            for (LogEvent log : job.getStatus().getLog(LogLevel.ERROR)) {
                this.logger.error(log.getMessage(), log.getArgumentArray());
            }
        } catch (JobException e) {
            this.logger.error("Failed to execute remote job [" + jobEvent + "]", e);
        }

    }
}
