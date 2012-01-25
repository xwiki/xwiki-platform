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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.Job;
import org.xwiki.extension.job.JobException;
import org.xwiki.extension.job.JobManager;
import org.xwiki.extension.job.Request;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.event.status.JobStatus;

/**
 * Default implementation of {@link JobManager}.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultJobManager implements JobManager, Runnable, Initializable
{
    /**
     * A hob to execute.
     * 
     * @version $Id$
     */
    private static class JobElement
    {
        /**
         * The job to execute.
         */
        public Job job;

        /**
         * The request to use to control the job.
         */
        public Request request;

        /**
         * @param job the job to execute
         * @param request the request to use to control the job
         */
        public JobElement(Job job, Request request)
        {
            this.job = job;
            this.request = request;
        }
    }

    /**
     * Used to lookup {@link Job} implementations.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * @see #getCurrentJob()
     */
    private volatile Job currentJob;

    /**
     * The queue of jobs to execute.
     */
    private BlockingQueue<JobElement> jobQueue = new LinkedBlockingQueue<JobElement>();

    /**
     * The thread on which the job manager is running.
     */
    private Thread thread;

    @Override
    public void initialize() throws InitializationException
    {
        this.thread = new Thread(this);
        this.thread.setDaemon(true);
        this.thread.start();
    }

    // Runnable

    @Override
    public void run()
    {
        while (!this.thread.isInterrupted()) {
            try {
                JobElement element = this.jobQueue.take();

                this.currentJob = element.job;

                // Wait in case synchronous job is running
                synchronized (this) {
                    this.currentJob.start(element.request);
                }
            } catch (InterruptedException e) {
                // Thread has been stopped
            }
        }
    }

    // JobManager

    @Override
    public Job getCurrentJob()
    {
        return this.currentJob;
    }

    @Override
    public Job install(InstallRequest request) throws JobException
    {
        return executeJob("install", request);
    }

    @Override
    public Job uninstall(UninstallRequest request) throws JobException
    {
        return executeJob("uninstall", request);
    }

    /**
     * @param jobId the job id
     * @return a new job
     * @throws JobException failed to create a job for the provided id
     */
    private Job createJob(String jobId) throws JobException
    {
        Job job;
        try {
            job = this.componentManager.lookup(Job.class, jobId);
        } catch (ComponentLookupException e) {
            throw new JobException("Failed to lookup any Job for role hint [" + jobId + "]", e);
        }

        return job;
    }

    @Override
    public synchronized Job executeJob(String jobId, Request request) throws JobException
    {
        if (this.jobQueue.isEmpty()
            && (this.currentJob != null && this.currentJob.getStatus().getState() != JobStatus.State.FINISHED)) {
            throw new JobException("A task is already running");
        }

        // The lock is used to block the explicit job queue thread
        synchronized (this) {
            this.currentJob = createJob(jobId);

            this.currentJob.start(request);
        }

        return this.currentJob;
    }

    @Override
    public Job addJob(String jobId, Request request) throws JobException
    {
        Job job = createJob(jobId);

        this.jobQueue.add(new JobElement(job, request));

        return job;
    }
}
