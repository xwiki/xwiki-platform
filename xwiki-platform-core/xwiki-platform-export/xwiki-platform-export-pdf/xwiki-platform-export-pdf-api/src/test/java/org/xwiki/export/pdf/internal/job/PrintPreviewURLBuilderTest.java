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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

/**
 * Unit tests for {@link PrintPreviewURLBuilder}.
 * 
 * @version $Id$
 */
@ComponentTest
class PrintPreviewURLBuilderTest
{
    @InjectMockComponents
    private PrintPreviewURLBuilder builder;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    private PDFExportJobRequest request = new PDFExportJobRequest();

    private DocumentReference currentDocumentReference = new DocumentReference("test", "Current", "Page");

    @Test
    void getPrintPreviewURL() throws Exception
    {
        this.request.setId("export", "pdf", "123");
        this.request.setBaseURL(new URL("http://localhost:8080/xwiki/bin/view/Some/Page?ke%7Cy=va%7Clue#fo%7Co"));

        when(this.documentAccessBridge.getCurrentDocumentReference()).thenReturn(currentDocumentReference);
        URL printPreviewURL = new URL("http://localhost:8080/print/preview/url");
        when(this.documentAccessBridge.getDocumentURL(currentDocumentReference, "export",
            "format=html-print&xpage=get&outputSyntax=plain&async=true"
                + "&sheet=XWiki.PDFExport.Sheet&jobId=export%2Fpdf%2F123&ke%7Cy=va%7Clue",
            "fo|o", true)).thenReturn(printPreviewURL.toString());

        assertEquals(printPreviewURL, this.builder.getPrintPreviewURL(this.request));
    }
}
