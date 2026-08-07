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
package org.xwiki.repository.job;

import java.util.List;

import org.xwiki.extension.ExtensionId;
import org.xwiki.job.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.stability.Unstable;

/**
 * The status of the XIP export job.
 * 
 * @version $Id$
 * @since 18.5.0RC1
 */
@Unstable
public class XIPExportJobStatus extends DefaultJobStatus<XIPExportJobRequest>
{
    private final TemporaryResourceReference xipFileReference;

    /**
     * Create a new XIP export job status.
     * 
     * @param jobType the job type
     * @param request the request provided when the job was started
     * @param observationManager the observation manager
     * @param loggerManager the logger manager
     */
    public XIPExportJobStatus(String jobType, XIPExportJobRequest request, ObservationManager observationManager,
        LoggerManager loggerManager)
    {
        super(jobType, request, null, observationManager, loggerManager);

        setCancelable(true);
        String idSuffix = request.getId().get(request.getId().size() - 1);
        this.xipFileReference = new TemporaryResourceReference("repository", List.of("xip", idSuffix + ".xip"),
            request.getAuthorReference());
        ExtensionId extensionId = request.getExtension();
        this.xipFileReference.addParameter("fileName", "%s-%s-xwiki-%s.xip".formatted(extensionId.getId(),
            extensionId.getVersion().getValue(), request.getXWikiVersion()));
    }

    /**
     * @return the reference of the generated temporary XIP file
     */
    public TemporaryResourceReference getXIPFileReference()
    {
        return this.xipFileReference;
    }
}
