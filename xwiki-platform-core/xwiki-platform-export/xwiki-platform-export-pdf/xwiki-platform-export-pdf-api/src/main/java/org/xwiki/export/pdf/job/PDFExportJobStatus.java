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
package org.xwiki.export.pdf.job;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.xwiki.job.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.resource.temporary.TemporaryResourceReference;

/**
 * The status of the PDF export job.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
public class PDFExportJobStatus extends DefaultJobStatus<PDFExportJobRequest>
{
    /**
     * The result obtained by rendering a given document.
     * 
     * @version $Id$
     */
    public static class DocumentRenderingResult
    {
        private final DocumentReference documentReference;

        /**
         * Don't serialize the XDOM because it can lead to a huge XML.
         */
        private final transient XDOM xdom;

        private final String html;

        private final Map<String, String> idMap;

        /**
         * Create a new rendering result for the specified document.
         * 
         * @param documentReference the document that has been rendered
         * @param xdom the XDOM obtained by rendering the specified document
         * @param html the HTML obtained by rendering the specified document
         */
        public DocumentRenderingResult(DocumentReference documentReference, XDOM xdom, String html)
        {
            this(documentReference, xdom, html, Collections.emptyMap());
        }

        /**
         * Create a new rendering result for the specified document.
         * 
         * @param documentReference the document that has been rendered
         * @param xdom the XDOM obtained by rendering the specified document
         * @param html the HTML obtained by rendering the specified document
         * @param idMap the mapping between local IDs (that would have been generated if the document were rendered
         *            alone) and global IDs (that were actually generated when the document was rendered together with
         *            the other documents included in the PDF export); this mapping can be used to convert external
         *            links into internal links
         * @since 14.10.6
         * @since 15.1RC1
         */
        public DocumentRenderingResult(DocumentReference documentReference, XDOM xdom, String html,
            Map<String, String> idMap)
        {
            this.documentReference = documentReference;
            this.xdom = xdom;
            this.html = html;
            this.idMap = idMap;
        }

        /**
         * @return the document that has been rendered
         */
        public DocumentReference getDocumentReference()
        {
            return documentReference;
        }

        /**
         * @return the XDOM obtained by rendering the specified document, after executing the rendering transformations;
         *         this is needed in order to generate the table of contents for the PDF export
         */
        public XDOM getXDOM()
        {
            return xdom;
        }

        /**
         * @return the HTML obtained by rendering the specified document; the PDF sheet aggregates the HTML from all the
         *         exported documents in order to produce the PDF file
         */
        public String getHTML()
        {
            return html;
        }

        /**
         * @return the mapping between local IDs (that would have been generated if the document were rendered alone)
         *         and global IDs (that were actually generated when the document was rendered together with the other
         *         documents included in the PDF export); this mapping can be used to convert external links into
         *         internal links
         * @since 14.10.6
         * @since 15.1RC1
         */
        public Map<String, String> getIdMap()
        {
            return Collections.unmodifiableMap(this.idMap);
        }
    }

    private final List<DocumentRenderingResult> documentRenderingResults = new LinkedList<>();

    private final TemporaryResourceReference pdfFileReference;

    private String requiredSkinExtensions;

    /**
     * Create a new PDF export job status.
     * 
     * @param jobType the job type
     * @param request the request provided when the job was started
     * @param observationManager the observation manager
     * @param loggerManager the logger manager
     */
    public PDFExportJobStatus(String jobType, PDFExportJobRequest request, ObservationManager observationManager,
        LoggerManager loggerManager)
    {
        super(jobType, request, null, observationManager, loggerManager);

        setCancelable(true);
        if (request.getDocuments().isEmpty() || !request.isServerSide()) {
            this.pdfFileReference = null;
        } else {
            this.pdfFileReference = new TemporaryResourceReference("export",
                Arrays.asList("pdf", UUID.randomUUID().toString() + ".pdf"), request.getDocuments().get(0));
            this.pdfFileReference.addParameter("fileName", request.getFileName());
        }
    }

    /**
     * @return the result of rendering each document specified in the PDF export job request
     */
    public List<DocumentRenderingResult> getDocumentRenderingResults()
    {
        return this.documentRenderingResults;
    }

    /**
     * @return the reference of the generated temporary PDF file
     */
    public TemporaryResourceReference getPDFFileReference()
    {
        return this.pdfFileReference;
    }

    /**
     * @return the HTML that needs to be placed in the page head in order to pull the resources (JavaScript, CSS) that
     *         were asked during the rendering of the documents specified in the PDF export job request
     */
    public String getRequiredSkinExtensions()
    {
        return requiredSkinExtensions;
    }

    /**
     * Sets the skin extensions required by the rendered documents.
     *
     * @param requiredSkinExtensions the HTML that needs to be placed in the page head in order to pull the skin
     *            extensions (JavaScript, CSS) required by the rendered documents
     */
    public void setRequiredSkinExtensions(String requiredSkinExtensions)
    {
        this.requiredSkinExtensions = requiredSkinExtensions;
    }
}
