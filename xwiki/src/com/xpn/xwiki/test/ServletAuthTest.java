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
 *
 * User: ludovic
 * Date: 13 mars 2004
 * Time: 15:02:45
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.store.XWikiHibernateStore;
import net.sf.hibernate.HibernateException;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.apache.cactus.client.authentication.Authentication;
import org.apache.cactus.client.authentication.BasicAuthentication;

import java.util.HashMap;


public class ServletAuthTest extends ServletTest {

    public void updateRight(String fullname, String user, String group, String level, boolean allow, boolean global) throws XWikiException {
        Utils.updateRight(xwiki, context, fullname, user, group, level, allow, global);
    }

    public void beginAuthNeeded(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        context.setDatabase("xwikitest");
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "AuthNeededTest", context);
        updateRight("Main.AuthNeededTest", "XWiki.LudovicDubost", "", "view", true, false);
        setUrl(webRequest, "view", "AuthNeededTest");
    }

    public void endAuthNeeded(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 302", 302, webResponse.getStatusCode());
            assertTrue("There should be a redirect to login page", webResponse.getText().indexOf("login")==-1);
        } finally {
            clientTearDown();
        }

    }

    public void testAuthNeeded() throws Throwable {
        launchTest();
    }

    public void beginAuth(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", "WebHome", context);
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("LudovicDubost", map, context);
        updateRight("Main.WebHome", "XWiki.LudovicDubost", "", "view", true, false);
        setUrl(webRequest, "login", "XWiki", "XWikiLogin", "");
        webRequest.addParameter("j_username", "LudovicDubost");
        webRequest.addParameter("j_password", "toto");
        webRequest.addParameter("j_rememberme", "true");
        // Authentication auth = new BasicAuthentication("LudovicDubost", "toto");
        // webRequest.setAuthentication(auth);
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


    public void beginCreateUser(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "$xwiki.createUser()";
        Utils.createDoc(hibstore, "Main", "CreateUserTest", context);
        Utils.content1 = content;

        // In order for createUser to work, we need programming right
        Utils.createDoc(hibstore, "XWiki", "XWikiPreferences", context);
        updateRight("XWiki.XWikiPreferences", "XWiki.LudovicDubost", "", "programming", true, true);

        setUrl(webRequest, "view", "CreateUserTest");
        webRequest.addParameter("xwikiname","LudovicDubost");
        webRequest.addParameter("register_password","toto");
        webRequest.addParameter("register2_password","toto");
        webRequest.addParameter("register_email","ludovic@pobox.com");
        webRequest.addParameter("register_fullname","Ludovic Dubost");
    }

    public void endCreateUser(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
            XWikiSimpleDoc doc = new XWikiSimpleDoc("XWiki", "LudovicDubost");
            doc = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc, context);
            assertFalse("User should exist", doc.isNew());
            assertEquals("Password is wrong", "toto", doc.getObject("XWiki.XWikiUsers",0).getStringValue("password"));
            assertEquals("Email is wrong", "ludovic@pobox.com", doc.getObject("XWiki.XWikiUsers",0).getStringValue("email"));
            assertEquals("Fullname is wrong", "Ludovic Dubost", doc.getObject("XWiki.XWikiUsers",0).getStringValue("fullname"));
        } finally {
            clientTearDown();
        }
    }

    public void testCreateUser() throws Throwable {
        launchTest();
    }


    public void beginCreateUserNoRight(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "$xwiki.createUser()";
        Utils.createDoc(hibstore, "Main", "CreateUserTest", context);
        Utils.content1 = content;

        setUrl(webRequest, "view", "CreateUserTest");
        webRequest.addParameter("xwikiname","LudovicDubost");
        webRequest.addParameter("register_password","toto");
        webRequest.addParameter("register2_password","toto");
        webRequest.addParameter("register_email","ludovic@pobox.com");
        webRequest.addParameter("register_fullname","Ludovic Dubost");
    }

    public void endCreateUserNoRight(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
            XWikiSimpleDoc doc = new XWikiSimpleDoc("XWiki", "LudovicDubost");
            doc = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc, context);
            assertTrue("User should not exist", doc.isNew());
        } finally {
            clientTearDown();
        }

    }

    public void testCreateUserNoRight() throws Throwable {
        launchTest();
    }


    public void beginCreateUserFail(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "$xwiki.createUser()";
        Utils.createDoc(hibstore, "Main", "CreateUserTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "CreateUserTest");
        webRequest.addParameter("xwikiname","LudovicDubost");
        webRequest.addParameter("register_password","toto");
        webRequest.addParameter("register2_password","tata");
        webRequest.addParameter("register_email","ludovic@pobox.com");
        webRequest.addParameter("register_fullname","Ludovic Dubost");
    }

    public void endCreateUserFail(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
            XWikiSimpleDoc doc = new XWikiSimpleDoc("XWiki", "LudovicDubost");
            doc = (XWikiSimpleDoc) hibstore.loadXWikiDoc(doc, context);
            assertTrue("User should not exist", doc.isNew());
        } finally {
            clientTearDown();
        }
    }

    public void testCreateUserFail() throws Throwable {
        launchTest();
    }


}