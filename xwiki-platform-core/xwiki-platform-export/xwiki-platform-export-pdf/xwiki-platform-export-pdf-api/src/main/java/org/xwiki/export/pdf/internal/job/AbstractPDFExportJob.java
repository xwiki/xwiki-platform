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
package org.xwiki.export.pdf.internal.job;

import java.util.Arrays;

import javax.inject.Inject;

import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobStatus;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * Base class for PDF export job.
 * 
 * @version $Id$
 */
public abstract class AbstractPDFExportJob extends AbstractJob<PDFExportJobRequest, PDFExportJobStatus>
    implements GroupedJob
{
    /**
     * The PDF export job type.
     */
    public static final String JOB_TYPE = "export/pdf";

    /**
     * Used to check access permissions.
     * 
     * @see #hasAccess(Right, EntityReference)
     */
    @Inject
    private AuthorizationManager authorization;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    public JobGroupPath getGroupPath()
    {
        return new JobGroupPath(Arrays.asList("export", "pdf"));
    }

    @Override
    protected PDFExportJobStatus createNewStatus(PDFExportJobRequest request)
    {
        return new PDFExportJobStatus(getType(), request, this.observationManager, this.loggerManager);
    }

    /**
     * Check access rights taking into account the job request.
     * 
     * @param right the access right to check
     * @param reference the target entity reference
     * @return return {@code true} if the current user or the entity author have the specified access right on the
     *         specified entity, depending on the job request
     */
    protected boolean hasAccess(Right right, EntityReference reference)
    {
        return ((!this.request.isCheckRights()
            || this.authorization.hasAccess(right, this.request.getUserReference(), reference))
            && (!this.request.isCheckAuthorRights()
                || this.authorization.hasAccess(right, this.request.getAuthorReference(), reference)));
    }
}
