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
package com.xpn.xwiki.plugin.zipexplorer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.web.XWikiRequest;
import org.jmock.Mock;

import java.util.Date;
import java.util.List;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import junit.framework.Assert;

/**
 * Unit tests for the {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPlugin} class.
 * 
 * @version $Id$
 */
public class ZipExplorerTest extends AbstractBridgedXWikiComponentTestCase
{
    private ZipExplorerPlugin plugin;

    protected void setUp() throws Exception
    {
        super.setUp();
        this.plugin = new ZipExplorerPlugin("zipexplorer", ZipExplorerPlugin.class.getName(), null);
    }

    public void testIsZipFile() throws Exception
    {
        byte txtbuf[] = {0x00, 0x01, 0x02, 0x03, 0x06, 0x07};
        ByteArrayInputStream txtBais = new ByteArrayInputStream(txtbuf);
        Assert.assertFalse(this.plugin.isZipFile(txtBais));

        byte tinybuf[] = {0x00};
        ByteArrayInputStream tinyBais = new ByteArrayInputStream(tinybuf);
        Assert.assertFalse(this.plugin.isZipFile(tinyBais));

        byte zipbuf[] = createZipFile("test");
        ByteArrayInputStream zipBais = new ByteArrayInputStream(zipbuf);
        Assert.assertTrue(this.plugin.isZipFile(zipBais));

    }

