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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.ListItem;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPlugin} class.
 *
 * @version $Id$
 */
@ComponentTest
public class ZipExplorerTest
{
    private ZipExplorerPlugin plugin;

    @BeforeEach
    public void setUp(MockitoComponentManager componentManager) throws Exception
    {
        this.plugin = new ZipExplorerPlugin("zipexplorer", ZipExplorerPlugin.class.getName(), null);

        Environment environment = componentManager.registerMockComponent(Environment.class);
        File tmpDir = new File("target/tmp");
        tmpDir.mkdirs();
        when(environment.getTemporaryDirectory()).thenReturn(tmpDir);
        Utils.setComponentManager(componentManager);

        Provider<XWikiContext> xwikiContextProvider =
            componentManager.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        when(xwikiContextProvider.get()).thenReturn(mock(XWikiContext.class));
    }

    @Test
    public void isZipFile() throws Exception
    {
        byte txtbuf[] = { 0x00, 0x01, 0x02, 0x03, 0x06, 0x07 };
        ByteArrayInputStream txtBais = new ByteArrayInputStream(txtbuf);
        assertFalse(this.plugin.isZipFile(txtBais));

        byte tinybuf[] = { 0x00 };
        ByteArrayInputStream tinyBais = new ByteArrayInputStream(tinybuf);
        assertFalse(this.plugin.isZipFile(tinyBais));

        byte zipbuf[] = createZipFile("test");
        ByteArrayInputStream zipBais = new ByteArrayInputStream(zipbuf);
        assertTrue(this.plugin.isZipFile(zipBais));
    }

    @Test
    public void testIsValidZipURL()
    {
        assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt", "download"));
        assertFalse(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt", "view"));
        assertFalse(
            this.plugin.isValidZipURL("http://server/xwiki/bin/download/Main/Document/zipfile.zip", "download"));
        assertFalse(this.plugin.isValidZipURL("http://server/xwiki/bin/download/Main/Document", "download"));

        // These tests should normally fail but we haven't implemented the check to verify if the
        // ZIP URL points to a file rather than a dir.
        assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/Dir2/", "download"));
        assertTrue(this.plugin.isValidZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/Dir2", "download"));
    }

    @Test
    public void downloadAttachmentWithInvalidZipURL() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", "Main", "Document"));
        XWikiAttachment originalAttachment =
            createAttachment("someFile.txt", "Some text".getBytes(), document);
        XWikiContext context = createXWikiContext("http://server/xwiki/bin/download/Main/Document/someFile.txt");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        assertSame(originalAttachment, newAttachment);
    }

    @Test
    public void downloadAttachment() throws Exception
    {
        String zipFileContent = "File.txt content";
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", "Main", "Document"));

        XWikiAttachment originalAttachment =
            createAttachment("zipfile.zip", createZipFile(zipFileContent), document);

        XWikiContext context =
            createXWikiContext("http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        assertEquals("Directory/File.txt", newAttachment.getFilename());
        assertEquals(zipFileContent.length(), newAttachment.getLongSize());
        assertEquals(zipFileContent.length(), newAttachment.getContentSize(context));
        assertEquals(zipFileContent, new String(newAttachment.getContent(context)));
    }

    @Test
    public void downloadAttachmentWhenURLIsNotZipFile() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", "Main", "Document"));
        XWikiAttachment originalAttachment = createAttachment("somefile.whatever", null, document);

        XWikiContext context = createXWikiContext("http://server/xwiki/bin/download/Main/Document/somefile.whatever");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        assertSame(originalAttachment, newAttachment);
    }

    @Test
    public void downloadAttachmentWhenURLIsZipButNotPointingInsideZip() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", "Main", "Document"));
        XWikiAttachment originalAttachment = createAttachment("zipfile.zip", null, document);

        XWikiContext context = createXWikiContext("http://server/xwiki/bin/download/Main/Document/zipfile.zip");

        XWikiAttachment newAttachment = this.plugin.downloadAttachment(originalAttachment, context);

        assertSame(originalAttachment, newAttachment);
    }

    @Test
    public void getFileList() throws Exception
    {
        XWikiDocument document = createXWikiDocumentWithZipFileAttachment();

        List<String> entries = this.plugin.getFileList(new Document(document, null), "zipfile.zip", null);

        assertEquals(2, entries.size());
        assertEquals("Directory/File.txt", entries.get(0));
        assertEquals("File2.txt", entries.get(1));
    }

    @Test
    public void getFileTreeList() throws Exception
    {
        XWikiDocument document = createXWikiDocumentWithZipFileAttachment();

        List<ListItem> entries = this.plugin.getFileTreeList(new Document(document, null), "zipfile.zip", null);

        assertEquals(3, entries.size());

        assertEquals("Directory/", entries.get(0).getId());
        assertEquals("Directory", entries.get(0).getValue());
        assertEquals("", entries.get(0).getParent());

        assertEquals("Directory/File.txt", entries.get(1).getId());
        assertEquals("File.txt", entries.get(1).getValue());
        assertEquals("Directory/", entries.get(1).getParent());

        assertEquals("File2.txt", entries.get(2).getId());
        assertEquals("File2.txt", entries.get(2).getValue());
        assertEquals("", entries.get(2).getParent());
    }

    @Test
    public void getFileLink() throws Exception
    {
        XWikiDocument xwikiDocument = mock(XWikiDocument.class);
        when(xwikiDocument.getAttachmentURL(eq("zipfile.zip"), any(XWikiContext.class))).thenReturn(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip");
        Document document = new Document(xwikiDocument, null);

        String link = this.plugin.getFileLink(document, "zipfile.zip", "filename", null);

        assertEquals("http://server/xwiki/bin/download/Main/Document/zipfile.zip/filename", link);
    }

    @Test
    public void getFileLocationFromZipURL()
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
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(new DocumentReference("wiki", "Main", "Document"));
        XWikiAttachment attachment = createAttachment("zipfile.zip", createZipFile("Some content"), document);
        when(document.clone()).thenReturn(document);
        when(document.getAttachment("zipfile.zip")).thenReturn(attachment);
        return document;
    }

    private XWikiContext createXWikiContext(String url)
    {
        XWikiRequest request = mock(XWikiRequest.class);
        when(request.getRequestURI()).thenReturn(url);
        XWikiContext context = new XWikiContext();
        context.setRequest(request);
        context.setAction("download");
        return context;
    }

    private XWikiAttachment createAttachment(String filename, byte[] content, XWikiDocument document) throws Exception
    {
        XWikiAttachment attachment = mock(XWikiAttachment.class);
        when(attachment.getFilename()).thenReturn(filename);
        when(attachment.getDoc()).thenReturn(document);
        when(attachment.getAuthor()).thenReturn("Vincent");
        when(attachment.getAuthorReference()).thenReturn(new DocumentReference("wiki", "XWiki", "Vincent"));
        when(attachment.getDate()).thenReturn(new Date());
        when(attachment.getFilesize()).thenReturn((content == null) ? 0 : content.length);
        when(attachment.getContentSize(any(XWikiContext.class))).thenReturn((content == null) ? 0 : content.length);
        when(attachment.getContent(any(XWikiContext.class))).thenReturn((content == null) ? new byte[0] : content);
        when(attachment.getContentInputStream(any(XWikiContext.class))).thenReturn(
            new ByteArrayInputStream((content == null) ? new byte[0] : content));
//        when(attachment.getReference()).thenReturn(new AttachmentReference(filename, document.getDocumentReference()));
        when(attachment.getReference()).thenReturn(
            new AttachmentReference(filename, new DocumentReference("wiki", "Main", "Document")));

        return attachment;
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
