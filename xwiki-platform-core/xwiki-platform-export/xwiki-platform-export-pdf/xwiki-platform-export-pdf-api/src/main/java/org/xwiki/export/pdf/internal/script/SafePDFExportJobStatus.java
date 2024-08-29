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
package org.xwiki.export.pdf.internal.script;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.xwiki.export.pdf.job.PDFExportJobStatus;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.job.internal.script.safe.SafeCancelableJobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.script.safe.ScriptSafeProvider;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

/**
 * Safe version of {@link PDFExportJobStatus}.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class SafePDFExportJobStatus extends SafeCancelableJobStatus<PDFExportJobStatus>
{
    private final UserReferenceResolver<DocumentReference> userResolver;

    private final UserReferenceResolver<CurrentUserReference> currentUserResolver;

    /**
     * Creates a new safe instance that wraps the given unsafe instance.
     * 
     * @param status the wrapped object
     * @param safeProvider the provider of instances safe for public scripts
     * @param userResolver used to resolve the reference of the user that triggered the PDF export
     * @param currentUserResolver used to resolve the reference of the current user accessing the job status
     */
    public SafePDFExportJobStatus(PDFExportJobStatus status, ScriptSafeProvider<?> safeProvider,
        UserReferenceResolver<DocumentReference> userResolver,
        UserReferenceResolver<CurrentUserReference> currentUserResolver)
    {
        super(status, safeProvider);

        this.userResolver = userResolver;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * @return the result of rendering each document specified in the PDF export job request
     */
    public List<DocumentRenderingResult> getDocumentRenderingResults()
    {
        // Expose the document rendering results only to the user that triggered the export in order to prevent a data
        // leak. The user that triggered the export was set as context user during the export, when the documents were
        // rendered, so the output may be specific to this user (e.g. it may contain information that only this user
        // should see).
        if (currentUserTriggeredTheExport()) {
            return getWrapped().getDocumentRenderingResults();
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * @return the HTML that needs to be placed in the page head in order to pull the resources (JavaScript, CSS) that
     *         were asked during the rendering of the documents specified in the PDF export job request
     */
    public String getRequiredSkinExtensions()
    {
        // Expose the required skin extensions only to the user that triggered the export in order to prevent a data
        // leak. The user that triggered the export was set as context user during the export, when the documents were
        // rendered, so the output may be specific to this user (e.g. it may contain information that only this user
        // should see).
        if (currentUserTriggeredTheExport()) {
            return getWrapped().getRequiredSkinExtensions();
        } else {
            return "";
        }
    }

    /**
     * @return the reference of the generated temporary PDF file
     */
    public TemporaryResourceReference getPDFFileReference()
    {
        // Expose the PDF file reference only to the user that triggered the export in order to prevent a data leak. The
        // user that triggered the export was set as context user during the export, when the documents were rendered,
        // so the generated PDF file may contain information that only this user should see.
        if (currentUserTriggeredTheExport()) {
            return getWrapped().getPDFFileReference();
        } else {
            return null;
        }
    }

    @Override
    public void cancel()
    {
        // Allow the user that triggered the export to cancel it.
        if (currentUserTriggeredTheExport()) {
            getWrapped().cancel();
        }
    }

    private boolean currentUserTriggeredTheExport()
    {
        UserReference currentUserReference = this.currentUserResolver.resolve(CurrentUserReference.INSTANCE);
        UserReference userReference = this.userResolver.resolve(getWrapped().getRequest().getUserReference());
        return Objects.equals(userReference, currentUserReference);
    }
}
