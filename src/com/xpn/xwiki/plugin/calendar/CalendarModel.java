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
 * Original Code from: the JRoller Project
 * Date: 9 oct. 2004
 * Time: 19:45:06
 */
package com.xpn.xwiki.plugin.calendar;

import java.util.Calendar;
import java.util.Date;

/**
 * Model interface for the CalendarTag. The CalendarTag will set a day,
 * then use the computeUrl method to get the URLs it needs.
 */
public interface CalendarModel
{
    public Calendar getCalendar();

	public void setDay( String month ) throws Exception;

	public Date getDay();

    public Date getNextMonth();

    public String computePrevMonthUrl();

    public String computeTodayMonthUrl();

    public String computeNextMonthUrl();

    /**
     * Create URL for use on edit-weblog page, preserves the request
     * parameters used by the tabbed-menu tag for navigation.
     *
     * @param day   Day for URL
     * @param valid Always return a URL, never return null
     * @return URL for day, or null if no weblog entry on that day
     */
    public String computeUrl( java.util.Date day, boolean valid );

    /**
     * Get calendar cell content or null if none.
     *
     * @param day Day for URL
     * @param valid Always return a URL, never return null
     * @return Calendar cell content or null if none.
     */
    public String getContent( java.util.Date day );
}