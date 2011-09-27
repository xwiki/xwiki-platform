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
package com.xpn.xwiki.plugin.scheduler;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Id$
 */
public class StatusListener implements SchedulerListener, JobListener
{
    /**
     * Log4j logger that records events for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(StatusListener.class);

    /**
     * {@inheritDoc}
     */
    public void jobScheduled(Trigger trigger)
    {
        LOGGER.info("Task [{}] scheduled", trigger.getJobName());
    }

    /**
     * {@inheritDoc}
     */
    public void jobUnscheduled(String name, String group)
    {
        LOGGER.info("Task [{}] unscheduled", name);
    }

    /**
     * {@inheritDoc}
     */
    public void triggerFinalized(Trigger trigger)
    {
    }

    /**
     * {@inheritDoc}
     */
    public void triggersPaused(String trigger, String group)
    {
    }

    /**
     * {@inheritDoc}
     */
    public void triggersResumed(String trigger, String group)
    {
    }

    /**
     * {@inheritDoc}
     */
    public void jobsPaused(String name, String group)
    {
        LOGGER.info("Task [{}] paused", name);
    }

    /**
     * {@inheritDoc}
     */
    public void jobsResumed(String name, String group)
    {
        LOGGER.info("Task [{}] resumed", name);
    }

    /**
     * {@inheritDoc}
     */
    public void schedulerError(String message, SchedulerException error)
    {
        LOGGER.error(message, error);
    }

    /**
     * {@inheritDoc}
     */
    public void schedulerShutdown()
    {
        LOGGER.warn("Scheduler is shutting down");
    }

    /**
     * {@inheritDoc}
     */
    public String getName()
    {
        return "StatusListener";
    }

    /**
     * {@inheritDoc}
     */
    public void jobToBeExecuted(JobExecutionContext context)
    {
        LOGGER.info("Task [{}] is about to be executed", context.getJobDetail().getName());
    }

    /**
     * {@inheritDoc}
     */
    public void jobExecutionVetoed(JobExecutionContext context)
    {
    }

    /**
     * {@inheritDoc}
     */
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException e)
    {
        LOGGER.info("Task [{}] executed: ", context.getJobDetail().getName(), e);
    }
}
