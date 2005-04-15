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
 * Date: 4 déc. 2004
 * Time: 12:36:14
 */
package com.xpn.xwiki.monitor.api;

import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.XWikiContext;

import java.net.URL;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.collections.buffer.CircularFifoBuffer;

public class MonitorPlugin extends XWikiDefaultPlugin {
    private static final Log log = LogFactory.getLog(MonitorPlugin.class);

    private boolean bActive = true;
    private long duration = 0;
    private long nbrequests = 0;
    private Map timerSummaries = new HashMap();
    private CircularFifoBuffer lastTimerDataList = new CircularFifoBuffer();
    private CircularFifoBuffer lastUnfinishedTimerDataList = new CircularFifoBuffer();
    private Map activeTimerDataList = new HashMap();

    public MonitorPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
    }

    public void init(XWikiContext context) {
        super.init(context);
        long size = context.getWiki().ParamAsLong("xwiki.monitor.lastlistsize", 20);
        lastTimerDataList = new CircularFifoBuffer((int)size);
        lastUnfinishedTimerDataList = new CircularFifoBuffer((int)size);
        long iActive = context.getWiki().ParamAsLong("xwiki.monitor", 1);
        setActive((iActive>0));
    }

    public String getName() {
        return "monitor";
    }

    public void startRequest(String page, String action, URL url) {
        if (isActive()==false)
            return;

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = (MonitorData) activeTimerDataList.get(cthread);
            if (mdata!=null) {
                activeTimerDataList.remove(cthread);
                lastUnfinishedTimerDataList.add(mdata);
                if (log.isInfoEnabled()) {
                    log.info("MONITOR: Thread " + cthread.getName() + " for page " + mdata.getWikiPage() + " did not call endRequest");
                }
                mdata.endRequest(false);
            }
            mdata = new MonitorData(page, action, url, cthread.getName());
            activeTimerDataList.put(cthread, mdata);
        } catch (Throwable e) {
            if (log.isWarnEnabled()) {
                log.warn("MONITOR: endRequest failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public void endRequest() {
        if (isActive()==false)
            return;

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = (MonitorData) activeTimerDataList.get(cthread);
            if (mdata==null) {
                if (log.isInfoEnabled()) {
                    log.info("MONITOR: Thread " + cthread.getName() + " for page " + mdata.getWikiPage() + " did not call startRequest");
                }
                return;
            }
            mdata.endRequest(true);
            addDuration(mdata.getDuration());
            addTimerDuration(mdata);
            activeTimerDataList.remove(cthread);
            lastTimerDataList.add(mdata);
        } catch (Throwable e) {
            if (log.isWarnEnabled()) {
                log.warn("MONITOR: endRequest failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public void setWikiPage(String page) {
        if (isActive()==false)
            return;

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = (MonitorData) activeTimerDataList.get(cthread);
            if (mdata!=null)
              mdata.setWikiPage(page);
        } catch (Throwable e) {}
    }

    private void addTimerDuration(MonitorData mdata) {
        Map map = mdata.getTimerSummaries();
        Map gmap = getTimerSummaries();
        Iterator it = map.values().iterator();
        while (it.hasNext()) {
            MonitorTimerSummary stimer = (MonitorTimerSummary) it.next();
            MonitorTimerSummary gtimer = (MonitorTimerSummary) gmap.get(stimer.getName());
            if (gtimer==null) {
                gtimer = new MonitorTimerSummary(stimer.getName());
                gmap.put(stimer.getName(), gtimer);
            }
            gtimer.add(stimer);
        }
    }

    private void addDuration(long duration) {
        this.duration += duration;
        this.nbrequests++;
    }

    public CircularFifoBuffer getLastTimerData() {
        return lastTimerDataList;
    }

    public CircularFifoBuffer getLastUnfinishedTimerData() {
        return lastUnfinishedTimerDataList;
    }


    public void startTimer(String timername) {
        startTimer(timername, null);
    }

    public void startTimer(String timername, String desc) {
        if (isActive()==false)
            return;

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = (MonitorData) activeTimerDataList.get(cthread);
            if (mdata!=null) {
                mdata.startTimer(timername, desc);
            }
        } catch (Throwable e) {
            if (log.isWarnEnabled()) {
                log.warn("MONITOR: startRequest for timer " + timername + " failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public void setTimerDesc(String timername, String desc) {
        if (isActive()==false)
            return;

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = (MonitorData) activeTimerDataList.get(cthread);
            if (mdata!=null) {
                mdata.setTimerDetails(timername, desc);
            }
        } catch (Throwable e) {
            if (log.isWarnEnabled()) {
                log.warn("MONITOR: setTimerDesc for timer " + timername + " failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public void endTimer(String timername) {
        if (isActive()==false)
            return;

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = (MonitorData) activeTimerDataList.get(cthread);
            if (mdata!=null) {
                mdata.endTimer(timername);
            }
        } catch (Throwable e) {
            if (log.isWarnEnabled()) {
                log.warn("MONITOR: endRequest for timer " + timername + " failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public Map getActiveTimerData() {
        return activeTimerDataList;
    }

    public Map getTimerSummaries() {
        return timerSummaries;
    }

    public long getDuration() {
        return duration;
    }

    public long getRequests() {
        return nbrequests;
    }

    public long getDuration(String timer) {
        MonitorTimerSummary tsummary = (MonitorTimerSummary) getTimerSummaries().get(timer);
        if (tsummary==null)
            return 0;
        else
            return tsummary.getDuration();
    }

    public long getNbCalls(String timer) {
        MonitorTimerSummary tsummary = (MonitorTimerSummary) getTimerSummaries().get(timer);
        if (tsummary==null)
            return 0;
        else
            return tsummary.getNbCalls();
    }

    public long getRequests(String timer) {
        MonitorTimerSummary tsummary = (MonitorTimerSummary) getTimerSummaries().get(timer);
        if (tsummary==null)
            return 0;
        else
            return tsummary.getRequests();
    }

    public boolean isActive() {
        return bActive;
    }

    public void setActive(boolean bActive) {
        this.bActive = bActive;
    }

}
