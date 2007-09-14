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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.SchedulerListener;
import org.quartz.Trigger;

/**
 * @version $Id: $
 */
public class StatusListener implements SchedulerListener, JobListener
{
    /**
     * Log object to log messages in this class.
     */
    private static final Log LOG = LogFactory.getLog(SchedulerPlugin.class);

    /**
     * {@inheritDoc}
     */
    public void jobScheduled(Trigger trigger)
    {
        LOG.info("Task '" + trigger.getJobName() + "' scheduled");
    }

    /**
     * {@inheritDoc}
     */
    public void jobUnscheduled(String name, String group)
    {
        LOG.info("Task '" + name + "' unscheduled");
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
        LOG.info("Task '" + name + "' paused");
    }

    /**
     * {@inheritDoc}
     */
    public void jobsResumed(String name, String group)
    {
        LOG.info("Task '" + name + "' resumed");
    }

    /**
     * {@inheritDoc}
     */
    public void schedulerError(String message, SchedulerException error)
    {
        LOG.error(message, error);
    }

    /**
     * {@inheritDoc}
     */
    public void schedulerShutdown()
    {
        LOG.warn("Scheduler is shutting down");
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
        LOG.info("Task '" + context.getJobDetail().getName() + "' is about to be executed");
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
        LOG.info("Task '" + context.getJobDetail().getName() + "' executed : " + e);
    }
}
