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
package org.xwiki.refactoring.job;

import org.xwiki.job.DefaultJobStatus;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

/**
 * Generic job status for a job that performs an {@link EntityRequest}.
 * 
 * @param <T> the request type
 * @version $Id$
 * @since 7.2M1
 */
public class EntityJobStatus<T extends EntityRequest> extends DefaultJobStatus<T>
{
    /**
     * Creates a new instance.
     * 
     * @param request the request provided when the job was started
     * @param observationManager the observation manager
     * @param loggerManager the logger manager
     * @param parentJobStatus the status of the parent job, or {@code null} if this job doesn't have a parent
     * @deprecated since 9.2RC1, use
     *             {@link #EntityJobStatus(String, EntityRequest, ObservationManager, LoggerManager, JobStatus)} instead
     */
    public EntityJobStatus(T request, ObservationManager observationManager, LoggerManager loggerManager,
        JobStatus parentJobStatus)
    {
        super(request, parentJobStatus, observationManager, loggerManager);

        setCancelable(true);
    }

    /**
     * Creates a new instance.
     * 
     * @param jobType the type of the job
     * @param request the request provided when the job was started
     * @param observationManager the observation manager
     * @param loggerManager the logger manager
     * @param parentJobStatus the status of the parent job, or {@code null} if this job doesn't have a parent
     */
    public EntityJobStatus(String jobType, T request, ObservationManager observationManager,
        LoggerManager loggerManager, JobStatus parentJobStatus)
    {
        super(jobType, request, parentJobStatus, observationManager, loggerManager);

        setCancelable(true);
    }
}
