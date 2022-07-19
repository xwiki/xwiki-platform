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
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.internal.RequiredSkinExtensionsRecorder;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobStatus;
import org.xwiki.job.AbstractJob;
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
public class PDFExportJob extends AbstractJob<PDFExportJobRequest, PDFExportJobStatus>
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
     * We use a provider instead of direct injection because:
     * <ul>
     * <li>the PDF printer connects to the Docker container in its initialization phase which can take some time and,
     * more importantly, can easily fail if the environment where XWiki runs was not set up properly (e.g. Docker not
     * installed, wrong version of Docker, missing network connection, Docker hub not accessible, etc.)</li>
     * <li>in case the PDF printer fails to be initialized we want the exception to appear in the job log</li>
     * <li>the PDF printer is used only when {@link PDFExportJobRequest#isServerSide()} returns {@code true} so it
     * doesn't make sense to initialize it (e.g. pull the Docker image, create and start the container, connect to the
     * head-less Chrome browser, etc.) unless it's used.</li>
     * </ul>
     */
    @Inject
    @Named("docker")
    private Provider<PDFPrinter<URL>> pdfPrinterProvider;

    @Inject
    private TemporaryResourceStore temporaryResourceStore;

    @Override
    public String getType()
    {
        return JOB_TYPE;
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
            for (DocumentReference documentReference : documentReferences) {
                if (this.status.isCanceled()) {
                    break;
                } else {
                    this.progressManager.startStep(this);
                    if (hasAccess(Right.VIEW, documentReference)) {
                        this.status.getDocumentRenderingResults().add(this.documentRenderer.render(documentReference));
                    }
                    Thread.yield();
                    this.progressManager.endStep(this);
                }
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }
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
