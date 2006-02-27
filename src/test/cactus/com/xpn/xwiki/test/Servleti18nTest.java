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
 * @author sdumitriu
 */


package com.xpn.xwiki.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import org.apache.cactus.Cookie;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.i18n.i18n;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;

public class Servleti18nTest extends ServletTest {

    public void createLocPage(String name) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.createDoc(hibstore, "Main", name, context);
        XWikiDocument doc2 = new XWikiDocument("Main", name);
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
            assertTrue("Could not find Loc WebHome Content: " + result, result.indexOf("Hello 1")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewWithLocalized() throws Throwable {
        launchTest();
    }


    public void beginViewLocalizedWithParam(WebRequest webRequest) throws HibernateException, XWikiException {
        createLocPage("ViewLocTest");
        webRequest.addHeader("Accept-Language", i18n.LANGUAGE_FRENCH);

        Cookie cookie = new Cookie("127.0.0.1", "language", i18n.LANGUAGE_FRENCH);
        cookie.setPath("/");
        webRequest.addCookie(cookie);

        // This one should win
        webRequest.addParameter("language", i18n.LANGUAGE_FRENCH);
        setUrl(webRequest, "view", "ViewLocTest");
    }

    public void endViewLocalizedWithParam(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content: " + result, result.indexOf("Bonjour")!=-1);
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
        webRequest.addHeader("Accept-Language", i18n.LANGUAGE_FRENCH);
        setUrl(webRequest, "view", "ViewLocTest");
    }

    public void endViewLocalizedWithAccept(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content: " + result, result.indexOf("Bonjour")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewLocalizedWithAccept() throws Throwable {
        launchTest();
    }

    public void beginViewLocalizedWithCookie(WebRequest webRequest) throws HibernateException, XWikiException {
        createLocPage("ViewLocTest");
        webRequest.addHeader("Accept-Language", i18n.LANGUAGE_FRENCH);

        // This one should win
        Cookie cookie = new Cookie("127.0.0.1", "language", i18n.LANGUAGE_FRENCH);
        cookie.setPath("/");
        webRequest.addCookie(cookie);
        setUrl(webRequest, "view", "ViewLocTest");
    }

    public void endViewLocalizedWithCookie(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content: " + result, result.indexOf("Bonjour")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewLocalizedWithCookie() throws Throwable {
        launchTest();
    }


    public void beginViewLocalizedWithUserParam(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        createLocPage("ViewLocTest");
        webRequest.addHeader("Accept-Language", i18n.LANGUAGE_FRENCH);

        // This one should win
        HashMap map = new HashMap();
        map.put("password", "toto");
        map.put("default_language", i18n.LANGUAGE_FRENCH);
        xwiki.createUser("LudovicDubost", map, "", "", "view, edit", context);

        setUrl(webRequest, "view", "ViewLocTest");
        MyFormAuthentication auth = new MyFormAuthentication("LudovicDubost", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/login/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
    }

    public void endViewLocalizedWithUserParam(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content: " + result, result.indexOf("Bonjour")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testViewLocalizedWithUserParam() throws Throwable {
        launchTest();
    }

    public void beginEditLocalized(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        createLocPage("EditLocTest");
        setUrl(webRequest, "edit", "EditLocTest");
        webRequest.addParameter("language", i18n.LANGUAGE_FRENCH);
    }

    public void endEditLocalized(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content: " + result, result.indexOf("Bonjour")!=-1);
        } finally {
            clientTearDown();
        }

    }

    public void testEditLocalized() throws Throwable {
        launchTest();
    }

    public void beginSaveLocalized(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        createLocPage("EditLocTest");
        setUrl(webRequest, "save", "EditLocTest");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("language", i18n.LANGUAGE_FRENCH);
    }

    public void endSaveLocalized(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            // Make sure we read from the db
            xwiki.flushCache();

            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content: " + result, result.indexOf("Exception")==-1);

            XWikiDocument doc = xwiki.getDocument("Main.EditLocTest", context);
            assertNotSame("English version should be unchanged", "toto", doc.getContent());
            assertEquals("French version should be changed", "toto", doc.getTranslatedContent(i18n.LANGUAGE_FRENCH, context));
        } finally {
            clientTearDown();
        }

    }

    public void testSaveLocalized() throws Throwable {
        launchTest();
    }

    public void beginSaveLocalized2(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        createLocPage("EditLocTest2");
        setUrl(webRequest, "save", "EditLocTest2");
        webRequest.addParameter("content", "toto");
        webRequest.addParameter("language", "es");
    }

