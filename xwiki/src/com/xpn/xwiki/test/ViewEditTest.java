
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import net.sf.hibernate.HibernateException;
import org.apache.cactus.ServletTestCase;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.struts.action.ActionServlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.*;

/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 11 janv. 2004
 * Time: 12:08:01
 */

public class ViewEditTest extends ServletTestCase {

    private String hibpath = "hibernate-test.cfg.xml";

    public void setUp() {
        flushCache();
    };

    public void cleanUp() {
    };

    public void clientSetUp(XWikiStoreInterface store) throws XWikiException {
    }


    private void setUrl(WebRequest webRequest, String action, String docname) {
        setUrl(webRequest, action, docname, "");
    }

    private void setUrl(WebRequest webRequest, String action, String docname, String query) {
        webRequest.setURL("127.0.0.1:9080", "/xwiki" , "/testbin", "/" + action + "/Main/" + docname, query);
    }


    public String getHibpath() {
        // Usefull in case we need to understand where we are
        String path = (new File(".")).getAbsolutePath();
        System.out.println("Current Directory is: " + path);

        File file = new File(hibpath);
        if (file.exists())
            return hibpath;

        file = new File("WEB-INF", hibpath);
        if (file.exists())
            return "./WEB-INF/" + hibpath;

        file = new File("test", hibpath);
        if (file.exists())
            return "./test/" + hibpath;

        if (config!=null)
        {
            ServletContext context = config.getServletContext();
            if (context!=null)
                return context.getRealPath("WEB-INF/" + hibpath);
        }

        return hibpath;
    }

    public void cleanSession(HttpSession session) {
        Vector names = new Vector();
        Enumeration enum = session.getAttributeNames();
        while (enum.hasMoreElements()) {
            String name = (String) enum.nextElement();
            names.add(name);
        }

        for (int i=0;i<names.size();i++)
        {
            session.removeAttribute((String)names.get(i));
        }
    }

    public void flushCache() {
        // We need to flush the server cache before running our tests
        // because we are modifiying the database behind the scenes
        // so if we are running the tests twice we won't necessarly
        // get the same results..
        try {
            XWiki xwiki = (XWiki) config.getServletContext().getAttribute("xwikitest");
            xwiki.flushCache();
        } catch (Exception e) {
        }
    }

