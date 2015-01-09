/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
 */
package com.xpn.xwiki.monitor.api;

import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;

public class MonitorPlugin extends XWikiDefaultPlugin
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(MonitorPlugin.class);

    private boolean bActive;

    private long duration = 0;

    private long nbrequests = 0;

    private Map<String, MonitorTimerSummary> timerSummaries = new HashMap<>();

    private CircularFifoQueue<MonitorData> lastTimerDataList = new CircularFifoQueue<>();

    private CircularFifoQueue<MonitorData> lastUnfinishedTimerDataList = new CircularFifoQueue<>();

    private Map<Thread, MonitorData> activeTimerDataList = new HashMap<>();

    public MonitorPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
    }

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
        reset(context);
        long iActive = context.getWiki().ParamAsLong("xwiki.monitor", 0);
        setActive((iActive > 0));
    }

    public void reset(XWikiContext context)
    {
        this.timerSummaries = new HashMap<>();
        this.activeTimerDataList = new HashMap<>();
        this.duration = 0;
        this.nbrequests = 0;
        long size = context.getWiki().ParamAsLong("xwiki.monitor.lastlistsize", 20);
        this.lastTimerDataList = new CircularFifoQueue<>((int) size);
        this.lastUnfinishedTimerDataList = new CircularFifoQueue<>((int) size);
    }

    @Override
    public String getName()
    {
        return "monitor";
    }

    public void startRequest(String page, String action, URL url)
    {
        if (isActive() == false) {
            return;
        }

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = this.activeTimerDataList.get(cthread);
            if (mdata != null) {
                removeFromActiveTimerDataList(cthread);
                addToLastUnfinishedTimerDataList(mdata);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("MONITOR: Thread " + cthread.getName() + " for page " + mdata.getWikiPage()
                        + " did not call endRequest");
                }
                mdata.endRequest(false);
            }
            mdata = new MonitorData(page, action, url, cthread.getName());
            this.activeTimerDataList.put(cthread, mdata);
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR: endRequest failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    private void addToLastUnfinishedTimerDataList(MonitorData mdata)
    {
        this.lastUnfinishedTimerDataList.add(mdata);
    }

    public void endRequest()
    {
        if (isActive() == false) {
            return;
        }

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = this.activeTimerDataList.get(cthread);
            if (mdata == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("MONITOR: Thread " + cthread.getName() + " did not call startRequest");
                }
                return;
            }
            mdata.endRequest(true);
            addDuration(mdata.getDuration());
            addTimerDuration(mdata);
            removeFromActiveTimerDataList(cthread);
            addToTimerDataList(mdata);
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR: endRequest failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    private void removeFromActiveTimerDataList(Thread cthread)
    {
        if (this.activeTimerDataList.containsKey(cthread)) {
            this.activeTimerDataList.remove(cthread);
        }
    }

    private void addToTimerDataList(MonitorData mdata)
    {
        this.lastTimerDataList.add(mdata);
    }

    public void setWikiPage(String page)
    {
        if (isActive() == false) {
            return;
        }

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = this.activeTimerDataList.get(cthread);
            if (mdata != null) {
                mdata.setWikiPage(page);
            }
        } catch (Throwable e) {
        }
    }

    private void addTimerDuration(MonitorData mdata)
    {
        Map<String, MonitorTimerSummary> map = mdata.getTimerSummaries();
        Map<String, MonitorTimerSummary> gmap = getTimerSummaries();
        Iterator<MonitorTimerSummary> it = map.values().iterator();
        while (it.hasNext()) {
            MonitorTimerSummary stimer = it.next();
            MonitorTimerSummary gtimer = gmap.get(stimer.getName());
            if (gtimer == null) {
                gtimer = new MonitorTimerSummary(stimer.getName());
                gmap.put(stimer.getName(), gtimer);
            }
            gtimer.add(stimer);
        }
    }

    private void addDuration(long duration)
    {
        this.duration += duration;
        this.nbrequests++;
    }

    public CircularFifoQueue<MonitorData> getLastTimerData()
    {
        return this.lastTimerDataList;
    }

    public CircularFifoQueue<MonitorData> getLastUnfinishedTimerData()
    {
        return this.lastUnfinishedTimerDataList;
    }

    public void startTimer(String timername)
    {
        startTimer(timername, null);
    }

    public void startTimer(String timername, String desc)
    {
        if (isActive() == false) {
            return;
        }

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = this.activeTimerDataList.get(cthread);
            if (mdata != null) {
                mdata.startTimer(timername, desc);
            }
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR: startRequest for timer " + timername + " failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public void setTimerDesc(String timername, String desc)
    {
        if (isActive() == false) {
            return;
        }

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = this.activeTimerDataList.get(cthread);
            if (mdata != null) {
                mdata.setTimerDetails(timername, desc);
            }
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR: setTimerDesc for timer " + timername + " failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public void endTimer(String timername)
    {
        if (isActive() == false) {
            return;
        }

        try {
            Thread cthread = Thread.currentThread();
            MonitorData mdata = this.activeTimerDataList.get(cthread);
            if (mdata != null) {
                mdata.endTimer(timername);
            }
        } catch (Throwable e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR: endRequest for timer " + timername + " failed with exception " + e);
                e.printStackTrace();
            }
        }
    }

    public Map<Thread, MonitorData> getActiveTimerData()
    {
        return this.activeTimerDataList;
    }

    public Map<String, MonitorTimerSummary> getTimerSummaries()
    {
        return this.timerSummaries;
    }

    public long getDuration()
    {
        return this.duration;
    }

    public long getRequests()
    {
        return this.nbrequests;
    }

    public long getDuration(String timer)
    {
        MonitorTimerSummary tsummary = getTimerSummaries().get(timer);
        if (tsummary == null) {
            return 0;
        } else {
            return tsummary.getDuration();
        }
    }

    public long getNbCalls(String timer)
    {
        MonitorTimerSummary tsummary = getTimerSummaries().get(timer);
        if (tsummary == null) {
            return 0;
        } else {
            return tsummary.getNbCalls();
        }
    }

    public long getRequests(String timer)
    {
        MonitorTimerSummary tsummary = getTimerSummaries().get(timer);
        if (tsummary == null) {
            return 0;
        } else {
            return tsummary.getRequests();
        }
    }

    public boolean isActive()
    {
        return this.bActive;
    }

    public void setActive(boolean bActive)
    {
        this.bActive = bActive;
    }

}
