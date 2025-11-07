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

package com.xpn.xwiki.internal.export;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.context.internal.DefaultExecutionContextManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.converter.OfficeConverter;
import org.xwiki.officeimporter.converter.OfficeConverterResult;
import org.xwiki.officeimporter.server.OfficeServer;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.xml.html.DefaultHTMLCleanerComponentList;
import org.xwiki.xml.internal.LocalEntityResolverComponent;
import org.xwiki.xml.internal.XMLReaderFactoryComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.pdf.XSLFORenderer;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OfficeExporter}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
@ComponentList({
    LocalEntityResolverComponent.class,
    XMLReaderFactoryComponent.class,
    // Needed to properly initialize the execution context in MockitoOldcore.
    DefaultExecutionContextManager.class
})
@DefaultHTMLCleanerComponentList
class OfficeExporterTest
{
    private static final DocumentReference CURRENT_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "Test", "TestDocument");

    @MockComponent
    private OfficeServer officeServer;

    @MockComponent
    private VelocityManager velocityManager;

    @MockComponent
    private AuthorExecutor authorExecutor;

    @MockComponent
    @Named("fop")
    private XSLFORenderer xslfoRenderer;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private OfficeExporter officeExporter;

    @BeforeEach
    void setUp(MockitoComponentManager componentManager)
    {
        when(this.oldcore.getDocumentAccessBridge().getCurrentDocumentReference())
            .thenReturn(CURRENT_DOCUMENT_REFERENCE);
        Utils.setComponentManager(componentManager);
        this.officeExporter = new OfficeExporter();
    }

    @Test
    void exportHTMLShouldReplaceElements() throws Exception
    {
        // Prepare input XHTML that contains <strong><em>...</em></strong> and <ins>...</ins> elements.
        String inputHTML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN"
                "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml">
                <body><p><strong style="color: red"><em>Bold and italic</em></strong><ins>Inserted</ins></p></body>
            </html>
            """;

        XWikiRequest request = mock();
        this.oldcore.getXWikiContext().setRequest(request);

        // Make sure that we can get an external URL for the current document.
        XWikiDocument currentDocument = this.oldcore.getSpyXWiki().getDocument(CURRENT_DOCUMENT_REFERENCE,
            this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(currentDocument);
        XWikiURLFactory urlFactory = mock();
        when(urlFactory.createExternalURL(currentDocument.getSpace(), currentDocument.getName(), "view", null, null,
            currentDocument.getDatabase(), this.oldcore.getXWikiContext()))
            .thenReturn(new URL("http://localhost/xwiki/bin/view/Test/TestDocument"));
        this.oldcore.getXWikiContext().setURLFactory(urlFactory);

        // Ensure no embedded objects to be added
        this.oldcore.getXWikiContext().put("pdfexport-file-mapping", Map.of());

        // Mock the office converter so we can capture the input HTML it receives.
        OfficeConverter converter = mock();
        when(this.officeServer.getConverter()).thenReturn(converter);

        // Return an empty result since we don't care about the output in this test.
        OfficeConverterResult converterResult = mock();
        when(converterResult.getAllFiles()).thenReturn(Set.of());
        when(converter.convertDocument(anyMap(), anyString(), anyString())).thenReturn(converterResult);

        // Verify that CSS can still be applied to strong elements.
        doReturn("strong { font-family: sans-serif; }")
            .when(this.oldcore.getSpyXWiki()).parseTemplate("pdf.css", this.oldcore.getXWikiContext());

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        // Simulate export to ODT.
        PdfExport.ExportType exportType = new PdfExport.ExportType("application/vnd.oasis.opendocument.text", "odt");

        this.officeExporter.exportHtml(inputHTML, out, exportType, this.oldcore.getXWikiContext());

        // Capture the HTML that would have been sent to the office converter.
        ArgumentCaptor<Map<String, InputStream>> captor = ArgumentCaptor.captor();
        ArgumentCaptor<String> inputFileNameCaptor = ArgumentCaptor.captor();
        verify(converter).convertDocument(captor.capture(), inputFileNameCaptor.capture(), anyString());

        Map<String, InputStream> inputStreams = captor.getValue();
        InputStream htmlInputStream = inputStreams.get(inputFileNameCaptor.getValue());
        String processedHtml = new String(htmlInputStream.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals("""
            <!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" \
            "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
            <html xmlns="http://www.w3.org/1999/xhtml"><head>\
            <META http-equiv="Content-Type" content="text/html; charset=UTF-8"></head><body>
                <p><b style="font-family: sans-serif; color: #f00; "><em>Bold and italic</em></b>\
            <span style="text-decoration:underline">Inserted</span></p>
            </body></html>""", processedHtml);
    }
}
