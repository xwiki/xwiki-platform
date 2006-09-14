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
 * @author Phung Hai Nam (phunghainam@xwiki.com)
 * @version 5 Sep 2006
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.hibernate.HibernateException;

import java.util.HashMap;
import java.net.URL;
import java.net.MalformedURLException;

public class ServletCaptchaTest  extends ServletTest {
    public void setUp() throws Exception {
        super.setUp();
    }

    public void cleanUp() {
        super.cleanUp();
    }

    // ====================== Test show a jcaptcha image to confirm for edit by anonymous ==============================
    public void beginShowCaptchaImageForAnonymousEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "Image", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        setUrl(webRequest, "edit", "WebHome", "");
    }

    public void endShowCaptchaImageForAnonymousEdit(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find captcha image : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
       } finally {
            clientTearDown();
       }
    }

    public void testShowCaptchaImageForAnonymousEdit() throws Throwable {
        launchTest();
    }

    // ====================== Test does not show a captcha  when preview edit by anonymous ==============================
    public void beginNotShowCaptchaForAnonymousPreviewEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "Image", context);
        Utils.setStringValue("XWiki.XWikiPreferences", "edit_registered", "Image", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        setUrl(webRequest, "preview", "Main", "WebHome", "");
    }

    public void endNotShowCaptchaForAnonymousPreviewEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertFalse("Find out captcha image  : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
            assertFalse("Find out simple math question : " + result, result.indexOf("Please answer this simple math question") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testNotShowCaptchaForAnonymousPreviewEdit() throws Throwable {
        launchTest();
    }

    // ====================== Test show a simple question to confirm for edit by anonymous =============================
    public void beginShowCaptchaTextForAnonymousEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "Text", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        setUrl(webRequest, "edit", "WebHome", "");
    }

    public void endShowCaptchaTextForAnonymousEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find simple math question : " + result, result.indexOf("Please answer this simple math question") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testShowCaptchaTextForAnonymousEdit() throws Throwable {
        launchTest();
    }

    // ======================== Test don't show jcaptcha to confirm for edit by anonymous ==============================
    public void beginNotShowCaptchaImageForAnonymousEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "---", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        setUrl(webRequest, "edit", "WebHome", "");
    }

    public void endNotShowCaptchaImageForAnonymousEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertFalse("Find out captcha image  : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
            assertFalse("Find out simple math question : " + result, result.indexOf("Please answer this simple math question") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testNotShowCaptchaImageForAnonymousEdit() throws Throwable {
        launchTest();
    }

    // ============ Verify the captcha does not show up for registered users when configured on anonymous ==============
    public void beginNotShowCaptchaForRegisteredUserEdit(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "Emage", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome1", context);
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("PhungHaiNam", map, "", "", "view, edit", context);
        Utils.updateRight(xwiki, context, "Main.WebHome1", "XWiki.PhungHaiNam", "", "edit", true, false);

        MyFormAuthentication auth = new MyFormAuthentication("PhungHaiNam", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/loginsubmit/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
        setUrl(webRequest, "edit", "WebHome1", "");
    }

    public void endNotShowCaptchaForRegisteredUserEdit(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome1 Content: " + result, result.indexOf("Hello 1")!=-1);
            assertFalse("Find out captcha image  : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
            assertFalse("Find out simple math question : " + result, result.indexOf("Please answer this simple math question") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testNotShowCaptchaForRegisteredUserEdit() throws Throwable {
        launchTest();
    }

    // =================== Test for require captcha again when comfirm to edit is not correct ==========================
    public void beginCaptchaNotCorrectForAnonymousEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "Image", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        setUrl(webRequest, "save", "WebHome", "");
        webRequest.addParameter("content", "This is modification");
        webRequest.addParameter("parent", "Main.WebHome");
        webRequest.addParameter("jcaptcha_response", "false");
    }

    public void endCaptchaNotCorrectForAnonymousEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find captcha form : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testCaptchaNotCorrectForAnonymousEdit() throws Throwable {
        launchTest();
    }

    // ====================== Test show a jcaptcha image to confirm for edit by registered user ========================
    public void beginShowCaptchaImageForRegisteredUserEdit(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.setStringValue("XWiki.XWikiPreferences", "edit_registered", "Image", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome1", context);
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("PhungHaiNam", map, "", "", "view, edit", context);
        Utils.updateRight(xwiki, context, "Main.WebHome1", "XWiki.PhungHaiNam", "", "edit", true, false);

        MyFormAuthentication auth = new MyFormAuthentication("PhungHaiNam", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/loginsubmit/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
        setUrl(webRequest, "edit", "WebHome1", "");
    }

    public void endShowCaptchaImageForRegisteredUserEdit(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome1 Content: " + result, result.indexOf("Hello 1")!=-1);
            assertTrue("Could not find captcha image : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
       } finally {
            clientTearDown();
       }
    }

    public void testShowCaptchaImageForRegisteredUserEdit() throws Throwable {
        launchTest();
    }

    // ====================== Test show a jcaptcha image to confirm for edit by registered user ========================
    public void beginShowCaptchaTextForRegisteredUserEdit(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.setStringValue("XWiki.XWikiPreferences", "edit_registered", "Text", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome1", context);
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("PhungHaiNam", map, "", "", "view, edit", context);
        Utils.updateRight(xwiki, context, "Main.WebHome1", "XWiki.PhungHaiNam", "", "edit", true, false);

        MyFormAuthentication auth = new MyFormAuthentication("PhungHaiNam", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/loginsubmit/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
        setUrl(webRequest, "edit", "WebHome1", "");
    }

    public void endShowCaptchaTextForRegisteredUserEdit(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome1 Content: " + result, result.indexOf("Hello 1")!=-1);
            assertTrue("Could not find simple math question : " + result, result.indexOf("Please answer this simple math question") != -1);
       } finally {
            clientTearDown();
       }
    }

    public void testShowCaptchaTextForRegisteredUserEdit() throws Throwable {
        launchTest();
    }

    // ====================== Test don't show a jcaptcha to confirm for edit by Administrator  =========================
    public void beginNotShowCaptchaTextForAdminEdit(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.setStringValue("XWiki.XWikiPreferences", "edit_registered", "Text", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome1", context);
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("Admin", map, "", "", "view, edit, admin", context);
        Utils.updateRight(xwiki, context, "Main.WebHome1", "XWiki.Admin", "", "edit", true, false);

        MyFormAuthentication auth = new MyFormAuthentication("admin", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/loginsubmit/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
        setUrl(webRequest, "edit", "WebHome1", "");
    }

    public void endNotShowCaptchaTextForAdminEdit(WebResponse webResponse) throws HibernateException {
        try {
            assertEquals("Response status should be 200", 200, webResponse.getStatusCode());
            String result = webResponse.getText();
            assertTrue("Could not find WebHome1 Content: " + result, result.indexOf("Hello 1")!=-1);
            assertFalse("Find out captcha image  : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
            assertFalse("Find out simple math question : " + result, result.indexOf("Please answer this simple math question") != -1);
       } finally {
            clientTearDown();
       }
    }

    public void testNotShowCaptchaTextForAdminEdit() throws Throwable {
        launchTest();
    }

    // ====================== Test don't show a jcaptcha again to when admin save modifications ========================
    public void beginNotShowCaptchaAgainForAdminEdit(WebRequest webRequest) throws HibernateException, XWikiException, MalformedURLException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        Utils.setStringValue("XWiki.XWikiPreferences", "edit_registered", "Text", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        HashMap map = new HashMap();
        map.put("password", "toto");
        xwiki.createUser("Admin", map, "", "", "view, edit, save", context);
        Utils.updateRight(xwiki, context, "Main.WebHome", "XWiki.Admin", "", "save", true, false);

        MyFormAuthentication auth = new MyFormAuthentication("admin", "toto");
        auth.setSecurityCheckURL(new URL("http://127.0.0.1:9080/xwiki/testbin/loginsubmit/XWiki/XWikiLogin"));
        webRequest.setAuthentication(auth);
        setUrl(webRequest, "save", "WebHome", "");

    }

    public void endNotShowCaptchaAgainForAdminEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertFalse("Find out captcha image  : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
            assertFalse("Find out simple math question : " + result, result.indexOf("Please answer this simple math question") != -1);
       } finally {
            clientTearDown();
       }
    }

    public void testNotShowCaptchaAgainForAdminEdit() throws Throwable {
        launchTest();
    }
}
