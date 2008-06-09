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
 *
 */

package com.xpn.xwiki.monitor.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URL;
import java.util.*;

public class MonitorData {
    private static final Log log = LogFactory.getLog(MonitorData.class);

    private URL url;
    private String wikiPage;
    private String action;
    private String threadName;
    private Date startTime;
    private Date endTime;

    private Map timers = new HashMap();
    private Map timerSummaries = new HashMap();
    private List timerList = new ArrayList();

    public MonitorData(String wikiPage, String action, URL url, String threadName) {
        this.setWikiPage(wikiPage);
        this.setURL(url);
        this.setThreadName(threadName);
        this.setStartTime(new Date());
        this.setAction(action);
    }

    public void startRequest(String page, URL url) {
        setWikiPage(page);
        setURL(url);
        setStartTime(new Date());
    }

    public void endRequest() {
        endRequest(true);
    }

    public void endRequest(boolean normal) {
        setEndTime(new Date());
        log();
    }

    public URL getURL() {
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public String getWikiPage() {
        return wikiPage;
    }

    public void setWikiPage(String page) {
        this.wikiPage = page;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public long getDuration() {
        return (endTime.getTime()-startTime.getTime());
    }

    public long getDuration(String timer) {
        MonitorTimerSummary tsummary = (MonitorTimerSummary) timerSummaries.get(timer);
        if (tsummary==null)
         return 0;
        else
         return tsummary.getDuration();
    }

    public void startTimer(String timername, String details) {

        if (startTime==null)
            return;

        MonitorTimer timer;
        timer = (MonitorTimer)timers.get(timername);
        if (timer!=null) {
          if (log.isDebugEnabled()) {
              log.debug("MONITOR: error recursive timers for " + timername);
          }
        } else {
          timer = new MonitorTimer(timername, details);
          timer.setStartDate();
          timers.put(timername, timer);
        }
    }

    public void startTimer(String timername) {
        startTimer(timername, "");
    }

    public void setTimerDetails(String timername, String details) {
        MonitorTimer timer;
        timer = (MonitorTimer)timers.get(timername);
        if (timer==null) {
          if (log.isDebugEnabled()) {
              log.debug("MONITOR: could not find timer for " + timername);
          }
        } else {
            timer.setDetails(details);
        }
    }

    public void endTimer(String timername) {
        if (startTime==null)
            return;

        MonitorTimer timer;
        timer = (MonitorTimer)timers.get(timername);
        if (timer==null) {
          if (log.isDebugEnabled()) {
              log.debug("MONITOR: could not find timer for " + timername);
          }
        } else {
          timer.setEndDate();
          if (timer.getDetails()!=null)
           timerList.add(timer);
          timers.remove(timername);
          MonitorTimerSummary tsummary = (MonitorTimerSummary) timerSummaries.get(timername);
          if (tsummary==null) {
              tsummary = new MonitorTimerSummary(timername);
              timerSummaries.put(timername, tsummary);
          }
          tsummary.addTimer(timer.getDuration());
            if (log.isDebugEnabled()) {
                   log.debug("MONITOR " + wikiPage + " " + action + " " + timer.getName() + ": " + timer.getDuration() + "ms " + timer.getDetails());
            }
        }
    }

    public List getTimerList() {
        return timerList;
    }

    public Map getTimerSummaries() {
        return timerSummaries;
    }

    public void log() {
        if (log.isDebugEnabled()) {
            log.debug("MONITOR " + wikiPage + ": " + getDuration() + "ms");
            Iterator it = timerSummaries.values().iterator();
            while (it.hasNext()) {
                MonitorTimerSummary tsummary = (MonitorTimerSummary) it.next();
                log.debug("MONITOR " + wikiPage + " " + action + " " + tsummary.getName() + ": " + tsummary.getDuration() + "ms " + tsummary.getNbCalls());
            }
        }
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public long getNbCalls(String timer) {
        MonitorTimerSummary tsummary = (MonitorTimerSummary) timerSummaries.get(timer);
        if (tsummary==null)
         return 0;
        else
         return tsummary.getNbCalls();
    }
    
    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String toString() {
        StringBuffer  str = new StringBuffer();
        str.append("WikiPage: ");
        str.append(wikiPage);
        str.append(" Action: ");
        str.append(action);
        str.append(" URL: ");
        str.append(url.toString());
        str.append(" Thread: ");
        str.append(threadName);
        str.append(" StartTime: " + startTime);
        str.append(" EndTime: " + endTime);
        str.append(getTimerSummaries());
        str.append(getTimerList());
        return str.toString();
    }
}
