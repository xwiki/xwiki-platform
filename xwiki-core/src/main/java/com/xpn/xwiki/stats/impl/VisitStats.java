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

package com.xpn.xwiki.stats.impl;

import java.util.Date;

import org.apache.commons.lang.StringUtils;

import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;

/**
 * The visit statistics database object.
 * 
 * @version $Id$
 */
public class VisitStats extends XWikiStats
{
    /**
     * The properties of visit statistics object.
     * 
     * @version $Id$
     */
    public enum Property
    {
        /**
         * The name of the property containing the number of saved pages during this visit.
         */
        pageSaves,

        /**
         * The name of the property containing the number of downloaded pages during this visit.
         */
        downloads,

        /**
         * The name of the property containing the starting date of the user visit.
         */
        startDate,

        /**
         * The name of the property containing the ending date of the user visit.
         */
        endDate,

        /**
         * The name of the property containing the unique id of the user visit.
         */
        uniqueID,

        /**
         * The name of the property containing the cookie id of the user.
         */
        cookie,

        /**
         * The name of the property containing the IP address of the user.
         */
        ip,

        /**
         * The name of the property containing the user agent of the user.
         */
        userAgent
    }

    /**
     * The previous visit object.
     */
    protected VisitStats oldObject;

    /**
     * Default {@link VisitStats} constructor.
     */
    public VisitStats()
    {
    }

    /**
     * @param user the user name.
     * @param uniqueID the visit object unique id.
     * @param cookie the cookie id.
     * @param ip the IP of the user.
     * @param userAgent the user agent of the user.
     * @param startDate the starting date of the visit.
     * @param periodType the type of the period.
     */
    public VisitStats(String user, String uniqueID, String cookie, String ip, String userAgent,
        Date startDate, PeriodType periodType)
    {
        super(startDate, periodType);

        setName(user);
        setStartDate(startDate);
        setUniqueID(uniqueID);
        setCookie(cookie);
        setIP(ip);
        setUserAgent(userAgent);
    }

    /**
     * Store previous object to be able to remove it from the database later.
     * 
     * @param vobject the previous object.
     */
    public void rememberOldObject(VisitStats vobject)
    {
        if (oldObject == null) {
            oldObject = vobject;
        }
    }

    /**
     * Set old visit object to null.
     */
    public void unrememberOldObject()
    {
        oldObject = null;
    }

    /**
     * @return the previous visit object.
     */
    public VisitStats getOldObject()
    {
        return oldObject;
    }

    /**
     * @return the number of saved pages during this visit.
     */
    public int getPageSaves()
    {
        return getIntValue(Property.pageSaves.toString());
    }

    /**
     * @param pageSaves the number of saved pages during this visit.
     */
    public void setPageSaves(int pageSaves)
    {
        setIntValue(Property.pageSaves.toString(), pageSaves);
    }

    /**
     * Add 1 to the number of saved pages during this visit.
     */
    public void incPageSaves()
    {
        setIntValue(Property.pageSaves.toString(), getPageSaves() + 1);
    }

    /**
     * @return the number of downloaded pages during this visit.
     */
    public int getDownloads()
    {
        return getIntValue(Property.downloads.toString());
    }

    /**
     * @param downloads the number of downloaded pages during this visit.
     */
    public void setDownloads(int downloads)
    {
        setIntValue(Property.downloads.toString(), downloads);
    }

    /**
     * Add 1 to the number of downloaded pages during this visit.
     */
    public void incDownloads()
    {
        setIntValue(Property.downloads.toString(), getDownloads() + 1);
    }

    /**
     * @return the starting date of the user visit.
     */
    public Date getStartDate()
    {
        return getDateValue(Property.startDate.toString());
    }

    /**
     * @param startDate the starting date of the user visit.
     */
    public void setStartDate(Date startDate)
    {
        setDateValue(Property.startDate.toString(), startDate);
    }

    /**
     * @return the ending date of the user visit.
     */
    public Date getEndDate()
    {
        return getDateValue(Property.endDate.toString());
    }

    /**
     * @param endDate the ending date of the user visit.
     */
    public void setEndDate(Date endDate)
    {
        setDateValue(Property.endDate.toString(), endDate);
    }

    /**
     * @return the unique id of the user visit.
     */
    public String getUniqueID()
    {
        return getStringValue(Property.uniqueID.toString());
    }

    /**
     * @param uniqueID the unique id of the user visit.
     */
    public void setUniqueID(String uniqueID)
    {
        // Changing the unique ID is changing the number
        if (getStartDate() != null) {
            String nb = uniqueID + getStartDate().getTime();
            setNumber(nb.hashCode());
        }

        setStringValue(Property.uniqueID.toString(), uniqueID);
    }

    /**
     * @return the cookie id of the user.
     */
    public String getCookie()
    {
        return getStringValue(Property.cookie.toString());
    }

    /**
     * @param cookie the cookie id of the user.
     */
    public void setCookie(String cookie)
    {
        setStringValue(Property.cookie.toString(), StringUtils.defaultString(cookie));
    }

    /**
     * @return the IP address of the user.
     */
    public String getIP()
    {
        return getStringValue(Property.ip.toString());
    }

    /**
     * @param ip the IP address of the user.
     */
    public void setIP(String ip)
    {
        setStringValue(Property.ip.toString(), ip);
    }

    /**
     * @return the user agent of the user.
     */
    public String getUserAgent()
    {
        return getStringValue(Property.userAgent.toString());
    }

    /**
     * @param userAgent the user agent of the user.
     */
    public void setUserAgent(String userAgent)
    {
        setStringValue(Property.userAgent.toString(), StringUtils.defaultString(userAgent));
    }

    /**
     * @return the user name.
     */
    public String getUser()
    {
        return getName();
    }

    /**
     * @param user the user name.
     */
    public void setUser(String user)
    {
        setName(user);
    }
}
