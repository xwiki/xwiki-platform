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
 * * User: ludovic
 * Date: 13 mars 2004
 * Time: 15:29:22
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import net.sf.hibernate.HibernateException;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.apache.cactus.client.authentication.Authentication;
import org.apache.cactus.client.authentication.BasicAuthentication;

import java.util.HashMap;
import java.util.Map;


public class ServletVirtualTest extends ServletTest {

    public void updateRight(String fullname, String user, String group, String level, boolean allow, boolean global) throws XWikiException {
        Utils.updateRight(xwiki, context, fullname, user, group, level, allow, global);
    }

    public void beginVirtualViewOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "VirtualViewOkTest", context);
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        setVirtualUrl(webRequest, "127.0.0.1", "xwiki", "view", "VirtualViewOkTest", "");
    }

    public void endVirtualViewOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testVirtualViewOk() throws Throwable {
        launchTest();
    }

    public void beginVirtualView2(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());

        // Setup database xwikitest
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);

        // Setup database xwikitest2
        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "VirtualViewOkTest2", context);

        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualViewOkTest2", "");
    }

    public void endVirtualView2(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testVirtualView2() throws Throwable {
        launchTest();
    }


    public void beginAuth(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);

        // Create User in the virtual wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, context);

        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthTest", context);
        updateRight("Main.VirtualAuthTest", "XWiki.LudovicDubost", "", "view", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthTest", "");
        Authentication auth = new BasicAuthentication("LudovicDubost", "toto");
        webRequest.setAuthentication(auth);
    }

    public void endAuth(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
        } finally {
            clientTearDown();
        }
    }

    /*
    // Deactivate the test until I know how to pass the parameters
    public void testAuth() throws Throwable {
        launchTest();
    }
    */

    public void beginAuthForWikiOwner(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);

        // Create User in the main wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, context);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthOwnerTest", context);
        updateRight("Main.VirtualAuthOwnerTest", "XWiki.LudovicDubost", "", "view", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthOwnerTest", "");
        Authentication auth = new BasicAuthentication("LudovicDubost", "toto");
        webRequest.setAuthentication(auth);
    }

    public void endAuthForWikiOwner(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content", result.indexOf("Hello")!=-1);
        } finally {
            clientTearDown();
        }
    }

    /*
    // Deactivate the test until I know how to pass the parameters
    public void testAuthForWikiOwner() throws Throwable {
        launchTest();
    }
    */
    
    public void testAddVirtualObject() throws Throwable {
        launchTest();
    }

    public void beginAddVirtualObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiSimpleDoc doc = new XWikiSimpleDoc();
        Utils.prepareObject(doc, "Main.PropAddVirtualObjectClass");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(hibstore, "Main", "PropAddVirtualObject", context);

        // Create the class in the second db
        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        Utils.createDoc(hibstore, "Main", "PropAddVirtualObjectClass", bobject, bclass, context);

        // Switch back to standard db
        context.setDatabase("xwikitest");

        // Add the class of the second db to a document of the first db
        setUrl(webRequest, "objectadd", "PropAddVirtualObject");
        webRequest.addParameter("classname", "xwikitest2:Main.PropAddVirtualObjectClass");
    }

    public void endAddVirtualObject(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Adding Class returned exception", result.indexOf("Exception")==-1);
            XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
            XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "PropAddVirtualObject");
            doc2 = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc2, context);
            Map bobjects = doc2.getxWikiObjects();
            BaseObject bobject = null;
            try { bobject = (BaseObject) doc2.getObject("xwikitest2:Main.PropAddVirtualObjectClass", 0); }
            catch (Exception e) {}
            assertNotNull("Added Object does not exist", bobject);

            BaseClass bclass = bobject.getxWikiClass(context);
            assertNotNull("Added Object does not have a wikiClass", bclass);

            assertNotNull("Added Object wikiClass should have ageclass property", bclass.safeget("age"));
            assertNotNull("Added Object wikiClass should have nameclass property", bclass.safeget("first_name"));
        } finally {
            clientTearDown();
        }
    }


}
