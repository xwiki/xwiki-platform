/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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
 *
 * User: ludovic
 * Date: 6 mars 2004
 * Time: 12:01:47
 */

package com.xpn.xwiki.test;

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.velocity.app.Velocity;
import org.hibernate.HibernateException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.web.XWikiServletURLFactory;


public class XWikiTest extends TestCase {

    private XWiki xwiki;
     private XWikiContext context;

     public XWikiHibernateStore getHibStore() {
         XWikiStoreInterface store = xwiki.getStore();
         if (store instanceof XWikiCacheStoreInterface)
             return (XWikiHibernateStore)((XWikiCacheStoreInterface)store).getStore();
         else
             return (XWikiHibernateStore) store;
     }

     public XWikiStoreInterface getStore() {
         return xwiki.getStore();
     }

     public void setUp() throws Exception {
         context = new XWikiContext();
         context.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
         xwiki = new XWiki("./xwiki.cfg", context, null, false);
         context.setWiki(xwiki);
         StoreHibernateTest.cleanUp(getHibStore(), context);
         Velocity.init("velocity.properties");
     }

     public void tearDown() throws HibernateException {
         getHibStore().shutdownHibernate(context);
         xwiki = null;
         context = null;
         System.gc();
     }

    public void testDefaultSkin() throws XWikiException {
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
        assertEquals("Skin should be default", "default", xwiki.getSkin(context));
    }

    public void testAlternSkin() throws XWikiException {
        Utils.setStringValue("XWiki.XWikiPreferences", "skin", "altern", context);
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
        assertEquals("Skin should be altern", "altern", xwiki.getSkin(context));
    }

    public void testDefaultSkinFile() throws XWikiException {
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(xwiki);
        assertEquals("Skin File should be default",  "/xwiki/skins/default/style.css", xwiki.getSkinFile("style.css", context));
    }

