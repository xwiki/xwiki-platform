
package com.xpn.xwiki.test;

import org.apache.cactus.WebRequest;
import org.apache.cactus.ServletTestCase;
import org.apache.cactus.WebResponse;
import org.apache.struts.action.ActionServlet;

import javax.servlet.ServletException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.File;
import java.util.Map;
import java.util.HashMap;
import java.util.Enumeration;
import java.util.Vector;

import net.sf.hibernate.HibernateException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.NumberProperty;
import com.xpn.xwiki.objects.StringProperty;

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

    private static String hibpath = "hibernate-test.cfg.xml";

    public void setUp() {
        flushCache();
    };

    public void cleanUp() {
    };

    public void clientSetUp(XWikiStoreInterface hibstore) throws HibernateException, XWikiException {
    }

    public void createDoc(XWikiStoreInterface hibstore, String name) throws HibernateException, XWikiException {
        createDoc(hibstore,name, null, null, null);
    }

    public void createDoc(XWikiStoreInterface hibstore, String name,
                          BaseObject bobject, BaseClass bclass) throws HibernateException, XWikiException {
        createDoc(hibstore, name, bobject, bclass, null);

    }

    public void createDoc(XWikiStoreInterface hibstore, String name,
                          BaseObject bobject, BaseClass bclass,
                          Map bobjects) throws HibernateException, XWikiException {
        XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Main", name);
        doc1.setContent("Hello 1\nHello 2\nHello 3\n");
        doc1.setAuthor("VictorHugo");
        doc1.setParent("Main.WebHome");

        if (bobject!=null)
         doc1.setxWikiObject(bobject);
        if (bclass!=null)
         doc1.setxWikiClass(bclass);
        if (bobjects!=null)
         doc1.setxWikiObjects(bobjects);

        hibstore.saveXWikiDoc(doc1);
    }

    private void setUrl(WebRequest webRequest, String action, String docname) {
        webRequest.setURL("127.0.0.1:9080", "/xwiki" , "/testbin", "/" + action + "/Main/" + docname, "");
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
        com.xpn.xwiki.test.StoreObjectTest.cleanUp(hibstore);
        setUrl(webRequest, "view", "ViewNotOkTest");
    }

    public void endViewNotOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Page should have generated an error", result.indexOf("No row")!=-1);
    }

    public void testViewNotOk() throws IOException, ServletException, HibernateException {
        ActionServlet servlet = new ActionServlet();
        servlet.init(config);
        servlet.service(request, response);
        cleanSession(session);
    }

    public void beginViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        com.xpn.xwiki.test.StoreObjectTest.cleanUp(hibstore);
        createDoc(hibstore, "ViewOkTest");
        setUrl(webRequest, "view", "ViewOkTest");
    }

    public void endViewOk(WebResponse webResponse) {
        String result = webResponse.getText();
        assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
    }

    public void testViewOk() throws IOException, ServletException {
        ActionServlet servlet = new ActionServlet();
        servlet.init(config);
        servlet.service(request, response);
        cleanSession(session);
    }

    public void testSave() throws IOException, ServletException {
         ActionServlet servlet = new ActionServlet();
         servlet.init(config);
         servlet.service(request, response);
         cleanSession(session);
     }

     public void beginSave(WebRequest webRequest) throws HibernateException, XWikiException {
         XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
         com.xpn.xwiki.test.StoreObjectTest.cleanUp(hibstore);
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

     public void testAddProp(Class cclass) throws IOException, ServletException {
         ActionServlet servlet = new ActionServlet();
         servlet.init(config);
         servlet.service(request, response);
         cleanSession(session);
     }

     public void beginAddProp(WebRequest webRequest, Class cclass) throws HibernateException, XWikiException {
         XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
         com.xpn.xwiki.test.StoreObjectTest.cleanUp(hibstore);
         createDoc(hibstore, "PropAddTest");
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

   public void testAddNumberProp() throws IOException, ServletException {
       testAddProp(NumberClass.class);
   }

    public void beginAddNumberProp(WebRequest webRequest) throws HibernateException, XWikiException {
       beginAddProp(webRequest, NumberClass.class);
   }

   public void endAddNumberProp(WebResponse response)  throws XWikiException {
       endAddProp(response, NumberClass.class);
   }

    public void testAddStringProp() throws IOException, ServletException {
        testAddProp(StringClass.class);
    }

     public void beginAddStringProp(WebRequest webRequest) throws HibernateException, XWikiException {
        beginAddProp(webRequest, StringClass.class);
    }

    public void endAddStringProp(WebResponse response)  throws XWikiException {
        endAddProp(response, StringClass.class);
    }

    public void testAddClass() throws IOException, ServletException {
        ActionServlet servlet = new ActionServlet();
        servlet.init(config);
        servlet.service(request, response);
        cleanSession(session);
    }

    public void beginAddClass(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        com.xpn.xwiki.test.StoreObjectTest.cleanUp(hibstore);
        BaseObject bobject = ObjectTest.prepareObject();
        createDoc(hibstore, "PropAddClassClass", bobject, bobject.getxWikiClass());
        createDoc(hibstore, "PropAddClass");
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
       try { bobject = (BaseObject) bobjects.get("Main.PropAddClassClass"); }
       catch (Exception e) {}
       assertNotNull("Added Class does not exist", bobject);

       BaseClass bclass = bobject.getxWikiClass();
       assertNotNull("Added Class does not have a wikiClass", bclass);

       assertNotNull("Added Class wikiClass should have ageclass property", bclass.safeget("age"));
       assertNotNull("Added Class wikiClass should have nameclass property", bclass.safeget("name"));
    }

    public void testUpdateClassProp() throws IOException, ServletException {
        ActionServlet servlet = new ActionServlet();
        servlet.init(config);
        servlet.service(request, response);
        cleanSession(session);
    }

    public void beginUpdateClassProp(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        com.xpn.xwiki.test.StoreObjectTest.cleanUp(hibstore);
        BaseObject bobject = ObjectTest.prepareObject();
        createDoc(hibstore, "PropUpdateClassClass", bobject, bobject.getxWikiClass());
        Map bobjects = new HashMap();
        bobject.setName("Main.PropUpdateClass");
        bobjects.put("Main.PropUpdateClassClass", bobject);
        createDoc(hibstore, "PropUpdateClass", null, null, bobjects);
        setUrl(webRequest, "save", "PropUpdateClass");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("parent", "");
        webRequest.addParameter("Main.PropUpdateClassClass_age", "12");
        webRequest.addParameter("Main.PropUpdateClassClass_name", "john");
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
       try { bobject = (BaseObject) bobjects.get("Main.PropUpdateClassClass"); }
       catch (Exception e) {}
       assertNotNull("Updated Class does not exist", bobject);

       BaseClass bclass = bobject.getxWikiClass();
       assertNotNull("Updated Class does not have a wikiClass", bclass);

       assertNotNull("Updated Class wikiClass should have age property", bclass.safeget("age"));
       assertNotNull("Updated Class wikiClass should have name property", bclass.safeget("name"));

       assertNotNull("Updated Class should have age property", bobject.safeget("age"));
       assertNotNull("Updated Class should have name property", bobject.safeget("name"));

       Number age = ((NumberProperty)bobject.safeget("age")).getValue();
       assertEquals("Updated Class age property value is incorrect", new Integer(12), age);
       String name = ((StringProperty)bobject.safeget("name")).getValue();
       assertEquals("Updated Class name property value is incorrect", "john", name);
    }
}
