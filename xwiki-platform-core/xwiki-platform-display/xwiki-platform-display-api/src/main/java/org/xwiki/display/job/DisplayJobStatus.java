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
package org.xwiki.display.job;

import org.xwiki.display.internal.job.DisplayJob;
import org.xwiki.job.AbstractJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

/**
 * The status of the {@link DisplayJob}.
 * 
 * @version $Id$
 * @since 10.9RC1
 */
public class DisplayJobStatus extends AbstractJobStatus<DisplayJobRequest>
{
    /**
     * @param request the request provided when started the job
     * @param observationManager the observation manager component
     * @param loggerManager the logger manager component
     */
    public DisplayJobStatus(DisplayJobRequest request, ObservationManager observationManager,
        LoggerManager loggerManager)
    {
        super(DisplayJob.JOB_TYPE, request, null, observationManager, loggerManager);

        setIsolated(false);
    }
}
