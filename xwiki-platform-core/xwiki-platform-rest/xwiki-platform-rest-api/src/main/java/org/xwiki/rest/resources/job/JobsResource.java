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
package org.xwiki.rest.resources.job;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.JobRequest;
import org.xwiki.rest.model.jaxb.JobStatus;

/**
 * @version $Id$
 * @since 9.1RC1
 */
@Path("/" + JobsResource.NAME)
public interface JobsResource
{
    /**
     * The entry name of the resource.
     */
    String NAME = "jobs";

    /**
     * Start a new Job.
     * 
     * @param jobType the type of the job to start
     * @param async true if the REST request should return without waiting for the end of the job
     * @param jobRequest the request controlling job behavior
     * @return the newly created job status
     * @throws XWikiRestException when failing to start job
     * @since 9.0RC1
     */
    @PUT
    JobStatus executeJob(@QueryParam("jobType") String jobType,
        @QueryParam("async") @DefaultValue("true") boolean async, JobRequest jobRequest) throws XWikiRestException;
}
