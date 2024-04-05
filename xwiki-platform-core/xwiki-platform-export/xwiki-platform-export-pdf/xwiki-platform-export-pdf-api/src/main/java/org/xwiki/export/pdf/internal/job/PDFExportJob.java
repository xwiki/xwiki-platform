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
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.internal.RequiredSkinExtensionsRecorder;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.export.pdf.job.PDFExportJobStatus.DocumentRenderingResult;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
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
public class PDFExportJob extends AbstractPDFExportJob
{
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

    @Inject
    private PrintPreviewURLBuilder printPreviewURLBuilder;

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
            // We use the same rendering parameters for all the documents included in this PDF export.
            DocumentRendererParameters rendererParameters = getDocumentRendererParameters();

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
                        contentSize += render(documentReference, rendererParameters);
                        // We enforce the maximum content size (if specified) only when multiple pages are exported
                        // because for computing the aggregated table of contents we're currently keeping in memory the
                        // XDOM of each of the included pages which for large exports can take a considerable amount of
                        // memory. See https://jira.xwiki.org/browse/XWIKI-20377 .
                        if (contentSize > maxContentSize && maxContentSize > 0 && documentReferences.size() > 1) {
                            throw new RuntimeException(String.format(
                                "The content size exceeds the configured %sKB limit."
                                    + " Wiki administrators can increase or disable this limit from the PDF Export "
                                    + "administration section or from XWiki properties.",
                                this.configuration.getMaxContentSize()));
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

    private DocumentRendererParameters getDocumentRendererParameters()
    {
        DocumentRendererParameters rendererParameters =
            new DocumentRendererParameters().withTitle(this.request.isWithTitle());
        DocumentReference templateReference = this.request.getTemplate();
        if (templateReference != null && hasAccess(Right.VIEW, templateReference)) {
            rendererParameters.withMetadataReference(new ObjectPropertyReference("metadata",
                new ObjectReference("XWiki.PDFExport.TemplateClass[0]", templateReference)));
        }
        return rendererParameters;
    }

    private int render(DocumentReference documentReference, DocumentRendererParameters rendererParameters)
        throws Exception
    {
        // TODO: Don't render the same document twice.
        // TODO: Collect the XDOMs only when the table of content is requested.
        // TODO: Keep only the headings in the collected XDOMs in order to reduce the memory footprint.
        DocumentRenderingResult renderingResult = this.documentRenderer.render(documentReference, rendererParameters);
        this.status.getDocumentRenderingResults().add(renderingResult);

        // We approximate the size by counting the characters, which take 1 byte most of the time. We don't have to be
        // very precise.
        return renderingResult.getHTML().length();
    }

    private void saveAsPDF() throws IOException
    {
        URL printPreviewURL = this.printPreviewURLBuilder.getPrintPreviewURL(this.request);
        try (InputStream pdfContent = this.pdfPrinterProvider.get().print(printPreviewURL)) {
            if (!this.status.isCanceled()) {
                this.temporaryResourceStore.createTemporaryFile(this.status.getPDFFileReference(), pdfContent);
            }
        }
    }
}
