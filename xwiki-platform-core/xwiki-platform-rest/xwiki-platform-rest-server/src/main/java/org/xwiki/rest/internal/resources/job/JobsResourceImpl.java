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
package org.xwiki.rest.internal.resources.job;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.job.DefaultRequest;
import org.xwiki.job.Job;
import org.xwiki.job.JobException;
import org.xwiki.rest.XWikiJobResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.ModelFactory;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.model.jaxb.JobStatus;
import org.xwiki.rest.resources.job.JobsResource;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.job.JobRequestContext;

/**
 * @version $Id$
 * @since 9.1RC1
 */
@Component
@Named("org.xwiki.rest.internal.resources.job.JobsResourceImpl")
public class JobsResourceImpl extends XWikiJobResource implements JobsResource
{
    @Inject
    private ModelFactory factory;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Override
    public JobStatus executeJob(String jobType, boolean async, JobRequest restJobRequest) throws XWikiRestException
    {
        // Restrict generic Job starting to programming right for now
        // TODO: provide extension point to decide of the access depending on the job
        if (!this.authorization.hasAccess(Right.PROGRAM, null)) {
            throw new WebApplicationException(Status.UNAUTHORIZED);
        }

        // Parse JobRequest
        DefaultRequest request = this.factory.toJobRequest(restJobRequest);
        if (request == null) {
            request = new DefaultRequest();
        }

        // Start job
        Job job;
        try {
            // Give a few context related values to the job
            if (request.getProperty(JobRequestContext.KEY) == null) {
                JobRequestContext.set(request, this.xcontextProvider.get());
            }

            job = this.jobExecutor.execute(jobType, request);
        } catch (JobException e) {
            throw new XWikiRestException("Failed to start job", e);
        }

        // Wait for the job end if asked
        if (!async) {
            try {
                job.join();
            } catch (InterruptedException e) {
                throw new XWikiRestException("The job has been interrupted", e);
            }

            // Fail the HTTP request if the job failed
            if (job.getStatus().getError() != null) {
                throw new XWikiRestException(String.format("The job failed with error [%s]",
                    ExceptionUtils.getRootCauseMessage(job.getStatus().getError())), job.getStatus().getError());
            }
        }

        // Get job status
        org.xwiki.job.event.status.JobStatus status = job.getStatus();

        // Convert Job status
        return this.factory.toRestJobStatus(status, null, true, false, false, null);
    }
}
