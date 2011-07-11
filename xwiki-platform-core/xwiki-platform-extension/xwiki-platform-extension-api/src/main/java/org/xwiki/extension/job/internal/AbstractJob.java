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

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.JobStatus;
import org.xwiki.extension.job.PopLevelProgressEvent;
import org.xwiki.extension.job.PushLevelProgressEvent;
import org.xwiki.extension.job.Request;
import org.xwiki.extension.job.StepProgressEvent;
import org.xwiki.observation.ObservationManager;

/**
 * Base class for {@link Job} implementations.
 * 
 * @param <R> the request type associated to the task
 * @version $Id$
 */
public abstract class AbstractJob<R extends Request> implements Job
{
    /**
     * Component manager.
     */
    @Inject
    protected ComponentManager componentManager;

    /**
     * Used to send extensions installation and upgrade related events.
     */
    @Inject
    protected ObservationManager observationManager;

    /**
     * The logger to log.
     */
    @Inject
    protected Logger logger;

    /**
     * @see #getStatus()
     */
    protected DefaultJobStatus<R> status;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.Job#getStatus()
     */
    public JobStatus getStatus()
    {
        return this.status;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.Job#getRequest()
     */
    public R getRequest()
    {
        return this.status.getRequest();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.extension.job.Job#start(org.xwiki.extension.job.Request)
     */
    public void start(Request request)
    {
        this.status = new DefaultJobStatus<R>((R) request, getId(), this.observationManager);

        this.status.startListening();

        try {
            start();
        } catch (Exception e) {
            logger.error("Failed to start job", e);
        }

        this.status.stopListening();

        this.status.setState(JobStatus.State.FINISHED);
    }

    /**
     * @return unique id for the task
     */
    protected String getId()
    {
        return getClass().getName() + "_" + Integer.toHexString(hashCode());
    }

    /**
     * Push new progression level.
     * 
     * @param steps number of steps in this new level
     */
    protected void notifyPushLevelProgress(int steps)
    {
        this.observationManager.notify(new PushLevelProgressEvent(steps), this);
    }

    /**
     * Next step.
     */
    protected void notifyStepPropress()
    {
        this.observationManager.notify(new StepProgressEvent(), this);
    }

    /**
     * Pop progression level.
     */
    protected void notifyPopLevelProgress()
    {
        this.observationManager.notify(new PopLevelProgressEvent(), this);
    }

    /**
     * Should be implemented by {@link Job} implementations.
     * 
     * @throws Exception errors during task execution
     */
    protected abstract void start() throws Exception;
}