    public void testPassword() throws XWikiException, HibernateException {
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);
        XWikiDocument doc = xwiki.getDocument("XWiki.LudovicDubost", context);
        String xml = doc.getXMLContent(context);
        assertTrue("XML should should contain password field", xml.indexOf("<password>")!=-1);
        assertTrue("XML should contain password", xml.indexOf("toto")!=-1);
        Document ddoc = new Document(doc, context);
        xml = ddoc.getXMLContent();
        assertTrue("XML should should contain password field", xml.indexOf("<password>")!=-1);
        assertTrue("XML should not contain password", xml.indexOf("toto")==-1);
    }

    public void testFormatDate() throws XWikiException {
        Date date;
        Calendar cal = Calendar.getInstance();
        cal.set(2004,1,4,22,33);
        date = cal.getTime();
        assertEquals("Format date failed", "2004/02/04", xwiki.formatDate(date,"yyyy/MM/dd", context) );
        assertEquals("Format date failed", "2004/02/04 22:33", xwiki.formatDate(date, null, context));
        assertEquals("Format date failed", "2004/02/04 22:33", xwiki.formatDate(date, "abc abcd efg", context));
    }

    public void testDocName() throws XWikiException {
        assertEquals("getDocName failed", "LudovicDubost", xwiki.getDocName("xwiki:XWiki.LudovicDubost"));
        assertEquals("getDocName failed", "LudovicDubost", xwiki.getDocName("XWiki.LudovicDubost"));
        assertEquals("getDocName failed", "LudovicDubost", xwiki.getDocName("LudovicDubost"));
        assertEquals("getDocName failed", "LudovicDubost", xwiki.getDocName("Main.LudovicDubost"));
    }

    public void testGetUserName() throws XWikiException, HibernateException {
        assertEquals("getUserName failed", "LudovicDubost", xwiki.getUserName("XWiki.LudovicDubost", context));
        assertEquals("getUserName failed", "LudovicDubost", xwiki.getLocalUserName("XWiki.LudovicDubost", context));
        assertEquals("getUserName failed", "LudovicDubost", xwiki.getLocalUserName("xwiki:XWiki.LudovicDubost", context));
        HashMap map = new HashMap();
        map.put("first_name", "Ludovic");
        map.put("last_name", "Dubost");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);
        String result = xwiki.getUserName("XWiki.LudovicDubost", context);
        assertEquals("getUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic Dubost</a></span>", result );
        result = xwiki.getUserName("xwikitest:XWiki.LudovicDubost", context);
        assertEquals("getUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic Dubost</a></span>", result);
        result = xwiki.getLocalUserName("XWiki.LudovicDubost", context);
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic Dubost</a></span>", result);
        result = xwiki.getLocalUserName("xwikitest:XWiki.LudovicDubost", context);
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic Dubost</a></span>", result);
        result = xwiki.getLocalUserName("XWiki.LudovicDubost", "$first_name", context);
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic</a></span>", result);
        result = xwiki.getLocalUserName("XWiki.LudovicDubost", "$first_name", false, context);
        assertEquals("getLocalUserName failed", "Ludovic", result);
    }

    public void testDocAttachURL() throws XWikiException {
        String attachURL = xwiki.getAttachmentURL("XWiki.LudovicDubost", "fichier avec blancs.gif", context);
        assertTrue("White spaces should be %20", (attachURL.indexOf("+")!=-1));
    }

    public void testSetLock() throws XWikiException {
        HashMap map = new HashMap();
        map.put("first_name", "Ludovic");
        map.put("last_name", "Dubost");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);
        XWikiDocument doc = xwiki.getDocument("XWiki.LudovicDubost", context);
        assertNull("No lock", doc.getLock(context));
        doc.setLock("AnyUser", context);
        XWikiLock thefirstlock = doc.getLock(context);
        assertEquals("AnyUser is locking", thefirstlock.getUserName(), "AnyUser");
        doc.removeLock(context);
        assertNull("No lock", doc.getLock(context));
    }

    public void testLockTimeout() throws XWikiException {
        HashMap map = new HashMap();
        map.put("first_name", "Ludovic");
        map.put("last_name", "Dubost");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);
        XWikiDocument doc = xwiki.getDocument("XWiki.LudovicDubost", context);
        assertNull("No lock", doc.getLock(context));
        doc.setLock("AnyUser", context);
        XWikiLock thefirstlock = doc.getLock(context);
        assertEquals("AnyUser is locking", thefirstlock.getUserName(), "AnyUser");
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "lock_Timeout", "1", context);

        try
        {
            Thread.sleep(1500);
        }
        catch(Exception e) {};

        assertNull("No lock", doc.getLock(context));
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "lock_Timeout", "3600", context);
        assertNull("Lock really removed", doc.getLock(context));

    }

    public void testCopyDocument() throws XWikiException {
        Utils.createDoc(getHibStore(), "Test", "CopyDocument", context);
        xwiki.copyDocument("Test.CopyDocument", "Test.CopyDocument2", context);
        xwiki.flushCache();

        XWikiDocument doc1 = xwiki.getDocument("Test.CopyDocument", context);
        XWikiDocument doc2 = xwiki.getDocument("Test.CopyDocument2", context);
        assertEquals("Copied doc name is not correct", "Test.CopyDocument2", doc2.getFullName());
        assertEquals("Copied doc content is not correct", doc1.getContent(), doc2.getContent());
        // Compare documents except the name and date
        Utils.assertEquals(doc1, doc2, false, false, false);
    }

    public void testCopyDocumentWithObject() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Test", "CopyDocumentWithObject");
        Utils.prepareObject(doc, "Test.CopyDocumentWithObject");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(getHibStore(), "Test", "CopyDocumentWithObject", bobject, bclass, context);
        xwiki.copyDocument("Test.CopyDocumentWithObject", "Test.CopyDocumentWithObject2", context);
        xwiki.flushCache();

        XWikiDocument doc1 = xwiki.getDocument("Test.CopyDocumentWithObject", context);
        XWikiDocument doc2 = xwiki.getDocument("Test.CopyDocumentWithObject2", context);
        assertEquals("Copied doc name is not correct", "Test.CopyDocumentWithObject2", doc2.getFullName());
        assertEquals("Copied doc content is not correct", doc1.getContent(), doc2.getContent());
        // Compare documents except the name and date
        Utils.assertEquals(doc1, doc2, false, false, false);
    }

    public void testCopyDocumentWithAdvancedObject() throws XWikiException {
        XWikiDocument doc = new XWikiDocument("Test", "CopyDocumentWithAdvObject");
        Utils.prepareAdvancedObject(doc, "Test.CopyDocumentWithAdvObject");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(getHibStore(), "Test", "CopyDocumentWithAdvObject", bobject, bclass, context);
        xwiki.copyDocument("Test.CopyDocumentWithAdvObject", "Test.CopyDocumentWithAdvObject2", context);
        xwiki.flushCache();

        XWikiDocument doc1 = xwiki.getDocument("Test.CopyDocumentWithAdvObject", context);
        XWikiDocument doc2 = xwiki.getDocument("Test.CopyDocumentWithAdvObject2", context);
        assertEquals("Copied doc name is not correct", "Test.CopyDocumentWithAdvObject2", doc2.getFullName());
        assertEquals("Copied doc content is not correct", doc1.getContent(), doc2.getContent());
        // Compare documents except the name and date
        Utils.assertEquals(doc1, doc2, false, false, false);
    }

    public void testCopyDocumentWithAttachment() throws XWikiException, IOException {
        Utils.createDoc(getHibStore(), "Test", "CopyDocumentWithAttachment", context);
        XWikiDocument doc1 = xwiki.getDocument("Test.CopyDocumentWithAttachment", context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        getHibStore().saveXWikiDoc(doc1, context);

        xwiki.copyDocument("Test.CopyDocumentWithAttachment", "Test.CopyDocumentWithAttachment2", context);
        xwiki.flushCache();

        XWikiDocument doc2 = xwiki.getDocument("Test.CopyDocumentWithAttachment2", context);
        assertEquals("Copied doc name is not correct", "Test.CopyDocumentWithAttachment2", doc2.getFullName());
        assertEquals("Copied doc content is not correct", doc1.getContent(), doc2.getContent());
        // Compare documents except the name and date
        Utils.assertEquals(doc1, doc2, false, false, false);
        assertEquals("Attachment number is incorrect", doc1.getAttachmentList().size(), doc2.getAttachmentList().size());
        XWikiAttachment attachment2 = doc2.getAttachment(attachment1.getFilename());
        assertEquals("Attachment filename incorrect", attachment1.getFilename(), attachment2.getFilename());
        assertEquals("Attachment file size", attachment1.getFilesize(), attachment2.getFilesize());
        assertEquals("Attachment author incorrect", attachment1.getAuthor(), attachment2.getAuthor());
        assertEquals("Attachment content incorrect", new String(attachment1.getContent(context)), new String(attachment2.getContent(context)));
    }

}
