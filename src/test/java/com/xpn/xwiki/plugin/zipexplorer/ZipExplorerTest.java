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

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.HibernateTestCase;
import com.xpn.xwiki.web.XWikiServletURLFactory;

import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipExplorerTest extends HibernateTestCase {

    String zipfilename = "test.zip";
    String author = "XWiki.LudovicDubost";
    String parent = "Main.WebHome";
    String content = "Hello 1\n" + "<Hello> 2\n" + "Hello 3";

    String filename1 = "test.doc";
    String filename2 = "test.txt";

    XWikiAttachment attachment;
    XWikiDocument doc;
    Document maindoc;

    List myExpectedList;

    protected void setUp() throws Exception {
        super.setUp();
        getXWiki().getPluginManager().addPlugin("zipexplorer", "com.xpn.xwiki.plugin.zipexplorer.ZipExplorerPlugin", getXWikiContext());
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/", "bin/"));
    }

    public void init() {
        doc = new XWikiDocument("Main", "ZipExplorerTest");
        doc.setContent(content);
        doc.setAuthor(author);
        doc.setParent(parent);

        myExpectedList = new ArrayList();
        myExpectedList.add(filename1);
        myExpectedList.add(filename2);

        try {

            attachment = new XWikiAttachment(doc, zipfilename);

            String attachcontent1 = "Hi i'm here";
            byte[] stream = attachcontent1.getBytes();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ZipOutputStream zos = new ZipOutputStream(baos);

            /* Each is the entry to the zip file */

            ZipEntry zipe = new ZipEntry(filename1);
            zos.putNextEntry(zipe);
            zos.write(stream, 0, stream.length);

            zipe = new ZipEntry(filename2);
            zos.putNextEntry(zipe);
            zos.write(stream, 0, stream.length);

            zos.closeEntry();

            attachment.setContent(baos.toByteArray());
            doc.saveAttachmentContent(attachment, context);

            doc.getAttachmentList().add(attachment);
            xwiki.saveDocument(doc, context);
            maindoc = new Document(doc, context);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void testZipExplorerTest() throws XWikiException {
        init();
        ZipExplorerPluginAPI zpa = (ZipExplorerPluginAPI) xwiki.getPluginApi("zipexplorer", context);
        compareList(myExpectedList, zpa.getFileList(maindoc, attachment.getFilename()));
        assertEquals("/xwiki/bin/download/Main/ZipExplorerTest/" + filename1,zpa.getFileLink(maindoc, attachment.getFilename(), filename1));
    }

    public void testGetFileNameFromZipURL()
    {
        ZipExplorerPlugin plugin = new ZipExplorerPlugin("zipexplorer",
            ZipExplorerPlugin.class.getName(), new XWikiContext());
        String fileName = plugin.getFileLocationFromZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip/Directory/File.txt",
            "download");
        assertEquals("Directory/File.txt", fileName);
    }

    public void testGetFileNameFromZipURLWhenInvalidURL()
    {
        ZipExplorerPlugin plugin = new ZipExplorerPlugin("zipexplorer",
            ZipExplorerPlugin.class.getName(), new XWikiContext());
        String fileName = plugin.getFileLocationFromZipURL(
            "http://server/xwiki/bin/download/Main/Document/zipfile.zip",
            "download");
        assertEquals("", fileName);
    }

    private void compareList(List myExpectedList, List myResult){
        assertEquals(myExpectedList.size(), myResult.size());
        Iterator it = myResult.iterator();
        while(it.hasNext()){
            Object value = it.next();
            assertTrue(myExpectedList.contains(value));
        }
    }
}
