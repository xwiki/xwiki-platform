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
 * @author vmassol
 * @author jeremi
 * @author sdumitriu
 */


package com.xpn.xwiki.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;


public class ServletVirtualTest extends ServletTest {

    public void updateRight(String fullname, String user, String group, String level, boolean allow, boolean global) throws XWikiException {
        Utils.updateRight(xwiki, context, fullname, user, group, level, allow, global);
    }

    public void addMember(String fullname, String group) throws XWikiException {
        Utils.addMember(xwiki, context, fullname, group);
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
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
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
        StoreHibernateTest.cleanUp(hibstore, false, true, context);
        Utils.createDoc(hibstore, "Main", "VirtualViewOkTest2", context);

        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualViewOkTest2", "");
    }

    public void endVirtualView2(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find VirtualViewOkTest2 Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testVirtualView2() throws Throwable {
        launchTest();
    }


    public void beginAuth(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
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
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthTest", context);
        updateRight("Main.VirtualAuthTest", "XWiki.LudovicDubost", "", "view", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthTest", "");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endAuth(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testAuth() throws Throwable {
        launchTest();
    }

    public void beginAuthForWikiOwner(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "owner", "XWiki.LudovicDubost", context);

        // Create User in the main wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthOwnerTest", context);
        // Let's add a right to force lookup of wiki owner
        updateRight("Main.VirtualAuthOwnerTest", "XWiki.toto", "", "view, edit", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthOwnerTest", "");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endAuthForWikiOwner(WebResponse webResponse) throws HibernateException {
        try {
            // The content should be always viewable by the wiki owner
            // since he is the owner !
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testAuthForWikiOwner() throws Throwable {
        launchTest();
    }

    public void beginAuthForMasterWikiAdmin(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "owner", "XWiki.JohnDoe", context);

        // Create User in the main wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        // Let's give master admin right to LudovicDubost
        updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost", "", "admin", true, true);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthMasterAdminTest", context);
        // Let's add a right to force lookup of wiki owner
        updateRight("Main.VirtualAuthMasterAdminTest", "XWiki.toto", "", "view, edit", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthMasterAdminTest", "");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endAuthForMasterWikiAdmin(WebResponse webResponse) throws HibernateException {
        try {
            // The content should be always viewable by the wiki owner
            // since he is the owner !
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testAuthForMasterWikiAdmin() throws Throwable {
        launchTest();
    }


    public void beginAuthForAllGroup(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "owner", "XWiki.JohnDoe", context);

        // Create User in the main wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        // By default LudovicDubost member of AllGroup on the master server
        // addMember("XWiki.LudovicDubost", "XWiki.XWikiAllGroup");

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthAllGroupTest", context);

        // Let's add a right for the AllGroup
        updateRight("Main.VirtualAuthAllGroupTest", "", "XWiki.XWikiAllGroup", "view, edit", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthAllGroupTest", "");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endAuthForAllGroup(WebResponse webResponse) throws HibernateException {
        try {
            // The content should not be viewable by a member of the AllGroup on the master wiki
            // If it does there is confusion between the groups on the master wiki and on the virtual wiki
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Document should not be allowed: " + result, result.indexOf("You are not allowed")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testAuthForAllGroup() throws Throwable {
        launchTest();
    }

    public void beginAuthForMasterAllGroup(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "owner", "XWiki.JohnDoe", context);

        // Create User in the main wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        // By default LudovicDubost member of AllGroup on the master server
        // addMember("XWiki.LudovicDubost", "XWiki.XWikiAllGroup");

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthAllGroupTest", context);

        // Let's add a right for the AllGroup
        updateRight("Main.VirtualAuthAllGroupTest", "", "xwikitest:XWiki.XWikiAllGroup", "view, edit", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthAllGroupTest", "");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endAuthForMasterAllGroup(WebResponse webResponse) throws HibernateException {
        try {
            // The content should be viewable to a member of the master AllGroup
            // since we gave explicit right to this group
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testAuthForMasterAllGroup() throws Throwable {
        launchTest();
    }

    public void beginAuthForLocalAllGroup(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "owner", "XWiki.JohnDoe", context);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthAllGroupTest", context);


        // Let's add a right for the AllGroup
        updateRight("Main.VirtualAuthAllGroupTest", "", "XWiki.XWikiAllGroup", "view, edit", true, false);

        // Create User in the local wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        // By default LudovicDubost is member of AllGroup on the virtual wiki
        // addMember("xwiki:XWiki.LudovicDubost", "XWiki.XWikiAllGroup");

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthAllGroupTest", "");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endAuthForLocalAllGroup(WebResponse webResponse) throws HibernateException {
        try {
            // The content should be viewable to a member of the local AllGroup
            // since we gave explicit right to this group
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testAuthForLocalAllGroup() throws Throwable {
        launchTest();
    }

    public void beginAuthForLocalAllGroup2(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerXwikitest2", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "server", "127.0.0.1", context);
        Utils.setStringValue("XWiki.XWikiServerXwikitest2", "XWiki.XWikiServerClass", "owner", "XWiki.JohnDoe", context);

        // Create User in the local wiki database
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, context);
        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthAllGroupTest", context);

        // Let's add a right for the AllGroup
        updateRight("Main.VirtualAuthAllGroupTest", "", "XWiki.XWikiAllGroup", "view, edit", true, false);

        // Let's add the global user to the local AllGroup
        addMember("xwikitest:XWiki.LudovicDubost", "XWiki.XWikiAllGroup");

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthAllGroupTest", "");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endAuthForLocalAllGroup2(WebResponse webResponse) throws HibernateException {
        try {
            // The content should be viewable to a member of the local AllGroup
            // since we gave explicit right to this group
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testAuthForLocalAllGroup2() throws Throwable {
        launchTest();
    }

    /*
    public void testAddVirtualObject() throws Throwable {
        usslaunchTest();
    }
    */

    public void beginAddVirtualObject(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
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
            assertTrue("Adding Class returned exception: " + result, result.indexOf("Exception")==-1);
            XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
            XWikiDocument doc2 = new XWikiDocument("Main", "PropAddVirtualObject");
            doc2 = (XWikiDocument) hibstore.loadXWikiDoc(doc2, context);
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


    public void beginCreateNewWiki(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Result: $xwiki.createNewWiki(\"xwikitest2\", \"localhost\", \"XWiki.LudovicDubost\", \"xwikitest\", true)";
        Utils.content1 += "\n$context.context.nbdocs\n";
        Utils.createDoc(hibstore, "Main", "CreateNewWikiTest", context);
        Utils.content1 = content;

        // In order for createUser to work, we need programming right
        Utils.createDoc(hibstore, "Test", "CreateNewWikiTestDoc", context);
        // Let's put a few documents
        for (int i=1;i<25;i++) {
         Utils.createDoc(hibstore,"Main", "CreateNewWikiTestDoc" + i, context);

         // Let's add a german translation
         XWikiDocument doc = xwiki.getDocument("Main.CreateNewWikiTestDoc" + i, context);
         doc.setLanguage("de");
         xwiki.saveDocument(doc, context);
        }

        Utils.createDoc(hibstore, "XWiki", "LudovicDubost", context);
        Utils.setStringValue("XWiki.LudovicDubost", "XWiki.XWikiUsers", "email", "ludovic@xwiki.org", context);
        Utils.setIntValue("XWiki.LudovicDubost", "XWiki.XWikiUsers", "active", 1, context);
        Utils.setStringValue("XWiki.LudovicDubost", "XWiki.XWikiUsers", "password", "toto", context);

        // In order for createUser to work, we need programming right
        Utils.createDoc(hibstore, "XWiki", "XWikiPreferences", context);
        updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost", "", "programming", true, true);

        context.setDatabase("xwikitest2");
        StoreHibernateTest.cleanUp(hibstore, true, false, context);
        context.setDatabase("xwikitest");

        setUrl(webRequest, "view", "CreateNewWikiTest");

    }

    public void endCreateNewWiki(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Response code is not correct: " + result, result.indexOf("Result: 1")!=-1 );

            context.setDatabase("xwikitest2");
            XWikiDocument doc = xwiki.getDocument("Test.CreateNewWikiTestDoc", context);
            assertTrue("Document in new wiki should exist", !doc.isNew());

            for (int i=1;i<25;i++) {
             doc = xwiki.getDocument("Main.CreateNewWikiTestDoc" + i, context);
             assertTrue("Document " + i + " in new wiki should exist", !doc.isNew());
             List tlist = doc.getTranslationList(context);
             assertEquals("Document " + i + " has a german version", tlist.size(), 1);
             String language = (String) tlist.get(0);
             assertEquals("Document " + i + " has a german version", language, "de");
            }


        } finally {
            clientTearDown();
        }

    }


    public void testCreateNewWiki() throws Throwable {
        launchTest();
    }



    public void beginAuthGroup(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
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
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        // Setup Authentication rights
        Utils.createDoc(hibstore, "Main", "VirtualAuthTest", context);
        updateRight("Main.VirtualAuthTest", "", "XWiki.XWikiAllGroup", "view", true, false);

        // Use a virtual server URL
        setVirtualUrl(webRequest, "127.0.0.1", "xwikitest2", "view", "VirtualAuthTest", "");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endAuthGroup(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }
    }

    // Deactivate the test until I know how to pass the parameters
    public void testAuthGroup() throws Throwable {
        launchTest();
    }


    public void beginVirtualEditWithIncludeTest(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        // Create virtual server page
        Utils.createDoc(hibstore, "XWiki", "XWikiServerLocalhost", context);
        Utils.setStringValue("XWiki.XWikiServerLocalhost", "XWiki.XWikiServerClass", "server", "localhost", context);

        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.VirtualEditWithIncludeTest");
        String content = Utils.content1;
        Utils.content1 = "#includeForm(\"localhost:Main.WebHome\")\r\n";
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);
        Utils.createDoc(hibstore, "Main", "VirtualEditWithIncludeTest", bobject, bclass, context);
        Utils.content1 = content;
        setUrl(webRequest, "edit", "VirtualEditWithIncludeTest");
    }

    public void endVirtualEditWithIncludeTest(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find localhost url Content: " + result, result.indexOf("http://localhost")!=-1);
        } finally {
            clientTearDown();
        }
    }

    public void testVirtualEditWithIncludeTest() throws Throwable {
        launchTest();
    }


}
