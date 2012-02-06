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
package org.xwiki.extension.job.event;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.extension.job.Request;

/**
 * Common implementation for job events.
 * 
 * @version $Id$
 */
abstract class AbstractJobEvent implements JobEvent
{
    /**
     * Serializable identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Related job request.
     */
    private Request request;

    /**
     * Related job id.
     */
    private String jobId;

    /**
     * Related job type.
     */
    private String jobType;

    /**
     * Default constructor.
     */
    public AbstractJobEvent()
    {
    }

    /**
     * @param jobType the event related job type
     */
    protected AbstractJobEvent(String jobType)
    {
        this.jobType = jobType;
    }

    /**
     * @param jobId the event related job id
     * @param jobType the event related job type
     * @param request the event related job request
     */
    protected AbstractJobEvent(String jobId, String jobType, Request request)
    {
        this.jobId = jobId;
        this.jobType = jobType;
        this.request = request;
    }

    @Override
    public String getJobId()
    {
        return this.jobId;
    }

    @Override
    public String getJobType()
    {
        return this.jobType;
    }

    @Override
    public Request getRequest()
    {
        return this.request;
    }

    @Override
    public boolean matches(Object event)
    {
        return this.getClass() == event.getClass() && matchesJobId((JobEvent) event)
            && matchesJobType((JobEvent) event);
    }

    /**
     * @param event the event to match
     * @return <code>true</code> if the passed event matches this event, <code>false</code> otherwise.
     */
    private boolean matchesJobId(JobEvent event)
    {
        return this.jobId == null || StringUtils.equals(this.jobId, event.getJobId());
    }

    /**
     * @param event the event to match
     * @return <code>true</code> if the passed event matches this event, <code>false</code> otherwise.
     */
    private boolean matchesJobType(JobEvent event)
    {
        return this.jobId == null || StringUtils.equals(this.jobId, event.getJobId());
    }
}