    public void testIsValidZipURL()
    {
        Assert.assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt", "download"));
        Assert.assertFalse(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt", "view"));
        Assert.assertFalse(
            this.plugin.isValidZipURL("http://server/xwiki/bin/download/Main/Document/zipfile.zip", "download"));
        Assert.assertFalse(this.plugin.isValidZipURL("http://server/xwiki/bin/download/Main/Document", "download"));

        // These tests should normally fail but we haven't implemented the check to verify if the
        // ZIP URL points to a file rather than a dir.
        Assert.assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/Dir2/", "download"));
        Assert.assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/Dir2", "download"));
    }

    public void testDownloadAttachmentWithInvalidZipURL() throws Exception
    {
        XWikiAttachment originalAttachment =
            createAttachment("someFile.txt", "Some text".getBytes(), (XWikiDocument) mock(XWikiDocument.class).proxy());
        XWikiContext context = createXWikiContext("http://server/xwiki/bin/download/Main/Document/someFile.txt");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        Assert.assertSame(originalAttachment, newAttachment);
    }

    public void testDownloadAttachment() throws Exception
    {
        String zipFileContent = "File.txt content";
        XWikiAttachment originalAttachment =
            createAttachment("zipfile.zip", createZipFile(zipFileContent),
                (XWikiDocument) mock(XWikiDocument.class).proxy());

        XWikiContext context =
            createXWikiContext("http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        Assert.assertEquals("Directory/File.txt", newAttachment.getFilename());
        Assert.assertEquals(zipFileContent.length(), newAttachment.getFilesize());
        Assert.assertEquals(zipFileContent.length(), newAttachment.getContentSize(context));
        Assert.assertEquals(zipFileContent, new String(newAttachment.getContent(context)));
    }

    public void testDownloadAttachmentWhenURLIsNotZipFile() throws Exception
    {
        XWikiAttachment originalAttachment =
            createAttachment("somefile.whatever", null, (XWikiDocument) mock(XWikiDocument.class).proxy());

        XWikiContext context = createXWikiContext("http://server/xwiki/bin/download/Main/Document/somefile.whatever");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        Assert.assertSame(originalAttachment, newAttachment);
    }

    public void testDownloadAttachmentWhenURLIsZipButNotPointingInsideZip() throws Exception
    {
        XWikiAttachment originalAttachment =
            createAttachment("zipfile.zip", null, (XWikiDocument) mock(XWikiDocument.class).proxy());

        XWikiContext context = createXWikiContext("http://server/xwiki/bin/download/Main/Document/zipfile.zip");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        Assert.assertSame(originalAttachment, newAttachment);
    }

    public void testGetFileList() throws Exception
    {
        XWikiDocument document = createXWikiDocumentWithZipFileAttachment();

        List<String> entries = this.plugin.getFileList(new Document(document, null), "zipfile.zip", null);

        Assert.assertEquals(2, entries.size());
        Assert.assertEquals("Directory/File.txt", entries.get(0));
        Assert.assertEquals("File2.txt", entries.get(1));
    }

    public void testGetFileTreeList() throws Exception
    {
        XWikiDocument document = createXWikiDocumentWithZipFileAttachment();

        List<ListItem> entries = this.plugin.getFileTreeList(new Document(document, null), "zipfile.zip", null);

        Assert.assertEquals(3, entries.size());

        Assert.assertEquals("Directory/", entries.get(0).getId());
        Assert.assertEquals("Directory", entries.get(0).getValue());
        Assert.assertEquals("", entries.get(0).getParent());

        Assert.assertEquals("Directory/File.txt", entries.get(1).getId());
        Assert.assertEquals("File.txt", entries.get(1).getValue());
        Assert.assertEquals("Directory/", entries.get(1).getParent());

        Assert.assertEquals("File2.txt", entries.get(2).getId());
        Assert.assertEquals("File2.txt", entries.get(2).getValue());
        Assert.assertEquals("", entries.get(2).getParent());
    }

    public void testGetFileLink() throws Exception
    {
        Mock mockDocument = mock(XWikiDocument.class);
        mockDocument.expects(once()).method("getAttachmentURL").will(
            returnValue("http://server/xwiki/bin/download/Main/Document/zipfile.zip"));
        Document document = new Document((XWikiDocument) mockDocument.proxy(), null);

        String link = this.plugin.getFileLink(document, "zipfile.zip", "filename", null);

        Assert.assertEquals("http://server/xwiki/bin/download/Main/Document/zipfile.zip/filename", link);
    }

    public void testGetFileLocationFromZipURL()
    {
        String urlPrefix = "server/xwiki/bin/download/Main/Document/zipfile.zip";

        assertEquals("Directory/File.txt", this.plugin.getFileLocationFromZipURL(urlPrefix + "/Directory/File.txt",
            "download"));
        assertEquals("", this.plugin.getFileLocationFromZipURL(urlPrefix, "download"));
        assertEquals("Some Directory/File WithSpace.txt", this.plugin.getFileLocationFromZipURL(urlPrefix
            + "/Some%20Directory/File%20WithSpace.txt", "download"));
    }

    private XWikiDocument createXWikiDocumentWithZipFileAttachment() throws Exception
    {
        Mock mockDocument = mock(XWikiDocument.class);
        XWikiDocument document = (XWikiDocument) mockDocument.proxy();
        XWikiAttachment attachment = createAttachment("zipfile.zip", createZipFile("Some content"), document);
        mockDocument.stubs().method("clone").will(returnValue(mockDocument.proxy()));
        mockDocument.stubs().method("getAttachment").will(returnValue(attachment));
        return document;
    }

    private XWikiContext createXWikiContext(String url)
    {
        Mock mockRequest = mock(XWikiRequest.class);
        mockRequest.expects(once()).method("getRequestURI").will(returnValue(url));
        XWikiContext context = new XWikiContext();
        context.setRequest((XWikiRequest) mockRequest.proxy());
        context.setAction("download");
        return context;
    }

    private XWikiAttachment createAttachment(String filename, byte[] content, XWikiDocument document) throws Exception
    {
        Mock mockAttachment = mock(XWikiAttachment.class);
        mockAttachment.stubs().method("getFilename").will(returnValue(filename));
        mockAttachment.stubs().method("getDoc").will(returnValue(document));
        mockAttachment.stubs().method("getAuthor").will(returnValue("Vincent"));
        mockAttachment.stubs().method("getDate").will(returnValue(new Date()));
        mockAttachment.stubs().method("getFilesize").will(returnValue((content == null) ? 0 : content.length));
        mockAttachment.stubs().method("getContentSize").will(returnValue((content == null) ? 0 : content.length));
        mockAttachment.stubs().method("getContent").will(returnValue((content == null) ? new byte[0] : content));
        mockAttachment.stubs().method("getContentInputStream").will(
            returnValue(new ByteArrayInputStream((content == null) ? new byte[0] : content)));
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
