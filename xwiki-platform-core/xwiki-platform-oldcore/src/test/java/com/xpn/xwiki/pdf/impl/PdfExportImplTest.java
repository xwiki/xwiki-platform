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
package com.xpn.xwiki.pdf.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.w3c.dom.Document;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.environment.Environment;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.xml.EntityResolver;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLCleanerConfiguration;
import org.xwiki.xml.html.filter.HTMLFilter;
import org.xwiki.xml.internal.XMLReaderFactoryComponent;
import org.xwiki.xml.internal.html.DefaultHTMLCleanerConfiguration;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.pdf.XSLFORenderer;
import com.xpn.xwiki.pdf.api.PdfExport;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.web.XWikiServletRequestStub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PdfExportImpl}.
 *
 * @version $Id$
 */
@ComponentList({
    XMLReaderFactoryComponent.class,
})
@ReferenceComponentList
@OldcoreTest
class PdfExportImplTest
{
    private static final DocumentReference AUTHOR_REFERENCE = new DocumentReference("xwiki", "XWiki", "XWikiAdmin");

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "PDFClass");

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @Mock
    private VelocityEngine velocityEngine;

    @MockComponent
    private DocumentAuthorizationManager authorizationManager;

    @MockComponent
    private HTMLCleaner htmlCleaner;

    @MockComponent
    private AuthorExecutor authorExecutor;

    private String htmlContent;

    private String cssProperties;

    private XWikiContext context;

    private PdfExportImpl pdfExport;

    @BeforeEach
    void setUp() throws Exception
    {
        this.oldcore.getMocker().registerMockComponent(PDFResourceResolver.class);
        this.oldcore.getMocker().registerMockComponent(Environment.class);
        this.oldcore.getMocker().registerMockComponent(TemplateManager.class);
        this.oldcore.getMocker().registerMockComponent(ObservationManager.class);
        this.oldcore.getMocker().registerMockComponent(JobProgressManager.class);
        this.oldcore.getMocker().registerMockComponent(EntityResolver.class);
        this.oldcore.getMocker().registerMockComponent(XSLFORenderer.class, "fop");
        this.oldcore.getMocker().registerMockComponent(HTMLFilter.class, "uniqueId");

        // The content below allows us to test several points:
        // 1) The SPAN below already has some style defined in shorthand notation( "background" is shorthand,
        //    see https://www.w3schools.com/css/css_background.asp). That's important for the test since that's what was
        //    failing in the past and why this test was written.
        // 2) We also test that HTML entities are correctly kept since we had issues with this at one point.
        this.htmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n"
            + "<title>\n"
            + "  Main.ttt - ttt\n"
            + "</title>\n"
            + "<meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\" />\n"
            + "<meta content=\"en\" name=\"language\" />\n"
            + "\n"
            + "</head><body class=\"exportbody\" id=\"body\" pdfcover=\"0\" pdftoc=\"0\">\n"
            + "\n"
            + "<div id=\"xwikimaincontainer\">\n"
            + "<div id=\"xwikimaincontainerinner\">\n"
            + "\n"
            + "<div id=\"xwikicontent\">\n"
            + "      <p><span style=\"background: white;\">Hello Cl&eacute;ment</span></p>\n"
            + "          </div>\n"
            + "</div>\n"
            + "</div>\n"
            + "\n"
            + "</body></html>";

        this.cssProperties = "span { color:red; }";

        // Set up HTML cleaner.
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        // Prevent network requests to w3.org to fetch the DTD.
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        Document htmlDocument = factory.newDocumentBuilder()
            .parse(new ByteArrayInputStream(this.htmlContent.getBytes()));
        HTMLCleanerConfiguration cleanerConfiguration = new DefaultHTMLCleanerConfiguration();
        when(this.htmlCleaner.getDefaultConfiguration()).thenReturn(cleanerConfiguration);
        when(this.htmlCleaner.clean(any(StringReader.class), eq(cleanerConfiguration))).thenReturn(htmlDocument);

        // Get a mocked Velocity Engine.
        VelocityManager velocityManager = this.oldcore.getMocker().registerMockComponent(VelocityManager.class);
        when(velocityManager.getVelocityEngine()).thenReturn(this.velocityEngine);

        // Prepare a document reference and author reference for the template.
        XWikiDocument template = new XWikiDocument(DOCUMENT_REFERENCE);
        UserReferenceResolver<DocumentReference> userReferenceResolver =
            this.oldcore.getMocker().getInstance(UserReferenceResolver.TYPE_DOCUMENT_REFERENCE, "document");
        UserReference userReference = userReferenceResolver.resolve(AUTHOR_REFERENCE);
        template.getAuthors().setEffectiveMetadataAuthor(userReference);

        // Return a non-empty template property.
        DocumentAccessBridge dab = this.oldcore.getDocumentAccessBridge();
        when(dab.getProperty(template.getDocumentReference(), template.getDocumentReference(), "style"))
            .thenReturn(this.cssProperties);
        when(dab.getDocumentInstance(DOCUMENT_REFERENCE)).thenReturn(template);

        // Set necessary parameters in the request.
        this.context = this.oldcore.getXWikiContext();
        XWikiServletRequestStub request = new XWikiServletRequestStub();
        request.put("pdftemplate", "XWiki.PDFClass");
        this.context.setRequest(request);
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getExternalURL("view", this.context)).thenReturn("http://localhost:8080/export");
        this.context.setDoc(doc);

        this.pdfExport = new PdfExportImpl();
    }

    /**
     * Verify that PDF Export can apply some CSS on the XHTML when that XHTML already has some style defined and in
     * shorthand notation.
     */
    @Test
    void applyCSSWhenExistingStyleDefinedUsingShorthandNotation()
    {
        // - Verify that element's style attributes are normalized and that the SPAN's color is set to red.
        // - Verify that the accent in the content is still there.
        //   TODO: right now we output the DOM with DOM4J and use the default of converting entities when using the
        //   XMLWriter. We need to decide if that's correct or if we should call XMLWriter#setResolveEntityRefs(false)
        //   instead.

        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" "
            + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">"
            + "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head>\n"
            + "<title>\n"
            + "  Main.ttt - ttt\n"
            + "</title>\n"
            + "<meta content=\"text/html; charset=UTF-8\" http-equiv=\"Content-Type\"/>\n"
            + "<meta content=\"en\" name=\"language\"/>\n\n"
            + "</head><body class=\"exportbody\" id=\"body\" pdfcover=\"0\" pdftoc=\"0\">\n\n"
            + "<div id=\"xwikimaincontainer\">\n"
            + "<div id=\"xwikimaincontainerinner\">\n\n"
            + "<div id=\"xwikicontent\">\n"
            + "      <p><span style=\"color: #f00; background-color: #fff; background-image: none; "
            + "background-position: 0% 0%; background-size: auto auto; background-origin: padding-box; "
            + "background-clip: border-box; background-repeat: repeat repeat; "
            + "background-attachment: scroll; \">Hello Clément</span></p>\n"
            + "          </div>\n"
            + "</div>\n"
            + "</div>\n\n"
            + "</body></html>";

        assertEquals(expected, this.pdfExport.applyCSS(this.htmlContent, this.cssProperties, this.context));
    }

    /**
     * Verify that the Velocity Engine is never accessed if the user does not have script rights.
     */
    @Test
    void applyPDFTemplateWithoutScriptRights() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE,
            DOCUMENT_REFERENCE)).thenReturn(false);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.pdfExport.exportHtml(this.htmlContent, baos, PdfExport.ExportType.PDF, this.context);
        verify(this.authorizationManager)
            .hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE, DOCUMENT_REFERENCE);
        verifyNoInteractions(this.authorExecutor);
        verifyNoInteractions(this.velocityEngine);
    }

    /**
     * Verify that the Velocity Engine is not accessed outside an Author Executor.
     */
    @Test
    void applyPDFTemplateWithAuthorExecutor() throws Exception
    {
        when(this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE,
            DOCUMENT_REFERENCE)).thenReturn(true);

        // Do not call the callable to check that the call to the Velocity engine is inside the author executor.
        doReturn("").when(this.authorExecutor).call(any(), any(), any());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        this.pdfExport.exportHtml(this.htmlContent, baos, PdfExport.ExportType.PDF, this.context);
        verify(this.authorizationManager)
            .hasAccess(Right.SCRIPT, EntityType.DOCUMENT, AUTHOR_REFERENCE, DOCUMENT_REFERENCE);
        verify(this.authorExecutor).call(any(), eq(AUTHOR_REFERENCE), eq(DOCUMENT_REFERENCE));
        verifyNoInteractions(this.velocityEngine);
    }
}
