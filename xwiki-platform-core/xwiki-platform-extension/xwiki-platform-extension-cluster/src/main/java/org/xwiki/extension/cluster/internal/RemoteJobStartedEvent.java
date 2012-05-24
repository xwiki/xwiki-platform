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
package org.xwiki.extension.cluster.internal;

import java.util.List;

import org.xwiki.job.Request;
import org.xwiki.job.event.JobStartedEvent;

/**
 * Internal event used to start a new job.
 * 
 * @version $Id$
 * @since 4.0M1
 */
public class RemoteJobStartedEvent extends JobStartedEvent
{
    /**
     * Match any {@link RemoteJobStartedEvent}.
     */
    public RemoteJobStartedEvent()
    {

    }

    /**
     * @param jobType the event related job type
     * @param request the event related job request
     */
    public RemoteJobStartedEvent(String jobType, Request request)
    {
        super((List<String>) null, jobType, request);
    }

    /**
     * @param jobEvent the event to copy
     */
    public RemoteJobStartedEvent(JobStartedEvent jobEvent)
    {
        super(jobEvent);
    }

}
