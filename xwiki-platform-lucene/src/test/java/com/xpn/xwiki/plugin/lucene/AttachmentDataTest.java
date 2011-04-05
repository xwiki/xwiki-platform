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

import org.apache.commons.lang.StringUtils;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;
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

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.document = new XWikiDocument(new DocumentReference("wiki", "space", "page"));
        this.attachment = new XWikiAttachment(this.document, "filename");
        this.document.getAttachmentList().add(this.attachment);

        this.attachmentData = new AttachmentData(this.attachment, getContext(), false);
    }

    private void assertGetFullText(String expect, String filename) throws XWikiException, IOException
    {
        this.attachment.setFilename(filename);
        this.attachment.setContent(getClass().getResourceAsStream("/" + filename));

        this.attachmentData.setFilename(filename);

        String fulllText = this.attachmentData.getFullText(this.document, getContext());

        StringBuffer sb = new StringBuffer();
        sb.append(this.document.getName()).append(" ").append(this.document.getSpace());
        
        if (!StringUtils.isEmpty(this.document.getAuthor())) {
            sb.append(" ").append(this.document.getAuthor());
        }

        if (!StringUtils.isEmpty(this.document.getCreator())) {
            sb.append(" ").append(this.document.getCreator());
        }

        sb.append(" ").append(expect);

        assertEquals(sb.toString(), fulllText);
    }

    public void testGetFullTextFromTxt() throws XWikiException, IOException
    {
        assertGetFullText("txt.txt text content\n", "txt.txt");
    }

    public void testGetFullTextFromMSOffice97() throws XWikiException, IOException
    {
        assertGetFullText("msoffice97.doc MS Office 97 content\n\n", "msoffice97.doc");
    }

    public void testGetFullTextFromOpenXML() throws XWikiException, IOException
    {
        assertGetFullText("openxml.docx OpenXML content\n", "openxml.docx");
    }

    public void testGetFullTextFromOpenDocument() throws XWikiException, IOException
    {
        assertGetFullText("opendocument.odt OpenDocument content\n", "opendocument.odt");
    }

    public void testGetFullTextFromPDF() throws XWikiException, IOException
    {
        assertGetFullText("pdf.pdf PDF content\n\n", "pdf.pdf");
    }

    public void testGetFullTextFromZIP() throws XWikiException, IOException
    {
        assertGetFullText("zip.zip zip.txt\nzip content\n\n\n\n", "zip.zip");
    }
}
