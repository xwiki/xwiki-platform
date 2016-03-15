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
package org.xwiki.rest;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.xwiki.job.Job;
import org.xwiki.job.JobExecutor;
import org.xwiki.job.JobStatusStore;
import org.xwiki.job.event.status.JobStatus;

/**
 * Base class for all job-related resources.
 * 
 * @version $Id$
 */
public class XWikiJobResource extends XWikiResource
{
    /**
     * Used to retrieve the job that is currently being executed.
     */
    @Inject
    protected JobExecutor jobExecutor;

    /**
     * Used to retrieve the job status.
     */
    @Inject
    protected JobStatusStore jobStore;

    protected JobStatus getRealJobStatus(String jobId) throws WebApplicationException
    {
        JobStatus jobStatus;

        String[] idArray = jobId.split("/");

        List<String> id = new ArrayList<>(idArray.length);

        // Unescape id sections
        for (String idElement : idArray) {
            try {
                id.add(URLDecoder.decode(idElement, "UTF8"));
            } catch (UnsupportedEncodingException e) {
                throw new WebApplicationException(e);
            }
        }

        // Search job
        Job job = this.jobExecutor.getJob(id);
        if (job == null) {
            jobStatus = this.jobStore.getJobStatus(id);
        } else {
            jobStatus = job.getStatus();
        }

        if (jobStatus == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return jobStatus;

    }

}
