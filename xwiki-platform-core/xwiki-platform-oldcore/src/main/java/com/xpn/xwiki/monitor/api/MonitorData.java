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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MonitorData
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorData.class);

    private URL url;

    private String wikiPage;

    private String action;

    private String threadName;

    private Date startTime;

    private Date endTime;

    private Map<String, MonitorTimer> timers = new HashMap<>();

    private Map<String, MonitorTimerSummary> timerSummaries = new HashMap<>();

    private List<MonitorTimer> timerList = new ArrayList<>();

    public MonitorData(String wikiPage, String action, URL url, String threadName)
    {
        this.setWikiPage(wikiPage);
        this.setURL(url);
        this.setThreadName(threadName);
        this.setStartTime(new Date());
        this.setAction(action);
    }

    public void startRequest(String page, URL url)
    {
        setWikiPage(page);
        setURL(url);
        setStartTime(new Date());
    }

    public void endRequest()
    {
        endRequest(true);
    }

    public void endRequest(boolean normal)
    {
        setEndTime(new Date());
        log();
    }

    public URL getURL()
    {
        return this.url;
    }

    public void setURL(URL url)
    {
        this.url = url;
    }

    public String getWikiPage()
    {
        return this.wikiPage;
    }

    public void setWikiPage(String page)
    {
        this.wikiPage = page;
    }

    public Date getStartTime()
    {
        return this.startTime;
    }

    public void setStartTime(Date startTime)
    {
        this.startTime = startTime;
    }

    public Date getEndTime()
    {
        return this.endTime;
    }

    public void setEndTime(Date endTime)
    {
        this.endTime = endTime;
    }

    public long getDuration()
    {
        return (this.endTime.getTime() - this.startTime.getTime());
    }

    public long getDuration(String timer)
    {
        MonitorTimerSummary tsummary = this.timerSummaries.get(timer);
        if (tsummary == null) {
            return 0;
        } else {
            return tsummary.getDuration();
        }
    }

    public void startTimer(String timername, String details)
    {

        if (this.startTime == null) {
            return;
        }

        MonitorTimer timer;
        timer = this.timers.get(timername);
        if (timer != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR: error recursive timers for " + timername);
            }
        } else {
            timer = new MonitorTimer(timername, details);
            timer.setStartDate();
            this.timers.put(timername, timer);
        }
    }

    public void startTimer(String timername)
    {
        startTimer(timername, "");
    }

    public void setTimerDetails(String timername, String details)
    {
        MonitorTimer timer;
        timer = this.timers.get(timername);
        if (timer == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR: could not find timer for " + timername);
            }
        } else {
            timer.setDetails(details);
        }
    }

    public void endTimer(String timername)
    {
        if (this.startTime == null) {
            return;
        }

        MonitorTimer timer;
        timer = this.timers.get(timername);
        if (timer == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR: could not find timer for " + timername);
            }
        } else {
            timer.setEndDate();
            if (timer.getDetails() != null) {
                this.timerList.add(timer);
            }
            this.timers.remove(timername);
            MonitorTimerSummary tsummary = this.timerSummaries.get(timername);
            if (tsummary == null) {
                tsummary = new MonitorTimerSummary(timername);
                this.timerSummaries.put(timername, tsummary);
            }
            tsummary.addTimer(timer.getDuration());
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("MONITOR " + this.wikiPage + " " + this.action + " " + timer.getName() + ": "
                    + timer.getDuration() + "ms " + timer.getDetails());
            }
        }
    }

    public List<MonitorTimer> getTimerList()
    {
        return this.timerList;
    }

    public Map<String, MonitorTimerSummary> getTimerSummaries()
    {
        return this.timerSummaries;
    }

    public void log()
    {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MONITOR " + this.wikiPage + ": " + getDuration() + "ms");
            Iterator<MonitorTimerSummary> it = this.timerSummaries.values().iterator();
            while (it.hasNext()) {
                MonitorTimerSummary tsummary = it.next();
                LOGGER.debug("MONITOR " + this.wikiPage + " " + this.action + " " + tsummary.getName() + ": "
                    + tsummary.getDuration() + "ms " + tsummary.getNbCalls());
            }
        }
    }

    public String getThreadName()
    {
        return this.threadName;
    }

    public void setThreadName(String threadName)
    {
        this.threadName = threadName;
    }

    public long getNbCalls(String timer)
    {
        MonitorTimerSummary tsummary = this.timerSummaries.get(timer);
        if (tsummary == null) {
            return 0;
        } else {
            return tsummary.getNbCalls();
        }
    }

    public String getAction()
    {
        return this.action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    @Override
    public String toString()
    {
        StringBuffer str = new StringBuffer();
        str.append("WikiPage: ");
        str.append(this.wikiPage);
        str.append(" Action: ");
        str.append(this.action);
        str.append(" URL: ");
        str.append(this.url.toString());
        str.append(" Thread: ");
        str.append(this.threadName);
        str.append(" StartTime: " + this.startTime);
        str.append(" EndTime: " + this.endTime);
        str.append(getTimerSummaries());
        str.append(getTimerList());
        return str.toString();
    }
}
