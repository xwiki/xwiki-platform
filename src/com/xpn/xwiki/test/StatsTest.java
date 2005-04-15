/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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
 * Date: 31 juil. 2004
 * Time: 11:42:43
 */
package com.xpn.xwiki.test;

import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.stats.impl.SearchEngineRule;
import net.sf.hibernate.HibernateException;

import java.util.List;
import java.net.URL;

import junit.framework.TestCase;
import org.apache.velocity.app.Velocity;

public class StatsTest extends TestCase {

    public XWikiHibernateStore store;
    public String hibpath = "hibernate-test.cfg.xml";
    public XWikiContext context;
    public XWiki xwiki;

    public XWikiHibernateStore getHibStore() {
        return (XWikiHibernateStore) getStore();
    }

    public void setUp() throws Exception {
        context = new XWikiContext();
        xwiki = new XWiki("./xwiki.cfg", context);
        xwiki.setDatabase("xwikitest");
        context.setWiki(xwiki);
        context.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
    }

    public XWikiStoreInterface getStore() {
       if (store!=null)
        return store;

       store = new XWikiHibernateStore(hibpath);
       return store;
   }

    public void tearDown() throws HibernateException {
        XWikiHibernateStore hibstore = getHibStore();
        hibstore.shutdownHibernate(context);
        hibstore = null;
        System.gc();
    }

    public void testStatRegexp() throws HibernateException, XWikiException {
        SearchEngineRule senginerule = new SearchEngineRule(".google.", "s/(^|.*&)q=(.*?)(&.*|$)/$2/");
        String query = context.getUtil().substitute(senginerule.getRegEx(), "q=ludovic");
        assertEquals("Google query param not extracted","ludovic",query);
        query = context.getUtil().substitute(senginerule.getRegEx(), "q=ludovic+dubost");
        assertEquals("Google query param not extracted","ludovic+dubost",query);
        query = context.getUtil().substitute(senginerule.getRegEx(), "q=ludovic&toto=1");
        assertEquals("Google query param not extracted","ludovic",query);
        query = context.getUtil().substitute(senginerule.getRegEx(), "&titi=12&q=ludovic&toto=1");
        assertEquals("Google query param not extracted","ludovic",query);
        query = context.getUtil().substitute(senginerule.getRegEx(), "q=ludovic+dubost&ie=UTF-8");
        assertEquals("Google query param not extracted","ludovic+dubost",query);
   }

    public void testRefererText() throws HibernateException, XWikiException {
        String ref = "http://www.google.fr/search?q=ludovic+dubost&ie=UTF-8";
        assertEquals("Google URL should be transformed", "google.fr:ludovic+dubost", context.getWiki().getRefererText(ref, context));
        ref = "http://www.ludovic.org/";
        assertEquals("Normal URL should be simplified", "www.ludovic.org", context.getWiki().getRefererText(ref, context));
        ref = "http://www.ludovic.org";
        assertEquals("Normal URL should be simplified", "www.ludovic.org", context.getWiki().getRefererText(ref, context));
        ref = "http://www.ludovic.org/blog/";
        assertEquals("Normal URL should be simplified", "www.ludovic.org/blog", context.getWiki().getRefererText(ref, context));
        ref = "http://www.ludovic.org";
        assertEquals("Normal URL should be simplified", "www.ludovic.org", context.getWiki().getRefererText(ref, context));
        ref = "http://www.ludovic.org/blog/index.rdf";
        assertEquals("Normal URL should be simplified", "www.ludovic.org/blog/index.rdf", context.getWiki().getRefererText(ref, context));
       }

}