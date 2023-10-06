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
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.model.reference.DocumentReference;

/**
 * Builds the print preview URL.
 * 
 * @version $Id$
 * @since 14.10.18
 * @since 15.5.3
 * @since 15.8
 */
@Component(roles = PrintPreviewURLBuilder.class)
@Singleton
public class PrintPreviewURLBuilder
{
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * The print preview URL is used to load the PDF template and the document rendering results (HTML) in the web
     * browser before printing to PDF. It should match as much as possible the base (context) URL that was used when the
     * PDF export job rendered the documents. The print preview uses the export action instead of view and it needs some
     * additional query string parameters (such as the PDF export job id and the PDF sheet) that should have priority.
     * Besides that it should match the base URL (e.g. same target document, same fragment identifier).
     * 
     * @param request the PDF export job request
     * @return the print preview URL
     * @throws IOException if the URL cannot be created
     */
    public URL getPrintPreviewURL(PDFExportJobRequest request) throws IOException
    {
        // Extend the base query string with the parameters required by the print preview.
        String baseQueryString = Objects.toString(request.getBaseURL().getQuery(), "");
        String queryString = getPrintPreviewQueryString(baseQueryString, request.getId());

        // Use the same hash (fragment identifier) as the base URL. It's important to preserve the hash
        // because it can influence the output: e.g. the live table saves and reads its state from the hash.
        String hash = Objects.toString(request.getBaseURL().getRef(), "");

        // Note that URL#getRef() returns the raw value, similar to URI#getRawFragment(), so we need to decode it
        // (because the fragment identifer can contain special characters that were URL encoded when the base URL was
        // created). Otherwise the fragment identifier will be double encoded in the print preview URL.
        hash = URLDecoder.decode(hash, StandardCharsets.UTF_8);

        DocumentReference documentReference = this.documentAccessBridge.getCurrentDocumentReference();
        return new URL(this.documentAccessBridge.getDocumentURL(documentReference, "export", queryString, hash, true));
    }

    private String getPrintPreviewQueryString(String originalQueryString, List<String> pdfExportJobId)
    {
        List<NameValuePair> printPreviewParams = Arrays.asList(new BasicNameValuePair("format", "html-print"),
            new BasicNameValuePair("xpage", "get"), new BasicNameValuePair("outputSyntax", "plain"),
            // Asynchronous rendering is disabled by default on the export action so we need to force it.
            new BasicNameValuePair("async", "true"), new BasicNameValuePair("sheet", "XWiki.PDFExport.Sheet"),
            new BasicNameValuePair("jobId", StringUtils.join(pdfExportJobId, '/')));
        return URLEncodedUtils.format(printPreviewParams, StandardCharsets.UTF_8) + '&' + originalQueryString;
    }
}
