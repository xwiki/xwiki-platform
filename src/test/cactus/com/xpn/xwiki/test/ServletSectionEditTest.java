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
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.hibernate.HibernateException;

public class ServletSectionEditTest extends ServletTest {

    public void setUp() throws Exception {
        super.setUp();
    }

    public void cleanUp() {
        super.cleanUp();
    }

    // Check if the "Edit" button for a section is present
    public void beginPresentEdit(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);

        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PresentSectionEditTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1.1 This is the subtitle 2\nThis is content of subtitle 2\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "PresentSectionEditTest", content, bobject, bclass, context);

        setUrl(webRequest, "view", "Main", "PresentSectionEditTest", "");
    }

    public void endPresentEdit(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Content should have edit button for title 1 : " + result, result.indexOf("<a style='text-decoration: none;' title='Edit section: This is title 1' href='/xwiki/testbin/edit/Main/PresentSectionEditTest?language=en&section=1'>edit</a>") != -1);
            assertTrue("Content should have edit button for subtitle 1 : " + result, result.indexOf("<a style='text-decoration: none;' title='Edit section: This is the subtitle 1' href='/xwiki/testbin/edit/Main/PresentSectionEditTest?language=en&section=2'>edit</a>") != -1);
            assertTrue("Content should have edit button for subtitle 2 : " + result, result.indexOf("<a style='text-decoration: none;' title='Edit section: This is the subtitle 2' href='/xwiki/testbin/edit/Main/PresentSectionEditTest?language=en&section=3'>edit</a>") != -1);
            assertTrue("Content should have edit button for title2 : " + result, result.indexOf("<a style='text-decoration: none;' title='Edit section: This is the title 2' href='/xwiki/testbin/edit/Main/PresentSectionEditTest?language=en&section=4'>edit</a>") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testPresentEdit() throws Throwable {
        launchTest();
    }

    /** Test edit section for title 1 */
    public void beginSectionEditTitle1(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.SectionEditTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "SectionEditTest", content, bobject, bclass, context);
        setUrl(webRequest, "edit", "Main", "SectionEditTest", "section=1");
    }

    public void endSectionEditTitle1(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find the contents of title 1 : " + result, result.indexOf("1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1") != -1);
            assertTrue("Find out the contents of title 2 :" + result, result.indexOf("1 This is the title 2\nThis is the content of title 2") == -1);
       } finally {
            clientTearDown();
       }
    }

    public void testSectionEditTitle1() throws Throwable {
        launchTest();
    }

    /** Test edit section for subtitle 1 */
    public void beginSectionEditSubtitle1(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.SectionEditTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1.1 This is the subtitle 2\nThis is content of subtitle 2\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "SectionEditTest", content, bobject, bclass, context);
        setUrl(webRequest, "edit", "Main", "SectionEditTest", "section=2");
    }

    public void endSectionEditSubtitle1(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find the contents of subtitle 1 : " + result, result.indexOf("1.1 This is the subtitle 1\nThis is content of subtitle 1") != -1);
            assertTrue("Find out the contents of title 1 :" + result, result.indexOf("1 This is title 1\nThis is content of title 1") == -1);
            assertTrue("Find out the contents of subtitle 2 :" + result, result.indexOf("1.1 This is the subtitle 2\nThis is content of subtitle 2") == -1);
            assertTrue("Find out the contents of title 2 :" + result, result.indexOf("1 This is title 2\nThis is content of title 2") == -1);
        } finally {
            clientTearDown();
        }
    }

    public void testSectionEditSubtitle1() throws Throwable {
        launchTest();
    }

    /** Test edit section for subtitle 2 */
    public void beginSectionEditSubtitle2(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.SectionEditTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1.1 This is subtitle 2\nThis is the content of subtitle 2\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "SectionEditTest", content, bobject, bclass, context);
        setUrl(webRequest, "edit", "Main", "SectionEditTest", "section=3");
    }

    public void endSectionEditSubtitle2(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find the contents of sub title 2 : " + result, result.indexOf("1.1 This is subtitle 2\nThis is the content of subtitle 2") != -1);
            assertFalse("Find out the contents of title 1 :" + result, result.indexOf("1 This is title 1\nThis is content of title 1") != -1);
            assertFalse("Find out the contents of subtile 1:" + result, result.indexOf("1.1 This is the subtitle 1\nThis is content of subtitle 1") != -1);
            assertFalse("Find out the contents of title 2 :" + result, result.indexOf("1 This is title 2\nThis is content of title 2") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testSectionEditSubtitle2() throws Throwable {
        launchTest();
    }

    /** Test edit section for Title 2 */
    public void beginSectionEditTitle2(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.SectionEditTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1.1 This is subtitle 2\nThis is the content of subtitle 2\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "SectionEditTest", content, bobject, bclass, context);
        setUrl(webRequest, "edit", "Main", "SectionEditTest", "section=4");
    }

    public void endSectionEditTitle2(WebResponse webResponse) throws HibernateException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find the contents of title 2 : " + result, result.indexOf("1 This is the title 2\nThis is the content of title 2") != -1);
            assertTrue("Find out the contents of title 1 :" + result, result.indexOf("1 This is title 1\nThis is content of title 1") == -1);
            assertTrue("Find out the contents of subtile 1:" + result, result.indexOf("1.1 This is the subtitle 1\nThis is content of subtitle 1") == -1);
            assertTrue("Find out the contents of subtitle 2 :" + result, result.indexOf("1.1 This is subtitle 2\nThis is content of subtitle 2") == -1);
        } finally {
            clientTearDown();
        }
    }

    public void testSectionEditTitle2() throws Throwable {
        launchTest();
    }

    /** Test save a section title after edit it */
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
        webRequest.addParameter("content", "This is modification");
        webRequest.addParameter("parent", "Main.WebHome");
    }

    public void endSaveSection(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);
            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc = xwiki.getDocument("Main.SaveSectionTest", context);
            String content = doc.getContent();
            assertEquals("Content is not indentical", "This is modification\n1 This is the title 2\nThis is the content of title 2", content);
            assertEquals("Parent is not identical", "Main.WebHome", doc.getParent());
        } finally {
            clientTearDown();
        }
    }

    public void testSaveSection() throws Throwable {
        launchTest();
    }

    /** Test save a section subtitle after edit it */
    public void beginSaveSectionSubTitle(WebRequest webRequest) throws HibernateException, XWikiException {
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
        setUrl(webRequest, "save", "Main", "SaveSectionTest", "section=2");
        webRequest.addParameter("content", "This is modification");
        webRequest.addParameter("parent", "Main.WebHome");
    }

    public void endSaveSectionSubTitle(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            // Verify return
            assertTrue("Saving returned exception: " + result, result.indexOf("Exception") == -1);
            // Flush cache to make sure we read from db
            xwiki.flushCache();

            XWikiDocument doc = xwiki.getDocument("Main.SaveSectionTest", context);
            String content = doc.getContent();
            assertEquals("Content is not indentical", "1 This is title 1\nThis is content of title 1\nThis is modification\n1 This is the title 2\nThis is the content of title 2", content);
            assertEquals("Parent is not identical", "Main.WebHome", doc.getParent());
        } finally {
            clientTearDown();
        }
    }

    public void testSaveSectionSubTitle() throws Throwable {
        launchTest();
    }

    /** Test preview section after edit section */
    public void beginPreviewSection(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        XWikiDocument doc = new XWikiDocument();
        Utils.prepareObject(doc, "Main.PreviewSectionTest");
        BaseClass bclass = doc.getxWikiClass();
        BaseObject bobject = doc.getObject(bclass.getName(), 0);

        String content = "1 This is title 1\nThis is content of title 1\n1.1 This is the subtitle 1\nThis is content of subtitle 1\n1 This is the title 2\nThis is the content of title 2";
        Utils.createDoc(xwiki.getStore(), "Main", "PreviewSectionTest", content, bobject, bclass, context);

        // preview section 1 with new content.
        setUrl(webRequest, "preview", "PreviewSectionTest", "section=1");
        webRequest.addParameter("content", "This is modification");
    }

    public void endPreviewSection(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find link back to edit with valid section : " + result, result.indexOf("<a href=\"/xwiki/testbin/edit/Main/PreviewSectionTest?&section=1") != -1);
            assertTrue("Could not find content hidden form value: " + result, result.indexOf("<input type=\"hidden\" name=\"content\" value=\"This is modification\" />") != -1);
        } finally {
            clientTearDown();
        }
    }

    public void testPreviewSection() throws Throwable {
        launchTest();
    }
}
