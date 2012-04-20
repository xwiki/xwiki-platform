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

import org.jmock.Mock;
import org.xwiki.display.internal.DisplayConfiguration;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

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

    public void testGetFullTextFromTxt() throws IOException
    {
        assertGetFullText("text content\n", "txt.txt");
    }

    public void testGetFullTextFromMSOffice97() throws IOException
    {
        assertGetFullText("ms office 97 content\n\n", "msoffice97.doc");
    }

    public void testGetFullTextFromOpenXML() throws IOException
    {
        assertGetFullText("openxml content\n", "openxml.docx");
    }

    public void testGetFullTextFromOpenDocument() throws IOException
    {
        assertGetFullText("opendocument content\n", "opendocument.odt");
    }

    public void testGetFullTextFromPDF() throws IOException
    {
        assertGetFullText("\npdf content\n\n\n", "pdf.pdf");
    }

    public void testGetFullTextFromZIP() throws IOException
    {
        assertGetFullText("zip.txt\nzip content\n\n\n\n", "zip.zip");
    }
}
