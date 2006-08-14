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
 * @author ludovic
 * @author vmassol
 * @author sdumitriu
 */

package com.xpn.xwiki.test;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.suigeneris.jrcs.rcs.Version;
import org.suigeneris.jrcs.rcs.Archive;
import org.suigeneris.jrcs.rcs.ParseException;
import org.suigeneris.jrcs.util.ToString;
import org.apache.tools.ant.filters.StringInputStream;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;

public class StoreTest extends HibernateTestCase {

    public void notExist(XWikiStoreInterface store, String web, String name) throws XWikiException {
        XWikiDocument doc2 = new XWikiDocument(web, name);
        boolean result = store.exists(doc2, getXWikiContext());
        assertFalse("Document should not exist", result);
    }

    public void exists(XWikiStoreInterface store, String web, String name) throws XWikiException {
        standardWrite(store, web, name, getXWikiContext());
        XWikiDocument doc2 = new XWikiDocument(web, name);
        boolean result = store.exists(doc2, getXWikiContext());
        assertTrue("Document should exist", result);
    }

    public void existsAfterRead(XWikiStoreInterface store, String web, String name) throws XWikiException {
        standardWrite(store, web, name, getXWikiContext());
        XWikiDocument doc2 = new XWikiDocument(web, name);
        doc2 = (XWikiDocument) store.loadXWikiDoc(doc2, getXWikiContext());
        boolean result = store.exists(doc2, getXWikiContext());
        assertTrue("Document should exist", result);
    }

    public void standardDelete(XWikiStoreInterface store, String web, String name) throws XWikiException {
           XWikiDocument doc1 = new XWikiDocument(web, name);
           doc1.setContent(Utils.content1);
           doc1.setAuthor(Utils.author);
           doc1.setParent(Utils.parent);
           store.saveXWikiDoc(doc1, getXWikiContext());

           XWikiDocument doc2 = new XWikiDocument(web, name);
           doc2 = (XWikiDocument) store.loadXWikiDoc(doc2, getXWikiContext());

           String content1 = doc2.getContent();
           assertEquals(Utils.content1, content1);

           XWikiDocument doc3 = new XWikiDocument(web, name);
           doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, getXWikiContext());
           store.deleteXWikiDoc(doc3, getXWikiContext());

           XWikiDocument doc4 = new XWikiDocument(web, name);
           doc4 = (XWikiDocument) store.loadXWikiDoc(doc4, getXWikiContext());

