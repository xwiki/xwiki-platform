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
 * Date: 18 avr. 2004
 * Time: 10:44:14
 */

package com.xpn.xwiki.test;

import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.apache.cactus.Cookie;
import net.sf.hibernate.HibernateException;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.i18n.i18n;
import com.xpn.xwiki.store.XWikiHibernateStore;

import java.util.HashMap;
import java.net.URL;
import java.net.MalformedURLException;

public class Servleti18nTest extends ServletTest {

    public void createLocPage(String name) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", name, context);
        XWikiDocInterface doc2 = new XWikiSimpleDoc("Main", name);
        doc2.setContent("Bonjour 1. Bonjour 2. Bonjour 3");
        doc2.setLanguage(i18n.LANGUAGE_FRENCH);
        doc2.setTranslation(i18n.TRANSLATION_CONTENT);
        xwiki.saveDocument(doc2, context);
    }

    public void beginViewWithLocalized(WebRequest webRequest) throws HibernateException, XWikiException {
        createLocPage("ViewLocTest");
        setUrl(webRequest, "view", "ViewLocTest");
    }

    public void endViewWithLocalized(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content", result.indexOf("Hello")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewWithLocalized() throws Throwable {
        launchTest();
    }


    public void beginViewLocalizedWithParam(WebRequest webRequest) throws HibernateException, XWikiException {
        createLocPage("ViewLocTest");
        webRequest.addHeader("Accept-Language", "en");

        Cookie cookie = new Cookie("127.0.0.1", "language", "en");
        cookie.setPath("/");
        webRequest.addCookie(cookie);

        // This one should win
        webRequest.addParameter("language", "fr");
        setUrl(webRequest, "view", "ViewLocTest");
    }

    public void endViewLocalizedWithParam(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content", result.indexOf("Bonjour")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewLocalizedWithParam() throws Throwable {
        launchTest();
    }

    public void beginViewLocalizedWithAccept(WebRequest webRequest) throws HibernateException, XWikiException {
        createLocPage("ViewLocTest");
        // This one should win
        webRequest.addHeader("Accept-Language", "fr");
        setUrl(webRequest, "view", "ViewLocTest");
    }

    public void endViewLocalizedWithAccept(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content", result.indexOf("Bonjour")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewLocalizedWithAccept() throws Throwable {
        launchTest();
    }

    public void beginViewLocalizedWithCookie(WebRequest webRequest) throws HibernateException, XWikiException {
        createLocPage("ViewLocTest");
        webRequest.addHeader("Accept-Language", "en");

        // This one should win
        Cookie cookie = new Cookie("127.0.0.1", "language", "fr");
        cookie.setPath("/");
        webRequest.addCookie(cookie);
        setUrl(webRequest, "view", "ViewLocTest");
    }

    public void endViewLocalizedWithCookie(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content", result.indexOf("Bonjour")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewLocalizedWithCookie() throws Throwable {
        launchTest();
    }


    public void beginViewLocalizedWithUserParam(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        createLocPage("ViewLocTest");
        webRequest.addHeader("Accept-Language", "en");

        // This one should win
        HashMap map = new HashMap();
        map.put("password", "toto");
        map.put("default_language", "fr");
        xwiki.createUser("LudovicDubost", map, "", "", context);

        setUrl(webRequest, "view", "ViewLocTest");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endViewLocalizedWithUserParam(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content", result.indexOf("Bonjour")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewLocalizedWithUserParam() throws Throwable {
        launchTest();
    }

    public void beginEditLocalized(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        createLocPage("EditLocTest");
        setUrl(webRequest, "save", "EditLocTest");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("language", "fr");
    }

    public void endEditLocalized(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            // Make sure we read from the db
            xwiki.flushCache();

            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content", result.indexOf("Exception")==-1);

            XWikiDocInterface doc = xwiki.getDocument("Main.EditLocTest", context);
            assertNotSame("English version should be unchanged", "toto", doc.getContent());
            assertEquals("French version should be changed", "toto", doc.getTranslatedContent("fr", context));
        } finally {
            clientTearDown();
        }

    }

    public void testEditLocalized() throws Throwable {
        launchTest();
    }

    public void beginEditLocalized2(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        createLocPage("EditLocTest2");
        setUrl(webRequest, "save", "EditLocTest2");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("language", "es");
    }

    public void endEditLocalized2(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            // Make sure we read from the db
            xwiki.flushCache();

            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content", result.indexOf("Exception")==-1);

            XWikiDocInterface doc = xwiki.getDocument("Main.EditLocTest2", context);
            assertNotSame("English version should be unchanged", "toto", doc.getContent());
            assertNotSame("French version should be unchanged", "toto", doc.getTranslatedContent("fr", context));
            assertEquals("Spanish version should be changed", "toto", doc.getTranslatedContent("es", context));
        } finally {
            clientTearDown();
        }
    }

    public void testEditLocalized2() throws Throwable {
        launchTest();
    }

}
