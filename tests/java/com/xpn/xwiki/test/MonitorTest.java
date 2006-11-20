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
 * @author vmassol
 * @author sdumitriu
 */

package com.xpn.xwiki.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;

import org.apache.commons.collections.buffer.CircularFifoBuffer;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.monitor.api.MonitorData;
import com.xpn.xwiki.monitor.api.MonitorPlugin;
import com.xpn.xwiki.monitor.api.MonitorTimer;
import com.xpn.xwiki.web.XWikiServletURLFactory;

public class MonitorTest extends HibernateTestCase  {

    private String surl = "http://127.0.0.1:9080/xwiki/bin/view/Main/WebHome";
    private String page = "xwiki:Main.WebHome1";
    private String action = "view";

    private XWikiContext context2;
    private String surl2 = "http://127.0.0.1:9080/xwiki/bin/view/Main/WebHome2";
    private String page2 = "xwiki:Main.WebHome2";
    private String action2 = "view";


    public void setUp() throws Exception {
        super.setUp();
        getXWiki().getPluginManager().addPlugin("monitor","com.xpn.xwiki.monitor.api.MonitorPlugin", getXWikiContext());
        getXWikiContext().setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));

        context2 = new XWikiContext();
        context2.setWiki(getXWiki());
        context2.setURLFactory(new XWikiServletURLFactory(new URL("http://www.xwiki.org/"), "xwiki/" , "bin/"));
     }

    public void testMonitorData() throws MalformedURLException {
        MonitorData monitordata = new MonitorData( page, action, new URL(surl), "");
        assertEquals("Monitor URL is incorrect", surl, monitordata.getURL().toString());
        assertEquals("Monitor Wiki page is incorrect", page, monitordata.getWikiPage());
        Date time1 = new Date();
        monitordata.startRequest(page, new URL(surl));
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
        }
        monitordata.endRequest();
        Date time2 = new Date();
        assertTrue("Time is smaller than sleep duraction: " + monitordata.getDuration(), (monitordata.getDuration()>=3000));
        assertTrue("Surrounding time is smaller than request time: " + monitordata.getDuration() + " " + (time2.getTime()-time1.getTime()), (time2.getTime()-time1.getTime())>=monitordata.getDuration());
        assertTrue("Surrounding time is smaller than sql time: " + (time2.getTime()-time1.getTime()) + " " + monitordata.getDuration("sql"), (time2.getTime()-time1.getTime())>=monitordata.getDuration("sql"));
    }

    public void testMonitorDataWithTimerDetails() throws MalformedURLException {
        MonitorData monitordata = new MonitorData( page, action, new URL(surl), "");
        assertEquals("Monitor URL is incorrect", surl, monitordata.getURL().toString());
        assertEquals("Monitor Wiki page is incorrect", page, monitordata.getWikiPage());
        Date time1 = new Date();
        monitordata.startRequest(page, new URL(surl));
        try { Thread.sleep(510);} catch (InterruptedException e) {}
        monitordata.startTimer("sql", "select count(*) from toto");
        try { Thread.sleep(1600);} catch (InterruptedException e) {}
        monitordata.endTimer("sql");
        try { Thread.sleep(510);} catch (InterruptedException e) {}
        monitordata.endRequest();
        Date time2 = new Date();
        assertTrue("Time is smaller than sleep duraction", (monitordata.getDuration()>=2000));
        assertTrue("SQL Time is smaller than sleep duraction", (monitordata.getDuration("sql")>=1000));
        assertTrue("Surrounding time is smaller than request time", (time2.getTime()-time1.getTime())>=monitordata.getDuration());
        assertTrue("Surrounding time is smaller than sql time", (time2.getTime()-time1.getTime())>=monitordata.getDuration("sql"));
        assertEquals("Timer list is of incorrect size", monitordata.getTimerList().size(), 1);
        MonitorTimer timer = (MonitorTimer) monitordata.getTimerList().get(0);
        assertEquals("Timer details are incorrect", timer.getDetails(), "select count(*) from toto");
        assertTrue("Timer time is incorrect", (timer.getDuration()>=1000));
    }

    public void testMonitorDataWithoutTimerDetails() throws MalformedURLException {
        MonitorData monitordata = new MonitorData( page, action, new URL(surl), "");
        monitordata.setURL(new URL(surl));
        assertEquals("Monitor URL is incorrect", surl, monitordata.getURL().toString());
        monitordata.setWikiPage(page);
        assertEquals("Monitor Wiki page is incorrect", page, monitordata.getWikiPage());
        Date time1 = new Date();
        monitordata.startRequest(page, new URL(surl));
        try { Thread.sleep(110);} catch (InterruptedException e) {}
        monitordata.startTimer("sql", null);
        try { Thread.sleep(1100);} catch (InterruptedException e) {}
        monitordata.endTimer("sql");
        try { Thread.sleep(100);} catch (InterruptedException e) {}
        monitordata.endRequest();
        Date time2 = new Date();
        assertTrue("Time is smaller than sleep duraction: " + monitordata.getDuration(), (monitordata.getDuration()>=1200));
        assertTrue("SQL Time is smaller than sleep duraction: " + monitordata.getDuration("sql"), (monitordata.getDuration("sql")>=1000));
        assertTrue("Surrounding time is smaller than request time", (time2.getTime()-time1.getTime())>=monitordata.getDuration());
        assertTrue("Surrounding time is smaller than sql time", (time2.getTime()-time1.getTime())>=monitordata.getDuration("sql"));
        assertEquals("Timer list should be zero because we have no details", monitordata.getTimerList().size(), 0);
    }

    public void testGetPlugin() {
        MonitorPlugin monitor = (MonitorPlugin) getXWiki().getPlugin("monitor", getXWikiContext());
        assertNotNull("Monitor Plugin is null", monitor);
    }

    public void testGetMonitorData() throws MalformedURLException {
        MonitorPlugin monitor = (MonitorPlugin) getXWiki().getPlugin("monitor", getXWikiContext());
        assertNotNull("Monitor Plugin is null",  monitor);
        long duration = 1100;
        // Test
        monitor.startRequest(page, action, new URL(surl));
        try { Thread.sleep(duration);} catch (InterruptedException e) {}
        monitor.startTimer("sql");
        try { Thread.sleep(duration);} catch (InterruptedException e) {}
        monitor.endTimer("sql");
        try { Thread.sleep(duration);} catch (InterruptedException e) {}
        monitor.startTimer("sql");
        try { Thread.sleep(duration);} catch (InterruptedException e) {}
        monitor.endTimer("sql");
        try { Thread.sleep(duration);} catch (InterruptedException e) {}

        // Verify it is empty
        CircularFifoBuffer list = monitor.getLastTimerData();
        assertEquals("Timer Data list is incorrect", 0, list.size());

        monitor.endRequest();

        list = monitor.getLastTimerData();
        assertEquals("Timer Data list is incorrect", 1, list.size());
        MonitorData mdata1 = (MonitorData) list.toArray()[0];
        // List is inverted.. last one first.. it should be the first request that was longer
        assertEquals("Timer Data list is incorrect", "xwiki:Main.WebHome1", mdata1.getWikiPage());
        assertTrue("Timer Data 1 is incorrect: " + mdata1.getDuration(), mdata1.getDuration()>=1500);
        assertTrue("Timer Total is incorrect: " + monitor.getDuration(), monitor.getDuration()>=2000);
    }

    public void testGetMonitorDataWithThread(MonitorPlugin monitor, int test, long duration1, long duration2) throws MalformedURLException {
        assertNotNull("Monitor Plugin is null",  monitor);
        MonitorTestThread monitorthread1 = new MonitorTestThread(getXWiki(), getXWikiContext(), page, action, new URL(surl), duration1, test);
        Thread t1 = new Thread(monitorthread1);
        MonitorTestThread monitorthread2 = new MonitorTestThread(getXWiki(), context2, page2, action, new URL(surl2), duration2, test);
        Thread t2 = new Thread(monitorthread2);
        t1.start();
        t2.start();

        while (t2.isAlive()) {
            try {
                t2.join();
            } catch (InterruptedException e) {
            }
        }
        // Verify it is empty
        Map map = monitor.getActiveTimerData();
        assertEquals("Active Timer Data Map is incorrect", 1, map.size());
        CircularFifoBuffer list = monitor.getLastTimerData();
        assertEquals("Intermediary Timer Data list is incorrect", 1, list.size());

        while (t1.isAlive()) {
            try {
                t1.join();
            } catch (InterruptedException e) {
            }
        }
    }

    public void testGetMonitorDataWithThread1() throws MalformedURLException {
        MonitorPlugin monitor = (MonitorPlugin) getXWiki().getPlugin("monitor", getXWikiContext());
        testGetMonitorDataWithThread(monitor, 1, 1100, 110);
        CircularFifoBuffer list = monitor.getLastTimerData();
        assertEquals("Timer Data list is incorrect", 2, list.size());
        MonitorData mdata1 = (MonitorData) list.toArray()[1];
        MonitorData mdata2 = (MonitorData) list.toArray()[0];
        // List is inverted.. last one first.. it should be the first request that was longer
        assertEquals("Timer Data list is incorrect", "xwiki:Main.WebHome1", mdata1.getWikiPage());
        assertEquals("Timer Data list is incorrect", "xwiki:Main.WebHome2", mdata2.getWikiPage());
        assertTrue("Timer Data 1 is incorrect: " + mdata1.getDuration(), mdata1.getDuration()>=1000);
        assertTrue("Timer Data 2 is incorrect: " + mdata2.getDuration(), mdata2.getDuration()>=100);
        assertTrue("Timer Total is incorrect: " + monitor.getDuration(), monitor.getDuration()>=1100);
    }

    public void testGetMonitorDataWithThread2() throws MalformedURLException {
        MonitorPlugin monitor = (MonitorPlugin) getXWiki().getPlugin("monitor", getXWikiContext());
        testGetMonitorDataWithThread(monitor, 2, 1100, 110);
        CircularFifoBuffer list = monitor.getLastTimerData();
        assertEquals("Timer Data list is incorrect", 2, list.size());
        MonitorData mdata1 = (MonitorData) list.toArray()[1];
        MonitorData mdata2 = (MonitorData) list.toArray()[0];
        // List is inverted.. last one first.. it should be the first request that was longer
        assertEquals("Timer Data list is incorrect", "xwiki:Main.WebHome1", mdata1.getWikiPage());
        assertEquals("Timer Data list is incorrect", "xwiki:Main.WebHome2", mdata2.getWikiPage());
        assertTrue("Timer Data 1 is incorrect: " + mdata1.getDuration(), mdata1.getDuration()>=5000);
        assertTrue("Timer Data 2 is incorrect: " + mdata2.getDuration(), mdata2.getDuration()>=500);
        assertTrue("Timer Total is incorrect: " + monitor.getDuration(), monitor.getDuration()>=5500);
        assertTrue("Timer Data 1 is incorrect: " + mdata1.getDuration("sql"), mdata1.getDuration("sql")>=2000);
        assertTrue("Timer Data 2 is incorrect: " + mdata2.getDuration("sql"), mdata2.getDuration("sql")>=200);
        assertEquals("Timer Data 1 is incorrect", 2, mdata1.getNbCalls("sql"));
        assertEquals("Timer Data 2 is incorrect", 2, mdata2.getNbCalls("sql"));
        assertTrue("Timer Total is incorrect: " + monitor.getDuration("sql"), monitor.getDuration("sql")>=2200);
        assertTrue("Timer Total is incorrect: " + monitor.getDuration(), monitor.getDuration()>=5500);
        assertEquals("Timer Total is incorrect", 4, monitor.getNbCalls("sql"));
        assertEquals("Timer Total is incorrect", 2, monitor.getRequests("sql"));
        assertEquals("Timer Total is incorrect", 2, monitor.getRequests());
    }

    public class MonitorTestThread implements Runnable {
        private XWiki xwiki;
        private XWikiContext context;
        private String page;
        private String action;
        private URL url;
        private long duration;
        private int test = 0;

        public MonitorTestThread(XWiki xwiki, XWikiContext context, String page, String action, URL url, long duration, int test) {
            this.xwiki = xwiki;
            this.context = context;
            this.url = url;
            this.page = page;
            this.duration = duration;
            this.test = test;
            this.action = action;
        }

        public void run1(MonitorPlugin monitor) {
            try {
                Thread.sleep(duration);
            } catch (InterruptedException e) {
            }
        }

        public void run2(MonitorPlugin monitor) {
            try { Thread.sleep(duration);} catch (InterruptedException e) {}
            monitor.startTimer("sql");
            try { Thread.sleep(duration);} catch (InterruptedException e) {}
            monitor.endTimer("sql");
            try { Thread.sleep(duration);} catch (InterruptedException e) {}
            monitor.startTimer("sql");
            try { Thread.sleep(duration);} catch (InterruptedException e) {}
            monitor.endTimer("sql");
            try { Thread.sleep(duration);} catch (InterruptedException e) {}
        }

        public void run() {
            context.setWiki(xwiki);
            MonitorPlugin monitor = (MonitorPlugin) xwiki.getPlugin("monitor", context);
            monitor.startRequest(page, action, url);
            switch (test) {
                case 1:
                    run1(monitor);
                    break;
                case 2:
                    run2(monitor);
                    break;
            }
            monitor.endRequest();
        }
    }



}
