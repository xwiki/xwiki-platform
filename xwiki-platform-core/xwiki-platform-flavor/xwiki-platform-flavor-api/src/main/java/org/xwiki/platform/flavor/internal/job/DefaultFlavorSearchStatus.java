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
package org.xwiki.platform.flavor.internal.job;

import java.util.Collections;
import java.util.List;

import org.xwiki.extension.Extension;
import org.xwiki.extension.job.plan.internal.DefaultExtensionPlan;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.platform.flavor.job.FlavorSearchRequest;

/**
 * A plan of extension related actions to perform.
 *
 * @version $Id$
 * @since 8.0RC1
 */
public class DefaultFlavorSearchStatus extends DefaultExtensionPlan<FlavorSearchRequest> implements FlavorSearchStatus
{
    /**
     * @see #getFlavors()
     */
    private List<Extension> flavors;

    /**
     * @param request the request provided when started the job
     * @param observationManager the observation manager component
     * @param loggerManager the logger manager component
     * @param flavors the flavors, it's not copied but taken as it is to allow filling it in the Job
     * @param parentJobStatus the status of the parent job (i.e. the status of the job that started this one); pass
     *            {@code null} if this job hasn't been started by another job (i.e. if this is not a sub-job)
     */
    public DefaultFlavorSearchStatus(FlavorSearchRequest request, ObservationManager observationManager,
        LoggerManager loggerManager, List<Extension> flavors, JobStatus parentJobStatus)
    {
        super(request, observationManager, loggerManager, null, parentJobStatus);

        this.flavors = Collections.unmodifiableList(flavors);
    }

    /**
     * @return the flavors found so far
     */
    public List<Extension> getFlavors()
    {
        return this.flavors;
    }

    @Override
    public String toString()
    {
        return this.flavors.toString();
    }
}
