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

import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.hibernate.HibernateException;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;

public class ServletCaptchaTest extends ServletTest {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void cleanUp() {
        super.cleanUp();
    }

    // This test for show captcha when edit to avoid spam robost
    public void beginShowCaptchaForEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.ShowCaptchaForEditTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        Utils.createDoc(xwiki.getStore(), "Main", "ShowCaptchaForEditTest", bobject, bclass, context);
        setUrl(webRequest, "edit", "Main", "ShowCaptchaForEditTest", "");
    }

    public void endShowCaptchaForEdit(WebResponse webResponse) throws HibernateException, XWikiException {
         try {
            String result = webResponse.getText();
            assertTrue("Content should have captcha for confirm edit to avoid spam robots : " + result, result.indexOf("<div id=\"captcha\">") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testShowCaptchaForEdit() throws Throwable {
        launchTest();
    }
}
