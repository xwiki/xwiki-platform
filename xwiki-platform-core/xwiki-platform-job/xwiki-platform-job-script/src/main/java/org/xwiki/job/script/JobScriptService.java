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
package org.xwiki.job.script;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobGroupPath;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.script.internal.safe.ScriptSafeProvider;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Provides job-specific scripting APIs.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("job")
@Singleton
public class JobScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    public static final String JOB_ERROR_KEY = "scriptservice.job.error";

    /**
     * Used to retrieve the job that is currently being executed.
     */
    @Inject
    private JobExecutor jobExecutor;

    /**
     * Used to retrieve the job status.
     */
    @Inject
    private JobStatusStore jobStore;

    /**
     * Used to check programming rights.
     */
    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    @SuppressWarnings("rawtypes")
    private ScriptSafeProvider scriptSafeProvider;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    @Inject
    private WikiDescriptorManager wikis;

    /**
     * @param jobId the job id
     * @return the status of the specified job, {@code null} if the status cannot be found
     */
    public JobStatus getJobStatus(List<String> jobId)
    {
        JobStatus jobStatus;

        Job job = this.jobExecutor.getJob(jobId);
        if (job == null) {
            jobStatus = this.jobStore.getJobStatus(jobId);
        } else {
            jobStatus = job.getStatus();
        }

        if (jobStatus != null && !this.authorization.hasAccess(Right.PROGRAM)) {
            jobStatus = safe(jobStatus);
        }

        return jobStatus;
    }

    /**
     * Get a reference to the currently job executed in the specified job group.
     * 
     * @param path specifies the job group where to look for a running job
     * @return currently executing job in the specified job group, or {@code null} if no job is being executed
     */
    public Job getCurrentJob(Collection<String> path)
    {
        setError(null);

        if (this.authorization.hasAccess(Right.PROGRAM)) {
            return this.jobExecutor.getCurrentJob(new JobGroupPath(path));
        } else {
            setError(new JobException("You need programming rights to get the current job."));
            return null;
        }
    }

    /**
     * Get the status of the currently executing job in the specified group job, if any.
     * 
     * @param path specifies the job group where to look for a running job
     * @return status of the currently executing job in the specified group, or {@code null} if no job is being executed
     */
    public JobStatus getCurrentJobStatus(Collection<String> path)
    {
        Job job = this.jobExecutor.getCurrentJob(new JobGroupPath(path));

        JobStatus jobStatus = null;
        if (job != null) {
            jobStatus = job.getStatus();
            if (!this.authorization.hasAccess(Right.PROGRAM)) {
                jobStatus = safe(jobStatus);
            }
        }

        return jobStatus;
    }

    /**
     * @param <T> the type of the object
     * @param unsafe the unsafe object
     * @return the safe version of the passed object
     */
    @SuppressWarnings("unchecked")
    protected <T> T safe(T unsafe)
    {
        return (T) this.scriptSafeProvider.get(unsafe);
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(JOB_ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(JOB_ERROR_KEY, e);
    }
}
