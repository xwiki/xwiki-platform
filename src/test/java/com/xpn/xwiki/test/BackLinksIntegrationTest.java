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
 * @author ludovic
 * @author thomas
 */

package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.api.XWiki;
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

    protected void getConfig() {
        super.getConfig();
        this.config.put("xwiki.backlinks", "1");
    }

    public void testBacklinksParam() throws XWikiException {

        getXWiki().getConfig().setProperty("xwiki.backlinks","");
        XWikiDocument doc = getXWiki().getDocument("XWiki.XWikiPreferences", getXWikiContext());
        BaseObject object = new BaseObject();
        object.setStringValue("backlinks", "0");
        doc.setObject("XWiki.XWikiPreferences", 1, object);
        assertTrue("Backlinks are activated", !getXWiki().hasBacklinks(getXWikiContext()));

        object.setStringValue("Backlinks", "1");
        doc.setObject("XWiki.XWikiPreferences", 1, object);
        assertTrue("Backlinks are activated", !getXWiki().hasBacklinks(getXWikiContext()));

        object.setStringValue("backlinks", "");
        doc.setObject("XWiki.XWikiPreferences", 1, object);
        assertTrue("Backlinks are activated", !getXWiki().hasBacklinks(getXWikiContext()));

        object.setStringValue("backlinks", "1");
        doc.setObject("XWiki.XWikiPreferences", 1, object);
        assertTrue("Backlinks not activated", getXWiki().hasBacklinks(getXWikiContext()));
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

    public void testVelocityRenderingOneBacklink () throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("This is the main page of my test web");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        List loadedLinks1 = getXWiki().getStore().loadLinks(doc1.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 0, loadedLinks1.size());
        List loadedBacklinks1 = getXWiki().getStore().loadBacklinks("Test.BacklinksTarget",getXWikiContext(),true);
        assertEquals("lists size are not equal", 0, loadedBacklinks1.size());

        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        doc2.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB] [Test.BacklinksTarget]");
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        List loadedLinks2 = getXWiki().getStore().loadLinks(doc2.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 3, loadedLinks2.size());
        List loadedBacklinks2 = getXWiki().getStore().loadBacklinks("Test.BacklinksTarget",getXWikiContext(),true);
        assertEquals("lists size are not equal", 1, loadedBacklinks2.size());

        String velocityPhrase = (" \n  " +
                "##now we add a velocity command \n" +
                "#set($backlinks = $doc.getBacklinks())\n" +
                "#foreach( $backlink in $backlinks)" +
                "${backlink}|" +
                "#end ");

        doc1.setContent(velocityPhrase);
        String result = getXWiki().getDocument(doc1.getFullName(),getXWikiContext()).getRenderedContent(getXWikiContext());
        List toto = getXWiki().getDocument(doc1.getFullName(),getXWikiContext()).getBacklinks(getXWikiContext());
        assertTrue("Backlink is not present", (result.indexOf("Test.BacklinksInput")!=-1));
    }

    public void testVelocityRenderingBacklinks () throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("This is the main page of my test web");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        List loadedLinks1 = getXWiki().getStore().loadLinks(doc1.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 0, loadedLinks1.size());

        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        doc2.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB] [Test.BacklinksTarget]");
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        List loadedLinks2 = getXWiki().getStore().loadLinks(doc2.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 3, loadedLinks2.size());

        List loadedBacklinks = getXWiki().getStore().loadBacklinks("Test.BacklinksTarget",getXWikiContext(),true);
        assertEquals("lists size are not equal", 1, loadedBacklinks.size());

        XWikiDocument doc3 = new XWikiDocument("Test", "BacklinksOutput");
        doc3.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AA] [Test.BBB] [Test.BacklinksTarget]");
        getXWiki().getStore().saveXWikiDoc(doc3,  getXWikiContext());
        List loadedLinks3 = getXWiki().getStore().loadLinks(doc3.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 3, loadedLinks3.size());

        List loadedBacklinks1 = getXWiki().getStore().loadBacklinks("Test.BacklinksTarget",getXWikiContext(),true);
        assertEquals("lists size are not equal", 2, loadedBacklinks1.size());
        List expected_list = new ArrayList();
        expected_list.add("Test.BacklinksInput");
        expected_list.add("Test.BacklinksOutput");
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks1.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        String velocityPhrase = (" \n  " +
                "##now we add a velocity command \n" +
                "#set($backlinks = $doc.getBacklinks())\n" +
                "#foreach( $backlink in $backlinks)" +
                "${backlink}|" +
                "#end ");

        doc1.setContent(velocityPhrase);
        String result = getXWiki().getDocument(doc1.getFullName(),getXWikiContext()).getRenderedContent(getXWikiContext());
        List toto = getXWiki().getDocument(doc1.getFullName(),getXWikiContext()).getBacklinks(getXWikiContext());
        assertTrue("Backlink1 is not present", (result.indexOf("Test.BacklinksInput")!=-1));
        assertTrue("Backlink2 is not present", (result.indexOf("Test.BacklinksOutput")!=-1));
    }

    public void testLinksBacklinksLinkedPages () throws XWikiException {
        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("This is the main page of my test web");
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());
        List loadedLinks1 = getXWiki().getStore().loadLinks(doc1.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 0, loadedLinks1.size());

        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        doc2.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AAA] [Test.BBBB] [Test.BacklinksTarget]");
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());
        List loadedLinks2 = getXWiki().getStore().loadLinks(doc2.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 3, loadedLinks2.size());

        List loadedBacklinks = getXWiki().getStore().loadBacklinks("Test.BacklinksTarget",getXWikiContext(),true);
        assertEquals("lists size are not equal", 1, loadedBacklinks.size());

        List gotLinks2 = doc2.getLinks(getXWikiContext());
        assertEquals("lists are not equal", gotLinks2, loadedLinks2);

        XWikiDocument doc3 = new XWikiDocument("Test", "BacklinksOutput");
        doc3.setContent("Bonjour Monde : This is a test " +
                "with links  [Test.AA] [Test.BBB] [Test.BacklinksTarget]");
        getXWiki().getStore().saveXWikiDoc(doc3,  getXWikiContext());
        List loadedLinks3 = getXWiki().getStore().loadLinks(doc3.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 3, loadedLinks3.size());
        List gotLinks3 = doc3.getLinks(getXWikiContext());
        assertEquals("lists are not equal", gotLinks3, loadedLinks3);

        List loadedBacklinks1 = getXWiki().getStore().loadBacklinks("Test.BacklinksTarget",getXWikiContext(),true);
        assertEquals("lists size are not equal", 2, loadedBacklinks1.size());
        List expected_list = new ArrayList();
        expected_list.add("Test.BacklinksInput");
        expected_list.add("Test.BacklinksOutput");
        for (int i=0;i<expected_list.size();i++) {
            String item = ((String)expected_list.get(i));
            boolean ok = loadedBacklinks1.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        getXWiki().deleteDocument(doc3, getXWikiContext());
        List loadedLinks4 = getXWiki().getStore().loadLinks(doc3.getId(),getXWikiContext(),true);
        assertEquals("lists size are not equal", 0, loadedLinks4.size());
        List gotLinks4 = doc3.getLinks(getXWikiContext());
        assertEquals("lists size are not equal", gotLinks4.size(), loadedLinks4.size());

        List loadedBacklinks2 = getXWiki().getStore().loadBacklinks("Test.BacklinksTarget",getXWikiContext(),true);
        assertEquals("lists size are not equal", 1, loadedBacklinks2.size());
        assertTrue("Link is not correct ", loadedBacklinks2.get(0).equals("Test.BacklinksInput"));

        String velocityPhrase = (" \n  " +
                "##now we add a velocity command \n" +
                "#set($backlinks = $doc.getBacklinks())\n" +
                "#foreach( $backlink in $backlinks)" +
                "${backlink}|" +
                "#end ");

        doc1.setContent(velocityPhrase);
        String result = getXWiki().getDocument(doc1.getFullName(),getXWikiContext()).getRenderedContent(getXWikiContext());
        List toto = getXWiki().getDocument(doc1.getFullName(),getXWikiContext()).getBacklinks(getXWikiContext());
        assertTrue("Backlink1 is not present", (result.indexOf("Test.BacklinksInput")!=-1));
    }

    public void testRefreshBacklinksEmpty() throws XWikiException {
        getXWiki().refreshLinks(getXWikiContext());
   }

    public void refreshBacklinks() throws XWikiException {
        List expected_list = new ArrayList();
        List global_list = new ArrayList();
        expected_list.add("Test.AAA");
        expected_list.add("Test.BBBB");

        XWikiDocument doc1 = new XWikiDocument("Test", "BacklinksTarget");
        doc1.setContent("Bonjour Monde : This is a test " +
                "with links [Test.AAA] [Test.BBBB] [Test.BacklinksInput]");
        List expected_list1 = new ArrayList();
        expected_list1.addAll(expected_list);
        expected_list1.add("Test.BacklinksInput");
        global_list.add(expected_list1);
        getXWiki().getStore().saveXWikiDoc(doc1,  getXWikiContext());

        XWikiDocument doc2 = new XWikiDocument("Test", "BacklinksInput");
        doc2.setContent("Bonjour Monde : This is a test " +
                "with links [Test.AAA] [Test.BBBB] [Test.BacklinksOutput]");
        List expected_list2 = new ArrayList();
        expected_list2.addAll(expected_list);
        expected_list2.add("Test.BacklinksOutput");
        global_list.add(expected_list2);
        getXWiki().getStore().saveXWikiDoc(doc2,  getXWikiContext());

        XWikiDocument doc3 = new XWikiDocument("Test", "BacklinksOutput");
        doc3.setContent("Bonjour Monde : This is a test " +
                " with links [Test.AAA] [Test.BBBB] [Test.BacklinksTarget]");
        List expected_list3 = new ArrayList();
        expected_list3.addAll(expected_list);
        expected_list3.add("Test.BacklinksTarget");
        global_list.add(expected_list3);
        getXWiki().getStore().saveXWikiDoc(doc3,  getXWikiContext());

        List expected_backList = new ArrayList();
        expected_backList.add("Test.BacklinksTarget");
        expected_backList.add("Test.BacklinksInput");
        expected_backList.add("Test.BacklinksOutput");
        List loadedBacklinks = getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);

        assertEquals("lists size are not equal", 3, loadedBacklinks.size());
        for (int i=0;i<expected_backList.size();i++) {
            String item = (String)expected_backList.get(i);
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        for (int i=0; i<global_list.size();i++){
            for (int j=0;j<expected_backList.size();j++) {
                List loadedLinks = getXWiki().getStore().loadLinks(getXWiki().getDocument((String)expected_backList.get(j), getXWikiContext()).getId(), getXWikiContext(), true);
                assertEquals("lists size are not equal", 3, loadedLinks.size());
                for (int k=0; k<((List)global_list.get(j)).size();k++){
                    String item = (((XWikiLink)loadedLinks.get(k)).getLink());
                    boolean ok = ((List)global_list.get(j)).contains(item);
                    assertTrue("Link in link list " + j + " item " + k + " is not correct", ok);
                }
            }
        }

        getXWiki().getStore().deleteLinks(doc1.getId(), getXWikiContext(), true);
        getXWiki().getStore().deleteLinks(doc2.getId(), getXWikiContext(), true);
        getXWiki().getStore().deleteLinks(doc3.getId(), getXWikiContext(), true);

        for (int i=0; i<global_list.size();i++){
            List loadedLinks = getXWiki().getStore().loadLinks(getXWiki().getDocument((String)expected_backList.get(i), getXWikiContext()).getId(), getXWikiContext(), true);
            assertEquals("lists size are not equal", 0, loadedLinks.size());
        }

        getXWiki().refreshLinks(getXWikiContext());

        for (int i=0; i<global_list.size();i++){
            for (int j=0;j<expected_backList.size();j++) {
                List loadedLinks = getXWiki().getStore().loadLinks(getXWiki().getDocument((String)expected_backList.get(j), getXWikiContext()).getId(), getXWikiContext(), true);
                assertEquals("lists size are not equal", 3, loadedLinks.size());
                for (int k=0; k<((List)global_list.get(j)).size();k++){
                    String item = (((XWikiLink)loadedLinks.get(k)).getLink());
                    boolean ok = ((List)global_list.get(j)).contains(item);
                    assertTrue("Link in link list " + j + " item " + k + " is not correct", ok);
                }
            }
        }

        loadedBacklinks = getXWiki().getStore().loadBacklinks("Test.AAA",getXWikiContext(),true);

        assertEquals("lists size are not equal", 3, loadedBacklinks.size());
        for (int i=0;i<expected_backList.size();i++) {
            String item = (String)expected_backList.get(i);
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }
    }

}
