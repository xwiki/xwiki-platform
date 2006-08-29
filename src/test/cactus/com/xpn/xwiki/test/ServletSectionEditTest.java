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
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.hibernate.HibernateException;

/**
 * @author Phung Hai Nam
 * @version 28 Aug 2006
 */
public class ServletSectionEditTest extends ServletTest {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void cleanUp() {
        super.cleanUp();
    }

    public void beginSectionEditOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.SectionEditTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "SectionEditTest", content, bobject, bclass, context);
        setUrl(webRequest, "edit", "SectionEditTest", "section=1");
    }

    public void endSectionEditOk(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find the contents of title 1 : " + result, result.indexOf("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1") != -1);
            assertTrue("Find out the contents of title 2 :" + result, result.indexOf("1 This is the title 2\nThis is the content of title 2") == -1);
       } finally {
            clientTearDown();
        }
    }

    public void testSectionEditOk() throws Throwable {
        launchTest();
    }

    /** Test save a section after edit it */
    public void beginSaveSection(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.SaveSectionTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "SaveSectionTest", content, bobject, bclass, context);

        // Save section 1 with new content.
        setUrl(webRequest, "save", "SaveSectionTest", "section=1");
        webRequest.addParameter("content", "This is modification\n");
        webRequest.addParameter("parent", "Main.SaveSectionTest");
    }

    public void endSaveSection(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);
            //  Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc = xwiki.getDocument("Main.SaveSectionTest", context);
            String content = doc.getContent();
            assertEquals("Content is not indentical", "This is modification\n1 This is the title 2\nThis is the content of title 2", content);
            assertEquals("Parent is not identical", "Main.SaveSectionTest", doc.getParent());
        } finally {
            clientTearDown();
        }
    }

    public void testSaveSection() throws Throwable {
        launchTest();
    }

    /** Test preview section after edit section */
    public void beginPreviewSectionOk(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PreviewSectionOkTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "PreviewSectionOkTest", content, bobject, bclass, context);

        // preview section 1 with new content.
        setUrl(webRequest, "preview", "PreviewSectionOkTest", "section=1");
        webRequest.addParameter("content", "This is modification\n");
    }

    public void endPreviewSectionOk(WebResponse webResponse) throws HibernateException, XWikiException {
         try {
            String result = webResponse.getText();
            assertTrue("Could not find PreviewSectionOkTest Content: " + result, result.indexOf("This is modification\n1 This is the title 2\nThis is the content of title 2") != -1);
            assertTrue("Could not find object hidden form value: " + result, result.indexOf("Main.PreviewSectionOkTest_0_first_name") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testPreviewSectionOk() throws Throwable {
        launchTest();
    }
}
