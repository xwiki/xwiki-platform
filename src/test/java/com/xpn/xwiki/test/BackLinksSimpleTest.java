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

public class BackLinksSimpleTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
    }

    public void testSimpleBackLinksHibStore0() throws XWikiException {
        XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        List expected_list = new ArrayList();
        testSimpleBackLinksHibStore(testDoc, expected_list);
    }

    public void testSimpleBackLinksHibStore1() throws XWikiException {
        XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        testDoc.setContent("[Test.B]");
        List expected_list = new ArrayList();
        expected_list.add("Test.B");
        testSimpleBackLinksHibStore(testDoc, expected_list);
    }

    public void testSimpleBackLinksHibStore2() throws XWikiException {
        XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        testDoc.setContent("[Test.C]");
        List expected_list = new ArrayList();
        expected_list.add("Test.C");
        testSimpleBackLinksHibStore(testDoc, expected_list);
    }

    public void testSimpleBackLinksHibStore3() throws XWikiException {
        XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        testDoc.setContent("[Test.B] [Test.C]");
        List expected_list = new ArrayList();
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinksHibStore(testDoc, expected_list);
    }

    public void testSimpleBackLinksHibStore4() throws XWikiException {
        XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        testDoc.setContent("[Test.A] [Test.B] [Test.C]");
        List expected_list = new ArrayList();
        expected_list.add("Test.A");
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinksHibStore(testDoc, expected_list);
    }

    public void testSimpleBackLinksHibStore5() throws XWikiException {
        XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        testDoc.setContent("[Test.A] [Test.B] [Test.C]");
        List expected_list = new ArrayList();
        expected_list.add("Test.A");
        expected_list.add("Test.B");
        expected_list.add("Test.C");
        testSimpleBackLinksHibStore(testDoc, expected_list);
        testSimpleBackLinksHibStore(testDoc, expected_list);
    }

    public void testSimpleBackLinksHibStore6() throws XWikiException {
        XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        XWikiHibernateStore store = getXWiki().getHibernateStore();
        store.beginTransaction(getXWikiContext());
   //     StoreHibernateTest.runSQL(getXWiki().getHibernateStore(), "insert into xwikilinks values ('0','Test.A','SaveBackLinks')", getXWikiContext() );
        String SQLStatement = new String("insert into xwikilinks  " +
        "values ('" + testDoc.getId() +  "', 'Test.A', '" + testDoc.getFullName() + "')");
        StoreHibernateTest.runSQL(getXWiki().getHibernateStore(), SQLStatement, getXWikiContext() );
        List expected_list = new ArrayList();
        expected_list.add("Test.A");
        testSimpleBackLinksHibStoreDelete(expected_list);
    }

    public void testSimpleBackLinksHibStore(XWikiDocument testDoc, List expected_list) throws XWikiException, HibernateException {
   //     XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        List formatted_list = new ArrayList();
        long id = testDoc.getId();
        String fullName = testDoc.getFullName();
        if (expected_list.size()!=0){
            for (int i=0 ; i<expected_list.size() ; i++){
                XWikiLink tempLink = new XWikiLink();
                tempLink.setDocId(id);
                tempLink.setFullName(fullName);
                tempLink.setLink((String)expected_list.get(i));
                formatted_list.add(tempLink);
            }
        }   else {
            XWikiLink tempLink = new XWikiLink(id,null,fullName);
            formatted_list.add(tempLink);
        }

        getXWiki().getStore().saveLinks(testDoc, getXWikiContext(), true);
        List loadedbacklinks = getXWiki().getStore().loadLinks(testDoc.getId(), getXWikiContext(), true);

        if (expected_list.size()==0){
            formatted_list.clear();
        }
        assertEquals("Size is not correct", formatted_list.size(), loadedbacklinks.size());
        for (int i=0 ; i<loadedbacklinks.size(); i++) {
            String item = ((XWikiLink)loadedbacklinks.get(i)).getLink();
            boolean ok = expected_list.contains(item);
            assertTrue("Link in link list item " + i + " is not correct", ok);
        }

        getXWiki().getStore().deleteLinks(testDoc.getId(),getXWikiContext(),true) ;
    }

    public void testSimpleBackLinksHibStoreDelete(List expected_list) throws XWikiException, HibernateException {
        XWikiDocument testDoc = new XWikiDocument( "Test", "SaveBackLinks");
        List formatted_list = new ArrayList();
        if (expected_list.size()!=0){
            long id = testDoc.getId();
            String fullName = testDoc.getFullName();
            for (int i=0 ; i<expected_list.size() ; i++){
                XWikiLink tempLink = new XWikiLink();
                tempLink.setDocId(id);
                tempLink.setFullName(fullName);
                tempLink.setLink((String)expected_list.get(i));
                formatted_list.add(tempLink);
            }
        }

        // we want to test a wrong deleteLinks where session.delete has been commented
        getXWiki().getStore().deleteLinks(testDoc.getId(),getXWikiContext(),true) ;

        // loadLinks is also trusted temporarily
        List loadedbacklinks = getXWiki().getStore().loadLinks(testDoc.getId(), getXWikiContext(), true);

        assertTrue("loadLinks has read something else than nothing",(loadedbacklinks.size()==0));
       
        for (int i=0;i<loadedbacklinks.size();i++) {
            String item = ((XWikiLink)loadedbacklinks.get(i)).getLink();
            boolean ok = expected_list.contains(item);
            assertTrue("loadLinks has really read 'from' expected_list", !ok);
        }
    }
}
