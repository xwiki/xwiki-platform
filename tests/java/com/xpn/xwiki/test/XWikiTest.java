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
 */

package com.xpn.xwiki.test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.render.DefaultXWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiRenderer;
import com.xpn.xwiki.render.filter.XWikiLinkFilter;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class XWikiTest extends HibernateTestCase {

     public void setUp() throws Exception {
         super.setUp();
         getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
     }

    public void testDefaultSkin() throws XWikiException {
        XWikiRenderingEngine wikiengine = new DefaultXWikiRenderingEngine(getXWiki(), getXWikiContext());
        assertEquals("Skin should be default", "albatross", getXWiki().getSkin(getXWikiContext()));
    }

    public void testAlternSkin() throws XWikiException {
        Utils.setStringValue("XWiki.XWikiPreferences", "skin", "altern", getXWikiContext());
        XWikiRenderingEngine wikiengine = new DefaultXWikiRenderingEngine(getXWiki(), getXWikiContext());
        assertEquals("Skin should be altern", "altern", getXWiki().getSkin(getXWikiContext()));
    }

    public void testDefaultSkinFile() throws XWikiException {
        XWikiRenderingEngine wikiengine = new DefaultXWikiRenderingEngine(getXWiki(), getXWikiContext());
        assertEquals("Skin File should be albatross",  "/xwiki/skins/albatross/style.css", getXWiki().getSkinFile("style.css", getXWikiContext()));
    }

    public void testPassword() throws XWikiException, HibernateException {
        HashMap map = new HashMap();
        map.put("password", "toto");
        getXWiki().createUser("LudovicDubost", map, "", "", "view, edit", getXWikiContext());
        XWikiDocument doc = getXWiki().getDocument("XWiki.LudovicDubost", getXWikiContext());
        String xml = doc.getXMLContent(getXWikiContext());
        assertTrue("XML should contain password field", xml.indexOf("<password>")!=-1);
        assertTrue("XML should contain password", xml.indexOf("toto")!=-1);
        Document ddoc = doc.newDocument(getXWikiContext());
        xml = ddoc.getXMLContent();
        assertTrue("XML should contain password field", xml.indexOf("<password>")!=-1);
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
        catch(Exception e) {}

        assertNull("No lock", doc.getLock(getXWikiContext()));
        Utils.setStringValue("XWiki.XWikiPreferences", "XWiki.XWikiPreferences", "lock_Timeout", "3600", getXWikiContext());
        assertNull("Lock really removed", doc.getLock(getXWikiContext()));

    }

    public void testCopyDocument() throws XWikiException {
        Utils.createDoc(getXWiki().getStore(), "Test", "CopyDocument", getXWikiContext());
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
        Utils.createDoc(getXWiki().getStore(), "Test", "CopyDocumentWithObject", bobject, bclass, getXWikiContext());
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
        Utils.createDoc(getXWiki().getStore(), "Test", "CopyDocumentWithAdvObject", bobject, bclass, getXWikiContext());
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
        Utils.createDoc(getXWiki().getStore(), "Test", "CopyDocumentWithAttachment", getXWikiContext());
        XWikiDocument doc1 = getXWiki().getDocument("Test.CopyDocumentWithAttachment", getXWikiContext());
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, getXWikiContext());
        doc1.getAttachmentList().add(attachment1);
        getXWiki().getStore().saveXWikiDoc(doc1, getXWikiContext());

        assertTrue( getXWiki().copyDocument("Test.CopyDocumentWithAttachment", "Test.CopyDocumentWithAttachment2", getXWikiContext()) );
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
        Utils.createDoc(getXWiki().getStore(), "Test", "SecureAPI", getXWikiContext());
        XWikiDocument doc1 = getXWiki().getDocument("Test.SecureAPI", getXWikiContext());
        AbstractRenderTest.renderTest(wikiengine, doc1, "$context.context.database", false, getXWikiContext());
    }

    public void testAccessSecureAPIAccess() throws XWikiException {
        XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
        Utils.content1 = "$context.context.database";
        Utils.author = "XWiki.LudovicDubost";
        Utils.updateRight(getXWiki(), getXWikiContext(), "XWiki.XWikiPreferences", "XWiki.LudovicDubost","","admin", true, true);
        Utils.createDoc(getXWiki().getStore(), "Test", "SecureAPI2", getXWikiContext());
        XWikiDocument doc1 = getXWiki().getDocument("Test.SecureAPI2", getXWikiContext());
        AbstractRenderTest.renderTest(wikiengine, doc1, "xwikitest", false, getXWikiContext());
    }

    public void testAccessSecureAPIAccessWithInclude() throws XWikiException {
        XWikiRenderingEngine wikiengine = getXWiki().getRenderingEngine();
        Utils.updateRight(getXWiki(), getXWikiContext(), "XWiki.XWikiPreferences", "XWiki.LudovicDubost","","admin", true, true);

        Utils.content1 = "$xwiki.includeForm(\"SecureAPI4\")";
        Utils.author = "XWiki.LudovicDubost";
        Utils.createDoc(getXWiki().getStore(), "Test", "SecureAPI3", getXWikiContext());

        Utils.content1 = "$context.context.database";
        Utils.author = "XWiki.JohnDoe";
        Utils.createDoc(getXWiki().getStore(), "Test", "SecureAPI4", getXWikiContext());

        XWikiDocument doc1 = getXWiki().getDocument("Test.SecureAPI3", getXWikiContext());
        AbstractRenderTest.renderTest(wikiengine, doc1, "$context.context.database", false, getXWikiContext());
    }

    /*
    public void testXWikiPrefs() throws XWikiException {
        XWikiHibernateStore hibstore = getXWiki().getHibernateStore();
        System.out.println("JDBC Calls for init");
        hibstore.getBatcherStats().printSQLList(System.out);
        hibstore.resetBatcherStats();

        getXWiki().getDocument("Main.WebHome", context);
        System.out.println("JDBC Calls for getDocument");
        hibstore.getBatcherStats().printSQLList(System.out);

        getXWiki().flushCache();
        hibstore.resetBatcherStats();
        getXWiki().getPrefsClass(context);
        System.out.println("JDBC Calls for first getPrefsClass");
        hibstore.getBatcherStats().printSQLList(System.out);

        getXWiki().flushCache();
        hibstore.resetBatcherStats();
        getXWiki().getPrefsClass(context);
        System.out.println("JDBC Calls for second getPrefsClass");
        hibstore.getBatcherStats().printSQLList(System.out);
    }
    */

    /*
    public void testXWikiInit() throws XWikiException {

        XWikiHibernateStore hibstore = getXWiki().getHibernateStore();
        System.out.println("JDBC Calls for init");
        hibstore.getBatcherStats().printSQLList(System.out);
        hibstore.resetBatcherStats();
        getXWiki().flushCache();

        // Let's reinit completely
        this.xwiki = new XWiki(this.config, this.context, null, true);
        this.xwiki.setDatabase("xwikitest");
        this.context.setWiki(this.xwiki);
        getXWiki().flushCache();

        System.out.println("JDBC Calls for re-init");
        hibstore.getBatcherStats().printSQLList(System.out);
        hibstore.resetBatcherStats();

        // Let's reinit completely a second time
        this.xwiki = new XWiki(this.config, this.context, null, true);
        this.xwiki.setDatabase("xwikitest");
        this.context.setWiki(this.xwiki);
        getXWiki().flushCache();

        System.out.println("JDBC Calls for re-init 2");
        hibstore.getBatcherStats().printSQLList(System.out);
        hibstore.resetBatcherStats();
        getXWiki().flushCache();

        // Let's read the home document
        getXWiki().getDocument("Main.WebHome", context);
        System.out.println("JDBC Calls for getDocument");
        hibstore.getBatcherStats().printSQLList(System.out);
    }
    */

    public void testXWikiDocElements() throws XWikiException {
        XWikiDocument doc = new XWikiDocument();

        assertTrue("doc should have attachments", doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
        assertTrue("doc should have objects", doc.hasElement(XWikiDocument.HAS_OBJECTS));

        doc.setElement(XWikiDocument.HAS_ATTACHMENTS, true);
        assertTrue("doc should have attachments", doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
        assertTrue("doc should have objects", doc.hasElement(XWikiDocument.HAS_OBJECTS));

        doc.setElement(XWikiDocument.HAS_ATTACHMENTS, false);
        doc.setElement(XWikiDocument.HAS_OBJECTS, true);
        assertFalse("doc should not have attachments", doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
        assertTrue("doc should  have objects", doc.hasElement(XWikiDocument.HAS_OBJECTS));

        doc.setElement(XWikiDocument.HAS_ATTACHMENTS, true);
        doc.setElement(XWikiDocument.HAS_OBJECTS, false);
        assertTrue("doc should have attachments", doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
        assertFalse("doc should not have objects", doc.hasElement(XWikiDocument.HAS_OBJECTS));

        doc.setElement(XWikiDocument.HAS_ATTACHMENTS, true);
        doc.setElement(XWikiDocument.HAS_OBJECTS, true);
        assertTrue("doc should have attachments", doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
        assertTrue("doc should have objects", doc.hasElement(XWikiDocument.HAS_OBJECTS));

        doc.setElement(XWikiDocument.HAS_ATTACHMENTS, false);
        doc.setElement(XWikiDocument.HAS_OBJECTS, false);
        assertFalse("doc should not have attachments", doc.hasElement(XWikiDocument.HAS_ATTACHMENTS));
        assertFalse("doc should not have objects", doc.hasElement(XWikiDocument.HAS_OBJECTS));
    }

    public void testExtractTitle() {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName("Test.MyDoc");
        doc.setContent("1 Hello\r\nThis is a text\r\n1.1 another title\r\nSome more text");
        assertEquals("Extract title incorrect", "Hello", doc.extractTitle());
        doc.setContent("1 Hello \r\nThis is a text\r\n1.1 another title\r\nSome more text");
        assertEquals("Extract title incorrect", "Hello", doc.extractTitle());
        doc.setContent("1.1 Hello\r\nThis is a text\r\n1.1 another title\r\nSome more text");
        assertEquals("Extract title incorrect", "Hello", doc.extractTitle());
        doc.setContent("1.1 Hello \r\nThis is a text\r\n1.1 another title\r\nSome more text");
        assertEquals("Extract title incorrect", "Hello", doc.extractTitle());
        doc.setContent("\r\n1 Hello\r\nThis is a text\r\n1.1 another title\r\nSome more text");
        assertEquals("Extract title incorrect", "Hello", doc.extractTitle());
        doc.setContent("\n1 Hello\nThis is a text\n1.1 another title\nSome more text");
        assertEquals("Extract title incorrect", "Hello", doc.extractTitle());
        doc.setContent("\nHello\nThis is a text\nAnother text\nSome more text");
        assertEquals("Extract title incorrect", "", doc.extractTitle());
    }
    public void testReplaceCapitals() {
        String text;
        text = "Hello John";
        assertEquals("Replacement failed for " + text, text, XWikiLinkFilter.convertWikiWords(text));
        text = "Hello john Wayne";
        assertEquals("Replacement failed for " + text, text, XWikiLinkFilter.convertWikiWords(text));
        text = "HelloJohn";
        assertEquals("Replacement failed for " + text, "Hello John", XWikiLinkFilter.convertWikiWords(text));
        text = "HellojohnWayne";
        assertEquals("Replacement failed" + text, "Hellojohn Wayne", XWikiLinkFilter.convertWikiWords(text));
        text = "helloJohnwayne";
        assertEquals("Replacement failed" + text, "hello Johnwayne", XWikiLinkFilter.convertWikiWords(text));
        text = "HelloJohnWayne";
        assertEquals("Replacement failed" + text, "Hello John Wayne", XWikiLinkFilter.convertWikiWords(text));
    }

    public void testVirtualCopyDocument() throws XWikiException, HibernateException {
        this.config.put("xwiki.virtual", "1");
        Utils.createDoc(getXWiki().getHibernateStore(), "XWiki", "XWikiServerXwikitest2", getXWikiContext());
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", getXWikiContext());

        getXWikiContext().setDatabase("xwikitest2");
        // TODO: Split into several tests if you need a new clean up.
        StoreHibernateTest.cleanUp(getXWiki().getHibernateStore(), false, true, getXWikiContext());
        getXWikiContext().setDatabase("xwikitest");
        Utils.createDoc(getXWiki().getHibernateStore(), "Main", "CopyTest", getXWikiContext());
        XWikiDocument sourcedoc = getXWiki().getDocument("Main.CopyTest", getXWikiContext());

        getXWiki().copyDocument("Main.CopyTest", "Main.CopyTest","xwikitest", "xwikitest2", "en", true, false, getXWikiContext());
        getXWiki().flushCache();

        getXWikiContext().setDatabase("xwikitest2");
        XWikiDocument targetdoc = getXWiki().getDocument("Main.CopyTest", getXWikiContext());

        assertEquals("Content of doc is different", sourcedoc.getContent(), targetdoc.getContent());
    }

    public void testVirtualCopyDocumentWithAttachment() throws XWikiException, HibernateException, IOException {
        this.config.put("xwiki.virtual", "1");
        Utils.createDoc(getXWiki().getHibernateStore(), "XWiki", "XWikiServerXwikitest2", getXWikiContext());
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", getXWikiContext());

        getXWikiContext().setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(getXWiki().getHibernateStore(), false, true, getXWikiContext());

        getXWikiContext().setDatabase("xwikitest");
        Utils.createDoc(getXWiki().getHibernateStore(), "Main", "CopyTest", getXWikiContext());
        XWikiDocument sourcedoc = getXWiki().getDocument("Main.CopyTest", getXWikiContext());

        XWikiAttachment attachment1 = new XWikiAttachment(sourcedoc, Utils.afilename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        sourcedoc.saveAttachmentContent(attachment1, getXWikiContext());
        sourcedoc.getAttachmentList().add(attachment1);
        getXWiki().saveDocument(sourcedoc, getXWikiContext());

        getXWiki().copyDocument("Main.CopyTest", "Main.CopyTest","xwikitest", "xwikitest2", "en", true, false, getXWikiContext());

        getXWiki().flushCache();
        getXWikiContext().setDatabase("xwikitest2");
        XWikiDocument targetdoc = getXWiki().getDocument("Main.CopyTest", getXWikiContext());

        assertEquals("Content of doc is different", sourcedoc.getContent(), targetdoc.getContent());

        List attachlist = targetdoc.getAttachmentList();
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

}
