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

public class ServletCapchaTest  extends ServletTest {
    public void setUp() throws Exception {
        super.setUp();
    }

    public void cleanUp() {
        super.cleanUp();
    }

    public void beginShowCaptchaImageForAnonymousEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "Image", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        setUrl(webRequest, "edit", "Main", "WebHome", "");
    }

    public void endShowCaptchaImageForAnonymousEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find captcha image form : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
       } finally {
            clientTearDown();
       }
    }

    public void testShowCaptchaImageForAnonymousEdit() throws Throwable {
        launchTest();
    }

    public void beginShowCaptchaTextForAnonymousEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "Text", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        setUrl(webRequest, "edit", "Main", "WebHome", "");
    }

    public void endShowCaptchaTextForAnonymousEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find captcha text form : " + result, result.indexOf("Please answer this simple math question") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testShowCaptchaTextForAnonymousEdit() throws Throwable {
        launchTest();
    }

    public void beginCaptchaNotCorrectAnonymous(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        Utils.setStringValue("XWiki.XWikiPreferences", "edit_anonymous", "Image", context);
        Utils.createDoc(xwiki.getStore(), "Main", "WebHome", context);
        setUrl(webRequest, "save", "Main", "WebHome", "");
        webRequest.addParameter("content", "This is modification");
        webRequest.addParameter("parent", "Main.WebHome");
        webRequest.addParameter("jcaptcha_response", "false");
    }

    public void endCaptchaNotCorrectAnonymous(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find captcha form : " + result, result.indexOf("<input type=\"text\" name=\"jcaptcha_response\"") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testCaptchaNotCorrectAnonymous() throws Throwable {
        launchTest();
    }
}