    public void beginViewNotOk(WebRequest webRequest) throws HibernateException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);
        setUrl(webRequest, "view", "ViewNotOkTest");
    }

    public void endViewNotOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Page should have generated an error", result.indexOf("No row")!=-1);
    }

    public void testViewNotOk() throws IOException, Throwable, HibernateException {
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

    public void beginViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);
        Utils.createDoc(hibstore, "Main", "ViewOkTest");
        setUrl(webRequest, "view", "ViewOkTest");
    }

    public void endViewOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testViewOk() throws IOException, Throwable {
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

    public void beginViewRevOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);
        Utils.createDoc(hibstore, "Main", "ViewRevOkTest");
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "ViewRevOkTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        hibstore.saveXWikiDoc(doc2);
        setUrl(webRequest, "view", "ViewRevOkTest", "rev=1.1");
    }

    public void endViewRevOk(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testViewRevOk() throws IOException, Throwable {
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
        StoreHibernateTest.cleanUp(hibstore);
        setUrl(webRequest, "save", "SaveTest");
        webRequest.addParameter("content","Hello1Hello2Hello3");
        webRequest.addParameter("parent","Main.WebHome");
    }

    public void endSave(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Saving returned exception", result.indexOf("Exception")==-1);

        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "SaveTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
        String content2 = doc2.getContent();
        assertEquals("Content is not indentical", "Hello1Hello2Hello3",content2);
        assertEquals("Parent is not identical", "Main.WebHome", doc2.getParent());
    }

    public void testAddProp(Class cclass) throws IOException, Throwable {
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

    public void beginAddProp(WebRequest webRequest, Class cclass) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);
        Utils.createDoc(hibstore, "Main", "PropAddTest");
        setUrl(webRequest, "propadd", "PropAddTest");
        webRequest.addParameter("propname", "score");
        webRequest.addParameter("proptype", cclass.getName());
    }

    public void endAddProp(WebResponse webResponse, Class cclass) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Property " + cclass.getName() + " returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
        BaseClass bclass = doc2.getxWikiClass();
        assertNotNull("Class does not exist for " + cclass.getName(), bclass);
        assertNotNull("Property of type " + cclass.getName() + " has not been added", bclass.safeget("score"));
        assertEquals("Property type is not correct for " + cclass.getName(), bclass.safeget("score").getClass(), cclass);
    }

    public void testAddNumberProp() throws IOException, Throwable {
        testAddProp(NumberClass.class);
    }

    public void beginAddNumberProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, NumberClass.class);
    }

    public void endAddNumberProp(WebResponse response)  throws XWikiException {
        endAddProp(response, NumberClass.class);
    }

    public void testAddStringProp() throws IOException, Throwable {
        testAddProp(StringClass.class);
    }

    public void beginAddStringProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, StringClass.class);
    }

    public void endAddStringProp(WebResponse response)  throws XWikiException {
        endAddProp(response, StringClass.class);
    }

    public void testAddClass() throws IOException, Throwable {
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

    public void beginAddClass(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);
        BaseObject bobject = Utils.prepareObject();
        Utils.createDoc(hibstore, "Main", "PropAddClassClass", bobject, bobject.getxWikiClass());
        Utils.createDoc(hibstore, "Main", "PropAddClass");
        setUrl(webRequest, "classadd", "PropAddClass");
        webRequest.addParameter("classname", "Main.PropAddClassClass");
    }

    public void endAddClass(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddClass");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropAddClassClass", 0); }
        catch (Exception e) {}
        assertNotNull("Added Class does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass();
        assertNotNull("Added Class does not have a wikiClass", bclass);

        assertNotNull("Added Class wikiClass should have ageclass property", bclass.safeget("age"));
        assertNotNull("Added Class wikiClass should have nameclass property", bclass.safeget("first_name"));
    }

    public void testAddSecondClass() throws IOException, Throwable {
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

    public void beginAddSecondClass(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);
        BaseObject bobject = Utils.prepareObject();
        BaseClass bclass = bobject.getxWikiClass();
        bclass.setName("PropAddSecondClassClass");
        Utils.createDoc(hibstore, "Main", "PropAddSecondClassClass", bobject, bclass);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropAddSecondClass");
        bobjlist.add(bobject);
        bobjects.put("Main.PropAddSecondClassClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropAddSecondClass", null, null, bobjects);
        setUrl(webRequest, "classadd", "PropAddSecondClass");
        webRequest.addParameter("classname", "Main.PropAddSecondClassClass");
    }

    public void endAddSecondClass(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddSecondClass");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try {
            bobject = (BaseObject) doc2.getObject("Main.PropAddSecondClassClass", 0); }
        catch (Exception e) {}
        assertNotNull("First Class does not exist", bobject);
        bobject = null;
        try {
            bobject = (BaseObject) doc2.getObject("Main.PropAddSecondClassClass", 1); }
        catch (Exception e) {}
        assertNotNull("Second Class does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass();
        assertNotNull("Added Class does not have a wikiClass", bclass);

        assertNotNull("Added Class wikiClass should have ageclass property", bclass.safeget("age"));
        assertNotNull("Added Class wikiClass should have nameclass property", bclass.safeget("first_name"));
    }


    public void testUpdateClassProp() throws IOException, Throwable {
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

    public void beginUpdateClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);
        BaseObject bobject = Utils.prepareObject();
        BaseClass bclass = bobject.getxWikiClass();
        bclass.setName("PropUpdateClassClass");
        Utils.createDoc(hibstore, "Main", "PropUpdateClassClass", bobject, bclass);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateClass");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateClassClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropUpdateClass", null, null, bobjects);
        setUrl(webRequest, "save", "PropUpdateClass");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("parent", "");
        webRequest.addParameter("Main.PropUpdateClassClass_nb", "1");
        webRequest.addParameter("Main.PropUpdateClassClass_0_age", "12");
        webRequest.addParameter("Main.PropUpdateClassClass_0_first_name", "john");
    }

    public void endUpdateClassProp(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Updated Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropUpdateClass");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropUpdateClassClass", 0); }
        catch (Exception e) {}
        assertNotNull("Updated Class does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass();
        assertNotNull("Updated Class does not have a wikiClass", bclass);

        assertNotNull("Updated Class wikiClass should have age property", bclass.safeget("age"));
        assertNotNull("Updated Class wikiClass should have name property", bclass.safeget("first_name"));

        assertNotNull("Updated Class should have age property", bobject.safeget("age"));
        assertNotNull("Updated Class should have name property", bobject.safeget("first_name"));

        Number age = (Number)((NumberProperty)bobject.safeget("age")).getValue();
        assertEquals("Updated Class age property value is incorrect", new Integer(12), age);
        String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Updated Class name property value is incorrect", "john", name);
    }


        public void testUpdateAdvancedClassProp() throws IOException, Throwable {
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

        public void beginUpdateAdvancedClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
            XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
            StoreHibernateTest.cleanUp(hibstore);
            BaseObject bobject = Utils.prepareAdvancedObject();
            BaseClass bclass = bobject.getxWikiClass();
            bclass.setName("PropUpdateAdvClassClass");
            Utils.createDoc(hibstore, "Main", "PropUpdateAdvClassClass", bobject, bclass);
            Map bobjects = new HashMap();
            Vector bobjlist = new Vector();
            bobject.setName("Main.PropUpdateAdvClass");
            bobjlist.add(bobject);
            bobjects.put("Main.PropUpdateAdvClassClass", bobjlist);
            Utils.createDoc(hibstore, "Main", "PropUpdateAdvClass", null, null, bobjects);
            setUrl(webRequest, "save", "PropUpdateAdvClass");
            webRequest.addParameter("content", "toto");
            webRequest.addParameter("parent", "");
            webRequest.addParameter("Main.PropUpdateAdvClassClass_nb", "1");
            webRequest.addParameter("Main.PropUpdateAdvClassClass_0_age", "12");
            webRequest.addParameter("Main.PropUpdateAdvClassClass_0_first_name", "john");
            webRequest.addParameter("Main.PropUpdateAdvClassClass_0_category", "2");
            webRequest.addParameter("Main.PropUpdateAdvClassClass_0_category2", "2");
            webRequest.addParameter("Main.PropUpdateAdvClassClass_0_category2", "3");
            webRequest.addParameter("Main.PropUpdateAdvClassClass_0_category3", "2");
            webRequest.addParameter("Main.PropUpdateAdvClassClass_0_category3", "3");
        }

        public void endUpdateAdvancedClassProp(WebResponse webResponse) throws XWikiException {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Updated Class returned exception", result.indexOf("Exception")==-1);
            XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
            XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropUpdateAdvClass");
            doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
            Map bobjects = doc2.getxWikiObjects();
            BaseObject bobject = null;
            try { bobject = (BaseObject) doc2.getObject("Main.PropUpdateAdvClassClass", 0); }
            catch (Exception e) {}
            assertNotNull("Updated Class does not exist", bobject);

            BaseClass bclass = bobject.getxWikiClass();
            assertNotNull("Updated Class does not have a wikiClass", bclass);

            assertNotNull("Updated Class wikiClass should have age property", bclass.safeget("age"));
            assertNotNull("Updated Class wikiClass should have name property", bclass.safeget("first_name"));

            assertNotNull("Updated Class should have age property", bobject.safeget("age"));
            assertNotNull("Updated Class should have name property", bobject.safeget("first_name"));

            Number age = (Number)((NumberProperty)bobject.safeget("age")).getValue();
            assertEquals("Updated Class age property value is incorrect", new Integer(12), age);
            String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
            assertEquals("Updated Class name property value is incorrect", "john", name);

            String category = (String)((StringProperty)bobject.safeget("category")).getValue();
            assertEquals("Updated Class category property value is incorrect", "2", category);

            List category2 = (List)((ListProperty)bobject.safeget("category2")).getValue();
            assertEquals("Updated Class category2 property size is incorrect", 2, category2.size());
            assertEquals("Updated Class category2 property item 1 is incorrect", "2", category2.get(0));
            assertEquals("Updated Class category2 property item 2 is incorrect", "3", category2.get(1));

            List category3 = (List)((ListProperty)bobject.safeget("category3")).getValue();
            assertEquals("Updated Class category3 property size is incorrect", 2, category3.size());
            assertEquals("Updated Class category3 property item 1 is incorrect", "2", category3.get(0));
            assertEquals("Updated Class category3 property item 2 is incorrect", "3", category3.get(1));
        }



    public void sendMultipart(WebRequest webRequest, File file) throws IOException {
        Part part = new FilePart("application/octet-stream", file);
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


    public void testAttach() throws IOException, Throwable {
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


    public void beginAttach(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);
        setUrl(webRequest, "upload", "AttachTest");
        webRequest.setContentType("multipart/form-data");
        File file = new File(Utils.filename);
        sendMultipart(webRequest, file);
    }

    public void endAttach(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Saving returned exception", result.indexOf("Exception")==-1);

        File fattach = new File(Utils.filename);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "AttachTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
        List list = doc2.getAttachmentList();
        assertEquals("Document has no attachement", 1, list.size());
        XWikiAttachment attachment = (XWikiAttachment) list.get(0);
        assertEquals("Attachment size is not correct", fattach.length(), attachment.getFilesize());
    }


    public void testAttachUpdate() throws IOException, Throwable {
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


    public void beginAttachUpdate(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore);

        XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Main", "AttachTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        hibstore.saveXWikiDoc(doc1);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1);
        doc1.getAttachmentList().add(attachment1);
        hibstore.saveXWikiDoc(doc1);

        setUrl(webRequest, "upload", "AttachTest");
        webRequest.setContentType("multipart/form-data");
        File file = new File(Utils.filename);
        sendMultipart(webRequest, file);
    }

    public void endAttachUpdate(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Saving returned exception", result.indexOf("Exception")==-1);

        File fattach = new File(Utils.filename);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "AttachTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2);
        List list = doc2.getAttachmentList();
        assertEquals("Document has no attachement", 1, list.size());
        XWikiAttachment attachment = (XWikiAttachment) list.get(0);
        assertEquals("Attachment version is not correct", attachment.getVersion(), "1.2");
        assertEquals("Attachment size is not correct", fattach.length(), attachment.getFilesize());
    }

    public static class MultipartSenderThread extends Thread  {

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
}