           assertTrue("Document should be new", doc4.isNew());
         }

    public void standardWriteAfterDelete(XWikiStoreInterface store, String web, String name) throws XWikiException {
           XWikiDocument doc1 = new XWikiDocument(web, name);
           doc1.setContent("toto");
           doc1.setAuthor(Utils.author);
           doc1.setParent(Utils.parent);
           store.saveXWikiDoc(doc1, getXWikiContext());

           XWikiDocument doc2 = new XWikiDocument(web, name);
           doc2 = (XWikiDocument) store.loadXWikiDoc(doc2, getXWikiContext());

           String content1 = doc2.getContent();
           assertEquals("toto", content1);
         }

    public void versionedReadWrite(XWikiStoreInterface store, XWikiVersioningStoreInterface versioningStore, String web, String name) throws XWikiException {
        XWikiDocument doc3 = new XWikiDocument(web, name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, getXWikiContext());
        XWikiDocument doc4 = versioningStore.loadXWikiDoc(doc3,Utils.version, getXWikiContext());
        String content4 = doc4.getContent();
        assertEquals(Utils.content1,content4);
        assertEquals(doc4.getVersion(),Utils.version);
        assertEquals(doc4.getAuthor(), Utils.author);
        Version[] versions = versioningStore.getXWikiDocVersions(doc4, getXWikiContext());
        assertTrue(versions.length==1);
    }

    public void testNotExist() throws XWikiException {
        Utils.setStandardData();
        notExist(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testExists() throws XWikiException {
        Utils.setStandardData();
        exists(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testStandardRead() throws XWikiException {
        Utils.setStandardData();
        standardWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        standardRead(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
    }

    public void testStandardRead2() throws XWikiException {
        Utils.setStandardData();
        standardWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        standardRead(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        standardRead(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
    }

    public void testStandardReadWrite() throws XWikiException {
        Utils.setStandardData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
    }

    public void testStandardIsoReadWrite() throws XWikiException {
        Utils.setStandardIsoData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
    }

    public void testStandardDelete() throws XWikiException {
        Utils.setStandardData();
        standardDelete(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testStandardWriteAfterDelete() throws XWikiException {
        Utils.setStandardData();
        standardDelete(getXWiki().getStore(), Utils.web, Utils.name);
        standardWriteAfterDelete(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testVersionedReadWrite() throws XWikiException {
        Utils.setStandardData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), getXWiki().getVersioningStore(), Utils.web, Utils.name);
    }

    public void testVersionedReadWriteWithAccents() throws XWikiException {
        Utils.setStandardAccentData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), getXWiki().getVersioningStore(),Utils.web, Utils.name);
    }

    public void testMediumReadWrite() throws XWikiException {
        Utils.setMediumData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), getXWiki().getVersioningStore(), Utils.web, Utils.name);
    }

    public void testBigVersionedReadWrite() throws XWikiException, IOException {
        Utils.setBigData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name2, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), getXWiki().getVersioningStore(), Utils.web, Utils.name2);
    }

    public void attachmentReadWrite(XWikiStoreInterface store, String web, String name) throws XWikiException, IOException {
        XWikiDocument doc1 = new XWikiDocument(web, name);
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        store.saveXWikiDoc(doc1, getXWikiContext());
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.afilename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, getXWikiContext());
        doc1.getAttachmentList().add(attachment1);
        store.saveXWikiDoc(doc1, getXWikiContext());

        XWikiDocument doc2 = new XWikiDocument(web, name);
        doc2 = (XWikiDocument) store.loadXWikiDoc(doc2, getXWikiContext());
        List attachlist = doc2.getAttachmentList();
        assertEquals("Attachment is not listed", 1, attachlist.size());
        XWikiAttachment attachment2 = (XWikiAttachment) attachlist.get(0);
        assertEquals("Attachment name is not correct", Utils.afilename, attachment2.getFilename());
        assertEquals("Attachment version is not correct", "1.1", attachment2.getVersion());
        byte[] attachcontent2 = attachment2.getContent(getXWikiContext());
        assertEquals("Attachment content size is not correct", attachcontent1.length, attachcontent2.length);
        for (int i=0;i<attachcontent1.length;i++) {
            assertEquals("Attachment content byte " + i + " is not correct", attachcontent1[i], attachcontent2[i]);
        }
    }

    public void testAttachmentReadWrite() throws XWikiException, IOException {
        Utils.setStandardData();
        attachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testAttachmentIsoReadWrite() throws XWikiException, IOException {
        Utils.setStandardIsoData();
        attachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void secondAttachmentReadWrite(XWikiStoreInterface store, String web, String name) throws XWikiException, IOException {

        XWikiDocument doc3 = new XWikiDocument(web, name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, getXWikiContext());
        XWikiAttachment attachment2 = new XWikiAttachment(doc3, Utils.filename2);
        byte[] attachcontent2 = Utils.getDataAsBytes(new File(Utils.filename2));
        attachment2.setContent(attachcontent2);
        doc3.saveAttachmentContent(attachment2, getXWikiContext());
        doc3.getAttachmentList().add(attachment2);
        store.saveXWikiDoc(doc3, getXWikiContext());

        XWikiDocument doc4 = new XWikiDocument(web, name);
        doc4 = (XWikiDocument) store.loadXWikiDoc(doc4, getXWikiContext());
        List attachlist = doc4.getAttachmentList();
        assertEquals("Attachment is not listed", 2, attachlist.size());
        XWikiAttachment attachment4 = (XWikiAttachment) attachlist.get(1);
        assertEquals("Attachment version is not correct", "1.1", attachment4.getVersion());
        byte[] attachcontent4 = attachment4.getContent(getXWikiContext());
        assertEquals("Attachment content size is not correct", attachcontent2.length, attachcontent4.length);
        for (int i=0;i<attachcontent2.length;i++) {
            assertEquals("Attachment content byte " + i + " is not correct", attachcontent2[i], attachcontent4[i]);
        }
    }

        public void testSecondAttachmentReadWrite() throws XWikiException, IOException {
            Utils.setStandardData();
            attachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
            secondAttachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
        }

    public void updateAttachmentReadWrite(XWikiStoreInterface store, String web, String name) throws XWikiException, IOException {

        XWikiDocument doc3 = new XWikiDocument(web, name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, getXWikiContext());
        List attachlist = doc3.getAttachmentList();
        XWikiAttachment attachment3 = (XWikiAttachment) attachlist.get(0);
        byte[] attachcontent3 = Utils.getDataAsBytes(new File(Utils.filename2));
        attachment3.setContent(attachcontent3);
        doc3.saveAttachmentContent(attachment3, getXWikiContext());
        store.saveXWikiDoc(doc3, getXWikiContext());

        XWikiDocument doc4 = new XWikiDocument(web, name);
        doc4 = (XWikiDocument) store.loadXWikiDoc(doc4, getXWikiContext());
        attachlist = doc4.getAttachmentList();
        assertEquals("Attachment is not listed", 1, attachlist.size());
        XWikiAttachment attachment4 = (XWikiAttachment) attachlist.get(0);
        assertEquals("Attachment version is not correct", "1.2", attachment4.getVersion());
        byte[] attachcontent4 = attachment4.getContent(getXWikiContext());
        assertEquals("Attachment content size is not correct", attachcontent3.length, attachcontent4.length);
        for (int i=0;i<attachcontent3.length;i++) {
            assertEquals("Attachment content byte " + i + " is not correct", attachcontent3[i], attachcontent4[i]);
        }
    }

    public void testUpdateAttachmentReadWrite() throws XWikiException, IOException {
        Utils.setStandardData();
        attachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
        updateAttachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void deleteAttachmentReadWrite(XWikiStoreInterface store, XWikiAttachmentStoreInterface attachmentStore, String web, String name) throws XWikiException, IOException {

        XWikiDocument doc3 = new XWikiDocument(web, name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, getXWikiContext());
        List attachlist = doc3.getAttachmentList();
        XWikiAttachment attachment3 = (XWikiAttachment) attachlist.get(0);

        attachmentStore.deleteXWikiAttachment(attachment3, getXWikiContext(), true);

        attachlist = doc3.getAttachmentList();
        assertEquals("Attachment is still there", 0, attachlist.size());

        XWikiDocument doc4 = new XWikiDocument(web, name);
        doc4 = (XWikiDocument) store.loadXWikiDoc(doc4, getXWikiContext());
        attachlist = doc4.getAttachmentList();
        assertEquals("Attachment is still there", 0, attachlist.size());
    }

    public void deleteDocWithAttachment(XWikiStoreInterface store, String web, String name) throws XWikiException, IOException {

        XWikiDocument doc3 = new XWikiDocument(web, name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, getXWikiContext());
        store.deleteXWikiDoc(doc3, getXWikiContext());

        XWikiDocument doc4 = new XWikiDocument(web, name);
        doc4.setContent(Utils.content1);
        doc4.setAuthor(Utils.author);
        doc4.setParent(Utils.parent);
        store.saveXWikiDoc(doc4, getXWikiContext());

        XWikiDocument doc5 = new XWikiDocument(web, name);
        doc5 = (XWikiDocument) store.loadXWikiDoc(doc5, getXWikiContext());
        List attachlist = doc5.getAttachmentList();
        assertEquals("Attachment is still there", 0, attachlist.size());
    }


    public void testDeleteAttachmentReadWrite() throws XWikiException, IOException {
        Utils.setStandardData();
        attachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
        deleteAttachmentReadWrite(getXWiki().getStore(), getXWiki().getAttachmentStore(), Utils.web, Utils.name);
    }

    public void testDeleteDocWithAttachment() throws XWikiException, IOException {
        Utils.setStandardData();
        attachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
        deleteDocWithAttachment(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public static void standardWrite(XWikiStoreInterface store, String web, String name, XWikiContext context) throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument(web, name);
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        doc1.setLanguage(Utils.language);
        doc1.setDefaultLanguage(Utils.defaultLanguage);
        store.saveXWikiDoc(doc1, context);
    }

    public static void standardRead(XWikiStoreInterface store, String web, String name, XWikiContext context) throws XWikiException {
        XWikiDocument doc2 = new XWikiDocument(web, name);
        doc2 = (XWikiDocument) store.loadXWikiDoc(doc2, context);
        String content2 = doc2.getContent();
        assertEquals(Utils.content1,content2);
        assertEquals(doc2.getVersion(), Utils.version);
        assertEquals(doc2.getParent(), Utils.parent);
        assertEquals(doc2.getAuthor(), Utils.author);
    }

    public static void standardReadWrite(XWikiStoreInterface store, String web, String name, XWikiContext context) throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument(web, name);
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        doc1.setLanguage(Utils.language);
        doc1.setDefaultLanguage(Utils.defaultLanguage);
        store.saveXWikiDoc(doc1, context);
        XWikiDocument doc2 = new XWikiDocument(web, name);
        doc2 = (XWikiDocument) store.loadXWikiDoc(doc2, context);
        String content2 = doc2.getContent();
        assertEquals(Utils.content1,content2);
        assertEquals(doc2.getVersion(), Utils.version);
        assertEquals(doc2.getParent(), Utils.parent);
        assertEquals(doc2.getAuthor(), Utils.author);
        assertEquals(doc2.getLanguage(), Utils.language);
        assertEquals(doc2.getDefaultLanguage(), Utils.defaultLanguage);
        doc2.setContent(Utils.content3);
        doc2.setAuthor(Utils.author2);
        store.saveXWikiDoc(doc2, context);
        XWikiDocument doc3 = new XWikiDocument(web, name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, context);
        String content3b = doc3.getContent();
        assertEquals(Utils.content3,content3b);
        assertEquals(doc3.getAuthor(), Utils.author2);
        assertEquals(doc3.getVersion(), Utils.version2);
    }


    public void testUpdateAttachmentArchiveReadWrite(XWikiStoreInterface store, byte[] attachcontenta, byte[] attachcontentb) throws XWikiException, IOException {
        // Prepare data
        XWikiDocument doc = xwiki.getDocument(Utils.web, Utils.name, getXWikiContext());
        doc.setContent("content 0");
        xwiki.saveDocument(doc, context);
        for (int i=0;i<100;i++) {
         doc.setContent("content " + i);
         xwiki.saveDocument(doc, context);
        }
        XWikiAttachment attach = new XWikiAttachment(doc, "file.txt");
        attach.setContent(attachcontenta);
        attach.setAuthor("XWiki.Author1");
        attach.setComment("Test comment 1");
        doc.getAttachmentList().add(attach);
        doc.saveAttachmentContent(attach, getXWikiContext());

        doc.setContent("content 1");
        xwiki.saveDocument(doc, context);

        attach.setContent(attachcontentb);
        attach.setAuthor("XWiki.Author2");
        attach.setComment("Test comment 2");
        doc.saveAttachmentContent(attach, getXWikiContext());

        getXWiki().flushCache();

        // Verify data
        XWikiDocument doc1 = new XWikiDocument(Utils.web, Utils.name);
        doc1 = (XWikiDocument) store.loadXWikiDoc(doc1, getXWikiContext());
        List attachlist = doc1.getAttachmentList();
        assertEquals("Attachment is not listed", 1, attachlist.size());
        XWikiAttachment attach1 = (XWikiAttachment) attachlist.get(0);
        assertEquals("Attachment version is not correct", "1.2", attach.getVersion());
        byte[] attachcontent1 = attach1.getContent(getXWikiContext());
        assertEquals("Attachment content size is not correct", attachcontentb.length, attachcontent1.length);
        for (int i=0;i<attachcontent1.length;i++) {
            assertEquals("Attachment content byte " + i + " is not correct", attachcontentb[i], attachcontent1[i]);
        }

        XWikiAttachment attach2 = attach1.getAttachmentRevision("1.1", context);
        // Test that content of attach is the one from version 1.1
        assertEquals("Attachment version is not correct","1.1",attach2.getVersion());
        byte[] attachcontent2 = attach2.getContent(getXWikiContext());
        assertEquals("Attachment content size is not correct", attachcontenta.length,attachcontent2.length);
        for (int i=0; i<attachcontent2.length; i++) {
           assertEquals("Attachment content byte " + i + " is not correct",attachcontenta[i], attachcontent2[i]);
        }
        assertEquals("Attachment author is not correct", attach2.getAuthor(),"XWiki.Author1");
        assertEquals("Attachment comment is not correct", attach2.getComment(),"Test comment 1");
    }


    public void testUpdateAttachmentArchiveReadWrite() throws XWikiException, IOException {
        XWikiStoreInterface store = getXWiki().getStore();
        String contenta = "content a";
        String contentb = "content b";
        byte[] attachcontenta = contenta.getBytes();
        byte[] attachcontentb = contentb.getBytes();
        testUpdateAttachmentArchiveReadWrite(store, attachcontenta, attachcontentb);
    }

    public void testUpdateAttachmentArchiveReadWrite2() throws XWikiException, IOException {
        XWikiStoreInterface store = getXWiki().getStore();
        String contenta = "this is content a with \nanother line and\nfinishing with an end line\n";
        String contentb = "this is content b with \nanother line and\nfinishing with tzo end line\n\n";
        byte[] attachcontenta = contenta.getBytes();
        byte[] attachcontentb = contentb.getBytes();
        testUpdateAttachmentArchiveReadWrite(store, attachcontenta, attachcontentb);
    }

    public void testUpdateAttachmentArchiveReadWriteBinary() throws XWikiException, IOException {
        XWikiStoreInterface store = getXWiki().getStore();
        byte[] attachcontenta = Utils.getDataAsBytes(new File(Utils.filename));
        byte[] attachcontentb = Utils.getDataAsBytes(new File(Utils.filename2));
        testUpdateAttachmentArchiveReadWrite(store, attachcontenta, attachcontentb);
    }

    public void testBadContentVersionedReadWriteOk() throws XWikiException {
        Utils.setBadData(34);
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), getXWiki().getVersioningStore(), Utils.web, Utils.name);
    }

    public void testBadContentVersionedReadWriteNotOk() throws XWikiException {
        Utils.setBadData(35);
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), getXWiki().getVersioningStore(), Utils.web, Utils.name);
    }

    public void testBadContentVersioningOk() throws XWikiException, ParseException {
        int nb = 10;
        String content = "";
        for (int i=0;i<nb;i++)
            content += "* ludovic@xwiki.com\n";

        Object[] lines = ToString.stringToArray(content);
        Archive archive = new Archive(lines, "", "1.1");
        String txt = archive.toString();
        StringInputStream is = new StringInputStream(txt);
        archive = new Archive("", is);
    }

    public void testBadContentVersioningNotOk() throws XWikiException, ParseException {
        int nb = 15;
            String content = "";
            for (int i=0;i<nb;i++)
                content += "* ludovic@xwiki.com\n";

            Object[] lines = ToString.stringToArray(content);
        Archive archive = new Archive(lines, "", "1.1");

        String txt = archive.toString();
        StringInputStream is = new StringInputStream(txt);
        archive = new Archive("", is);
    }
}
