/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.web.XWikiServletURLFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: HAL_2005
 * Date: 11 août 2005
 * Time: 12:30:26
 * To change this template use File | Settings | File Templates.
 */

public class BackLinksIntegrationTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
    }
    
    public void testUniqueBacklink () throws XWikiException {
        List docs = new ArrayList();
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("Bonjour Monde : This is a test " +
                "with links [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        docs.add(doc1);
        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        doc2.setContent("Bonjour Monde : This is a test " +
                "with links [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        docs.add(doc2);
        XWikiDocument doc3 = new XWikiDocument("Test", "BacklinksOutput");
        doc3.setContent("Bonjour Monde : This is a test " +
                " with links [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc3,  getXWikiContext());
        docs.add(doc3);

        List loadedBacklinks = getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);
        List expected_list = new ArrayList();
        expected_list.add("Test.BacklinksTarget");
        expected_list.add("Test.BacklinksInput");
        expected_list.add("Test.BacklinksOutput");

        assertEquals("lists size are not equal", 3, loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = (String)expected_list.get(i);
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }
    }

    public void testUniqueConfusedBacklink () throws XWikiException {

        List docs = new ArrayList();
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        // blank database for this doc
//        getXWiki().getStore().deleteXWikiDoc(doc1, getXWikiContext());
        doc1.setContent("Bonjour Monde : this is the content of doc1 " +
                "with links [Test.AAA] [Test.BBBB]"+
                " Hello World  : [Test.AAA] [Test.AA] [Test.AAAA] [Test. AAA] " +
                " [Test. AA] [Test.  AAAA] [Test..AA]");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        docs.add(doc1);
        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        // blank database for this doc
        //      getXWiki().getStore().deleteXWikiDoc(doc2, getXWikiContext());
        doc2.setContent("Bonjour Monde : this is the content of doc2 " +
                "with links [Test.AAA] [Test.BBBB] " +
                " Hello World : [Test.B] [Test.AAA]");
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        docs.add(doc2);
        XWikiDocument doc3 = new XWikiDocument("Test", "BacklinksOutput");
        // blank database for this doc
        //     getXWiki().getStore().deleteXWikiDoc(doc3, getXWikiContext());
        doc3.setContent("Bonjour Monde : this is the content of doc3 " +
                "with links [Test.AAA] [Test.BBBB] " +
                " Hello World : [Test.HelloWorld] [ Test. A A A ] + [[Test.AAA] ");
        getXWiki().getStore().saveXWikiDoc(doc3,  getXWikiContext());
        docs.add(doc3);

        List loadedBacklinks =getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);
        List expected_list = new ArrayList();
        expected_list.add("Test.BacklinksTarget");
        expected_list.add("Test.BacklinksInput");
        expected_list.add("Test.BacklinksOutput");

        assertEquals("lists size are not equal", 3, loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }
    }

    public void testRemoveLinksInOneDoc () throws XWikiException {
        List docs = new ArrayList();
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        docs.add(doc1);
        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        doc2.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        docs.add(doc2);
        XWikiDocument doc3 = new XWikiDocument("Test", "BacklinksOutput");
        doc3.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc3,  getXWikiContext());
        docs.add(doc3);

        List loadedBacklinks =getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);
        List expected_list = new ArrayList();
        expected_list.add("Test.BacklinksTarget");
        expected_list.add("Test.BacklinksInput");
        expected_list.add("Test.BacklinksOutput");

        assertEquals("lists size are not equal", 3, loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        doc1.setContent("Now we delete these links in that doc1");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        expected_list.remove("Test.BacklinksTarget");
        loadedBacklinks = getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);

        assertEquals("lists size are not equal", 2, loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }
    }

    public void testModifyLinksInOneDoc () throws XWikiException {
        List docs = new ArrayList();
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        docs.add(doc1);
        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        doc2.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        docs.add(doc2);
        XWikiDocument doc3 = new XWikiDocument("Test", "BacklinksOutput");
        doc3.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc3,  getXWikiContext());
        docs.add(doc3);

        List loadedBacklinks =getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);
        List expected_list = new ArrayList();
        expected_list.add("Test.BacklinksTarget");
        expected_list.add("Test.BacklinksInput");
        expected_list.add("Test.BacklinksOutput");

        assertEquals("lists size are not equal", 3, loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        doc1.setContent("Now we delete these links in that doc1");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        expected_list.remove("Test.BacklinksTarget");

        doc2.setContent(doc2.getContent().concat("[Test.AA] and [Test.AAAA]"));
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        loadedBacklinks = getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);

        assertEquals("lists size are not equal", 2, loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        List loadedLinks = getXWiki().getStore().loadLinks(doc2.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 4, loadedLinks.size());
        expected_list.clear();
        expected_list.add("Test.AA");
        expected_list.add("Test.AAA");
        expected_list.add("Test.AAAA");
        expected_list.add("Test.BBBB");
        for (int i=0;i<expected_list.size();i++) {
            String item = ((XWikiLink)loadedLinks.get(i)).getLink();
            boolean ok = expected_list.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }
    }

    public void testRemoveDoc () throws XWikiException {
        List docs = new ArrayList();
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        docs.add(doc1);
        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        doc2.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        docs.add(doc2);
        XWikiDocument doc3 = new XWikiDocument("Test", "BacklinksOutput");
        doc3.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB]");
        getXWiki().getStore().saveXWikiDoc(doc3,  getXWikiContext());
        docs.add(doc3);

        List loadedBacklinks =getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);
        List expected_list = new ArrayList();
        expected_list.add("Test.BacklinksTarget");
        expected_list.add("Test.BacklinksInput");
        expected_list.add("Test.BacklinksOutput");

        assertEquals("lists size are not equal", 3, loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        getXWiki().getStore().deleteXWikiDoc(doc1,  getXWikiContext());
        expected_list.remove("Test.BacklinksTarget");
        loadedBacklinks = getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);

        assertEquals("lists size are not equal", 2, loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        List loadedLinks = getXWiki().getStore().loadLinks(doc1.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 0, loadedLinks.size());
    }

    public void testVelocityRendering () throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "Main");
        doc1.setContent("This is the main page of my test web");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());

        doc1.setContent(doc1.getContent().concat(" \n  " +
           "##now we add a velocity command \n" +
           "#set($backlinks = $doc.getBacklinks())\n" +
           "<table>" +
               " #foreach( $backlink in $backlinks )\n" +
               "<tr><td>$backlink</td></tr>" +
               " #end " +
           "</table>"));

        getXWiki().getDocument(doc1.getFullName(), getXWikiContext()).getRenderedContent(getXWikiContext());
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        getXWikiContext().setAction("view") ;

    }
}