    public void endSaveLocalized2(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            // Make sure we read from the db
            xwiki.flushCache();
            String result = webResponse.getText();
            assertTrue("Could not find Loc WebHome Content: " + result, result.indexOf("Exception")==-1);

            XWikiDocument doc = xwiki.getDocument("Main.EditLocTest2", context);
            assertNotSame("English version should be unchanged", "toto", doc.getContent());
            assertNotSame("French version should be unchanged", "toto", doc.getTranslatedContent(i18n.LANGUAGE_FRENCH, context));
            assertEquals("Spanish version should be changed", "toto", doc.getTranslatedContent("es", context));
        } finally {
            clientTearDown();
        }
    }

    public void testSaveLocalized2() throws Throwable {
        launchTest();
    }


    public void testDeleteWithTranslation() throws Throwable {
        launchTest();
    }

    public void beginDeleteWithTranslation(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.createDoc(getHibStore(), "Test", "DeleteWithTranslationTest", context);
        XWikiDocument doc = xwiki.getDocument("Test.DeleteWithTranslationTest", context);
        doc.setDefaultLanguage(i18n.LANGUAGE_ENGLISH);
        xwiki.saveDocument(doc, context);
        XWikiDocument doc2 = new XWikiDocument("Test", "DeleteWithTranslationTest");
        doc2.setContent("Bonjour 1. Bonjour 2. Bonjour 3");
        doc2.setLanguage(i18n.LANGUAGE_FRENCH);
        doc2.setTranslation(i18n.TRANSLATION_CONTENT);
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "delete", "Test", "DeleteWithTranslationTest", "");
        webRequest.addParameter("confirm","1");
    }

    public void endDeleteWithTranslation(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Delete returned exception: " + result, result.indexOf("Exception")==-1);

            // Flush cache to check deletions
            xwiki.flushCache();

            XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
            XWikiDocument doc2 = new XWikiDocument("Test", "DeleteWithTranslationTest");
            doc2 = (XWikiDocument) hibstore.loadXWikiDoc(doc2, context);
            assertTrue("Document should not exist", doc2.isNew());

            // Check if the documentation translation was also removed
            Utils.createDoc(getHibStore(), "Test", "DeleteWithTranslationTest", context);
            XWikiDocument doc = xwiki.getDocument("Test.DeleteWithTranslationTest", context);
            doc.setDefaultLanguage(i18n.LANGUAGE_ENGLISH);
            xwiki.saveDocument(doc, context);
            assertNotSame("Translated content should not be french",
                          doc2.getTranslatedContent(i18n.LANGUAGE_FRENCH, context), "Bonjour 1. Bonjour 2. Bonjour 3");
        } finally {
            clientTearDown();
        }
    }

    public void testDeleteTranslation() throws Throwable {
        launchTest();
    }

    public void beginDeleteTranslation(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.createDoc(getHibStore(), "Test", "DeleteTranslationTest", context);
        XWikiDocument doc = xwiki.getDocument("Test.DeleteTranslationTest", context);
        doc.setDefaultLanguage(i18n.LANGUAGE_ENGLISH);
        xwiki.saveDocument(doc, context);
        XWikiDocument doc2 = new XWikiDocument("Test", "DeleteTranslationTest");
        doc2.setContent("Bonjour 1. Bonjour 2. Bonjour 3");
        doc2.setLanguage(i18n.LANGUAGE_FRENCH);
        doc2.setTranslation(i18n.TRANSLATION_CONTENT);
        xwiki.saveDocument(doc2, context);
        setUrl(webRequest, "delete", "Test", "DeleteTranslationTest", "");
        webRequest.addParameter("confirm","1");
        webRequest.addParameter("language", i18n.LANGUAGE_FRENCH);
    }

    public void endDeleteTranslation(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Delete returned exception: " + result, result.indexOf("Exception")==-1);

            // Flush cache to check deletions
            xwiki.flushCache();

            XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
            XWikiDocument doc2 = new XWikiDocument("Test", "DeleteTranslationTest");
            doc2 = (XWikiDocument) hibstore.loadXWikiDoc(doc2, context);

            assertFalse("Document should exist", doc2.isNew());
            assertNotSame("Translated content should not be french",
                          doc2.getTranslatedContent(context), "Bonjour 1. Bonjour 2. Bonjour 3");

        } finally {
            clientTearDown();
        }
    }

    // TODO: Switch translation to change the default language of a document
    // We haven't implemented switch translation yet.
    /*
    public void testSwitchTranslation() throws Throwable {
        launchTest();
    }

    public void beginSwitchTranslation(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.createDoc(getHibStore(), "Test", "SwitchTranslationTest", context);
        XWikiDocument doc = xwiki.getDocument("Test.SwitchTranslationTest", context);
        doc.setDefaultLanguage(i18n.LANGUAGE_ENGLISH);
        xwiki.saveDocument(doc, context);
        XWikiDocument doc2 = new XWikiDocument("Test", "SwitchTranslationTest");
        doc2.setContent("Bonjour 1. Bonjour 2. Bonjour 3");
        doc2.setLanguage(i18n.LANGUAGE_FRENCH);
        doc2.setTranslation(i18n.TRANSLATION_CONTENT);
        xwiki.saveDocument(doc2, context);

        List translations = doc2.getTranslationList(context);
        assertEquals("Translation list should have 1 translation", 1, translations.size());
        assertEquals("First translation should be french", "fr", translations.get(0));

        setUrl(webRequest, "switch", "Test", "SwitchTranslationTest", "");
        webRequest.addParameter("language","fr");
    }

    public void endSwitchTranslation(WebResponse webResponse) throws XWikiException, HibernateException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Switch returned exception: " + result, result.indexOf("Exception")==-1);

            // Flush cache to check deletions
            xwiki.flushCache();

            XWikiStoreInterface hibstore = new XWikiHibernateStore(getHibpath());
            XWikiDocument doc2 = new XWikiDocument("Main", "SwitchTranslationTest");
            doc2 = (XWikiDocument) hibstore.loadXWikiDoc(doc2, context);
            assertFalse("Document should exist", doc2.isNew());

            List translations = doc2.getTranslationList(context);
            assertEquals("Translation list should still have 1 translation", 1, translations.size());
            assertEquals("First translation should be english", "en", translations.get(0));

            // Check if the documentation translation is now french
            assertEquals("Default content should now be french",
                          doc2.getContent(), "Bonjour 1. Bonjour 2. Bonjour 3");
        } finally {
            clientTearDown();
        }
    }
    */
}
