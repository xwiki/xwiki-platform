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
package org.xwiki.rest.internal.resources.jobstatus;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.rest.XWikiResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.internal.DomainObjectFactory;
import org.xwiki.rest.model.jaxb.JobStatus;
import org.xwiki.rest.resources.jobstatus.JobStatusResource;

import com.xpn.xwiki.XWikiException;

/**
 * @version $Id$
 * @since 7.2M3
 */
@Component
@Named("org.xwiki.rest.internal.resources.jobstatus.JobStatusResourceImpl")
public class JobStatusResourceImpl extends XWikiResource implements JobStatusResource
{
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
    
    @Override
    public JobStatus getJobStatus(String jobId) throws XWikiRestException
    {
        try {
            org.xwiki.job.event.status.JobStatus jobStatus;

            List<String> id = Arrays.asList(jobId.split("/"));

            Job job = this.jobExecutor.getJob(id);
            if (job == null) {
                jobStatus = this.jobStore.getJobStatus(id);
            } else {
                jobStatus = job.getStatus();
            }
            
            if (jobStatus == null) {
                throw new WebApplicationException(Response.Status.NOT_FOUND);
            }

            return DomainObjectFactory.createJobStatus(objectFactory, uriInfo.getAbsolutePath(), jobStatus);
        } catch (XWikiException e) {
            throw new XWikiRestException(e);
        }
    }
}
