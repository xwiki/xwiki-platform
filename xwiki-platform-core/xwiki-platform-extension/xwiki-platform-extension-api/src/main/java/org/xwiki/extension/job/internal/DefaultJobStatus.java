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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.xwiki.extension.job.JobProgress;
import org.xwiki.extension.job.JobStatus;
import org.xwiki.extension.job.Request;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.logging.event.LogLevel;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

/**
 * @param <R>
 * @version $Id$
 */
public class DefaultJobStatus<R extends Request> implements JobStatus, EventListener
{
    /**
     * The matched events.
     */
    private static final List<Event> EVENTS = Collections.<Event> singletonList(new LogEvent());

    /**
     * Used register itself to receive logging and progress related events.
     */
    private ObservationManager observationManager;

    /**
     * Unique name mostly used for observation.
     */
    private String name;

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
    private ConcurrentLinkedQueue<LogEvent> logs = new ConcurrentLinkedQueue<LogEvent>();

    /**
     * Take care of progress related events to produce a progression information usually used in a progress bar.
     */
    private DefaultJobProgress progress;

    /**
     * The thread used to filter event for the job thread.
     */
    private Thread thread;

    /**
     * @param request the request provided when started the job
     * @param id the unique id of the job
     * @param observationManager the observation manager component
     */
    public DefaultJobStatus(R request, String id, ObservationManager observationManager)
    {
        this.request = request;
        this.observationManager = observationManager;

        this.name = getClass().getName() + '_' + id;

        this.progress = new DefaultJobProgress(id);

        this.thread = Thread.currentThread();
    }

    /**
     * Start listening to events.
     */
    void startListening()
    {
        this.observationManager.addListener(this);
        this.observationManager.addListener(this.progress);
    }

    /**
     * Stop listening to events.
     */
    void stopListening()
    {
        this.observationManager.removeListener(this.getName());
        this.observationManager.removeListener(this.progress.getName());
    }

    // EventListener

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    @Override
    public String getName()
    {
        return this.name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    @Override
    public List<Event> getEvents()
    {
        return EVENTS;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public void onEvent(Event event, Object arg1, Object arg2)
    {
        if (Thread.currentThread() == this.thread) {
            this.logs.add((LogEvent) event);
        }
    }

    // JobStatus

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.JobStatus#getState()
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.JobStatus#getRequest()
     */
    public R getRequest()
    {
        return this.request;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.JobStatus#getLog()
     */
    public ConcurrentLinkedQueue<LogEvent> getLog()
    {
        return this.logs;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.JobStatus#getLog(org.xwiki.logging.event.LogLevel)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.JobStatus#getProgress()
     */
    public JobProgress getProgress()
    {
        return this.progress;
    }
}
