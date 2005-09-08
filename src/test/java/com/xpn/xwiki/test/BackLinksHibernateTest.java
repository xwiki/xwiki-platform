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
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLink;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import org.hibernate.HibernateException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class BackLinksHibernateTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
    }

    public void testBackLinksHibernateDelete() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        // this test aims at validating the deleteLinks method

        // creation of the initial conditions with direct SQL order
        XWikiHibernateStore store = getXWiki().getHibernateStore();
        store.beginTransaction(getXWikiContext());
        String SQLStatement = new String("insert into xwikilinks  " +
                "values ('" + testDoc1.getId() +  "', 'Test.A', '" + testDoc1.getFullName() + "')");
        StoreHibernateTest.runSQL(getXWiki().getHibernateStore(), SQLStatement, getXWikiContext() );

        // creation of the list to give to the method
        List expected_list = new ArrayList();
        XWikiLink link = new XWikiLink ( testDoc1.getId() ,"Test.A",testDoc1.getFullName());
        expected_list.add(link);

        testBackLinksHibernateStoreDelete(expected_list);
    }

    public void testBackLinkHibernateWrite() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        // this test aims at validating the writeLinks method

        // creation of the initial conditions with direct SQL order
        XWikiHibernateStore store = getXWiki().getHibernateStore();
        store.beginTransaction(getXWikiContext());
        String SQLStatement = new String("insert into xwikilinks  " +
                "values ('" + testDoc1.getId() +  "', 'Test.A', '" + testDoc1.getFullName() + "')");
        StoreHibernateTest.runSQL(getXWiki().getHibernateStore(), SQLStatement, getXWikiContext() );

        // creation of the list to give to the method
        List expected_list = new ArrayList();
        XWikiLink link = new XWikiLink ( testDoc1.getId() ,"Test.B",testDoc1.getFullName());
        expected_list.add(link);
        testDoc1.setContent("[Test.B]");

        // the write is
        testBackLinksHibernateStoreWrite(testDoc1, expected_list);
    }

    public void testBackLinkHibernateEmpty() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        testBackLinksHibernateStoreEmpty(testDoc1);
        List loadedlinks = getXWiki().getStore().loadLinks(testDoc1.getId(), getXWikiContext(), true);
        assertEquals("lists size are not equal", 0, loadedlinks.size());
    }

    public void testBackLinksHibernateStoreOneLink() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        // the expected list contains only one link for only one docId
        List expected_list = new ArrayList();

        // first test is with empty initial conditions
        testBackLinksHibernateStoreEmpty(testDoc1);

        // using this list as test
        XWikiLink link = new XWikiLink ( testDoc1.getId() ,"Test.A", testDoc1.getFullName());
        expected_list.add(link);
        testDoc1.setContent("[Test.A]");
        testBackLinksHibernateStoreWrite(testDoc1, expected_list);

        // using this list as initial conditions
        testBackLinksHibernateStoreWrite(testDoc1, expected_list);
    }

    public void testBackLinksHibernateStoreMultiDocIds() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        XWikiDocument testDoc2 = new XWikiDocument( "Test", "LinksForBackLinks");
        // the expected list contains two different link for only one docId
        List expected_list = new ArrayList();
        List expected_list2 = new ArrayList();

        // first test is with empty initial conditions
        testBackLinksHibernateStoreEmpty(testDoc1);
        testBackLinksHibernateStoreEmpty(testDoc2);

        // using this list as test
        XWikiLink link = new XWikiLink ( testDoc1.getId(),"Test.A", testDoc1.getFullName());
        expected_list.add(link);
        testDoc1.setContent("[Test.A]");
        XWikiLink link1 = new XWikiLink ( testDoc2.getId(),"Test.B",testDoc2.getFullName());
        expected_list2.add(link1);
        testDoc2.setContent("[Test.B]");

        // writing both lists
        testBackLinksHibernateStoreWrite(testDoc2, expected_list2);
        testBackLinksHibernateStoreWrite(testDoc1, expected_list);

        // verifying the lists
        testBackLinksHibernateStoreRead(expected_list2);
        testBackLinksHibernateStoreRead(expected_list);
    }

    public void testBackLinksHibernateStoreMultiLinks() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        XWikiDocument testDoc2 = new XWikiDocument( "Test", "LinksForBackLinks");
        // the expected list contains the same link for two docIds
        List expected_list = new ArrayList();
        XWikiLink link = new XWikiLink (testDoc1.getId(),"Test.A", testDoc1.getFullName());
        expected_list.add(link);
        testDoc1.setContent("[Test.A]");
        XWikiLink link1 = new XWikiLink (testDoc1.getId(),"Test.B", testDoc1.getFullName());
        expected_list.add(link1);
        testDoc1.setContent("[Test.A] [Test.B]");

        // first test is with empty initial conditions
        testBackLinksHibernateStoreEmpty(testDoc1);
        testBackLinksHibernateStoreWrite(testDoc1, expected_list);

        // using this list as initial conditions
        testBackLinksHibernateStoreWrite(testDoc1, expected_list);
    }

    public void testBackLinksHibernateStoreDoubleMulti() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        XWikiDocument testDoc2 = new XWikiDocument( "Test", "LinksForBackLinks");
        // the expected lists contain the same two different links for two docIds
        List expected_list = new ArrayList();
        XWikiLink link = new XWikiLink (testDoc1.getId(),"Test.A",testDoc1.getFullName());
        expected_list.add(link);
        testDoc1.setContent("[Test.A]");
        XWikiLink link1 = new XWikiLink (testDoc1.getId(),"Test.B",testDoc1.getFullName());
        expected_list.add(link1);
        testDoc1.setContent("[Test.A] [Test.B]");
        getXWiki().getStore().saveLinks(testDoc1, getXWikiContext(), true);

        List second_list = new ArrayList();
        XWikiLink link2 = new XWikiLink (testDoc2.getId(),"Test.A",testDoc2.getFullName());
        second_list.add(link2);
        testDoc2.setContent("[Test.A]");
        XWikiLink link3 = new XWikiLink (testDoc2.getId(),"Test.B",testDoc2.getFullName());
        second_list.add(link3);
        testDoc2.setContent("[Test.A] [Test.B]");
        getXWiki().getStore().saveLinks(testDoc2, getXWikiContext(), true);

        testBackLinksHibernateStoreWrite(testDoc1, expected_list);
        testBackLinksHibernateStoreRead(second_list) ;

        // this one should work fine
        List backlinks_list2 = new ArrayList();
        backlinks_list2.add(link);
        backlinks_list2.add(link2);
        testSimpleReturnBackLinks("Test.A", backlinks_list2);
    }

    public void testBackLinksHibernateStoreUniqueSeparate() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        XWikiDocument testDoc2 = new XWikiDocument( "Test", "LinksForBackLinks");
        // the expected list contains one different link for two different docIds
        List expected_list = new ArrayList();
        XWikiLink link = new XWikiLink (testDoc1.getId(),"Test.A",testDoc1.getFullName());
        expected_list.add(link);
        testDoc1.setContent("[Test.A]");
        getXWiki().getStore().saveLinks(testDoc1, getXWikiContext(), true);

        List second_list = new ArrayList();
        XWikiLink link1 = new XWikiLink (testDoc2.getId(),"Test.B",testDoc2.getFullName());
        second_list.add(link1);
        testDoc2.setContent("[Test.B]");
        getXWiki().getStore().saveLinks(testDoc2, getXWikiContext(), true);

        testBackLinksHibernateStoreWrite(testDoc1, expected_list);
        testBackLinksHibernateStoreWrite(testDoc2, second_list);
        testBackLinksHibernateStoreRead(expected_list);
        testBackLinksHibernateStoreRead(second_list);
    }

    public void testBackLinksHibernateStoreMultiSeparate() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        XWikiDocument testDoc2 = new XWikiDocument( "Test", "LinksForBackLinks");
        // the expected list contains four different links, two for each two different docIds
        List expected_list = new ArrayList();
        XWikiLink link = new XWikiLink (testDoc1.getId(),"Test.A",testDoc1.getFullName());
        expected_list.add(link);
        testDoc1.setContent("[Test.A]");
        XWikiLink link1 = new XWikiLink (testDoc1.getId(),"Test.B",testDoc1.getFullName());
        expected_list.add(link1);
        testDoc1.setContent("[Test.A] [Test.B]");
        getXWiki().getStore().saveLinks(testDoc1, getXWikiContext(), true);

        List second_list = new ArrayList();
        XWikiLink link3 = new XWikiLink (testDoc2.getId(),"Test.C",testDoc2.getFullName());
        second_list.add(link3);
        testDoc2.setContent("[Test.C]");
        XWikiLink link4 = new XWikiLink (testDoc2.getId(),"Test.D",testDoc2.getFullName());
        second_list.add(link4);
        testDoc2.setContent("[Test.C] [Test.D]");
        getXWiki().getStore().saveLinks(testDoc2, getXWikiContext(), true);

        testBackLinksHibernateStoreWrite(testDoc1, expected_list);
        testBackLinksHibernateStoreWrite(testDoc2, second_list);
        testBackLinksHibernateStoreRead(expected_list);
        testBackLinksHibernateStoreRead(second_list);

        // testing search of backlinks
        List backlinks_list = new ArrayList();
        backlinks_list.add(link4);
        testSimpleReturnBackLinks("Test.D", backlinks_list);
    }

    public void testBackLinksHibernateStoreMultiMix() throws XWikiException {
        XWikiDocument testDoc1 = new XWikiDocument( "Test", "SaveBackLinks");
        XWikiDocument testDoc2 = new XWikiDocument( "Test", "LinksForBackLinks");
        // the expected list contains a mix of a same and a different link, two for each two different docIds
        List expected_list = new ArrayList();
        XWikiLink link = new XWikiLink (testDoc1.getId(),"Test.A",testDoc1.getFullName());
        expected_list.add(link);
        testDoc1.setContent("[Test.A]");
        XWikiLink link1 = new XWikiLink (testDoc1.getId(),"Test.B",testDoc1.getFullName());
        expected_list.add(link1);
        testDoc1.setContent("[Test.A] [Test.B]");
        getXWiki().getStore().saveLinks(testDoc1, getXWikiContext(), true);

        List second_list = new ArrayList();
        XWikiLink link2 = new XWikiLink (testDoc2.getId(),"Test.B",testDoc2.getFullName());
        second_list.add(link2);
        testDoc2.setContent("[Test.B]");
        XWikiLink link3 = new XWikiLink (testDoc2.getId(),"Test.D",testDoc2.getFullName());
        second_list.add(link3);
        testDoc2.setContent("[Test.B] [Test.D]");        
        getXWiki().getStore().saveLinks(testDoc2, getXWikiContext(), true);

        testBackLinksHibernateStoreWrite(testDoc1, expected_list);
        testBackLinksHibernateStoreWrite(testDoc2, second_list);
        testBackLinksHibernateStoreRead(expected_list);
        testBackLinksHibernateStoreRead(second_list);

        // testing search of backlinks
        List backlinks_list = new ArrayList();
        backlinks_list.add(link1);
        backlinks_list.add(link2);
        testSimpleReturnBackLinks("Test.B", backlinks_list);
    }

    public void testBackLinksHibernateStoreDelete(List expected_list) throws XWikiException, HibernateException {
        // we want to test a wrong deleteLinks where session.delete has been commented
        getXWiki().getStore().deleteLinks(((XWikiLink)expected_list.get(0)).getDocId(),getXWikiContext(),true);

        // loadLinks is trusted temporarily
        List loadedlinks = getXWiki().getStore().loadLinks(((XWikiLink)expected_list.get(0)).getDocId(), getXWikiContext(), true);

        // it goes wrong if the session.delete has been commented
        assertTrue("loadLinks has read something else than nothing",loadedlinks.size() == 0);
    }

    public void testBackLinksHibernateStoreWrite(XWikiDocument testDoc, List expected_list) throws XWikiException, HibernateException {
        // we try to save the expected list
        getXWiki().getStore().saveLinks(testDoc, getXWikiContext(), true);

        testBackLinksHibernateStoreRead(expected_list);
    }

    public void testBackLinksHibernateStoreRead (List expected_list) throws XWikiException, HibernateException{
        // Reading of impact on the doc Backlinks
        List loadedlinks = getXWiki().getStore().loadLinks(((XWikiLink)expected_list.get(0)).getDocId(), getXWikiContext(), true);

        assertEquals("lists size are not equal", expected_list.size(), loadedlinks.size());
        for (int i=0;i<loadedlinks.size();i++) {
            XWikiLink item = (XWikiLink)loadedlinks.get(i);
            boolean ok = expected_list.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }
    }

    public void testBackLinksHibernateStoreEmpty(XWikiDocument testDoc) throws XWikiException {
        // the expected list is empty ;
        List expected_list = new ArrayList();
        XWikiLink link = new XWikiLink (testDoc.getId(), null, testDoc.getFullName());
        expected_list.add(link);

        // creating the initial conditions
        getXWiki().getStore().deleteLinks(testDoc.getId(), getXWikiContext(), true);

        testBackLinksHibernateStoreDelete(expected_list);
    }

    public void testSimpleReturnBackLinks (String fullName, List expected_list) throws XWikiException, HibernateException{
        List loadedBacklinks = new ArrayList();

       if (fullName != null){
            loadedBacklinks = getXWiki().getStore().loadBacklinks(fullName, getXWikiContext(),true);
        }

        assertEquals("lists size are not equal", expected_list.size(), loadedBacklinks.size());
        for (int i=0;i<expected_list.size();i++) {
            String item = ((XWikiLink)expected_list.get(i)).getFullName();
            boolean ok = loadedBacklinks.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }
    }
}
