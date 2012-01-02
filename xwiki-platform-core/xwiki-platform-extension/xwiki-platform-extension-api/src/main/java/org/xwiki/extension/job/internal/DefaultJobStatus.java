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
package org.xwiki.extension.job.internal;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.extension.job.Request;
import org.xwiki.extension.job.event.status.JobProgress;
import org.xwiki.extension.job.event.status.JobStatus;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.LoggerManager;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.event.LogQueueListener;
import org.xwiki.observation.ObservationManager;

/**
 * Default implementation of {@link JobStatus}.
 * 
 * @param <R>
 * @version $Id$
 */
public class DefaultJobStatus<R extends Request> implements JobStatus
{
    /**
     * Used register itself to receive logging and progress related events.
     */
    private ObservationManager observationManager;

    /**
     * Used to isolate job related log.
     */
    private LoggerManager loggerManager;

    /**
     * The unique id of the job.
     */
    private String id;

    /**
     * General state of the job.
     */
    private State state;

    /**
     * Request provided when starting thee job.
     */
    private R request;

    /**
     * Log sent during job execution.
     */
    private LogQueue logs = new LogQueue();

    /**
     * Take care of progress related events to produce a progression information usually used in a progress bar.
     */
    private DefaultJobProgress progress;

    /**
     * @param request the request provided when started the job
     * @param id the unique id of the job
     * @param observationManager the observation manager component
     * @param loggerManager the logger manager component
     */
    public DefaultJobStatus(R request, String id, ObservationManager observationManager, LoggerManager loggerManager)
    {
        this.request = request;
        this.observationManager = observationManager;
        this.loggerManager = loggerManager;
        this.id = id;

        this.progress = new DefaultJobProgress(this.id);
    }

    /**
     * Start listening to events.
     */
    void startListening()
    {
        // Register progress listener
        this.observationManager.addListener(this.progress);

        // Isolate log for the job status
        this.loggerManager.pushLogListener(new LogQueueListener(LogQueueListener.class.getName() + '_' + this.id,
            this.logs));
    }

    /**
     * Stop listening to events.
     */
    void stopListening()
    {
        this.loggerManager.popLogListener();
        this.observationManager.removeListener(this.progress.getName());
    }

    // JobStatus

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.event.status.JobStatus#getState()
     */
    @Override
    public State getState()
    {
        return this.state;
    }

    /**
     * @param state the general state of the job
     */
    public void setState(State state)
    {
        this.state = state;
    }

    @Override
    public R getRequest()
    {
        return this.request;
    }

    @Override
    public LogQueue getLog()
    {
        return this.logs;
    }

    @Override
    public List<LogEvent> getLog(LogLevel level)
    {
        List<LogEvent> levelLogs = new ArrayList<LogEvent>();

        for (LogEvent log : this.logs) {
            if (log.getLevel() == level) {
                levelLogs.add(log);
            }
        }

        return levelLogs;
    }

    @Override
    public JobProgress getProgress()
    {
        return this.progress;
    }
}
