
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import net.sf.hibernate.HibernateException;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.struts.action.ActionServlet;

import javax.servlet.ServletException;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

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

public class ViewEditTest extends ServletTest {

    public void beginViewNotOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        setUrl(webRequest, "view", "ViewNotOkTest");
    }

    public void endViewNotOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Page should have no version", result.indexOf("1.1")==-1);
    }

    public void testViewNotOk() throws Throwable {
        launchTest();
    }

    public void beginViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "ViewOkTest", context);
        setUrl(webRequest, "view", "ViewOkTest");
    }

    public void endViewOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testViewOk() throws IOException, Throwable {
        launchTest();
    }



    public void beginViewGetDocument(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "ViewGetDocumentContent", context);
        String content = Utils.content1;
        Utils.content1 = "test\n$xwiki.getDocument(\"Main.ViewGetDocumentContent\").getContent()\ntest\n";
        Utils.createDoc(hibstore, "Main", "ViewGetDocument", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewGetDocument");
    }

    public void endViewGetDocument(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find content in result:\n" + result, result.indexOf("Hello")!=-1);
    }

    public void testViewGetDocument() throws Throwable {
        launchTest();
    }


    public void beginViewDocumentLink(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        // Utils.createDoc(hibstore, "Main", "ViewDocumentTestLinkTest", context);
        String content = Utils.content1;
        Utils.content1 = "[Main.ViewDocumentLink]";
        Utils.createDoc(hibstore, "Main", "ViewDocumentLink", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "ViewDocumentLink");
    }

    public void endViewDocumentLink(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find content in result:\n" + result, result.indexOf("<a href")!=-1);
    }

    public void testViewDocumentLink() throws Throwable {
        launchTest();
    }


    public void beginPreviewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.PreviewOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(hibstore, "Main", "PreviewOkTest", bobject, bclass, context);
        setUrl(webRequest, "preview", "PreviewOkTest");
    }

    public void endPreviewOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
        assertTrue("Could not find object hidden form nb", result.indexOf("<input type=\"hidden\" name=\"Main.PreviewOkTest_nb\" value=\"1\"")!=-1);
        assertTrue("Could not find object hidden form value", result.indexOf("Main.PreviewOkTest_0_first_name")!=-1);
        assertTrue("Could not find object hidden form value", result.indexOf("Ludovic")!=-1);
    }

    public void testPreviewOk() throws Throwable {
        launchTest();
    }

    public void beginEditOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.EditOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(hibstore, "Main", "EditOkTest", bobject, bclass, context);
        setUrl(webRequest, "edit", "EditOkTest");
    }

    public void endEditOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testEditOk() throws Throwable {
        launchTest();
    }


    public void beginEditObjectOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.EditObjectOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(hibstore, "Main", "EditObjectOkTest", bobject, bclass, context);
        setUrl(webRequest, "edit", "EditObjectOkTest");
        webRequest.addParameter("xpage", "editobject");
    }

    public void endEditObjectOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find object hidden form nb", result.indexOf("<input type=\"hidden\" name=\"Main.EditObjectOkTest_nb\" value=\"1\"")!=-1);
        assertTrue("Could not find object form value", result.indexOf("Main.EditObjectOkTest_0_first_name")!=-1);
        assertTrue("Could not find object form value", result.indexOf("Ludovic")!=-1);
    }

    public void testEditObjectOk() throws Throwable {
        launchTest();
    }


    public void beginEditClassOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.EditClassOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(hibstore, "Main", "EditClassOkTest", bobject, bclass, context);
        setUrl(webRequest, "edit", "EditClassOkTest");
        webRequest.addParameter("xpage", "editclass");
    }

    public void endEditClassOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.NumberClass")!=-1);
        assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.StringClass")!=-1);
        assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.TextAreaClass")!=-1);
        assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.PasswordClass")!=-1);
        assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.BooleanClass")!=-1);
        assertTrue("Could not find Add Class", result.indexOf("com.xpn.xwiki.objects.classes.DBListClass")!=-1);
    }

    public void testEditClassOk() throws Throwable {
        launchTest();
    }


    public void beginEditWithTemplateOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.EditOkTestTemplate");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        String content = Utils.content1;
        Utils.content1 = "Template content";
        Utils.createDoc(hibstore, "Main", "EditOkTestTemplate", bobject, bclass, context);
        Utils.content1 = content;
        setUrl(webRequest, "edit", "EditOkWithTestTemplate");
        webRequest.addParameter("template", "Main.EditOkTestTemplate");
        webRequest.addParameter("parent", "XWiki.TestParentComesFromTemplate");
    }

    public void endEditWithTemplateOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Template content")!=-1);
        assertTrue("Could not find parent", result.indexOf("TestParentComesFromTemplate")!=-1);
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
        Utils.createDoc(hibstore, "Main", "EditOkTestWithTemplate", context);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.EditOkTestTemplate");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        String content = Utils.content1;
        Utils.content1 = "Template content";
        Utils.createDoc(hibstore, "Main", "EditOkTestTemplate", bobject, bclass, context);
        Utils.content1 = content;
        setUrl(webRequest, "edit", "EditOkTestWithTemplate");
        webRequest.addParameter("template", "Main.EditOkTestTemplate");
        webRequest.addParameter("parent", "XWiki.TestParentComesFromTemplate");
    }

    public void endEditWithTemplateNotOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertFalse("Template content should be ignored", result.indexOf("Template content")!=-1);
    }

    public void testEditWithTemplateOk() throws Throwable {
        launchTest();
    }



    public void beginViewRevOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "ViewRevOkTest", context);
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "ViewRevOkTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        doc2.setContent("zzzzzzzzzzzzzzzzzzzzzzzz");
        hibstore.saveXWikiDoc(doc2, context);
        setUrl(webRequest, "view", "ViewRevOkTest", "rev=1.1");
    }



    public void endViewRevOk(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testViewRevOk() throws Throwable {
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
        webRequest.addParameter("content","Hello1Hello2Hello3");
        webRequest.addParameter("parent","Main.WebHome");
    }

    public void endSave(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Saving returned exception", result.indexOf("Exception")==-1);

        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "SaveTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        String content2 = doc2.getContent();
        assertEquals("Content is not indentical", "Hello1Hello2Hello3",content2);
        assertEquals("Parent is not identical", "Main.WebHome", doc2.getParent());
    }


    public void testDelete() throws Throwable {
        launchTest();
    }

    public void beginDelete(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "DeleteTest", context);
        setUrl(webRequest, "delete", "DeleteTest");
        webRequest.addParameter("confirm","1");
    }

    public void endDelete(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Delete returned exception", result.indexOf("Exception")==-1);

        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "DeleteTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);

        assertTrue("Document should not exist", doc2.isNew());
    }


    public void testDeleteWithoutConfirm() throws Throwable {
        launchTest();
    }

    public void beginDeleteWithoutConfirm(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "DeleteTest2", context);
        setUrl(webRequest, "delete", "DeleteTest2");
    }

    public void endDeleteWithoutConfirm(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("DeleteWithoutConfirm returned exception", result.indexOf("Exception")==-1);

        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "DeleteTest2");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);

        assertFalse("Document should exist", doc2.isNew());
    }


    public void testAddProp(Class cclass) throws Throwable {
        launchTest();
    }

    public void beginAddProp(WebRequest webRequest, String name, Class cclass) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "PropAddTest", context);
        setUrl(webRequest, "propadd", "PropAddTest");
        webRequest.addParameter("propname", name);
        webRequest.addParameter("proptype", cclass.getName());
    }

    public void endAddProp(WebResponse webResponse, String name, Class cclass) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Property " + cclass.getName() + " returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseClass bclass = doc2.getxWikiClass();
        assertNotNull("Class does not exist for " + cclass.getName(), bclass);
        assertNotNull("Property of type " + cclass.getName() + " has not been added", bclass.safeget(name));
        assertEquals("Property type is not correct for " + cclass.getName(), bclass.safeget(name).getClass(), cclass);
    }

    public void testAddNumberProp() throws IOException, Throwable {
        testAddProp(NumberClass.class);
    }

    public void beginAddNumberProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, "score", NumberClass.class);
    }

    public void endAddNumberProp(WebResponse response)  throws XWikiException {
        endAddProp(response, "score", NumberClass.class);
    }

    public void testAddStringProp() throws IOException, Throwable {
        testAddProp(StringClass.class);
    }

    public void beginAddStringProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, "category", StringClass.class);
    }

    public void endAddStringProp(WebResponse response)  throws XWikiException {
        endAddProp(response, "category", StringClass.class);
    }

    public void testAddObject() throws Throwable {
        launchTest();
    }

    public void beginAddObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.PropAddObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(hibstore, "Main", "PropAddObjectClass", bobject, bclass, context);
        Utils.createDoc(hibstore, "Main", "PropAddObject", context);
        setUrl(webRequest, "objectadd", "PropAddObject");
        webRequest.addParameter("classname", "Main.PropAddObjectClass");
    }

    public void endAddObject(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropAddObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("Added Object does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass(context);
        assertNotNull("Added Object does not have a wikiClass", bclass);

        assertNotNull("Added Object wikiClass should have ageclass property", bclass.safeget("age"));
        assertNotNull("Added Object wikiClass should have nameclass property", bclass.safeget("first_name"));
    }

    public void testAddSecondObject() throws Throwable {
        launchTest();
    }


    public void beginAddSecondObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.PropAddSecondObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("PropAddSecondObjectClass");
        Utils.createDoc(hibstore, "Main", "PropAddSecondObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropAddSecondObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropAddSecondObjectClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropAddSecondObject", null, null, bobjects, context);
        setUrl(webRequest, "objectadd", "PropAddSecondObject");
        webRequest.addParameter("classname", "Main.PropAddSecondObjectClass");
    }

    public void endAddSecondObject(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddSecondObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseObject bobject = null;
        try {
            bobject = (BaseObject) doc2.getObject("Main.PropAddSecondObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("First Object does not exist", bobject);
        bobject = null;
        try {
            bobject = (BaseObject) doc2.getObject("Main.PropAddSecondObjectClass", 1); }
        catch (Exception e) {}
        assertNotNull("Second Object does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass(context);
        assertNotNull("Added Object does not have a wikiClass", bclass);

        assertNotNull("Added Object wikiClass should have ageclass property", bclass.safeget("age"));
        assertNotNull("Added Object wikiClass should have nameclass property", bclass.safeget("first_name"));
    }

    public void testRemoveObject() throws Throwable {
        launchTest();
    }

    public void beginRemoveObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.PropRemoveObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("PropRemoveObjectClass");
        Utils.createDoc(hibstore, "Main", "PropRemoveObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropRemoveObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropRemoveObjectClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropRemoveObject", null, null, bobjects, context);
        if (hibstore instanceof XWikiCacheInterface)
            ((XWikiCacheInterface) hibstore).flushCache();
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropRemoveObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseObject bobject2 = null;
        try {
            bobject2 = (BaseObject) doc2.getObject("Main.PropRemoveObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("Object does not exists", bobject2);

        setUrl(webRequest, "objectremove", "PropRemoveObject");
        webRequest.addParameter("classname", "Main.PropRemoveObjectClass");
        webRequest.addParameter("classid", "0");
    }

    public void endRemoveObject(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Adding Object returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropRemoveObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseObject bobject = null;
        try {
            bobject = (BaseObject) doc2.getObject("Main.PropRemoveObjectClass", 0); }
        catch (Exception e) {}
        assertNull("Object still exists", bobject);
    }


    public void testUpdateObjectProp() throws Throwable {
        launchTest();
    }

    public void beginUpdateObjectProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.PropUpdateObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("Main.PropUpdateObjectClass");
        Utils.createDoc(hibstore, "Main", "PropUpdateObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateObjectClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropUpdateObject", null, null, bobjects, context);

        setUrl(webRequest, "save", "PropUpdateObject");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("parent", "");
        webRequest.addParameter("Main.PropUpdateObjectClass_nb", "1");
        webRequest.addParameter("Main.PropUpdateObjectClass_0_age", "12");
        webRequest.addParameter("Main.PropUpdateObjectClass_0_first_name", "john");
    }

    public void endUpdateObjectProp(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Updated Object returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropUpdateObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropUpdateObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("Updated Object does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass(context);
        assertNotNull("Updated Object does not have a wikiClass", bclass);

        assertNotNull("Updated Object wikiClass should have age property", bclass.safeget("age"));
        assertNotNull("Updated Object wikiClass should have name property", bclass.safeget("first_name"));

        assertNotNull("Updated Object should have age property", bobject.safeget("age"));
        assertNotNull("Updated Object should have name property", bobject.safeget("first_name"));

        Number age = (Number)((NumberProperty)bobject.safeget("age")).getValue();
        assertEquals("Updated Object age property value is incorrect", new Integer(12), age);
        String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Updated Object name property value is incorrect", "john", name);
    }


    public void testUpdateClassProp() throws Throwable {
        launchTest();
    }

    public void beginUpdateClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.PropUpdateClassPropClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("Main.PropUpdateClassPropClass");
        Utils.createDoc(hibstore, "Main", "PropUpdateClassPropClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateClassProp");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateClassPropClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropUpdateClassProp", null, null, bobjects, context);
        setUrl(webRequest, "propupdate", "PropUpdateClassPropClass");
        webRequest.addParameter("age_name", "age");
        webRequest.addParameter("age_size", "20");
        webRequest.addParameter("age_prettyName", "Age of person");
    }

    public void endUpdateClassProp(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Updated Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc = new XWikiSimpleDoc("Main", "PropUpdateClassPropClass");
        doc = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc, context);
        BaseClass bclass = doc.getxWikiClass();
        NumberClass ageclass = (NumberClass) bclass.safeget("age");
        assertNotNull("Updated Class wikiClass should have age property", ageclass);
        assertEquals("Updated Class age numberclass size is incorrect", 20, ageclass.getSize());
        assertEquals("Updated Class age numberclass pretty name is incorrect", "Age of person", ageclass.getPrettyName());
        assertNotNull("Updated Class wikiClass should have name property", bclass.safeget("first_name"));


        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropUpdateClassProp");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropUpdateClassPropClass", 0); }
        catch (Exception e) {}
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

        Number age = (Number)((NumberProperty)bobject.safeget("age")).getValue();
        assertEquals("Updated Class age property value is incorrect", new Integer(33), age);
        String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Updated Class name property value is incorrect", "Ludovic", name);
    }



    public void testRenameClassProp() throws Throwable {
        launchTest();
    }

    public void beginRenameClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.PropRenameClassPropClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("Main.PropRenameClassPropClass");
        Utils.createDoc(hibstore, "Main", "PropRenameClassPropClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropRenameClassProp");
        bobjlist.add(bobject);
        bobjects.put("Main.PropRenameClassPropClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropRenameClassProp", null, null, bobjects, context);
        setUrl(webRequest, "propupdate", "PropRenameClassPropClass");
        webRequest.addParameter("age_name", "age2");
        webRequest.addParameter("age_size", "40");
        webRequest.addParameter("age_prettyName", "Age of person");
    }

    public void endRenameClassProp(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Rename Class returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc = new XWikiSimpleDoc("Main", "PropRenameClassPropClass");
        doc = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc, context);
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
        try { bobject = (BaseObject) doc.getObject("Main.PropRenameClassPropClass", 0); }
        catch (Exception e) {}
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

        Number age = (Number)((NumberProperty)bobject.safeget("age2")).getValue();
        assertEquals("Rename Class age property value is incorrect", new Integer(33), age);
        String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Rename Class name property value is incorrect", "Ludovic", name);

        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropRenameClassProp");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropRenameClassPropClass", 0); }
        catch (Exception e) {}
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

        age = (Number)((NumberProperty)bobject.safeget("age2")).getValue();
        assertEquals("Rename Class age property value is incorrect", new Integer(33), age);
        name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Rename Class name property value is incorrect", "Ludovic", name);
    }



    public void testUpdateAdvancedObjectProp() throws Throwable {
        launchTest();
    }

    public void beginUpdateAdvancedObjectProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareAdvancedObject(doc, "Main.PropUpdateAdvObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        bclass.setName("PropUpdateAdvObjectClass");
        Utils.createDoc(hibstore, "Main", "PropUpdateAdvObjectClass", bobject, bclass, context);
        Map bobjects = new HashMap();
        Vector bobjlist = new Vector();
        bobject.setName("Main.PropUpdateAdvObject");
        bobjlist.add(bobject);
        bobjects.put("Main.PropUpdateAdvObjectClass", bobjlist);
        Utils.createDoc(hibstore, "Main", "PropUpdateAdvObject", null, null, bobjects, context);
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

    public void endUpdateAdvancedObjectProp(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Updated Object returned exception", result.indexOf("Exception")==-1);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropUpdateAdvObject");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        Map bobjects = doc2.getxWikiObjects();
        BaseObject bobject = null;
        try { bobject = (BaseObject) doc2.getObject("Main.PropUpdateAdvObjectClass", 0); }
        catch (Exception e) {}
        assertNotNull("Updated Object does not exist", bobject);

        BaseClass bclass = bobject.getxWikiClass(context);
        assertNotNull("Updated Object does not have a wikiClass", bclass);

        assertNotNull("Updated Object wikiClass should have age property", bclass.safeget("age"));
        assertNotNull("Updated Object wikiClass should have name property", bclass.safeget("first_name"));

        assertNotNull("Updated Object should have age property", bobject.safeget("age"));
        assertNotNull("Updated Object should have name property", bobject.safeget("first_name"));

        Number age = (Number)((NumberProperty)bobject.safeget("age")).getValue();
        assertEquals("Updated Object age property value is incorrect", new Integer(12), age);
        String name = (String)((StringProperty)bobject.safeget("first_name")).getValue();
        assertEquals("Updated Object name property value is incorrect", "john", name);

        String category = (String)((StringProperty)bobject.safeget("category")).getValue();
        assertEquals("Updated Object category property value is incorrect", "2", category);

        List category2 = (List)((ListProperty)bobject.safeget("category2")).getValue();
        assertEquals("Updated Object category2 property size is incorrect", 2, category2.size());
        assertEquals("Updated Object category2 property item 1 is incorrect", "2", category2.get(0));
        assertEquals("Updated Object category2 property item 2 is incorrect", "3", category2.get(1));

        List category3 = (List)((ListProperty)bobject.safeget("category3")).getValue();
        assertEquals("Updated Object category3 property size is incorrect", 2, category3.size());
        assertEquals("Updated Object category3 property item 1 is incorrect", "2", category3.get(0));
        assertEquals("Updated Object category3 property item 2 is incorrect", "3", category3.get(1));
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

    public void endAttach(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Saving returned exception", result.indexOf("Exception")==-1);

        File fattach = new File(Utils.filename);
        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "AttachTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        List list = doc2.getAttachmentList();
        assertEquals("Document has no attachement", 1, list.size());
        XWikiAttachment attachment = (XWikiAttachment) list.get(0);
        assertEquals("Attachment size is not correct", fattach.length(), attachment.getFilesize());
    }


    public void testAttachUpdate() throws Throwable {
        launchTest();
    }


    public void beginAttachUpdate(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Main", "AttachTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        hibstore.saveXWikiDoc(doc1, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        hibstore.saveXWikiDoc(doc1, context);

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
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        List list = doc2.getAttachmentList();
        assertEquals("Document has no attachement", 1, list.size());
        XWikiAttachment attachment = (XWikiAttachment) list.get(0);
        assertEquals("Attachment version is not correct", attachment.getVersion(), "1.2");
        assertEquals("Attachment size is not correct", fattach.length(), attachment.getFilesize());
    }

    public void testAttachDownload() throws Throwable {
        launchTest();
    }


    public void beginAttachDownload(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Main", "AttachDownloadTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        hibstore.saveXWikiDoc(doc1, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        String attachcontent1 = "blablabla";
        attachment1.setContent(attachcontent1.getBytes());
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        hibstore.saveXWikiDoc(doc1, context);
        setUrl(webRequest, "download", "AttachDownloadTest/" + Utils.filename);
    }

    public void endAttachDownload(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Attach Delete returned exception", result.indexOf("Exception")==-1);
        assertTrue("Attach Delete should contain attachment text", result.indexOf("blablabla")!=-1);

        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "AttachDownloadTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        List list = doc2.getAttachmentList();
        assertEquals("Document should have an attachement", 1, list.size());
    }


    public void testAttachDelete() throws Throwable {
        launchTest();
    }


    public void beginAttachDelete(WebRequest webRequest) throws HibernateException, XWikiException, IOException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Main", "AttachDeleteTest");
        doc1.setContent(Utils.content1);
        doc1.setAuthor(Utils.author);
        doc1.setParent(Utils.parent);
        hibstore.saveXWikiDoc(doc1, context);
        XWikiAttachment attachment1 = new XWikiAttachment(doc1, Utils.filename);
        byte[] attachcontent1 = Utils.getDataAsBytes(new File(Utils.filename));
        attachment1.setContent(attachcontent1);
        doc1.saveAttachmentContent(attachment1, context);
        doc1.getAttachmentList().add(attachment1);
        hibstore.saveXWikiDoc(doc1, context);
        setUrl(webRequest, "delattachment", "AttachDeleteTest/" + Utils.filename);
    }

    public void endAttachDelete(WebResponse webResponse) throws XWikiException {
        String result = webResponse.getText();
        // Verify return
        assertTrue("Attach Delete returned exception", result.indexOf("Exception")==-1);

        XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "AttachDeleteTest");
        doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
        List list = doc2.getAttachmentList();
        assertEquals("Document should have no attachement", 0, list.size());
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

    public void beginInclude(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Included Page";
        Utils.createDoc(hibstore, "Main", "IncludeTest2", context);
        Utils.content1 = "#includeTopic(\"Main.IncludeTest2\")\n";
        Utils.createDoc(hibstore, "Main", "IncludeTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "IncludeTest");
    }

    public void endInclude(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Included Page")!=-1);
    }

    public void testIncludeEdit() throws IOException, Throwable {
        launchTest();
    }

    public void beginIncludeEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Included Page";
        Utils.createDoc(hibstore, "Main", "IncludeTest2", context);
        Utils.content1 = "#includeTopic(\"IncludeTest2\")\n#includeForm(\"Main.IncludeTest3\")";
        Utils.createDoc(hibstore, "Main", "IncludeTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "edit", "IncludeTest");
    }

    public void endIncludeEdit(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("includeTopic")!=-1);
        assertTrue("Could not find Include page messages", result.indexOf("edit/Main/IncludeTest2")!=-1);
        assertTrue("Could not find Include page messages", result.indexOf("edit/Main/IncludeTest3")!=-1);
    }

    public void testInclude() throws IOException, Throwable {
        launchTest();
    }

}
