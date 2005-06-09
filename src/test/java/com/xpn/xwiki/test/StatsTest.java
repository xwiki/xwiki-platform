/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
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
 */
package com.xpn.xwiki.test;

import java.net.URL;

import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.stats.impl.SearchEngineRule;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class StatsTest extends HibernateTestCase {

    public void setUp() throws Exception {
        super.setUp();
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
    }

    public void testStatRegexp() throws HibernateException, XWikiException {
        SearchEngineRule senginerule = new SearchEngineRule(".google.", "s/(^|.*&)q=(.*?)(&.*|$)/$2/");
        String query = getXWikiContext().getUtil().substitute(senginerule.getRegEx(), "q=ludovic");
        assertEquals("Google query param not extracted","ludovic",query);
        query = getXWikiContext().getUtil().substitute(senginerule.getRegEx(), "q=ludovic+dubost");
        assertEquals("Google query param not extracted","ludovic+dubost",query);
        query = getXWikiContext().getUtil().substitute(senginerule.getRegEx(), "q=ludovic&toto=1");
        assertEquals("Google query param not extracted","ludovic",query);
        query = getXWikiContext().getUtil().substitute(senginerule.getRegEx(), "&titi=12&q=ludovic&toto=1");
        assertEquals("Google query param not extracted","ludovic",query);
        query = getXWikiContext().getUtil().substitute(senginerule.getRegEx(), "q=ludovic+dubost&ie=UTF-8");
        assertEquals("Google query param not extracted","ludovic+dubost",query);
   }

    public void testRefererText() throws HibernateException, XWikiException {
        String ref = "http://www.google.fr/search?q=ludovic+dubost&ie=UTF-8";
        assertEquals("Google URL should be transformed", "google.fr:ludovic+dubost", getXWikiContext().getWiki().getRefererText(ref, getXWikiContext()));
        ref = "http://www.ludovic.org/";
        assertEquals("Normal URL should be simplified", "www.ludovic.org", getXWikiContext().getWiki().getRefererText(ref, getXWikiContext()));
        ref = "http://www.ludovic.org";
        assertEquals("Normal URL should be simplified", "www.ludovic.org", getXWikiContext().getWiki().getRefererText(ref, getXWikiContext()));
        ref = "http://www.ludovic.org/blog/";
        assertEquals("Normal URL should be simplified", "www.ludovic.org/blog", getXWikiContext().getWiki().getRefererText(ref, getXWikiContext()));
        ref = "http://www.ludovic.org";
        assertEquals("Normal URL should be simplified", "www.ludovic.org", getXWikiContext().getWiki().getRefererText(ref, getXWikiContext()));
        ref = "http://www.ludovic.org/blog/index.rdf";
        assertEquals("Normal URL should be simplified", "www.ludovic.org/blog/index.rdf", getXWikiContext().getWiki().getRefererText(ref, getXWikiContext()));
       }
}
