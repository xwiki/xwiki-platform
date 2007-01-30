/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ravenees
 */
package com.xpn.xwiki.plugin.zipexplorer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
import org.jmock.Mock;

import java.util.Date;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.ByteArrayOutputStream;

/**
 * Unit tests for the {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPlugin} class.
 *
 * @version $Id: $
 */
public class ZipExplorerTest extends org.jmock.cglib.MockObjectTestCase
{
    private ZipExplorerPlugin plugin;

    protected void setUp()
    {
        this.plugin = new ZipExplorerPlugin("zipexplorer", ZipExplorerPlugin.class.getName(), null);
    }

    public void testIsZipFile()
    {
        assertTrue(this.plugin.isZipFile("test.zip"));
        assertFalse(this.plugin.isZipFile("test.txt"));
        assertFalse(this.plugin.isZipFile("testzip"));
    }

    public void testIsValidZipURL()
    {
        assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt",
            "download"));
        assertFalse(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt",
            "view"));
        assertFalse(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip", "download"));
        assertFalse(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document", "download"));

        // These tests should normally fail but we haven't implemented the check to verify if the
        // ZIP URL points to a file rather than a dir.
        assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/Dir2/",
            "download"));
        assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/Dir2",
            "download"));
    }

    public void testDownloadAttachmentWithInvalidZipURL() throws Exception {
        XWikiAttachment originalAttachment = createAttachment("someFile.txt",
            "Some text".getBytes(), (XWikiDocument) mock(XWikiDocument.class).proxy());
        XWikiContext context =
            createXWikiContext("http://server/xwiki/bin/download/Main/Document/someFile.txt");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        assertSame(originalAttachment, newAttachment);
    }

    public void testDownloadAttachment() throws Exception {
        String zipFileContent = "File.txt content";
        XWikiAttachment originalAttachment = createAttachment("zipfile.zip",
            createZipFile(zipFileContent), (XWikiDocument) mock(XWikiDocument.class).proxy());
            
        XWikiContext context = createXWikiContext(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        assertEquals("Directory/File.txt", newAttachment.getFilename());
        assertEquals(zipFileContent.length(), newAttachment.getFilesize());
        assertEquals(zipFileContent, new String(newAttachment.getContent(context)));
    }

    public void testDownloadAttachmentWhenURLIsNotZipFile() throws Exception {
        XWikiAttachment originalAttachment = createAttachment("somefile.whatever", null,
            (XWikiDocument) mock(XWikiDocument.class).proxy());

        XWikiContext context = createXWikiContext(
            "http://server/xwiki/bin/download/Main/Document/somefile.whatever");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        assertSame(originalAttachment, newAttachment);
    }

    public void testDownloadAttachmentWhenURLIsZipButNotPointingInsideZip() throws Exception {
        XWikiAttachment originalAttachment = createAttachment("zipfile.zip", null,
            (XWikiDocument) mock(XWikiDocument.class).proxy());

        XWikiContext context = createXWikiContext(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        assertSame(originalAttachment, newAttachment);
    }

    public void testGetFileList() throws Exception {
        XWikiDocument document = createXWikiDocumentWithZipFileAttachment();

        List entries = this.plugin.getFileList(new Document(document, null), "zipfile.zip", null);

        assertEquals(2, entries.size());
        assertEquals("Directory/File.txt", (String) entries.get(0));
        assertEquals("File2.txt", (String) entries.get(1));
    }

    public void testGetFileTreeList() throws Exception {
        XWikiDocument document = createXWikiDocumentWithZipFileAttachment();

        List entries =
            this.plugin.getFileTreeList(new Document(document, null), "zipfile.zip", null);

        assertEquals(3, entries.size());

        assertEquals("Directory/", ((ListItem) entries.get(0)).getId());
        assertEquals("Directory", ((ListItem) entries.get(0)).getValue());
        assertEquals("", ((ListItem) entries.get(0)).getParent());

        assertEquals("Directory/File.txt", ((ListItem) entries.get(1)).getId());
        assertEquals("File.txt", ((ListItem) entries.get(1)).getValue());
        assertEquals("Directory/", ((ListItem) entries.get(1)).getParent());

        assertEquals("File2.txt", ((ListItem) entries.get(2)).getId());
        assertEquals("File2.txt", ((ListItem) entries.get(2)).getValue());
        assertEquals("", ((ListItem) entries.get(2)).getParent());
    }

    public void testGetFileLink() throws Exception {
        Mock mockDocument = mock(XWikiDocument.class);
        mockDocument.expects(once()).method("getAttachmentURL").will(
            returnValue("http://server/xwiki/bin/download/Main/Document/zipfile.zip"));
        Document document = new Document((XWikiDocument) mockDocument.proxy(), null);

        String link = this.plugin.getFileLink(document, "zipfile.zip", "filename", null);

        assertEquals("http://server/xwiki/bin/download/Main/Document/zipfile.zip/filename", link);
    }

    public void testGetFileLocationFromZipURL()
    {
        assertEquals("Directory/File.txt", this.plugin.getFileLocationFromZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt",
            "download"));
        assertEquals("", this.plugin.getFileLocationFromZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip", "download"));
    }

    private XWikiDocument createXWikiDocumentWithZipFileAttachment() throws Exception {
        Mock mockDocument = mock(XWikiDocument.class);
        XWikiDocument document = (XWikiDocument) mockDocument.proxy();
        XWikiAttachment attachment =
            createAttachment("zipfile.zip", createZipFile("Some content"), document);
        mockDocument.stubs().method("clone").will(returnValue(mockDocument.proxy()));
        mockDocument.stubs().method("getAttachment").will(returnValue(attachment));
        return document;         
    }

    private XWikiContext createXWikiContext(String url) {
        Mock mockRequest = mock(XWikiRequest.class);
        mockRequest.expects(once()).method("getRequestURI").will(returnValue(url));
        XWikiContext context = new XWikiContext();
        context.setRequest((XWikiRequest) mockRequest.proxy());
        context.setAction("download");
        return context;
    }

    private XWikiAttachment createAttachment(String filename, byte[] content,
        XWikiDocument document) throws Exception
    {
        Mock mockAttachment = mock(XWikiAttachment.class);
        mockAttachment.stubs().method("getFilename").will(returnValue(filename));
        mockAttachment.stubs().method("getDoc").will(returnValue(document));
        mockAttachment.stubs().method("getAuthor").will(returnValue("Vincent"));
        mockAttachment.stubs().method("getDate").will(returnValue(new Date()));
        mockAttachment.stubs().method("getContent").will(returnValue(content));
        return (XWikiAttachment) mockAttachment.proxy();
    }

    private byte[] createZipFile(String content) throws Exception
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        ZipEntry zipe = new ZipEntry("Directory/File.txt");
        zos.putNextEntry(zipe);
        zos.write(content.getBytes());
        ZipEntry zipe2 = new ZipEntry("File2.txt");
        zos.putNextEntry(zipe2);
        zos.write(content.getBytes());
        zos.closeEntry();
        return baos.toByteArray();
    }
}
