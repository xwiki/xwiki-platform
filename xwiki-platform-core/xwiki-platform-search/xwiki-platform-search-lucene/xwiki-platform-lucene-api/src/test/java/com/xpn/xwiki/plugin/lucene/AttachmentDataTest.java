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

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.lucene.internal.AttachmentData;
import com.xpn.xwiki.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.web.XWikiServletContext;
import com.xpn.xwiki.XWikiException;

/**
 * Unit tests for {@link AttachmentData}.
 * 
 * @version $Id$
 */
public class AttachmentDataTest extends AbstractBridgedComponentTestCase
{
    private XWikiDocument document;

    private XWikiAttachment attachment;

    private AttachmentData attachmentData;

    private ServletContext mockServletContext;

    @Override
    @Before
    public void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.document.setSyntax(Syntax.XWIKI_2_0);
        this.attachment = new XWikiAttachment(this.document, "filename");
        this.document.getAttachmentList().add(this.attachment);

        this.mockServletContext = getMockery().mock(ServletContext.class, "AttachmentDataTest");
        getMockery().checking(new Expectations()
        {
            {
                oneOf(mockServletContext).getMimeType(with(any(String.class)));
                will(returnValue("default"));
            }
        });

        XWikiServletContext xwikiServletContext = new XWikiServletContext(mockServletContext);
        getContext().setEngineContext(xwikiServletContext);

        this.attachmentData = new AttachmentData(this.attachment, getContext(), false);
    }

    @Override
    protected void registerComponents() throws Exception
    {
        super.registerComponents();

        // Setup display configuration.
        final DisplayConfiguration mockDisplayConfiguration = registerMockComponent(DisplayConfiguration.class);
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockDisplayConfiguration).getDocumentDisplayerHint();
                will(returnValue("default"));
                allowing(mockDisplayConfiguration).getTitleHeadingDepth();
                will(returnValue(2));
            }
        });
    }

    private void assertGetFullText(String expect, String filename) throws IOException
    {
        this.attachment.setFilename(filename);
        this.attachment.setContent(getClass().getResourceAsStream("/" + filename));

        this.attachmentData.setFilename(filename);

        String fullText = this.attachmentData.getFullText(this.document, getContext());

        Assert.assertEquals("Wrong attachment content indexed", expect, fullText);
    }

    private void assertGetMimeType(String expect, String filename)
    { 
       
        this.attachmentData.setMimeType(this.attachment.getMimeType(getContext()));
        String mimeType = this.attachmentData.getMimeType();
        Assert.assertEquals("Wrong mimetype content indexed", expect, mimeType);
        
    }

    private void setUpMockServletContext(final String expectedFileName, final String expectedMimeType)
    {
        getMockery().checking(new Expectations()
        {
            {
                allowing(mockServletContext).getMimeType(with(expectedFileName));
                will(returnValue(expectedMimeType));
            }
        });
        getContext().setEngineContext(new XWikiServletContext(mockServletContext));
    }

    @Test
    public void getFullTextFromTxt() throws IOException, XWikiException
    {
        setUpMockServletContext("txt.txt", "text/plain");
        assertGetFullText("text content\n", "txt.txt");
        assertGetMimeType("text/plain", "txt.txt");
    }

    @Test
    public void getFullTextFromMSOffice97() throws IOException, XWikiException
    {
        setUpMockServletContext("msoffice97.doc", "application/msword");
        assertGetFullText("ms office 97 content\n\n", "msoffice97.doc");
        assertGetMimeType("application/msword" , "msoffice97.doc");
        
    }

    @Test
    public void getFullTextFromOpenXML() throws IOException, XWikiException
    {
        setUpMockServletContext("openxml.docx",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        assertGetFullText("openxml content\n", "openxml.docx");
        assertGetMimeType("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "openxml.docx");
        
    }

    @Test
    public void getFullTextFromOpenDocument() throws IOException, XWikiException
    {
        setUpMockServletContext("opendocument.odt", "application/vnd.oasis.opendocument.text");
        assertGetFullText("opendocument content\n", "opendocument.odt");
        assertGetMimeType("application/vnd.oasis.opendocument.text", "opendocument.odt");
    }

    @Test
    public void getFullTextFromPDF() throws IOException, XWikiException
    {
        setUpMockServletContext("pdf.pdf", "application/pdf");
        assertGetFullText("\npdf content\n\n\n", "pdf.pdf");
        assertGetMimeType("application/pdf", "pdf.pdf");
    }

    @Test
    public void getFullTextFromZIP() throws IOException , XWikiException
    {
        setUpMockServletContext("zip.zip", "application/zip");
        assertGetFullText("\nzip.txt\nzip content\n\n\n\n", "zip.zip");
        assertGetMimeType("application/zip", "zip.zip");
    }

    @Test
    public void getFullTextFromHTML() throws IOException, XWikiException
    {
        setUpMockServletContext("html.html", "text/html");
        assertGetFullText("something\n", "html.html");
        assertGetMimeType("text/html", "html.html");
    }

    @Test
    @Ignore("Remove once http://jira.xwiki.org/browse/XWIKI-8656 is fixed")
    public void getFullTextFromClass() throws IOException, XWikiException
    {
        // TODO: For some unknown reason XWikiAttachment.getMimeType puts the filename in lowercase...
        setUpMockServletContext("helloworld.class", "application/java-vm");
        String expectedContent = "public synchronized class helloworld {\n"
            + "    public void helloworld();\n"
            + "    public static void main(string[]);\n"
            + "}\n\n";
        assertGetFullText(expectedContent, "HelloWorld.class");
        assertGetMimeType("application/java-vm", "HelloWorld.class");
    }
}
