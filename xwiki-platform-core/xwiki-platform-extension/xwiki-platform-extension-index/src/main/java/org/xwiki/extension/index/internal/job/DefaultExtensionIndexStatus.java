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
package org.xwiki.extension.index.internal.job;

import org.xwiki.extension.index.ExtensionIndexStatus;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

/**
 * Default implementation of {@link ExtensionIndexStatus}.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
public class DefaultExtensionIndexStatus extends DefaultJobStatus<ExtensionIndexRequest> implements ExtensionIndexStatus
{
    /**
     * @param request the request provided when started the job
     * @param observationManager the observation manager component
     * @param loggerManager the logger manager component
     */
    public DefaultExtensionIndexStatus(ExtensionIndexRequest request, ObservationManager observationManager,
        LoggerManager loggerManager)
    {
        super(ExtensionIndexJob.JOB_TYPE, request, null, observationManager, loggerManager);
    }
}
