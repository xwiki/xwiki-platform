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
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
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
public class DefaultJobManager implements JobManager
{
    /**
     * Used to lookup {@link Job} implementations.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * @see #getCurrentJob()
     */
    private volatile Job currentJob;

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

    @Override
    public synchronized Job executeJob(String taskId, Request request) throws JobException
    {
        if (this.currentJob != null && this.currentJob.getStatus().getState() != JobStatus.State.FINISHED) {
            throw new JobException("A task is already running");
        }

        try {
            this.currentJob = this.componentManager.lookup(Job.class, taskId);
        } catch (ComponentLookupException e) {
            throw new JobException("Failed to lookup any Task for role hint [" + taskId + "]", e);
        }

        // TODO: make non-blocker
        this.currentJob.start(request);

        return this.currentJob;
    }
}
