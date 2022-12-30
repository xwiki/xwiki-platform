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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.internal.RequiredSkinExtensionsRecorder;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobStatus;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.GroupedJob;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

/**
 * The PDF export job.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@Named(PDFExportJob.JOB_TYPE)
public class PDFExportJob extends AbstractJob<PDFExportJobRequest, PDFExportJobStatus> implements GroupedJob
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

    @Inject
    private DocumentRenderer documentRenderer;

    @Inject
    private RequiredSkinExtensionsRecorder requiredSkinExtensionsRecorder;

    /**
     * We use a provider instead of direct injection because we want the printer to be initialized only when server-side
     * PDF printing is requested, as per {@link PDFExportJobRequest#isServerSide()}.
     */
    @Inject
    @Named("chrome")
    private Provider<PDFPrinter<URL>> pdfPrinterProvider;

    @Inject
    private TemporaryResourceStore temporaryResourceStore;

    @Inject
    private PDFExportConfiguration configuration;

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

    @Override
    protected void runInternal() throws Exception
    {
        if (!this.request.getDocuments().isEmpty()) {
            this.requiredSkinExtensionsRecorder.start();
            render(this.request.getDocuments());
            if (!this.status.isCanceled()) {
                this.status.setRequiredSkinExtensions(this.requiredSkinExtensionsRecorder.stop());
            }

            if (this.request.isServerSide() && !this.status.isCanceled()) {
                saveAsPDF();
                this.status.getDocumentRenderingResults().clear();
            }
        }
    }

    private void render(List<DocumentReference> documentReferences) throws Exception
    {
        this.progressManager.pushLevelProgress(documentReferences.size(), this);

        try {
            // The max content size configuration is expressed in kilobytes (KB), so we approximate the actual limit by
            // multiplying with 1000 (bytes).
            int maxContentSize = this.configuration.getMaxContentSize() * 1000;
            int contentSize = 0;
            for (DocumentReference documentReference : documentReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);
                    if (hasAccess(Right.VIEW, documentReference)) {
                        contentSize += render(documentReference);
                        if (contentSize > maxContentSize && maxContentSize != 0) {
                            throw new RuntimeException("Maximum content size limit exceeded.");
                        }
                    }
                    Thread.yield();
                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private int render(DocumentReference documentReference) throws Exception
    {
        // TODO: Don't render the same document twice.
        // TODO: Collect the XDOMs only when the table of content is requested.
        // TODO: Keep only the headings in the collected XDOMs in order to reduce the memory footprint.
        DocumentRenderingResult renderingResult =
            this.documentRenderer.render(documentReference, this.request.isWithTitle());
        this.status.getDocumentRenderingResults().add(renderingResult);

        // We approximate the size by counting the characters, which take 1 byte most of the time. We don't have to be
        // very precise.
        return renderingResult.getHTML().length();
    }

    /**
     * Check access rights taking into account the job request.
     * 
     * @param right the access right to check
     * @param reference the target entity reference
     * @return return {@code true} if the current user or the entity author have the specified access right on the
     *         specified entity, depending on the job request
     */
    private boolean hasAccess(Right right, EntityReference reference)
    {
        return ((!this.request.isCheckRights()
            || this.authorization.hasAccess(right, this.request.getUserReference(), reference))
            && (!this.request.isCheckAuthorRights()
                || this.authorization.hasAccess(right, this.request.getAuthorReference(), reference)));
    }

    private void saveAsPDF() throws IOException
    {
        URL printPreviewURL = (URL) this.request.getContext().get("request.url");
        try (InputStream pdfContent = this.pdfPrinterProvider.get().print(printPreviewURL)) {
            if (!this.status.isCanceled()) {
                this.temporaryResourceStore.createTemporaryFile(this.status.getPDFFileReference(), pdfContent);
            }
        }
    }
}
