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

import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.export.DocumentSelectionResolver;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultPDFExportJobRequestFactory}.
 * 
 * @version $Id$
 */
@ComponentTest
class DefaultPDFExportJobRequestFactoryTest
{
    @InjectMockComponents
    private DefaultPDFExportJobRequestFactory requestFactory;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentSelectionResolver documentSelectionResolver;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    @Named("local")
    private EntityReferenceSerializer<String> localStringEntityReferenceSerializer;

    @MockComponent
    private ContextStoreManager contextStoreManager;

    @Test
    void createRequest() throws Exception
    {
        // Setup

        DocumentReference aliceReference = new DocumentReference("test", "Users", "Alice");
        DocumentReference bobReference = new DocumentReference("test", "Users", "Bob");

        XWikiContext xcontext = mock(XWikiContext.class);
        when(xcontext.getUserReference()).thenReturn(aliceReference);
        when(xcontext.getAuthorReference()).thenReturn(bobReference);

        XWikiRequest httpRequest = mock(XWikiRequest.class);
        when(xcontext.getRequest()).thenReturn(httpRequest);
        when(httpRequest.get("pdfQueryString")).thenReturn("color=red");
        when(httpRequest.get("pdfHash")).thenReturn("foo");

        XWikiDocument currentDocument = mock(XWikiDocument.class);
        when(currentDocument.getDocumentReference()).thenReturn(new DocumentReference("test", "Some", "Page"));
        when(xcontext.getDoc()).thenReturn(currentDocument);
        when(this.localStringEntityReferenceSerializer
            .serialize(currentDocument.getDocumentReference().getLastSpaceReference())).thenReturn("Some");

        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(xcontext.getURLFactory()).thenReturn(urlFactory);

        URL printPreviewURL = new URL("http://localhost:8080/xwiki/bin/export/Some/Page?key=value#hash");
        String queryString = "format=html-print&xpage=get&outputSyntax=plain&async=true&"
            + "sheet=XWiki.PDFExport.Sheet&jobId=export%2Fpdf%2Ftest&color=red";
        when(urlFactory.createExternalURL("Some", "Page", "export", queryString, "foo", "test", xcontext))
            .thenReturn(printPreviewURL);

        when(this.xcontextProvider.get()).thenReturn(xcontext);

        List<String> supportedContextEntries = Arrays.asList("one", "two");
        when(this.contextStoreManager.getSupportedEntries()).thenReturn(supportedContextEntries);

        Map<String, Serializable> pdfExportContext = new HashMap<>();
        when(this.contextStoreManager.save(supportedContextEntries)).thenReturn(pdfExportContext);

        List<DocumentReference> selectedDocuments = Arrays.asList(new DocumentReference("test", "First", "Page"),
            new DocumentReference("test", "Second", "Page"));
        when(this.documentSelectionResolver.getSelectedDocuments()).thenReturn(selectedDocuments);

        when(httpRequest.get("pdftemplate")).thenReturn("Some.Template");
        DocumentReference templateReference = new DocumentReference("test", "Some", "Template");
        when(this.currentDocumentReferenceResolver.resolve("Some.Template")).thenReturn(templateReference);

        when(httpRequest.get("pdfcover")).thenReturn("1");
        when(httpRequest.get("pdftoc")).thenReturn("0");
        when(httpRequest.get("pdfheader")).thenReturn("false");

        // Execution

        PDFExportJobRequest request = this.requestFactory.createRequest("test");

        // Checks

        assertEquals(3, request.getId().size());
        assertEquals(Arrays.asList("export", "pdf"), request.getId().subList(0, 2));

        assertTrue(request.isCheckRights());
        assertEquals(aliceReference, request.getUserReference());
        assertEquals(bobReference, request.getAuthorReference());

        assertEquals("export", request.getContext().get("action"));
        @SuppressWarnings("unchecked")
        Map<String, String[]> requestParameters =
            (Map<String, String[]>) request.getContext().get("request.parameters");
        assertEquals(Collections.singleton("key"), requestParameters.keySet());
        assertEquals(Collections.singletonList("value"), Arrays.asList(requestParameters.get("key")));
        assertEquals(printPreviewURL, request.getContext().get("request.url"));

        assertEquals(selectedDocuments, request.getDocuments());
        assertEquals(templateReference, request.getTemplate());
        assertTrue(request.isWithCover());
        assertFalse(request.isWithToc());
        assertTrue(request.isWithHeader());
        assertTrue(request.isWithFooter());
    }
}
