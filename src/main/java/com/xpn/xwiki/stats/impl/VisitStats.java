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
 * @author sdumitriu
 */

package com.xpn.xwiki.stats.impl;

import java.util.Date;

public class VisitStats extends XWikiStats {
    protected VisitStats oldObject = null;

    public VisitStats() {
    }

    public VisitStats(String user, String uniqueID, String cookie, String ip, String userAgent, Date startDate, int periodtype) {
        super(startDate, periodtype);
        setName(user);
        setClassName("internal");
        setStartDate(startDate);
        setUniqueID(uniqueID);
        setCookie(cookie);
        setIP(ip);
        setUserAgent(userAgent);
    }

    public void rememberOldObject(VisitStats vobject) {
        if (oldObject==null)
         oldObject = vobject;
    }

    public VisitStats getOldObject() {
        return oldObject;
    }

    public int getPageSaves() {
        return getIntValue("pageSaves");
    }

    public void  setPageSaves(int pageSaves) {
        setIntValue("pageSaves", pageSaves);
    }

    public void  incPageSaves() {
        setIntValue("pageSaves", getPageSaves() + 1);
    }

    public int getDownloads() {
        return getIntValue("downloads");
    }

    public void  setDownloads(int downloads) {
        setIntValue("downloads", downloads);
    }

    public void  incDownloads() {
        setIntValue("downloads", getDownloads() + 1);
    }

    public Date getStartDate() {
        return getDateValue("startDate");
    }

    public void  setStartDate(Date startDate) {
        setDateValue("startDate", startDate);
    }

    public Date getEndDate() {
        return getDateValue("endDate");
    }

    public void  setEndDate(Date endDate) {
        setDateValue("endDate", endDate);
    }

    public String getUniqueID() {
        return getStringValue("uniqueID");
    }

    public void setUniqueID(String uniqueID) {
        // Changing the unique ID is changing the number
        if (getStartDate()!=null) {
         String nb =  uniqueID + getStartDate().getTime();
         setNumber(nb.hashCode());
        }
        setStringValue("uniqueID", uniqueID);
    }

    public String getCookie() {
        return getStringValue("cookie");
    }

    public void setCookie(String cookie) {
        setStringValue("cookie", cookie);
    }

    public String getIP() {
        return getStringValue("ip");
    }

    public void setIP(String ip) {
        setStringValue("ip", ip);
    }

    public String getUserAgent() {
        return getStringValue("userAgent");
    }

    public void setUserAgent(String userAgent) {
        setStringValue("userAgent", userAgent);
    }

    public String getUser() {
        return getName();
    }

    public void setUser(String user) {
        setName(user);
    }

}
