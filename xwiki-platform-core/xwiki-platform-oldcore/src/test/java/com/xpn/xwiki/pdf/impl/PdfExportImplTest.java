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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.AllLogRule;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcoreRule;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PdfExportImpl}.
 *
 * @version $Id$
 */
public class PdfExportImplTest
{
    @Rule
    public AllLogRule logRule = new AllLogRule();

    @Rule
    public MockitoOldcoreRule oldcoreRule = new MockitoOldcoreRule();

    /**
     * Verify that PDF Export can apply some CSS on the XHTML.
     */
    @Test
    @Ignore("Test for XWIKI-14241 which is currently failing")
    public void applyCSSWhenStyleDefined() throws Exception
    {
        this.oldcoreRule.getMocker().registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        this.oldcoreRule.getMocker().registerMockComponent(EntityReferenceSerializer.TYPE_STRING);
        this.oldcoreRule.getMocker().registerMockComponent(DocumentAccessBridge.class);
        this.oldcoreRule.getMocker().registerMockComponent(DocumentAccessBridge.class);
        this.oldcoreRule.getMocker().registerMockComponent(PDFResourceResolver.class);
        this.oldcoreRule.getMocker().registerMockComponent(Environment.class);
        this.oldcoreRule.getMocker().registerMockComponent(VelocityManager.class);

        PdfExportImpl pdfExport = new PdfExportImpl();

        String html = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
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
            + "      <p><span style=\"background: white;\">Hello</span></p>\n"
            + "          </div>\n"
            + "</div>\n"
            + "</div>\n"
            + "\n"
            + "</body></html>";

        String css = "span { color:red; }";

        XWikiContext xcontext = this.oldcoreRule.getXWikiContext();
        XWikiDocument doc = mock(XWikiDocument.class);
        when(doc.getExternalURL("view", xcontext)).thenReturn("http://localhost:8080/export");
        xcontext.setDoc(doc);

        assertTrue(pdfExport.applyCSS(html, css, xcontext).contains("color: red"));
    }
}
