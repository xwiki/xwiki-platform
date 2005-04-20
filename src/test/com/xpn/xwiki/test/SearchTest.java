
package com.xpn.xwiki.test;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import junit.framework.TestCase;
import org.hibernate.HibernateException;

import java.util.List;

/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
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

 * Created by
 * User: Ludovic Dubost
 * Date: 4 janv. 2004
 * Time: 10:19:35
 */

public class SearchTest extends TestCase {

    public XWikiHibernateStore store;
    public String hibpath = "hibernate-test.cfg.xml";
    public XWikiContext context = new XWikiContext();

    public XWikiHibernateStore getHibStore() {
        return (XWikiHibernateStore) getStore();
    }

    public XWikiStoreInterface getStore() {
       if (store!=null)
        return store;

       store = new XWikiHibernateStore(hibpath);
       return store;
   }


    public void setUp() throws HibernateException, XWikiException {
        context.setDatabase("xwikitest");
        XWikiHibernateStore hibstore = getHibStore();
        StoreHibernateTest.cleanUp(hibstore, context);
    }

    public void tearDown() throws HibernateException {
        XWikiHibernateStore hibstore = getHibStore();
        hibstore.shutdownHibernate(context);
        hibstore = null;
        System.gc();
    }

    public void testSearch(XWikiStoreInterface hibstore, String wheresql, String[] expected) throws HibernateException, XWikiException {
        List lresult = hibstore.searchDocumentsNames(wheresql, context);
        String[] result = {};
        result = (String[]) lresult.toArray(result);
        assertEquals("Number of results of search " + wheresql + " is different", result.length, expected.length);
        for (int i=0;i<result.length;i++) {
            assertEquals("Element " + i + " of search " + wheresql + " is different",
                    result[i], expected[i]);
        }
    }

    public void testSearchWithOrder(XWikiStoreInterface hibstore, String wheresql, String[] expected) throws HibernateException, XWikiException {
        List lresult = hibstore.searchDocumentsNames(wheresql + " order by doc.date desc", 0, 0, "doc.date", context);
        String[] result = {};
        result = (String[]) lresult.toArray(result);
        assertEquals("Number of results of search " + wheresql + " is different", result.length, expected.length);
        for (int i=0;i<result.length;i++) {
            assertEquals("Element " + i + " of search " + wheresql + " is different",
                    result[i], expected[i]);
        }
    }

    public void testSearch() throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = getHibStore();
        testSearch(hibstore, "", new String[] {} );
        testSearch(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {} );
        XWikiDocument doc1 = new XWikiDocument("Main", "WebHome");
        doc1.setContent("no content");
        doc1.setAuthor("Ludovic Dubost");
        doc1.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc1, context);
        testSearch(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {"Main.WebHome"});
        XWikiDocument doc2 = new XWikiDocument("Main", "WebHome2");
        doc2.setContent("no content");
        doc2.setAuthor("Ludovic Dubost");
        doc2.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc2, context);
        testSearch(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {"Main.WebHome"});
        testSearch(hibstore, "doc.web='Main'", new String[] {"Main.WebHome", "Main.WebHome2"});
    }

    public void testSearchWithOrder() throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = getHibStore();
        testSearch(hibstore, "", new String[] {} );
        testSearch(hibstore, "doc.web='Main' and doc.name='WebHome' order by doc.date desc", new String[] {} );
        XWikiDocument doc1 = new XWikiDocument("Main", "WebHome");
        doc1.setContent("no content");
        doc1.setAuthor("Ludovic Dubost");
        doc1.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc1, context);
        testSearchWithOrder(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {"Main.WebHome"});
        XWikiDocument doc2 = new XWikiDocument("Main", "WebHome2");
        doc2.setContent("no content");
        doc2.setAuthor("Ludovic Dubost");
        doc2.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc2, context);
        testSearchWithOrder(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {"Main.WebHome"});
        testSearchWithOrder(hibstore, "doc.web='Main'", new String[] {"Main.WebHome", "Main.WebHome2"});
    }
}
