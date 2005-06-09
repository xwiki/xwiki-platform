/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiStoreInterface;
import org.apache.commons.jrcs.rcs.Version;

import java.io.File;
import java.io.IOException;
import java.util.List;

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

    public void versionedReadWrite(XWikiStoreInterface store,String web, String name) throws XWikiException {
        XWikiDocument doc3 = new XWikiDocument(web, name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, getXWikiContext());
        XWikiDocument doc4 = store.loadXWikiDoc(doc3,Utils.version, getXWikiContext());
        String content4 = doc4.getContent();
        assertEquals(Utils.content1,content4);
        assertEquals(doc4.getVersion(),Utils.version);
        assertEquals(doc4.getAuthor(), Utils.author);
        Version[] versions = store.getXWikiDocVersions(doc4, getXWikiContext());
        assertTrue(versions.length==2);
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
        versionedReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testVersionedReadWriteWithAccents() throws XWikiException {
        Utils.setStandardAccentData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testMediumReadWrite() throws XWikiException {
        Utils.setMediumData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testBigVersionedReadWrite() throws XWikiException, IOException {
        Utils.setBigData();
        standardReadWrite(getXWiki().getStore(), Utils.web, Utils.name2, getXWikiContext());
        versionedReadWrite(getXWiki().getStore(), Utils.web, Utils.name2);
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

    public void deleteAttachmentReadWrite(XWikiStoreInterface store, String web, String name) throws XWikiException, IOException {

        XWikiDocument doc3 = new XWikiDocument(web, name);
        doc3 = (XWikiDocument) store.loadXWikiDoc(doc3, getXWikiContext());
        List attachlist = doc3.getAttachmentList();
        XWikiAttachment attachment3 = (XWikiAttachment) attachlist.get(0);

        store.deleteXWikiAttachment(attachment3, getXWikiContext(), true);

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


    public void testUpdateAttachmentReadWrite() throws XWikiException, IOException {
        Utils.setStandardData();
        attachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
        updateAttachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
    }

    public void testDeleteAttachmentReadWrite() throws XWikiException, IOException {
        Utils.setStandardData();
        attachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
        deleteAttachmentReadWrite(getXWiki().getStore(), Utils.web, Utils.name);
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

}
