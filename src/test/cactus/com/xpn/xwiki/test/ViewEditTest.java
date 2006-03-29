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
 * @author namphunghai
 * @author vmassol
 * @author sdumitriu
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.store.XWikiBatcher;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.struts.action.ActionServlet;
import org.dom4j.DocumentException;
import org.hibernate.HibernateException;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;


public class ViewEditTest extends ServletTest {

    public void beginViewNotOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        setUrl(webRequest, "view", "ViewNotOkTest");
    }

    public void endViewNotOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Page should have no version: " + result, result.indexOf("1.1") == -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewNotOk() throws Throwable {
        launchTest();
    }

    public void beginViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewOkTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewOkTest");
    }

    public void endViewOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find page name in content: " + result, result.indexOf("ViewOkTest") != -1);
            assertTrue("Could not find hr in content: " + result, result.indexOf("<hr") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewOk() throws IOException, Throwable {
        launchTest();
    }

    public void beginViewRawOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewRawOkTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewRawOkTest");
        webRequest.addParameter("raw", "1");
    }

    public void endViewRawOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find raw page name in content: " + result, result.indexOf("$doc.name") != -1);
            assertTrue("Could not find raw hr in content: " + result, result.indexOf("----") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewRawOk() throws IOException, Throwable {
        launchTest();
    }

    public void beginViewCodeOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewRawOkTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewRawOkTest");
        webRequest.addParameter("xpage", "code");
    }

    public void endViewCodeOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find raw page name in content: " + result, result.indexOf("$doc.name") != -1);
            assertTrue("Could not find raw hr in content: " + result, result.indexOf("----") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewCodeOk() throws IOException, Throwable {
        launchTest();
    }

    public void beginViewXMLOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewXMLOkTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewXMLOkTest");
        webRequest.addParameter("xpage", "xml");
    }

    public void endViewXMLOk(WebResponse webResponse) throws HibernateException, IllegalAccessException, DocumentException, ParseException, ClassNotFoundException, InstantiationException, XWikiException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find raw page name in content: " + result, result.indexOf("$doc.name") != -1);
            assertTrue("Could not find raw hr in content: " + result, result.indexOf("----") != -1);
            assertTrue("Could not find xml tags in content: " + result, result.indexOf("<content") != -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc1 = (XWikiDocument) xwiki.getDocument("Main.ViewXMLOkTest", context);
            XWikiDocument doc2 = new XWikiDocument();
            doc2.fromXML(result);
            Utils.assertEquals(doc1, doc2);
        } finally {
            clientTearDown();
        }
    }

    public void testViewXMLOk() throws IOException, Throwable {
        launchTest();
    }


    public void beginViewGetDocument(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(xwiki.getStore(), "Main", "ViewGetDocumentContent", context);
        String content = Utils.content1;
        Utils.content1 = "test\n$xwiki.getDocument(\"Main.ViewGetDocumentContent\").getContent()\ntest\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewGetDocument", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewGetDocument");
    }

    public void endViewGetDocument(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find content in result:\n" + result, result.indexOf("Hello") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewGetDocument() throws Throwable {
        launchTest();
    }


    public void beginViewDocumentLink(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        // Utils.createDoc(xwiki.getStore(), "Main", "ViewDocumentTestLinkTest", context);
        String content = Utils.content1;
        Utils.content1 = "[Main.ViewDocumentLink]";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewDocumentLink", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewDocumentLink");
    }

    public void endViewDocumentLink(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find content in result:\n" + result, result.indexOf("<a href") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewDocumentLink() throws Throwable {
        launchTest();
    }


    public void beginPreviewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PreviewOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(xwiki.getStore(), "Main", "PreviewOkTest", bobject, bclass, context);
        setUrl(webRequest, "preview", "PreviewOkTest");
    }

    public void endPreviewOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find object hidden form nb: " + result, result.indexOf("<input type=\"hidden\" name=\"Main.PreviewOkTest_nb\" value=\"1\"") != -1);
            assertTrue("Could not find object hidden form value: " + result, result.indexOf("Main.PreviewOkTest_0_first_name") != -1);
            assertTrue("Could not find object hidden form value: " + result, result.indexOf("Ludovic") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testPreviewOk() throws Throwable {
        launchTest();
    }

    public void beginEditOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.EditOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(xwiki.getStore(), "Main", "EditOkTest", bobject, bclass, context);
        setUrl(webRequest, "edit", "EditOkTest");
    }

    public void endEditOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testEditOk() throws Throwable {
        launchTest();
    }


    public void beginEditObjectOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.EditObjectOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(xwiki.getStore(), "Main", "EditObjectOkTest", bobject, bclass, context);
        setUrl(webRequest, "edit", "EditObjectOkTest");
        webRequest.addParameter("xpage", "editobject");
    }

    public void endEditObjectOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find object hidden form nb: " + result, result.indexOf("<input type=\"hidden\" name=\"Main.EditObjectOkTest_nb\" value=\"1\"") != -1);
            assertTrue("Could not find object form value: " + result, result.indexOf("Main.EditObjectOkTest_0_first_name") != -1);
            assertTrue("Could not find object form value: " + result, result.indexOf("Ludovic") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testEditObjectOk() throws Throwable {
        launchTest();
    }


    public void beginEditClassOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.EditClassOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(xwiki.getStore(), "Main", "EditClassOkTest", bobject, bclass, context);
        setUrl(webRequest, "edit", "EditClassOkTest");
        webRequest.addParameter("xpage", "editclass");
    }

    public void endEditClassOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.NumberClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.StringClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.TextAreaClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.PasswordClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.BooleanClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.DBListClass") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testEditClassOk() throws Throwable {
        launchTest();
    }


    public void beginEditWithTemplateOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.EditOkTestTemplate");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        String content = Utils.content1;
        Utils.content1 = "Template content";
        Utils.createDoc(xwiki.getStore(), "Main", "EditOkTestTemplate", bobject, bclass, context);
        Utils.content1 = content;
        setUrl(webRequest, "edit", "EditOkWithTestTemplate");
        webRequest.addParameter("template", "Main.EditOkTestTemplate");
        webRequest.addParameter("parent", "XWiki.TestParentComesFromTemplate");
    }

    public void endEditWithTemplateOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Template content") != -1);
            assertTrue("Could not find parent: " + result, result.indexOf("TestParentComesFromTemplate") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testEditWithTemplateOk() throws Throwable {
        launchTest();
    }


    public void testEditWithTemplateNotOk() throws Throwable {
        try {
            launchTest();
        } catch (XWikiException e) {
        }
    }

    public void beginEditWithTemplateNotOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Document already exist.. This will make template system fail
        Utils.createDoc(xwiki.getStore(), "Main", "EditOkTestWithTemplate", context);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.EditOkTestTemplate");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        String content = Utils.content1;
        Utils.content1 = "Template content";
        Utils.createDoc(xwiki.getStore(), "Main", "EditOkTestTemplate", bobject, bclass, context);
        Utils.content1 = content;
        setUrl(webRequest, "edit", "EditOkTestWithTemplate");
        webRequest.addParameter("template", "Main.EditOkTestTemplate");
        webRequest.addParameter("parent", "XWiki.TestParentComesFromTemplate");
    }

    public void endEditWithTemplateNotOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertFalse("Template content should be ignored: " + result, result.indexOf("Template content") != -1);
        } finally {
            clientTearDown();
        }
    }


    public void beginViewLatestRevOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewRevOkTest", context);
        Utils.content1 = content;
        XWikiDocument doc2 = xwiki.getDocument("Main.ViewRevOkTest", context);
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "viewrev", "ViewRevOkTest", "");
        webRequest.addParameter("rev", "1.2");
    }


    public void endViewLatestRevOk(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find zzz in Content: " + result, result.indexOf("zzzzzzzzzzzzzzzzzzzzzzzz") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewLatestRevOk() throws Throwable {
        launchTest();
    }


    public void beginViewRevOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewRevOkTest", context);
        Utils.content1 = content;
        XWikiDocument doc2 = xwiki.getDocument("Main.ViewRevOkTest", context);
        xwiki.saveDocument(doc2, context);
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "viewrev", "ViewRevOkTest", "");
        webRequest.addParameter("rev", "1.2");
    }


    public void endViewRevOk(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find raw page name in content: " + result, result.indexOf("ViewRevOkTest") != -1);
            assertTrue("Could not find raw hr in content: " + result, result.indexOf("<hr") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewRevOk() throws Throwable {
        launchTest();
    }


    public void beginViewRevWithObjOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.ViewRevWithObjOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        String content = Utils.content1;
        Utils.content1 = "Hello First name is $doc.first_name";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewRevWithObjOkTest", bobject, bclass, context);
        Utils.content1 = content;

        XWikiDocument doc2 = xwiki.getDocument("Main.ViewRevWithObjOkTest", context);
        xwiki.saveDocument(doc2, context);
        doc2.setStringValue("Main.ViewRevWithObjOkTest", "first_name", "John");
        doc2.setContent("Hello First name now is $doc.last_name");
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "viewrev", "ViewRevWithObjOkTest", "");
        webRequest.addParameter("rev", "1.2");
    }


    public void endViewRevWithObjOk(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find raw page name in content: " + result, result.indexOf("First name is Ludovic") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testRollbackRevWithObjOk() throws Throwable {
        launchTest();
    }

    public void beginRollbackRevWithObjOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.RollbackRevWithObjOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(xwiki.getStore(), "Main", "RollbackRevWithObjOkTest", bobject, bclass, context);

        XWikiDocument doc2 = xwiki.getDocument("Main.RollbackRevWithObjOkTest", context);
        doc2.setContent("Hello First name is $doc.first_name");
        xwiki.saveDocument(doc2, context);
        doc2.setStringValue("Main.RollbackRevWithObjOkTest", "first_name", "John");
        doc2.setContent("Hello First name now is $doc.last_name");
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "rollback", "RollbackRevWithObjOkTest", "");
        webRequest.addParameter("rev", "1.2");
    }

    public void endRollbackRevWithObjOk(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.RollbackRevWithObjOkTest", context);
            String content2 = doc2.getContent();
            String name = doc2.getStringValue("Main.RollbackRevWithObjOkTest", "first_name");

            assertEquals("Version is incorrect", "1.4", doc2.getVersion());
            assertEquals("Content has not been rolled back", "Hello First name is $doc.first_name", content2);
            assertEquals("First name had not been rolledback", "Ludovic", name);
        } finally {
            clientTearDown();
        }
    }

    public void testViewRevWithObjOk() throws Throwable {
        launchTest();
    }

    public void beginViewRawRevOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewRawRevOkTest", context);
        Utils.content1 = content;
        XWikiDocument doc2 = xwiki.getDocument("Main.ViewRawRevOkTest", context);
        xwiki.saveDocument(doc2, context);
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "viewrev", "ViewRawRevOkTest", "");
        webRequest.addParameter("rev", "1.2");
        webRequest.addParameter("raw", "1");
    }


    public void endViewRawRevOk(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find raw page name in content: " + result, result.indexOf("$doc.name") != -1);
            assertTrue("Could not find raw hr in content: " + result, result.indexOf("----") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewRawRevOk() throws Throwable {
        launchTest();
    }

    public void beginViewCodeRevOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewRawRevOkTest", context);
        Utils.content1 = content;
        XWikiDocument doc2 = xwiki.getDocument("Main.ViewRawRevOkTest", context);
        xwiki.saveDocument(doc2, context);
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "viewrev", "ViewRawRevOkTest", "");
        webRequest.addParameter("rev", "1.2");
        webRequest.addParameter("xpage", "code");
    }


    public void endViewCodeRevOk(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find raw page name in content: " + result, result.indexOf("$doc.name") != -1);
            assertTrue("Could not find raw hr in content: " + result, result.indexOf("----") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewCodeRevOk() throws Throwable {
        launchTest();
    }

    public void testSave() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }

    public void beginSave(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        setUrl(webRequest, "save", "SaveTest");
        webRequest.addParameter("content", "Hello1Hello2Hello3");
        webRequest.addParameter("parent", "Main.WebHome");
    }

    public void endSave(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.SaveTest", context);
            String content2 = doc2.getContent();
            assertEquals("Content is not indentical", "Hello1Hello2Hello3", content2);
            assertEquals("Parent is not identical", "Main.WebHome", doc2.getParent());
        } finally {
            clientTearDown();
        }
    }


    public void testDelete() throws Throwable {
        launchTest();
    }

    public void beginDelete(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(xwiki.getStore(), "Main", "DeleteTest", context);
        setUrl(webRequest, "delete", "DeleteTest");
        webRequest.addParameter("confirm", "1");
    }

    public void endDelete(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Delete returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.DeleteTest", context);
            assertTrue("Document should not exist", doc2.isNew());
        } finally {
            clientTearDown();
        }
    }


    public void testDeleteWithoutConfirm() throws Throwable {
        launchTest();
    }

    public void beginDeleteWithoutConfirm(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(xwiki.getStore(), "Main", "DeleteTest2", context);
        setUrl(webRequest, "delete", "DeleteTest2");
    }

    public void endDeleteWithoutConfirm(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("DeleteWithoutConfirm returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.DeleteTest2", context);
            assertFalse("Document should exist", doc2.isNew());
        } finally {
            clientTearDown();
        }
    }


    public void testAddProp(Class cclass) throws Throwable {
        launchTest();
    }

    public void beginAddProp(WebRequest webRequest, String name, Class cclass) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(xwiki.getStore(), "Main", "PropAddTest", context);
        setUrl(webRequest, "propadd", "PropAddTest");
        webRequest.addParameter("propname", name);
        webRequest.addParameter("proptype", cclass.getName());
    }

    public void endAddProp(WebResponse webResponse, String name, Class cclass) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Adding Property " + cclass.getName() + " returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.PropAddTest", context);
            BaseClass bclass = doc2.getxWikiClass();
            assertNotNull("Class does not exist for " + cclass.getName(), bclass);
            assertNotNull("Property of type " + cclass.getName() + " has not been added", bclass.safeget(name));
            assertEquals("Property type is not correct for " + cclass.getName(), cclass, bclass.safeget(name).getClass());
        } finally {
            clientTearDown();
        }
    }

    public void testAddNumberProp() throws IOException, Throwable {
        testAddProp(NumberClass.class);
    }

    public void beginAddNumberProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, "score", NumberClass.class);
    }

    public void endAddNumberProp(WebResponse response) throws XWikiException, HibernateException {
        endAddProp(response, "score", NumberClass.class);
    }

    public void testAddDateProp() throws IOException, Throwable {
        testAddProp(DateClass.class);
    }

    public void beginAddDateProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, "birthday", DateClass.class);
    }

    public void endAddDateProp(WebResponse response) throws XWikiException, HibernateException {
        endAddProp(response, "birthday", DateClass.class);
    }

    public void testAddStringProp() throws IOException, Throwable {
        testAddProp(StringClass.class);
    }

    public void beginAddStringProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, "category", StringClass.class);
    }

    public void endAddStringProp(WebResponse response) throws XWikiException, HibernateException {
        endAddProp(response, "category", StringClass.class);
    }

    public void testAddObject() throws Throwable {
        launchTest();
    }

    public void beginAddObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PropAddObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(xwiki.getStore(), "Main", "PropAddObjectClass", bobject, bclass, context);
        Utils.createDoc(xwiki.getStore(), "Main", "PropAddObject", context);
        setUrl(webRequest, "objectadd", "PropAddObject");
        webRequest.addParameter("classname", "Main.PropAddObjectClass");
    }

    public void endAddObject(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Adding Class returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.PropAddObject", context);
            BaseObject bobject = null;
            try {
                bobject = (BaseObject) doc2.getObject("Main.PropAddObjectClass", 0);
            }
            catch (Exception e) {
            }
            assertNotNull("Added Object does not exist", bobject);

            BaseClass bclass = bobject.getxWikiClass(context);
            assertNotNull("Added Object does not have a wikiClass", bclass);

            assertNotNull("Added Object wikiClass should have ageclass property", bclass.safeget("age"));
            assertNotNull("Added Object wikiClass should have nameclass property", bclass.safeget("first_name"));
        } finally {
            clientTearDown();
        }
    }

    public void testAddSecondObject() throws Throwable {
        launchTest();
    }


    public void beginAddSecondObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PropAddSecondObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("PropAddSecondObjectClass");
        Utils.createDoc(xwiki.getStore(), "Main", "PropAddSecondObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropAddSecondObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropAddSecondObjectClass", bobjlist);
        Utils.createDoc(xwiki.getStore(), "Main", "PropAddSecondObject", null, null, bobjects, context);
        setUrl(webRequest, "objectadd", "PropAddSecondObject");
        webRequest.addParameter("classname", "Main.PropAddSecondObjectClass");
    }

    public void endAddSecondObject(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Adding Class returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.PropAddSecondObject", context);
            BaseObject bobject = null;
            try {
                bobject = (BaseObject) doc2.getObject("Main.PropAddSecondObjectClass", 0);
            }
            catch (Exception e) {
            }
            assertNotNull("First Object does not exist", bobject);
            bobject = null;
            try {
                bobject = (BaseObject) doc2.getObject("Main.PropAddSecondObjectClass", 1);
            }
            catch (Exception e) {
            }
            assertNotNull("Second Object does not exist", bobject);

            BaseClass bclass = bobject.getxWikiClass(context);
            assertNotNull("Added Object does not have a wikiClass", bclass);

            assertNotNull("Added Object wikiClass should have ageclass property", bclass.safeget("age"));
            assertNotNull("Added Object wikiClass should have nameclass property", bclass.safeget("first_name"));
        } finally {
            clientTearDown();
        }
    }

    public void testRemoveObject() throws Throwable {
        launchTest();
    }

    public void beginRemoveObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PropRemoveObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("PropRemoveObjectClass");
        Utils.createDoc(xwiki.getStore(), "Main", "PropRemoveObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropRemoveObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropRemoveObjectClass", bobjlist);
        Utils.createDoc(xwiki.getStore(), "Main", "PropRemoveObject", null, null, bobjects, context);
        if (hibstore instanceof XWikiCacheStoreInterface)
            ((XWikiCacheStoreInterface) hibstore).flushCache();
        XWikiDocument doc2 = xwiki.getDocument("Main.PropRemoveObject", context);
        BaseObject bobject2 = null;
        try {
            bobject2 = (BaseObject) doc2.getObject("Main.PropRemoveObjectClass", 0);
        }
        catch (Exception e) {
        }
        assertNotNull("Object does not exists", bobject2);

        setUrl(webRequest, "objectremove", "PropRemoveObject");
        webRequest.addParameter("classname", "Main.PropRemoveObjectClass");
        webRequest.addParameter("classid", "0");
    }

    public void endRemoveObject(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Remove Object returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.PropRemoveObject", context);
            BaseObject bobject = null;
            try {
                bobject = (BaseObject) doc2.getObject("Main.PropRemoveObjectClass", 0);
            }
            catch (Exception e) {
            }
            assertNull("Object still exists", bobject);
        } finally {
            clientTearDown();
        }
    }


    public void testUpdateObjectProp() throws Throwable {
        launchTest();
    }

    public void beginUpdateObjectProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PropUpdateObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("Main.PropUpdateObjectClass");
        Utils.createDoc(xwiki.getStore(), "Main", "PropUpdateObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateObjectClass", bobjlist);
        Utils.createDoc(xwiki.getStore(), "Main", "PropUpdateObject", null, null, bobjects, context);

        setUrl(webRequest, "save", "PropUpdateObject");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("parent", "");
        webRequest.addParameter("Main.PropUpdateObjectClass_nb", "1");
        webRequest.addParameter("Main.PropUpdateObjectClass_0_age", "12");
        webRequest.addParameter("Main.PropUpdateObjectClass_0_first_name", "john");
    }

    public void endUpdateObjectProp(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Updated Object returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.PropUpdateObject", context);
            BaseObject bobject = null;
            try {
                bobject = (BaseObject) doc2.getObject("Main.PropUpdateObjectClass", 0);
            }
            catch (Exception e) {
            }
            assertNotNull("Updated Object does not exist", bobject);

            BaseClass bclass = bobject.getxWikiClass(context);
            assertNotNull("Updated Object does not have a wikiClass", bclass);

            assertNotNull("Updated Object wikiClass should have age property", bclass.safeget("age"));
            assertNotNull("Updated Object wikiClass should have name property", bclass.safeget("first_name"));

            assertNotNull("Updated Object should have age property", bobject.safeget("age"));
            assertNotNull("Updated Object should have name property", bobject.safeget("first_name"));

            Number age = (Number) ((NumberProperty) bobject.safeget("age")).getValue();
            assertEquals("Updated Object age property value is incorrect", new Integer(12), age);
            String name = (String) ((StringProperty) bobject.safeget("first_name")).getValue();
            assertEquals("Updated Object name property value is incorrect", "john", name);
        } finally {
            clientTearDown();
        }
    }


    public void testUpdateClassProp() throws Throwable {
        launchTest();
    }

    public void beginUpdateClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PropUpdateClassPropClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("Main.PropUpdateClassPropClass");
        Utils.createDoc(xwiki.getStore(), "Main", "PropUpdateClassPropClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateClassProp");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateClassPropClass", bobjlist);
        Utils.createDoc(xwiki.getStore(), "Main", "PropUpdateClassProp", null, null, bobjects, context);
        setUrl(webRequest, "propupdate", "PropUpdateClassPropClass");
        webRequest.addParameter("age_name", "age");
        webRequest.addParameter("age_size", "20");
        webRequest.addParameter("age_prettyName", "Age of person");
    }

    public void endUpdateClassProp(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Updated Class returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc = xwiki.getDocument("Main.PropUpdateClassPropClass", context);
            BaseClass bclass = doc.getxWikiClass();
            NumberClass ageclass = (NumberClass) bclass.safeget("age");
            assertNotNull("Updated Class wikiClass should have age property", ageclass);
            assertEquals("Updated Class age numberclass size is incorrect", 20, ageclass.getSize());
            assertEquals("Updated Class age numberclass pretty name is incorrect", "Age of person", ageclass.getPrettyName());
            assertNotNull("Updated Class wikiClass should have name property", bclass.safeget("first_name"));


            XWikiDocument doc2 = xwiki.getDocument("Main.PropUpdateClassProp", context);
            BaseObject bobject = null;
            try {
                bobject = (BaseObject) doc2.getObject("Main.PropUpdateClassPropClass", 0);
            }
            catch (Exception e) {
            }
            assertNotNull("Updated Class does not exist", bobject);

            BaseClass bclass2 = bclass;
            assertNotNull("Updated Class object does not have a wikiClass", bclass2);

            NumberClass ageclass2 = (NumberClass) bclass2.safeget("age");
            assertNotNull("Updated Class wikiClass from object should have age property", ageclass2);
            assertEquals("Updated Class age numberclass from object size is incorrect", 20, ageclass2.getSize());
            assertEquals("Updated Class age numberclass from object pretty name is incorrect", "Age of person", ageclass2.getPrettyName());
            assertNotNull("Updated Class wikiClass from object should have name property", bclass2.safeget("first_name"));

            assertNotNull("Updated Class should have age property", bobject.safeget("age"));
            assertNotNull("Updated Class should have name property", bobject.safeget("first_name"));

            Number age = (Number) ((NumberProperty) bobject.safeget("age")).getValue();
            assertEquals("Updated Class age property value is incorrect", new Integer(33), age);
            String name = (String) ((StringProperty) bobject.safeget("first_name")).getValue();
            assertEquals("Updated Class name property value is incorrect", "Ludovic", name);
        } finally {
            clientTearDown();
        }
    }


    public void testRenameClassProp() throws Throwable {
        launchTest();
    }

    public void beginRenameClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PropRenameClassPropClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("Main.PropRenameClassPropClass");
        Utils.createDoc(xwiki.getStore(), "Main", "PropRenameClassPropClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropRenameClassProp");
        bobjlist.add(bobject);
        bobjects.put("Main.PropRenameClassPropClass", bobjlist);
        Utils.createDoc(xwiki.getStore(), "Main", "PropRenameClassProp", null, null, bobjects, context);
        setUrl(webRequest, "propupdate", "PropRenameClassPropClass");
        webRequest.addParameter("age_name", "age2");
        webRequest.addParameter("age_size", "40");
        webRequest.addParameter("age_prettyName", "Age of person");
    }

    public void endRenameClassProp(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Rename Class returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc = xwiki.getDocument("Main.PropRenameClassPropClass", context);
            BaseClass bclass = doc.getxWikiClass();
            NumberClass ageclass2 = (NumberClass) bclass.safeget("age2");
            assertNotNull("Rename Class wikiClass should have age2 property", ageclass2);
            assertEquals("Rename Class age2 numberclass size is incorrect", 40, ageclass2.getSize());

            NumberClass ageclass = (NumberClass) bclass.safeget("age");
            assertNull("Rename Class wikiClass should not have age property", ageclass);

            assertEquals("Rename Class age2 numberclass pretty name is incorrect", "Age of person", ageclass2.getPrettyName());
            assertNotNull("Updated Class wikiClass should have name property", bclass.safeget("first_name"));

            // Check object in the Class
            BaseObject bobject = null;
            try {
                bobject = (BaseObject) doc.getObject("Main.PropRenameClassPropClass", 0);
            }
            catch (Exception e) {
            }
            assertNotNull("Rename Class object does not exist", bobject);

            BaseClass bclass2 = bclass;
            assertNotNull("Rename Class does not have a wikiClass", bclass2);

            ageclass2 = (NumberClass) bclass.safeget("age2");
            assertNotNull("Rename Class wikiClass from object should have age2 property", ageclass2);

            ageclass = (NumberClass) bclass.safeget("age");
            assertNull("Rename Class wikiClass from object should not have age property", ageclass);

            assertEquals("Rename Class age numberclass from object size is incorrect", 40, ageclass2.getSize());
            assertEquals("Rename Class age numberclass from object pretty name is incorrect", "Age of person", ageclass2.getPrettyName());


            assertNotNull("Rename Class wikiClass should have name property", bclass2.safeget("first_name"));

            assertNull("Rename Class object should not have age property", bobject.safeget("age"));
            assertNotNull("Rename Class object should have age property", bobject.safeget("age2"));
            assertNotNull("Rename Class object should have name property", bobject.safeget("first_name"));

            Number age = (Number) ((NumberProperty) bobject.safeget("age2")).getValue();
            assertEquals("Rename Class age property value is incorrect", new Integer(33), age);
            String name = (String) ((StringProperty) bobject.safeget("first_name")).getValue();
            assertEquals("Rename Class name property value is incorrect", "Ludovic", name);

            XWikiDocument doc2 = xwiki.getDocument("Main.PropRenameClassProp", context);
            bobject = null;
            try {
                bobject = (BaseObject) doc2.getObject("Main.PropRenameClassPropClass", 0);
            }
            catch (Exception e) {
            }
            assertNotNull("Rename Class object does not exist", bobject);

            bclass2 = bclass;
            assertNotNull("Rename Class does not have a wikiClass", bclass2);

            ageclass2 = (NumberClass) bclass.safeget("age2");
            assertNotNull("Rename Class wikiClass from object should have age2 property", ageclass2);

            ageclass = (NumberClass) bclass.safeget("age");
            assertNull("Rename Class wikiClass from object should not have age property", ageclass);

            assertEquals("Rename Class age numberclass from object size is incorrect", 40, ageclass2.getSize());
            assertEquals("Rename Class age numberclass from object pretty name is incorrect", "Age of person", ageclass2.getPrettyName());


            assertNotNull("Rename Class wikiClass should have name property", bclass2.safeget("first_name"));

            assertNull("Rename Class object should not have age property", bobject.safeget("age"));
            assertNotNull("Rename Class object should have age property", bobject.safeget("age2"));
            assertNotNull("Rename Class object should have name property", bobject.safeget("first_name"));

            age = (Number) ((NumberProperty) bobject.safeget("age2")).getValue();
            assertEquals("Rename Class age property value is incorrect", new Integer(33), age);
            name = (String) ((StringProperty) bobject.safeget("first_name")).getValue();
            assertEquals("Rename Class name property value is incorrect", "Ludovic", name);
        } finally {
            clientTearDown();
        }
    }


    public void testUpdateAdvancedObjectProp() throws Throwable {
        launchTest();
    }

    public void beginUpdateAdvancedObjectProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareAdvancedObject(doc, "Main.PropUpdateAdvObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("PropUpdateAdvObjectClass");
        Utils.createDoc(xwiki.getStore(), "Main", "PropUpdateAdvObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateAdvObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateAdvObjectClass", bobjlist);
        Utils.createDoc(xwiki.getStore(), "Main", "PropUpdateAdvObject", null, null, bobjects, context);
        setUrl(webRequest, "save", "PropUpdateAdvObject");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("parent", "");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_nb", "1");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_age", "12");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_first_name", "john");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category", "2");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category2", "2");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category2", "3");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category3", "2");
        webRequest.addParameter("Main.PropUpdateAdvObjectClass_0_category3", "3");
    }

    public void endUpdateAdvancedObjectProp(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Updated Object returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.PropUpdateAdvObject", context);
            BaseObject bobject = null;
            try {
                bobject = (BaseObject) doc2.getObject("Main.PropUpdateAdvObjectClass", 0);
            }
            catch (Exception e) {
            }
            assertNotNull("Updated Object does not exist", bobject);

            BaseClass bclass = bobject.getxWikiClass(context);
            assertNotNull("Updated Object does not have a wikiClass", bclass);

            assertNotNull("Updated Object wikiClass should have age property", bclass.safeget("age"));
            assertNotNull("Updated Object wikiClass should have name property", bclass.safeget("first_name"));

            assertNotNull("Updated Object should have age property", bobject.safeget("age"));
            assertNotNull("Updated Object should have name property", bobject.safeget("first_name"));

            Number age = (Number) ((NumberProperty) bobject.safeget("age")).getValue();
            assertEquals("Updated Object age property value is incorrect", new Integer(12), age);
            String name = (String) ((StringProperty) bobject.safeget("first_name")).getValue();
            assertEquals("Updated Object name property value is incorrect", "john", name);

            String category = (String) ((StringProperty) bobject.safeget("category")).getValue();
            assertEquals("Updated Object category property value is incorrect", "2", category);

            List category2 = (List) ((ListProperty) bobject.safeget("category2")).getValue();
            assertEquals("Updated Object category2 property size is incorrect", 2, category2.size());
            assertEquals("Updated Object category2 property item 1 is incorrect", "2", category2.get(0));
            assertEquals("Updated Object category2 property item 2 is incorrect", "3", category2.get(1));

            List category3 = (List) ((ListProperty) bobject.safeget("category3")).getValue();
            assertEquals("Updated Object category3 property size is incorrect", 2, category3.size());
            assertEquals("Updated Object category3 property item 1 is incorrect", "2", category3.get(0));
            assertEquals("Updated Object category3 property item 2 is incorrect", "3", category3.get(1));
        } finally {
            clientTearDown();
        }
    }


    public void sendMultipart(WebRequest webRequest, File file) throws IOException {
        Part part = new FilePart("filepath", file);
        Part[] parts = new Part[1];
        parts[0] = part;

        if (Part.getBoundary() != null) {
            webRequest.setContentType(
                    "multipart/form-data" + "; boundary=" + Part.getBoundary());
        }

        PipedInputStream pipedin = new PipedInputStream();
        PipedOutputStream pipedout = new PipedOutputStream(pipedin);
        MultipartSenderThread sender = new MultipartSenderThread(pipedout, parts);
        sender.start();
        webRequest.setUserData(pipedin);
    }


    public void testAttach() throws Throwable {
        launchTest();
    }

    public void beginAttach(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        setUrl(webRequest, "upload", "AttachTest");
        webRequest.setContentType("multipart/form-data");
        File file = new File(Utils.filename);
        sendMultipart(webRequest, file);
    }

    public void endAttach(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            File fattach = new File(Utils.filename);
            XWikiDocument doc2 = xwiki.getDocument("Main.AttachTest", context);
            List list = doc2.getAttachmentList();
            assertEquals("Document has no attachement", 1, list.size());
            XWikiAttachment attachment = (XWikiAttachment) list.get(0);
            assertEquals("Attachment size is not correct", fattach.length(), attachment.getFilesize());
        } finally {
            clientTearDown();
        }
    }


    public void testAttachUpdate() throws Throwable {
        launchTest();
    }


    public void beginAttachUpdate(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiDocument doc1 = new XWikiDocument("Main", "AttachTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        xwiki.saveDocument(doc1, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        xwiki.saveDocument(doc1, context);

        setUrl(webRequest, "upload", "AttachTest");
        webRequest.setContentType("multipart/form-data");
        File file = new File(Utils.filename);
        sendMultipart(webRequest, file);
    }

    public void endAttachUpdate(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            File fattach = new File(Utils.filename);
            XWikiDocument doc2 = xwiki.getDocument("Main.AttachTest", context);
            List list = doc2.getAttachmentList();
            assertEquals("Document has no attachement", 1, list.size());
            XWikiAttachment attachment = (XWikiAttachment) list.get(0);
            assertEquals("Attachment version is not correct", attachment.getVersion(), "1.2");
            assertEquals("Attachment size is not correct", fattach.length(), attachment.getFilesize());
        } finally {
            clientTearDown();
        }
    }

    public void testAttachDownload() throws Throwable {
        launchTest();
    }


    public void beginAttachDownload(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiDocument doc1 = new XWikiDocument("Main", "AttachDownloadTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        xwiki.saveDocument(doc1, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        String attachcontent1 = "blablabla";
        attachment1.setContent(attachcontent1.getBytes());
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        xwiki.saveDocument(doc1, context);
        setUrl(webRequest, "download", "AttachDownloadTest/" + Utils.filename);
    }

    public void endAttachDownload(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Attach Download  returned exception: " + result, result.indexOf("Exception")==-1);
            assertTrue("Attach Download should contain attachment text: " + result, result.indexOf("blablabla")!=-1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.AttachDownloadTest", context);
            List list = doc2.getAttachmentList();
            assertEquals("Document should have an attachement", 1, list.size());
        } finally {
            clientTearDown();
        }
    }


    public void testAttachDelete() throws Throwable {
        launchTest();
    }


    public void beginAttachDelete(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiDocument doc1 = new XWikiDocument("Main", "AttachDeleteTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        xwiki.saveDocument(doc1, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        xwiki.saveDocument(doc1, context);
        setUrl(webRequest, "delattachment", "AttachDeleteTest/" + Utils.filename);
    }

    public void endAttachDelete(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Attach Delete returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.AttachDeleteTest", context);
            List list = doc2.getAttachmentList();
            assertEquals("Document should have no attachement", 0, list.size());
        } finally {
            clientTearDown();
        }
    }


    public static class MultipartSenderThread extends Thread {

        private PipedOutputStream out;
        private Part[] parts;

        protected MultipartSenderThread(PipedOutputStream outs, Part[] source) {
            out = outs;
            parts = source;
        }

        public void run() {
            try {
                Part.sendParts(out, parts);
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void beginInclude(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Included Page";
        Utils.createDoc(xwiki.getStore(), "Main", "IncludeTest2", context);
        Utils.content1 = "#includeTopic(\"Main.IncludeTest2\")\n";
        Utils.createDoc(xwiki.getStore(), "Main", "IncludeTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "IncludeTest");
    }

    public void endInclude(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Included Page") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testInclude() throws IOException, Throwable {
        launchTest();
    }

    public void beginIncludeEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Included Page";
        Utils.createDoc(xwiki.getStore(), "Main", "IncludeTest2", context);
        Utils.content1 = "#includeTopic(\"IncludeTest2\")\n#includeForm(\"Main.IncludeTest3\")";
        Utils.createDoc(xwiki.getStore(), "Main", "IncludeTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "edit", "IncludeTest");
    }

    public void endIncludeEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("includeTopic") != -1);
            assertTrue("Could not find Include page messages: " + result, result.indexOf("edit/Main/IncludeTest2") != -1);
            assertTrue("Could not find Include page messages: " + result, result.indexOf("edit/Main/IncludeTest3") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void beginDefaultSkin(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(xwiki.getStore(), "Main", "DefaultSkinTest", context);
        setUrl(webRequest, "view", "DefaultSkinTest");
    }

    public void testIncludeEdit() throws IOException, Throwable {
        launchTest();
    }

    public void endDefaultSkin(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find style in header: " + result, result.indexOf("<link href=\"/xwiki/skins/default/style.css\"") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testDefaultSkin() throws IOException, Throwable {
        launchTest();
    }


    public void beginAlternSkin(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.setStringValue("XWiki.XWikiPreferences", "skin", "altern", context);
        Utils.createDoc(xwiki.getStore(), "Main", "AlternSkinTest", context);
        setUrl(webRequest, "view", "AlternSkinTest");
    }

    public void endAlternSkin(WebResponse webResponse) throws HibernateException {
        try {
            // TODO: add and altern skin to reactivate this test
            //assertTrue("Could not find style in header: " + result, result.indexOf("<link href=\"/xwiki/skins/altern/style.css\"")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testAlternSkin() throws IOException, Throwable {
        launchTest();
    }

    public void beginWikiPageSkin(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.setStringValue("XWiki.XWikiPreferences", "skin", "Main.TestSkin", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WikiPageSkinTest", context);
        setUrl(webRequest, "view", "WikiPageSkinTest");
    }

    public void endWikiPageSkin(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find style in header: " + result, result.indexOf("<link href=\"/xwiki/skins/default/style.css\"") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testWikiPageSkin() throws IOException, Throwable {
        launchTest();
    }

    public void beginWikiPageSkin2(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.setStringValue("XWiki.XWikiPreferences", "skin", "Main.TestSkin", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WikiPageSkinTest", context);
        Utils.createDoc(xwiki.getStore(), "Main", "TestSkin", context);
        Utils.setStringValue("Main.TestSkin", "XWiki.XWikiSkins", "style.css", "// no style", context);
        setUrl(webRequest, "view", "WikiPageSkinTest");
    }

    public void endWikiPageSkin2(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find style in header: " + result, result.indexOf("<link href=\"/xwiki/testbin/skin/Main/TestSkin/style.css\"") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testWikiPageSkin2() throws IOException, Throwable {
        launchTest();
    }

    public void beginIncludeMacros(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "#macro(hello)\ncoucou#end";
        Utils.createDoc(xwiki.getStore(), "Main", "IncludeMacros2", context);
        Utils.content1 = "#includeMacros(\"Main.IncludeMacros2\")\n#hello()";
        Utils.createDoc(xwiki.getStore(), "Main", "IncludeMacros", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "IncludeMacros");
    }

    public void endIncludeMacros(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("coucou") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testIncludeMacros() throws IOException, Throwable {
        launchTest();
    }

    public void beginPutLockOnEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.EditLock");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(xwiki.getStore(), "Main", "EditLock", bobject, bclass, context);
        setUrl(webRequest, "edit", "EditLock");
        webRequest.addParameter("xpage", "editclass");
    }

    public void endPutLockOnEdit(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.NumberClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.StringClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.TextAreaClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.PasswordClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.BooleanClass") != -1);
            assertTrue("Could not find Add Class: " + result, result.indexOf("com.xpn.xwiki.objects.classes.DBListClass") != -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc = xwiki.getDocument("Main.EditLock", context);
            XWikiLock thefirstlock = doc.getLock(context);
            assertNotNull("Lock exist", thefirstlock);
            assertEquals("Lock user", "XWiki.XWikiGuest", thefirstlock.getUserName());
            doc.removeLock(context);
        } finally {
            clientTearDown();
        }
    }

    public void testPutLockOnEdit() throws Throwable {
        launchTest();
    }

    public void testUnlockOnSave() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }

    public void beginUnlockOnSave(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = xwiki.getDocument("Main.SaveTest", context);
        doc.setLock("XWiki.XWikiGuest", context);
        XWikiLock thefirstlock = doc.getLock(context);
        assertNotNull("Lock exist", thefirstlock);
        setUrl(webRequest, "save", "SaveTest");
        webRequest.addParameter("content", "Hello1Hello2Hello3");
        webRequest.addParameter("parent", "Main.WebHome");
    }

    public void endUnlockOnSave(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.SaveTest", context);
            String content2 = doc2.getContent();
            assertEquals("Content is not indentical", "Hello1Hello2Hello3", content2);
            assertEquals("Parent is not identical", "Main.WebHome", doc2.getParent());
            XWikiLock thefirstlock = doc2.getLock(context);
            assertNull("Lock removed", thefirstlock);
        } finally {
            clientTearDown();
        }
    }

    public void testUnlockOnCancel() throws IOException, Throwable {
        try {
            ActionServlet servlet = new ActionServlet();
            servlet.init(config);
            servlet.service(request, response);
            cleanSession(session);
        } catch (ServletException e) {
            e.getRootCause().printStackTrace();
            throw e.getRootCause();
        }

    }

    public void beginUnlockOnCancel(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = xwiki.getDocument("Main.SaveTest", context);
        doc.setLock("XWiki.XWikiGuest", context);
        XWikiLock thefirstlock = doc.getLock(context);
        assertNotNull("Lock exist", thefirstlock);
        setUrl(webRequest, "cancel", "SaveTest");
//        webRequest.addParameter("content","Hello1Hello2Hello3");
//        webRequest.addParameter("parent","Main.WebHome");
    }

    public void endUnlockOnCancel(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.SaveTest", context);
            String content2 = doc2.getContent();
            assertEquals("Content is not indentical", "\n", content2);
            assertEquals("Parent is not identical", "", doc2.getParent());
            XWikiLock thefirstlock = doc2.getLock(context);
            assertNull("Lock removed", thefirstlock);
        } finally {
            clientTearDown();
        }
    }

    public void beginViewSimpleURL(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewOkTest", context);
        Utils.content1 = content;
        setSimpleUrl(webRequest, "Main", "ViewOkTest", "");
    }

    public void endViewSimpleURL(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find page name in content: " + result, result.indexOf("ViewOkTest") != -1);
            assertTrue("Could not find hr in content: " + result, result.indexOf("<hr") != -1);
        } finally {
            clientTearDown();
        }
    }

    /*
     TODO: We need to setup the tomcat server to handle simple URL to be able to run this test
    public void testViewSimpleURL() throws IOException, Throwable {
        launchTest();
    }
    */

    public void beginViewVerySimpleURL(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewOkTest", context);
        Utils.content1 = content;
        setVerySimpleUrl(webRequest, "Main", "ViewOkTest", "");
    }

    public void endViewVerySimpleURL(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find page name in content: " + result, result.indexOf("ViewOkTest") != -1);
            assertTrue("Could not find hr in content: " + result, result.indexOf("<hr") != -1);
        } finally {
            clientTearDown();
        }
    }

    /*
     TODO: We need to setup the tomcat server to handle simple URL to be able to run this test
    public void testViewVerySimpleURL() throws IOException, Throwable {
        launchTest();
    }
    */

    public void beginViewUltraSimpleURL(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewOkTest", context);
        Utils.content1 = content;
        setVerySimpleUrl(webRequest, "", "ViewOkTest", "");
    }

    public void endViewUltraSimpleURL(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find page name in content: " + result, result.indexOf("ViewOkTest") != -1);
            assertTrue("Could not find hr in content: " + result, result.indexOf("<hr") != -1);
        } finally {
            clientTearDown();
        }
    }

/*
     TODO: We need to setup the tomcat server to handle simple URL to be able to run this test
    public void testViewUltraSimpleURL() throws IOException, Throwable {
        launchTest();
    }
*/


    public void beginViewCountQueries(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewOkTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewOkTest");
    }

    public void endViewCountQueries(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello") != -1);
            assertTrue("Could not find page name in content: " + result, result.indexOf("ViewOkTest") != -1);
            assertTrue("Could not find hr in content: " + result, result.indexOf("<hr") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testViewCountQueries() throws IOException, Throwable {
        XWikiBatcher.getSQLStats().resetStats();
        launchTest();
        XWikiBatcher.getSQLStats().printSQLList(System.out);
        XWikiBatcher.getSQLStats().resetStats();
        launchTest();
        XWikiBatcher.getSQLStats().printSQLList(System.out);
    }


    public void beginViewRevNotOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "ViewRevNotOkTest", context);
        Utils.content1 = content;
        XWikiDocument doc2 = xwiki.getDocument("Main.ViewRevNotOkTest", context);
        xwiki.saveDocument(doc2, context);
        xwiki.flushCache();
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "viewrev", "ViewRevNotOkTest", "");
        webRequest.addParameter("rev", "1.4");
    }


    public void endViewRevNotOk(WebResponse webResponse) throws XWikiException, HibernateException {

        try {
            String result = webResponse.getText();

            assertTrue("Could not find This document does not exist in Content: " + result, result.indexOf("This document does not exist") != -1);
            // verify
            //  WebRequest request =  webResponse.getWebRequest();
            //setUrl(request,"view","ViewRevOKTest");
            // request.addParameter("rev","1.423");
        } catch (Exception e) {
            System.out.println("This document ");
        }
        finally {
            clientTearDown();
        }
    }

    public void testViewRevNotOk() throws Throwable {
        launchTest();

    }


    public void beginNewViewRevOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(xwiki.getStore(), "Main", "NewViewRevOkTest", context);
        Utils.content1 = content;
        XWikiDocument doc2 = xwiki.getDocument("Main.NewViewRevOkTest", context);
        xwiki.saveDocument(doc2, context);
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "viewrev", "NewViewRevOkTest", "");
        webRequest.addParameter("rev", "1.4");
    }


    public void endNewViewRevOk(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find This document does not exist in Content: " + result, result.indexOf("This document does not exist in this version") != -1);
        } finally {
            // clientTearDown();
        }
    }

    public void testNewViewRevOk() throws Throwable {
        launchTest();
    }

    public void testSaveWithTemplateNotOk() throws Throwable {
      launchTest();
    }

    public void beginSaveWithTemplateNotOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Test.MyTestTemplate");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 1);
        Utils.createDoc(xwiki.getStore(), "Test", "MyTestTemplate", bobject, bclass, context);
        setUrl(webRequest, "save", "Test", "MyTestPage", "");
        webRequest.addParameter("template", "Test.MyTestTemplate");
        webRequest.addParameter("parent", "XWiki.TestParentComesFromTemplate");
        webRequest.addParameter("content", "XYZ");
    }

    public void endSaveWithTemplateNotOk(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Test.MyTestPage", context);
            String content2 = doc2.getContent();
            assertEquals("Content is not identical", "XYZ",content2);

        } finally {
            clientTearDown();
        }
    }

    public void testSaveWithTemplateToDocExistNotOk() throws Throwable {
      launchTest();
    }

    public void beginSaveWithTemplateToDocExistNotOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Test.MyTestTemplate");
        Utils.prepareObject(doc, "Test.MyTestPage");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 2);
        String content = Utils.content1;
        Utils.content1 = "XYZ";
        Utils.createDoc(xwiki.getStore(), "Test", "MyTestTemplate", bobject, bclass, context);
        Utils.content1 = content;

        Utils.createDoc(xwiki.getStore(), "Test", "MyTestPage", bobject, bclass, context);

        setUrl(webRequest, "save", "Test", "MyTestPage", "");
        webRequest.addParameter("template", "Test.MyTestTemplate");
        webRequest.addParameter("parent", "XWiki.TestParentComesFromTemplate");
        webRequest.addParameter("content", "New content");
    }

    public void endSaveWithTemplateToDocExistNotOk(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            XWikiDocument doc2 = xwiki.getDocument("Test.MyTestPage", context);
            String content2 = doc2.getContent();
            assertEquals("Content should still be XYZ", Utils.content1,content2);
            // Verify return
            assertTrue("Saving did not returned nice error message: " + result, (result.indexOf("This document already exist")!=-1));
        } finally {
            clientTearDown();
        }
    }

    public void testAttachDownloadRev() throws Throwable {
        launchTest();
    }


    public void beginAttachDownloadRev(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiDocument doc1 = new XWikiDocument("Test", "AttachDownloadRevTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        xwiki.saveDocument(doc1, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        String attachcontent1 = "blablabla";
        attachment1.setContent(attachcontent1.getBytes());

        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        xwiki.saveDocument(doc1, context);

        String attachcontent2 = "Another content";
        attachment1.setContent(attachcontent2.getBytes());

        doc1.saveAttachmentContent(attachment1, context);
        //doc1.getAttachmentList().add(attachment1);
        xwiki.saveDocument(doc1, context);
        setUrl(webRequest, "downloadrev", "Test", "AttachDownloadRevTest/" + Utils.filename, "");
        webRequest.addParameter("rev","1.1");
    }

    public void endAttachDownloadRev(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Attach Download  returned exception: " + result, result.indexOf("Exception")==-1);
            assertEquals("Attach Download should contain attachment text of revision 1.1", "blablabla", result);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Test.AttachDownloadRevTest", context);
            List list = doc2.getAttachmentList();
            assertEquals("Document should have an attachement", 1, list.size());
            XWikiAttachment attach1 = (XWikiAttachment)list.get(0);
            assertEquals("Attachment should have version 1.2","1.2",attach1.getVersion());
        } finally {
            clientTearDown();
        }
    }

    public void beginViewRevAttach(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiDocument doc = new XWikiDocument("Test", "ViewRevAttachTest");
        doc.setContent(Utils.content1);
        doc.setAuthor(Utils.author);
        doc.setParent(Utils.parent);
        xwiki.saveDocument(doc, context);

        XWikiAttachment attachment1 = new XWikiAttachment(doc, Utils.filename);
        String attachcontent1 = "rev 1";
        attachment1.setContent(attachcontent1.getBytes());
        doc.getAttachmentList().add(attachment1);
        doc.saveAttachmentContent(attachment1,context);
        // xwiki.saveDocument(doc, context);

        String attachcontent2 ="rev 2";
        attachment1.setContent(attachcontent2.getBytes());
        doc.saveAttachmentContent(attachment1,context);
//        doc.getAttachmentList().add(attachment1);
//        xwiki.saveDocument(doc,context);

        setUrl(webRequest, "viewattachrev", "Test","ViewRevAttachTest/" + Utils.filename, "");
        }

    public void endViewRevAttach(WebResponse webResponse) throws HibernateException,XWikiException {
        try {
            String result = webResponse.getText();
            assertTrue("Page does not contain 1.1", result.indexOf("1.1")!=-1);
            assertTrue("Page does not contain 1.2", result.indexOf("1.2")!=-1);

        } finally {
            clientTearDown();
        }
    }

    public void testViewRevAttach() throws Throwable {
        launchTest();
    }

     public void testSaveWithTemplateOk() throws Throwable {
        launchTest();
    }

    public void beginSaveWithTemplateOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.SaveOkTestTemplate");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        String content = Utils.content1;
       // Utils.content1 = "Template content";
        Utils.createDoc(xwiki.getStore(), "Main", "SaveOkTestTemplate", bobject, bclass, context);
        Utils.content1 = content;
        setUrl(webRequest, "save", "SaveOkWithTestTemplate");
        webRequest.addParameter("template", "Main.SaveOkTestTemplate");
        webRequest.addParameter("parent", "XWiki.TestParentComesFromTemplate");
        webRequest.addParameter("content", "Template content");
    }

    public void endSaveWithTemplateOk(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);

            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.SaveOkWithTestTemplate", context);
            String content2 = doc2.getContent();
            assertEquals("Content is not identical", "Template content", content2);
            assertEquals("Parent is not identical", "XWiki.TestParentComesFromTemplate", doc2.getParent());
        } finally {
            clientTearDown();
        }
    }

    public void testSaveWithTemplateLong() throws Throwable {
        launchTest();
    }

    public void beginSaveWithTemplateLong(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.SaveErrorLongTemplate");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 1);
        String content  =  "template content" ;
        Utils.createDoc(xwiki.getStore(),"Main","SaveErrorLongtemplate",content,bobject,bclass,context);

        setUrl(webRequest, "save", "SaveWithLongTemplate");
        webRequest.addParameter("template", "Main.SaveErrorLongTemplate");
        webRequest.addParameter("parent", "XWiki.TestParentComesFromTemplate");


    }

    public void endSaveWithTemplateLong(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertFalse("Saving not returned exception: " + result, result.indexOf("Exception") == -1);
            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc2 = xwiki.getDocument("Main.SaveWithLongTemplate",context);
            String content2 = doc2.getContent() ;
            String parent = doc2.getParent();
            assertEquals("Content is identical", false, content2.equals("template content"));
            assertEquals("Parent is  identical", false, parent.equals("XWiki.TestParentComesFromTemplate"));

        } finally {
            clientTearDown();
        }
    }

    
}
