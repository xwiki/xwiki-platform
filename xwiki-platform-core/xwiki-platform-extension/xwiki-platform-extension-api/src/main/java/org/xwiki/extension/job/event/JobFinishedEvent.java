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

import org.xwiki.extension.job.Request;
import org.xwiki.observation.event.EndEvent;

/**
 * Job finished event launched when a job is finished.
 * Additional data may contains an exception if the job has not been finished with success.
 *
 * @version $Id$
 */
public class JobFinishedEvent extends AbstractJobEvent implements EndEvent
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor.
     */
    public JobFinishedEvent()
    {

    }

    /**
     * @param jobId the event related job id
     */
    public JobFinishedEvent(String jobId)
    {
        super(jobId);
    }

    /**
     * @param jobId the event related job id
     * @param request the event related job request
     */
    public JobFinishedEvent(String jobId, Request request)
    {
        super(jobId, request);
    }
}
