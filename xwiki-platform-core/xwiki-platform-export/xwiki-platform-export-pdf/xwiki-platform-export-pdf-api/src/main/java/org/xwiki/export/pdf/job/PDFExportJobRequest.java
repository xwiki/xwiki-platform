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

import java.util.Collections;
import java.util.List;

import org.xwiki.job.Request;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Represents a request to export multiple XWiki documents as PDF.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Unstable
public class PDFExportJobRequest extends AbstractCheckRightsRequest
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    private static final String PROPERTY_DOCUMENTS = "documents";

    private static final String PROPERTY_TEMPLATE = "template";

    private static final String PROPERTY_COVER = "cover";

    private static final String PROPERTY_TOC = "toc";

    private static final String PROPERTY_HEADER = "header";

    private static final String PROPERTY_FOOTER = "footer";

    private static final String PROPERTY_SERVER_SIDE = "serverSide";

    /**
     * Default constructor.
     */
    public PDFExportJobRequest()
    {
    }

    /**
     * @param request the request to copy
     * @since 14.7RC1
     * @since 14.4.4
     * @since 13.10.9
     */
    public PDFExportJobRequest(Request request)
    {
        super(request);
    }

    /**
     * @return the list of documents to include in the PDF export; the PDF export job will render each of these
     *         documents to HTML and aggregate the results using the PDF template, printing the result to PDF in the
     *         end; the first document in the list is considered the main document and will be used to generate the
     *         cover page, table of contents, header and footer
     */
    public List<DocumentReference> getDocuments()
    {
        return getProperty(PROPERTY_DOCUMENTS, Collections.emptyList());
    }

    /**
     * Sets the list of documents to export as PDF.
     * 
     * @param documents the list of documents to export as PDF
     */
    public void setDocuments(List<DocumentReference> documents)
    {
        setProperty(PROPERTY_DOCUMENTS, documents);
    }

    /**
     * @return the PDF template reference, i.e. the document that controls what is displayed in the cover page, table of
     *         contents, header and footer, as well as the styles used when generating the PDF; the PDF template should
     *         have an object of type {@code XWiki.PDFExport.TemplateClass}
     */
    public DocumentReference getTemplate()
    {
        return getProperty(PROPERTY_TEMPLATE);
    }

    /**
     * Sets the PDF template.
     * 
     * @param template the PDF template to use
     */
    public void setTemplate(DocumentReference template)
    {
        setProperty(PROPERTY_TEMPLATE, template);
    }

    /**
     * @return whether to generate the cover page or not
     */
    public boolean isWithCover()
    {
        return getProperty(PROPERTY_COVER, true);
    }

    /**
     * Sets whether to generate the cover page or not.
     * 
     * @param withCover {@code true} to generate the cover page, {@code false} otherwise
     */
    public void setWithCover(boolean withCover)
    {
        setProperty(PROPERTY_COVER, withCover);
    }

    /**
     * @return whether to generate the table of contents page or not
     */
    public boolean isWithToc()
    {
        return getProperty(PROPERTY_TOC, true);
    }

    /**
     * Sets whether to generate the table of contents page or not.
     * 
     * @param withToc {@code true} to generate the table of contents page, {@code false} otherwise
     */
    public void setWithToc(boolean withToc)
    {
        setProperty(PROPERTY_TOC, withToc);
    }

    /**
     * @return whether to generate the page header or not
     */
    public boolean isWithHeader()
    {
        return getProperty(PROPERTY_HEADER, true);
    }

    /**
     * Sets whether to generate the page header or not.
     * 
     * @param withHeader {@code true} to generate the page header, {@code false} otherwise
     */
    public void setWithHeader(boolean withHeader)
    {
        setProperty(PROPERTY_HEADER, withHeader);
    }

    /**
     * @return whether to generate the page footer or not
     */
    public boolean isWithFooter()
    {
        return getProperty(PROPERTY_FOOTER, true);
    }

    /**
     * Sets whether to generate the page footer or not.
     * 
     * @param withFooter {@code true} to generate the page footer, {@code false} otherwise
     */
    public void setWithFooter(boolean withFooter)
    {
        setProperty(PROPERTY_FOOTER, withFooter);
    }

    /**
     * @return whether to generate the PDF file server-side, as a temporary resource (using a headless browser), or
     *         client-side using the user's browser
     */
    public boolean isServerSide()
    {
        return getProperty(PROPERTY_SERVER_SIDE, true);
    }

    /**
     * Sets whether to generate the PDF file server-side, as a temporary resource, or client-side.
     * 
     * @param serverSide {@code true} to generate the PDF file server-side, {@code false} to let the users print the PDF
     *            file themselves
     */
    public void setServerSide(boolean serverSide)
    {
        setProperty(PROPERTY_SERVER_SIDE, serverSide);
    }
}
