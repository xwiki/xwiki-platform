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

import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.io.File;
import java.io.IOException;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class XWikiTest extends HibernateTestCase {

     public void setUp() throws Exception {
         super.setUp();
         getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
     }

    public void testDefaultSkin() throws XWikiException {
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(getXWiki(), getXWikiContext());
        assertEquals("Skin should be default", "default", getXWiki().getSkin(getXWikiContext()));
    }

    public void testAlternSkin() throws XWikiException {
        Utils.setStringValue("XWiki.XWikiPreferences", "skin", "altern", getXWikiContext());
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(getXWiki(), getXWikiContext());
        assertEquals("Skin should be altern", "altern", getXWiki().getSkin(getXWikiContext()));
    }

    public void testDefaultSkinFile() throws XWikiException {
        XWikiRenderingEngine wikiengine = new XWikiRenderingEngine(getXWiki(), getXWikiContext());
        assertEquals("Skin File should be default",  "/xwiki/skins/default/style.css", getXWiki().getSkinFile("style.css", getXWikiContext()));
    }

    public void testPassword() throws XWikiException, HibernateException {
        HashMap map = new HashMap();
        map.put("password", "toto");
        getXWiki().createUser("LudovicDubost", map, "", "", "view, edit", getXWikiContext());
        XWikiDocument doc = getXWiki().getDocument("XWiki.LudovicDubost", getXWikiContext());
        String xml = doc.getXMLContent(getXWikiContext());
        assertTrue("XML should should contain password field", xml.indexOf("<password>")!=-1);
        assertTrue("XML should contain password", xml.indexOf("toto")!=-1);
        Document ddoc = new Document(doc, getXWikiContext());
        xml = ddoc.getXMLContent();
        assertTrue("XML should should contain password field", xml.indexOf("<password>")!=-1);
        assertTrue("XML should not contain password", xml.indexOf("toto")==-1);
    }

    public void testFormatDate() throws XWikiException {
        Date date;
        Calendar cal = Calendar.getInstance();
        cal.set(2004,1,4,22,33);
        date = cal.getTime();
        assertEquals("Format date failed", "2004/02/04", getXWiki().formatDate(date,"yyyy/MM/dd", getXWikiContext()) );
        assertEquals("Format date failed", "2004/02/04 22:33", getXWiki().formatDate(date, null, getXWikiContext()));
        assertEquals("Format date failed", "2004/02/04 22:33", getXWiki().formatDate(date, "abc abcd efg", getXWikiContext()));
    }

    public void testDocName() throws XWikiException {
        assertEquals("getDocName failed", "LudovicDubost", getXWiki().getDocName("xwiki:XWiki.LudovicDubost"));
        assertEquals("getDocName failed", "LudovicDubost", getXWiki().getDocName("XWiki.LudovicDubost"));
        assertEquals("getDocName failed", "LudovicDubost", getXWiki().getDocName("LudovicDubost"));
        assertEquals("getDocName failed", "LudovicDubost", getXWiki().getDocName("Main.LudovicDubost"));
    }

    public void testGetUserName() throws XWikiException, HibernateException {
        assertEquals("getUserName failed", "LudovicDubost", getXWiki().getUserName("XWiki.LudovicDubost", getXWikiContext()));
        assertEquals("getUserName failed", "LudovicDubost", getXWiki().getLocalUserName("XWiki.LudovicDubost", getXWikiContext()));
        assertEquals("getUserName failed", "LudovicDubost", getXWiki().getLocalUserName("xwiki:XWiki.LudovicDubost", getXWikiContext()));
        HashMap map = new HashMap();
        map.put("first_name", "Ludovic");
        map.put("last_name", "Dubost");
        getXWiki().createUser("LudovicDubost", map, "", "", "view, edit", getXWikiContext());
        String result = getXWiki().getUserName("XWiki.LudovicDubost", getXWikiContext());
        assertEquals("getUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic Dubost</a></span>", result );
        result = getXWiki().getUserName("xwikitest:XWiki.LudovicDubost", getXWikiContext());
        assertEquals("getUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic Dubost</a></span>", result);
        result = getXWiki().getLocalUserName("XWiki.LudovicDubost", getXWikiContext());
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic Dubost</a></span>", result);
        result = getXWiki().getLocalUserName("xwikitest:XWiki.LudovicDubost", getXWikiContext());
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic Dubost</a></span>", result);
        result = getXWiki().getLocalUserName("XWiki.LudovicDubost", "$first_name", getXWikiContext());
        assertEquals("getLocalUserName failed", "<span class=\"wikilink\"><a href=\"/xwiki/bin/view/XWiki/LudovicDubost\">Ludovic</a></span>", result);
        result = getXWiki().getLocalUserName("XWiki.LudovicDubost", "$first_name", false, getXWikiContext());
        assertEquals("getLocalUserName failed", "Ludovic", result);
    }

    public void testDocAttachURL() throws XWikiException {
        String attachURL = getXWiki().getAttachmentURL("XWiki.LudovicDubost", "fichier avec blancs.gif", getXWikiContext());
        assertTrue("White spaces should be %20", (attachURL.indexOf("+")!=-1));
    }

    public void testSetLock() throws XWikiException {
        HashMap map = new HashMap();
        map.put("first_name", "Ludovic");
        map.put("last_name", "Dubost");
        getXWiki().createUser("LudovicDubost", map, "", "", "view, edit", getXWikiContext());
        XWikiDocument doc = getXWiki().getDocument("XWiki.LudovicDubost", getXWikiContext());
        assertNull("No lock", doc.getLock(getXWikiContext()));
        doc.setLock("AnyUser", getXWikiContext());
        XWikiLock thefirstlock = doc.getLock(getXWikiContext());
        assertEquals("AnyUser is locking", thefirstlock.getUserName(), "AnyUser");
        doc.removeLock(getXWikiContext());
        assertNull("No lock", doc.getLock(getXWikiContext()));
    }

    public void testLockTimeout() throws XWikiException {
        HashMap map = new HashMap();
        map.put("first_name", "Ludovic");
        map.put("last_name", "Dubost");
        getXWiki().createUser("LudovicDubost", map, "", "", "view, edit", getXWikiContext());
        XWikiDocument doc = getXWiki().getDocument("XWiki.LudovicDubost", getXWikiContext());
        assertNull("No lock", doc.getLock(getXWikiContext()));
        doc.setLock("AnyUser", getXWikiContext());
        XWikiLock thefirstlock = doc.getLock(getXWikiContext());
        assertEquals("AnyUser is locking", thefirstlock.getUserName(), "AnyUser");
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "lock_Timeout", "1", getXWikiContext());

        try
        {
            Thread.sleep(1500);
        }
        catch(Exception e) {};

        assertNull("No lock", doc.getLock(getXWikiContext()));
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "lock_Timeout", "3600", getXWikiContext());
        assertNull("Lock really removed", doc.getLock(getXWikiContext()));

    }

    public void testCopyDocument() throws XWikiException {
        Utils.createDoc(getXWiki().getHibernateStore(), "Test", "CopyDocument", getXWikiContext());
        getXWiki().copyDocument("Test.CopyDocument", "Test.CopyDocument2", getXWikiContext());
        getXWiki().flushCache();

        XWikiDocument doc1 = getXWiki().getDocument("Test.CopyDocument", getXWikiContext());
        XWikiDocument doc2 = getXWiki().getDocument("Test.CopyDocument2", getXWikiContext());
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
        Utils.createDoc(getXWiki().getHibernateStore(), "Test", "CopyDocumentWithObject", bobject, bclass, getXWikiContext());
        getXWiki().copyDocument("Test.CopyDocumentWithObject", "Test.CopyDocumentWithObject2", getXWikiContext());
        getXWiki().flushCache();

        XWikiDocument doc1 = getXWiki().getDocument("Test.CopyDocumentWithObject", getXWikiContext());
        XWikiDocument doc2 = getXWiki().getDocument("Test.CopyDocumentWithObject2", getXWikiContext());
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
        Utils.createDoc(getXWiki().getHibernateStore(), "Test", "CopyDocumentWithAdvObject", bobject, bclass, getXWikiContext());
        getXWiki().copyDocument("Test.CopyDocumentWithAdvObject", "Test.CopyDocumentWithAdvObject2", getXWikiContext());
        getXWiki().flushCache();

        XWikiDocument doc1 = getXWiki().getDocument("Test.CopyDocumentWithAdvObject", getXWikiContext());
        XWikiDocument doc2 = getXWiki().getDocument("Test.CopyDocumentWithAdvObject2", getXWikiContext());
        assertEquals("Copied doc name is not correct", "Test.CopyDocumentWithAdvObject2", doc2.getFullName());
        assertEquals("Copied doc content is not correct", doc1.getContent(), doc2.getContent());
        // Compare documents except the name and date
        Utils.assertEquals(doc1, doc2, false, false, false);
    }

    public void testCopyDocumentWithAttachment() throws XWikiException, IOException {
        Utils.createDoc(getXWiki().getHibernateStore(), "Test", "CopyDocumentWithAttachment", getXWikiContext());
        XWikiDocument doc1 = getXWiki().getDocument("Test.CopyDocumentWithAttachment", getXWikiContext());
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, getXWikiContext());
        doc1.getAttachmentList().add(attachment1);
        getXWiki().getHibernateStore().saveXWikiDoc(doc1, getXWikiContext());

        getXWiki().copyDocument("Test.CopyDocumentWithAttachment", "Test.CopyDocumentWithAttachment2", getXWikiContext());
        getXWiki().flushCache();

        XWikiDocument doc2 = getXWiki().getDocument("Test.CopyDocumentWithAttachment2", getXWikiContext());
        assertEquals("Copied doc name is not correct", "Test.CopyDocumentWithAttachment2", doc2.getFullName());
        assertEquals("Copied doc content is not correct", doc1.getContent(), doc2.getContent());
        // Compare documents except the name and date
        Utils.assertEquals(doc1, doc2, false, false, false);
        assertEquals("Attachment number is incorrect", doc1.getAttachmentList().size(), doc2.getAttachmentList().size());
        XWikiAttachment attachment2 = doc2.getAttachment(attachment1.getFilename());
        assertEquals("Attachment filename incorrect", attachment1.getFilename(), attachment2.getFilename());
        assertEquals("Attachment file size", attachment1.getFilesize(), attachment2.getFilesize());
        assertEquals("Attachment author incorrect", attachment1.getAuthor(), attachment2.getAuthor());
        assertEquals("Attachment content incorrect", new String(attachment1.getContent(getXWikiContext())), new String(attachment2.getContent(getXWikiContext())));
    }

    public void testAccessSecureAPINoAccess() throws XWikiException {
        XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
        Utils.content1 = "$context.context.database";
        Utils.createDoc(getXWiki().getHibernateStore(), "Test", "SecureAPI", getXWikiContext());
        XWikiDocument doc1 = getXWiki().getDocument("Test.SecureAPI", getXWikiContext());
        RenderTest.renderTest(wikiengine, doc1, "$context.context.database", false, getXWikiContext());
    }

    public void testAccessSecureAPIAccess() throws XWikiException {
        XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
        Utils.content1 = "$context.context.database";
        Utils.author = "XWiki.LudovicDubost";
        Utils.updateRight(getXWiki(), getXWikiContext(), "XWiki.XWikiPreferences", "XWiki.LudovicDubost","","admin", true, true);
        Utils.createDoc(getXWiki().getHibernateStore(), "Test", "SecureAPI2", getXWikiContext());
        XWikiDocument doc1 = getXWiki().getDocument("Test.SecureAPI2", getXWikiContext());
        RenderTest.renderTest(wikiengine, doc1, "xwikitest", false, getXWikiContext());
    }

    public void testAccessSecureAPIAccessWithInclude() throws XWikiException {
        XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
        Utils.updateRight(getXWiki(), getXWikiContext(), "XWiki.XWikiPreferences", "XWiki.LudovicDubost","","admin", true, true);

        Utils.content1 = "#includeForm(\"SecureAPI4\")";
        Utils.author = "XWiki.LudovicDubost";
        Utils.createDoc(getXWiki().getHibernateStore(), "Test", "SecureAPI3", getXWikiContext());

        Utils.content1 = "$context.context.database";
        Utils.author = "XWiki.JohnDoe";
        Utils.createDoc(getXWiki().getHibernateStore(), "Test", "SecureAPI4", getXWikiContext());

        XWikiDocument doc1 = getXWiki().getDocument("Test.SecureAPI3", getXWikiContext());
        RenderTest.renderTest(wikiengine, doc1, "$context.context.database", false, getXWikiContext());
    }


}
