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
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import org.hibernate.HibernateException;
import org.apache.commons.lang.ArrayUtils;

import java.util.List;

public class SearchTest extends HibernateTestCase {

    public void testsearchDocumentsNames(XWikiStoreInterface hibstore, String wheresql, String[] expected) throws HibernateException, XWikiException {
        List lresult = hibstore.searchDocumentsNames(wheresql, getXWikiContext());
        String[] result = {};
        result = (String[]) lresult.toArray(result);
        assertEquals("Number of results of search " + wheresql + " is different", result.length, expected.length);
        for (int i=0;i<result.length;i++) {
            assertEquals("Element " + i + " of search " + wheresql + " is different",
                    result[i], expected[i]);
        }
    }

    public void testsearch(XWikiStoreInterface hibstore, String sql, String[] expected) throws XWikiException {
        List lresult = hibstore.search(sql, 0, 0, getXWikiContext());
        String[] result = {};
        result = (String[]) lresult.toArray(result);
        assertEquals("Number of results of search " + sql + " is different", result.length, expected.length);
        for (int i=0;i<result.length;i++) {
            assertEquals("Element " + i + " of search " + sql + " is different",
                    result[i], expected[i]);
        }
    }

    public void testsearch(XWikiStoreInterface hibstore, String sql, Object[][] whereParams, String[] expected) throws XWikiException {
       testsearch(hibstore, sql, whereParams, expected, false);
    }

    public void testsearch(XWikiStoreInterface hibstore, String sql, Object[][] whereParams, String[] expected, boolean withorder) throws XWikiException {
        List lresult = hibstore.search(sql, 0, 0, whereParams , getXWikiContext());
        String[] result = {};
        result = (String[]) lresult.toArray(result);
        assertEquals("Number of results of search " + sql + " is different", result.length, expected.length);
        for (int i=0;i<result.length;i++) {
            if (withorder)
                assertEquals("Element " + i + " of search " + sql + " is different",
                        result[i], expected[i]);
            else
                assertTrue("Element " + i + " of search " + sql + " is different",
                        ArrayUtils.contains(expected, result[i]));
        }
    }

    public void testsearchDocumentsNamesWithOrder(XWikiStoreInterface hibstore, String wheresql, String[] expected) throws HibernateException, XWikiException {
        List lresult = hibstore.searchDocumentsNames(wheresql, 0, 0, getXWikiContext());
        String[] result = {};
        result = (String[]) lresult.toArray(result);
        assertEquals("Number of results of search " + wheresql + " is different", result.length, expected.length);
        for (int i=0;i<result.length;i++) {
            assertEquals("Element " + i + " of search " + wheresql + " is different",
                    result[i], expected[i]);
        }
    }

    public void testSearch() throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = getXWiki().getHibernateStore();
        testsearchDocumentsNames(hibstore, "", new String[] {} );
        testsearchDocumentsNames(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {} );
        XWikiDocument doc1 = new XWikiDocument("Main", "WebHome");
        doc1.setContent("no content");
        doc1.setAuthor("Ludovic Dubost");
        doc1.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc1, getXWikiContext());
        testsearchDocumentsNames(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {"Main.WebHome"});
        XWikiDocument doc2 = new XWikiDocument("Main", "WebHome2");
        doc2.setContent("no content");
        doc2.setAuthor("Ludovic Dubost");
        doc2.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc2, getXWikiContext());
        testsearchDocumentsNames(hibstore, "doc.web='Main' and doc.name='WebHome'", new String[] {"Main.WebHome"});
        testsearchDocumentsNames(hibstore, "doc.web='Main'", new String[] {"Main.WebHome", "Main.WebHome2"});

        testsearch(hibstore, "select distinct doc.web from XWikiDocument doc", new String[] {"Main"});
        testsearch(hibstore, "select distinct doc.name from XWikiDocument doc", new Object[][] {{"doc.web", "Main"}},  new String[] {"WebHome", "WebHome2"});

        XWikiDocument doc3 = new XWikiDocument("Other", "Web2");
        doc2.setContent("no content");
        doc2.setAuthor("Ludovic Dubost");
        doc2.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc3, getXWikiContext());

        testsearch(hibstore, "select distinct doc.web from XWikiDocument doc", new String[] {"Main", "Other"});
        testsearch(hibstore, "select distinct doc.name from XWikiDocument doc", new Object[][] {{"doc.web", "Main"}},  new String[] {"WebHome", "WebHome2"});
        testsearch(hibstore, "select distinct doc.name from XWikiDocument doc", new Object[][] {{"doc.web", "Other"}},  new String[] {"Web2"});

        testsearch(hibstore, "select distinct doc.name from XWikiDocument doc", new Object[][] {{"doc.web", "Main", "", "and"}, {"doc.name", "WebHome"}},  new String[] {"WebHome"});
        testsearch(hibstore, "select distinct doc.name from XWikiDocument doc", new Object[][] {{"doc.web", "Main", "=", "and"}, {"doc.name", "WebHome2"}},  new String[] {"WebHome2"});

        testsearch(hibstore, "select distinct doc.name from XWikiDocument doc", new Object[][] {{"doc.web", "Main", "=", "or"}, {"doc.name", "Web2"}},  new String[] {"WebHome", "WebHome2", "Web2"});

        testsearch(hibstore, "select distinct doc.web from XWikiDocument doc", new Object[][] {{"doc.web", "Main", "!="}}, new String[] {"Other"});

        testsearch(hibstore, "select distinct doc.name from XWikiDocument doc", new Object[][] {{"doc.web", "Other"}},  new String[] {"Web2"});

    }

    public void testSearchWithOrder() throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = getXWiki().getHibernateStore();
        testsearchDocumentsNamesWithOrder(hibstore, "", new String[] {} );
        testsearchDocumentsNamesWithOrder(hibstore, "doc.web='Main' and doc.name='WebHome' order by doc.fullName desc", new String[] {} );
        XWikiDocument doc1 = new XWikiDocument("Main", "WebHome");
        doc1.setContent("no content");
        doc1.setAuthor("Ludovic Dubost");
        doc1.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc1, getXWikiContext());
        testsearchDocumentsNamesWithOrder(hibstore, "doc.web='Main' and doc.name='WebHome' order by doc.fullName desc", new String[] {"Main.WebHome"});
        XWikiDocument doc2 = new XWikiDocument("Main", "WebHome2");
        doc2.setContent("no content");
        doc2.setAuthor("Ludovic Dubost");
        doc2.setParent("Main.WebHome");
        hibstore.saveXWikiDoc(doc2, getXWikiContext());
        testsearchDocumentsNamesWithOrder(hibstore, "doc.web='Main' and doc.name='WebHome' order by doc.fullName desc", new String[] {"Main.WebHome"});
        testsearchDocumentsNamesWithOrder(hibstore, "doc.web='Main' order by doc.fullName desc", new String[] {"Main.WebHome2", "Main.WebHome"});
    }
}
