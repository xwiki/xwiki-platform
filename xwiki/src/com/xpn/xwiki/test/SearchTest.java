
package com.xpn.xwiki.test;

import junit.framework.TestCase;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.store.XWikiHibernateStore;
import net.sf.hibernate.HibernateException;

import java.util.List;

/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at 
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.

 * Created by
 * User: Ludovic Dubost
 * Date: 4 janv. 2004
 * Time: 10:19:35
 */

public class SearchTest extends TestCase {

    public static String hibpath = "hibernate-test.cfg.xml";

    public SearchTest() throws XWikiException {
    }

    public void setUp() throws HibernateException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        StoreTest.cleanUp(hibstore);
    }

    public void testSearch(XWikiHibernateStore hibstore, String wheresql, String[] expected) throws HibernateException, XWikiException {
        List lresult = hibstore.searchDocuments(wheresql);
        String[] result = {};
        result = (String[]) lresult.toArray(result);
        assertEquals("Number of results of search " + wheresql + " is different", result.length, expected.length);
        for (int i=0;i<result.length;i++) {
            assertEquals("Element " + i + " of search " + wheresql + " is different",
                    result[i], expected[i]);
        }
    }

    public void testSearch() throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(hibpath);
        testSearch(hibstore, "", new String[] {} );
        testSearch(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {} );
        XWikiSimpleDoc doc1 = new XWikiSimpleDoc("Main", "WebHome");
        doc1.setContent("no content");
        doc1.setAuthor("Ludovic Dubost");
        doc1.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc1);
        testSearch(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {"Main.WebHome"});
        XWikiSimpleDoc doc2 = new XWikiSimpleDoc("Main", "WebHome2");
        doc2.setContent("no content");
        doc2.setAuthor("Ludovic Dubost");
        doc2.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc2);
        testSearch(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {"Main.WebHome"});
        testSearch(hibstore, "doc.web='Main'", new String[] {"Main.WebHome", "Main.WebHome2"});
    }
}
