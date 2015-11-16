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
import org.xwiki.stability.Unstable;

/**
 * Generic job status for a job that performs an {@link EntityRequest}.
 * 
 * @param <T> the request type
 * @version $Id$
 * @since 7.2M1
 */
@Unstable
public class EntityJobStatus<T extends EntityRequest> extends DefaultJobStatus<T>
{
    /**
     * Flag indicating if the job was canceled.
     */
    private boolean canceled;

    /**
     * Creates a new instance.
     * 
     * @param request the request provided when the job was started
     * @param observationManager the observation manager
     * @param loggerManager the logger manager
     * @param parentJobStatus the status of the parent job, or {@code null} if this job doesn't have a parent
     */
    public EntityJobStatus(T request, ObservationManager observationManager, LoggerManager loggerManager,
        JobStatus parentJobStatus)
    {
        super(request, parentJobStatus, observationManager, loggerManager);
    }

    /**
     * Cancel the job.
     */
    public void cancel()
    {
        this.canceled = true;
    }

    /**
     * @return {@code true} if the job was canceled, {@code false} otherwise
     */
    public boolean isCanceled()
    {
        return this.canceled;
    }
}
