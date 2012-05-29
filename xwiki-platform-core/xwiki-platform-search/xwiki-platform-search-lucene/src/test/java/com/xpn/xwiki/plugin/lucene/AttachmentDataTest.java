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
package com.xpn.xwiki.plugin.lucene;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.jmock.Mock;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiServletContext;
import org.apache.lucene.document.Document;
import com.xpn.xwiki.XWikiException;

/**
 * Unit tests for {@link AttachmentData}.
 * 
 * @version $Id$
 */
public class AttachmentDataTest extends AbstractBridgedXWikiComponentTestCase
{
    private XWikiDocument document;

    private XWikiAttachment attachment;

    private AttachmentData attachmentData;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.attachment = new XWikiAttachment(this.document, "filename");
        this.document.getAttachmentList().add(this.attachment);

        Mock mockServletContext = mock(ServletContext.class);
        mockServletContext.stubs().method("getMimeType").will(returnValue("default"));
        XWikiServletContext xwikiServletContext = new XWikiServletContext((ServletContext) mockServletContext.proxy());
        getContext().setEngineContext(xwikiServletContext);

        this.attachmentData = new AttachmentData(this.attachment, getContext(), false);
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Setup display configuration.
        Mock mockDisplayConfiguration = registerMockComponent(DisplayConfiguration.class);
        mockDisplayConfiguration.stubs().method("getDocumentDisplayerHint").will(returnValue("default"));
        mockDisplayConfiguration.stubs().method("getTitleHeadingDepth").will(returnValue(2));
    }

    private void assertGetFullText(String expect, String filename) throws IOException
    {
        this.attachment.setFilename(filename);
        this.attachment.setContent(getClass().getResourceAsStream("/" + filename));

        this.attachmentData.setFilename(filename);

        String fullText = this.attachmentData.getFullText(this.document, getContext());

        assertEquals("Wrong attachment content indexed", expect, fullText);
    }

    private void assertGetMimeType(String expect, String filename)throws XWikiException
    { 
       // Document luceneDoc = new Document();
        //this.attachmentData.addDataToLuceneDocument(luceneDoc, getContext());
        
        this.attachmentData.setMimeType(this.attachment.getMimeType(getContext()));
       // String mimeType = luceneDoc.get(IndexFields.MIMETYPE);
        String mimeType = this.attachmentData.getMimeType();
        assertEquals("Wrong mimetype content indexed", expect, mimeType);
        
    }
    
    public void testGetFullTextFromTxt() throws IOException, XWikiException
    {    
        Mock mockServletContext = mock(ServletContext.class);
        mockServletContext.stubs().method("getMimeType").with(eq("txt.txt")).will(returnValue("text/plain"));
        XWikiServletContext xwikiServletContext = new XWikiServletContext((ServletContext) mockServletContext.proxy());
        getContext().setEngineContext(xwikiServletContext);
        assertGetFullText("text content\n", "txt.txt");
        assertGetMimeType("text/plain", "txt.txt");
    }

    public void testGetFullTextFromMSOffice97() throws IOException, XWikiException
    {   
        Mock mockServletContext = mock(ServletContext.class);
        mockServletContext.stubs().method("getMimeType").with(eq("msoffice97.doc")).will(returnValue("application/msword"));
        XWikiServletContext xwikiServletContext = new XWikiServletContext((ServletContext) mockServletContext.proxy());
        getContext().setEngineContext(xwikiServletContext);
        assertGetFullText("ms office 97 content\n\n", "msoffice97.doc");
        assertGetMimeType("application/msword" , "msoffice97.doc");
        
    }

    public void testGetFullTextFromOpenXML() throws IOException, XWikiException
    {   
        Mock mockServletContext = mock(ServletContext.class);
        mockServletContext.stubs().method("getMimeType").with(eq("openxml.docx")).will(returnValue("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        XWikiServletContext xwikiServletContext = new XWikiServletContext((ServletContext) mockServletContext.proxy());
        getContext().setEngineContext(xwikiServletContext);
        assertGetFullText("openxml content\n", "openxml.docx");
        assertGetMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "openxml.docx");
        
    }

    public void testGetFullTextFromOpenDocument() throws IOException, XWikiException
    {   
        Mock mockServletContext = mock(ServletContext.class);
        mockServletContext.stubs().method("getMimeType").with(eq("opendocument.odt")).will(returnValue("application/vnd.oasis.opendocument.text"));
        XWikiServletContext xwikiServletContext = new XWikiServletContext((ServletContext) mockServletContext.proxy());
        getContext().setEngineContext(xwikiServletContext);
        assertGetFullText("opendocument content\n", "opendocument.odt");
        assertGetMimeType("application/vnd.oasis.opendocument.text", "opendocument.odt");
    }

    public void testGetFullTextFromPDF() throws IOException, XWikiException
    {   
        Mock mockServletContext = mock(ServletContext.class);
        mockServletContext.stubs().method("getMimeType").with(eq("pdf.pdf")).will(returnValue("application/pdf"));
        XWikiServletContext xwikiServletContext = new XWikiServletContext((ServletContext) mockServletContext.proxy());
        getContext().setEngineContext(xwikiServletContext);
        assertGetFullText("\npdf content\n\n\n", "pdf.pdf");
        assertGetMimeType("application/pdf", "pdf.pdf");
    }

    public void testGetFullTextFromZIP() throws IOException , XWikiException
    {   
        Mock mockServletContext = mock(ServletContext.class);
        mockServletContext.stubs().method("getMimeType").with(eq("zip.zip")).will(returnValue("application/zip"));
        XWikiServletContext xwikiServletContext = new XWikiServletContext((ServletContext) mockServletContext.proxy());
        getContext().setEngineContext(xwikiServletContext);
        assertGetFullText("zip.txt\nzip content\n\n\n\n", "zip.zip");
        assertGetMimeType("application/zip", "zip.zip");
    }
}
