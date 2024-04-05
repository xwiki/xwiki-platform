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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.export.pdf.PDFExportConfiguration;
import org.xwiki.export.pdf.PDFPrinter;
import org.xwiki.export.pdf.job.PDFExportJobRequest;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.export.DocumentSelectionResolver;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;

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

    @MockComponent
    private PDFExportConfiguration configuration;

    @MockComponent
    @Named("chrome")
    private PDFPrinter<URL> pdfPrinter;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWikiRequest httpRequest;

    @BeforeEach
    void configure()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getRequest()).thenReturn(this.httpRequest);

        XWikiDocument currentDocument = mock(XWikiDocument.class);
        when(currentDocument.getDocumentReference()).thenReturn(new DocumentReference("test", "Some", "Page"));
        when(currentDocument.getRenderedTitle(Syntax.PLAIN_1_0, this.xcontext)).thenReturn("Page Title");
        when(this.xcontext.getDoc()).thenReturn(currentDocument);
        when(this.localStringEntityReferenceSerializer
            .serialize(currentDocument.getDocumentReference().getLastSpaceReference())).thenReturn("Some");

        XWikiURLFactory urlFactory = mock(XWikiURLFactory.class);
        when(this.xcontext.getURLFactory()).thenReturn(urlFactory);
    }

    @Test
    void createRequest() throws Exception
    {
        // Setup

        DocumentReference aliceReference = new DocumentReference("test", "Users", "Alice");
        when(this.xcontext.getUserReference()).thenReturn(aliceReference);

        DocumentReference bobReference = new DocumentReference("test", "Users", "Bob");
        when(this.xcontext.getAuthorReference()).thenReturn(bobReference);

        when(this.httpRequest.get("pdfQueryString")).thenReturn("color=red");
        when(this.httpRequest.get("pdfHash")).thenReturn("foo");

        when(this.httpRequest.get("pdfcover")).thenReturn("1");
        when(this.httpRequest.get("pdftoc")).thenReturn("0");
        when(this.httpRequest.get("pdfheader")).thenReturn("false");

        when(this.httpRequest.get("pdftemplate")).thenReturn("Some.Template");
        DocumentReference templateReference = new DocumentReference("test", "Some", "Template");
        when(this.currentDocumentReferenceResolver.resolve("Some.Template")).thenReturn(templateReference);

        URL baseURL = new URL("http://localhost:8080/xwiki/bin/view/Some/Page?color=red#foo");
        when(this.xcontext.getURLFactory().createExternalURL("Some", "Page", "view", "color=red", "foo", "test",
            this.xcontext)).thenReturn(baseURL);

        List<String> supportedContextEntries = Arrays.asList("one", "two");
        when(this.contextStoreManager.getSupportedEntries()).thenReturn(supportedContextEntries);

        Map<String, Serializable> pdfExportContext = new HashMap<>();
        when(this.contextStoreManager.save(supportedContextEntries)).thenReturn(pdfExportContext);

        List<DocumentReference> selectedDocuments = Arrays.asList(new DocumentReference("test", "First", "Page"),
            new DocumentReference("test", "Second", "Page"));
        when(this.documentSelectionResolver.getSelectedDocuments(true)).thenReturn(selectedDocuments);

        // Execution

        PDFExportJobRequest request = this.requestFactory.createRequest("test");

        // Checks

        assertEquals(3, request.getId().size());
        assertEquals(Arrays.asList("export", "pdf"), request.getId().subList(0, 2));
        assertFalse(request.isServerSide());

        assertTrue(request.isCheckRights());
        assertEquals(aliceReference, request.getUserReference());
        assertEquals(bobReference, request.getAuthorReference());

        assertEquals("export", request.getContext().get("action"));
        @SuppressWarnings("unchecked")
        Map<String, String[]> requestParameters =
            (Map<String, String[]>) request.getContext().get("request.parameters");
        assertEquals(Collections.singleton("color"), requestParameters.keySet());
        assertEquals(Collections.singletonList("red"), Arrays.asList(requestParameters.get("color")));
        assertEquals(baseURL, request.getContext().get("request.url"));
        assertEquals(baseURL, request.getBaseURL());

        assertEquals(selectedDocuments, request.getDocuments());
        assertEquals(templateReference, request.getTemplate());
        assertTrue(request.isWithCover());
        assertFalse(request.isWithToc());
        assertTrue(request.isWithHeader());
        assertTrue(request.isWithFooter());

        assertEquals("Page Title.pdf", request.getFileName());
    }

    @Test
    void createRequestWithDefaultTemplate() throws Exception
    {
        when(this.configuration.isServerSide()).thenReturn(true);
        when(this.pdfPrinter.isAvailable()).thenReturn(true);

        DocumentReference templateReference =
            new DocumentReference("test", Arrays.asList("XWiki", "PDFExport"), "Template");
        when(this.currentDocumentReferenceResolver.resolve("XWiki.PDFExport.Template")).thenReturn(templateReference);

        when(this.xcontext.getURLFactory().createExternalURL(eq("Some"), eq("Page"), eq("export"), anyString(), eq(""),
            eq("test"), same(this.xcontext))).thenReturn(new URL("https://www.xwiki.org"));

        PDFExportJobRequest request = this.requestFactory.createRequest();
        assertTrue(request.isServerSide());
        assertEquals(templateReference, request.getTemplate());
    }
}
