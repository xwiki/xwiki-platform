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
 * Time: 12:32:55
 */
package com.xpn.xwiki.test;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.cactus.WebRequest;
import org.apache.cactus.WebResponse;
import org.hibernate.HibernateException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.DocumentStats;
import com.xpn.xwiki.stats.impl.RefererStats;
import com.xpn.xwiki.store.XWikiHibernateStore;

public class ServletStatsTest extends ServletTest {

    public void beginStats(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(hibstore, "Main", "StatsViewOkTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "StatsViewOkTest");
    }

    public void endStats(WebResponse webResponse) throws HibernateException, XWikiException {
        try {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello")!=-1);
            assertTrue("Could not find page name in content: " + result, result.indexOf("StatsViewOkTest")!=-1);
            assertTrue("Could not find hr in content: " + result, result.indexOf("<hr")!=-1);

            try {
                // We need to sleep and wait that the servlet updates the stats
                // because this is done after the response is sent to the client
                Thread.sleep(2000);
            } catch (InterruptedException e) {}

            XWikiStatsService stats = xwiki.getStatsService(context);
            XWikiDocument doc = xwiki.getDocument("Main.StatsViewOkTest", context);
            DocumentStats docstats = stats.getDocMonthStats(doc.getFullName(), "view", new Date(), context);
            assertEquals("Stats are not updated", 1, docstats.getPageViews());
        } finally {
            clientTearDown();
        }
    }

    public void testStats() throws IOException, Throwable {
        launchTest();
    }


    public void beginRefStats(WebRequest webRequest) throws HibernateException, XWikiException {
        XWikiHibernateStore hibstore = new XWikiHibernateStore(getHibpath());
        StoreHibernateTest.cleanUp(hibstore, context);
        clientSetUp(hibstore);
        String content = Utils.content1;
        Utils.content1 = "Hello $doc.name\n----\n";
        Utils.createDoc(hibstore, "Main", "StatsViewOkTest", context);
        Utils.content1 = content;
        setUrl(webRequest, "view", "StatsViewOkTest");
        webRequest.addHeader("referer","http://www.ludovic.org/");
    }

    public void endRefStats(WebResponse webResponse) throws HibernateException, XWikiException {
            String result = webResponse.getText();
            assertTrue("Could not find Hello in Content: " + result, result.indexOf("Hello")!=-1);
            assertTrue("Could not find page name in content: " + result, result.indexOf("StatsViewOkTest")!=-1);
            assertTrue("Could not find hr in content: " + result, result.indexOf("<hr")!=-1);

            try {
                // We need to sleep and wait that the servlet updates the stats
                // because this is done after the response is sent to the client
                Thread.sleep(2000);
            } catch (InterruptedException e) {}
            XWikiStatsService stats = xwiki.getStatsService(context);
            XWikiDocument doc = xwiki.getDocument("Main.StatsViewOkTest", context);
            DocumentStats docstats = stats.getDocMonthStats(doc.getFullName(), "view", new Date(), context);
            assertEquals("Stats are not updated", 1, docstats.getPageViews());
            List refstats = stats.getRefMonthStats(doc.getFullName(), new Date(), context);
            assertEquals("Ref Stats are not updated", 1, refstats.size());
            RefererStats refstat = (RefererStats) refstats.get(0);
            assertEquals("Ref Stats are not updated", "http://www.ludovic.org/", refstat.getReferer() );
            assertEquals("Ref Stats are not updated", 1, refstat.getPageViews());
            clientTearDown();
    }

    public void testRefStats() throws IOException, Throwable {
        launchTest();
    }

}
