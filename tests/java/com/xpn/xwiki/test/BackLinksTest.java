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
 * @author thomas
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiServletURLFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BackLinksTest extends HibernateTestCase {

    protected void setUp() throws Exception {
        super.setUp();
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
    }

    public void testSimpleBackLinksExtract() throws XWikiException {
        List expected_list = (new ArrayList());
        testSimpleBackLinks("There is no link in this content", expected_list);
    }

    public void testSimpleBackLinksExtract1() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.B");
        testSimpleBackLinks("[B]", expected_list);
    }

    public void testSimpleBackLinksExtract2() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.C");
        testSimpleBackLinks("[C]", expected_list);
    }

    public void testSimpleBackLinksExtract3() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinks("[B] \n [C] \n", expected_list);
    }

    public void testSimpleBackLinksExtract4() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinks("[C] [B]", expected_list);
    }

    public void testSimpleBackLinksExtract5() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinks("[C] [B] [C]", expected_list);
    }

    public void testSimpleBackLinksExtract6() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinks("[Test.B] [Test.C]", expected_list);
    }

    public void testSimpleBackLinksExtract7() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinks("[Hello>B] [Hello>C]", expected_list);
    }

    public void testSimpleBackLinksExtract8() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinks("[Hello>Test.B] [Hello>Test.C]", expected_list);
    }

    public void testSimpleBackLinksExtract9() throws XWikiException {
        List expected_list = (new ArrayList());
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinks("[Hello>${doc.web}.B] [Hello>${doc.web}.C]", expected_list);
    }

    public void testSimpleBackLinks (String content, List expected_list) throws XWikiException {
        XWikiDocument testDoc = new XWikiDocument("Test", "A");
        testDoc.setContent(content);
        getXWikiContext().setDoc(testDoc);
        testDoc.getRenderedContent(getXWikiContext());


        List links = (List)getXWikiContext().get("links");
        if (expected_list.size() == 0 && links == null){
            links = new ArrayList();
        }
        assertEquals("Link list size is not correct", expected_list.size(), links.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = (String)expected_list.get(i);
            boolean ok = links.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);

        }
    }

}
