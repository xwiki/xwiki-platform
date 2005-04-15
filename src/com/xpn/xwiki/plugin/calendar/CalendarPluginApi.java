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
 * Date: 7 nov. 2004
 * Time: 16:46:12
 */
package com.xpn.xwiki.plugin.calendar;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;

import java.util.Calendar;
import java.util.Date;

public class CalendarPluginApi extends Api {
    private CalendarPlugin plugin;

    public CalendarPluginApi(CalendarPlugin plugin, XWikiContext context) {
        super(context);
        setPlugin(plugin);
    }

    public CalendarParams getCalendarParams(String month, String year) {
        return getPlugin().getCalendarParams(month, year, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, String user) throws XWikiException {
        return getPlugin().getHTMLCalendar(calendarParams, user, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, Document doc, String user) throws XWikiException {
        return getPlugin().getHTMLCalendar(calendarParams, doc.getDocument(), user, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, String hql, String user) throws XWikiException {
        return getPlugin().getHTMLCalendar(calendarParams, hql, user, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, String hql, int nb) throws XWikiException {
        return getPlugin().getHTMLCalendar(calendarParams, hql, nb, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, CalendarData calendarData) throws XWikiException
    {
        return getPlugin().getHTMLCalendar(calendarParams, calendarData, context);
    }

    public CalendarParams getCalendarParams() {
        return new CalendarParams();
    }

    public CalendarEvent getCalendarEvent() {
        return new CalendarEvent();
    }

    public CalendarEvent getCalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description) {
        return new CalendarEvent(dateStart, dateEnd, user, description);
    }

    public CalendarEvent getCalendarEvent(Date dateStart, Date dateEnd, String user, String description) {
        Calendar cdateStart = Calendar.getInstance();
        cdateStart.setTime(dateStart);
        Calendar cdateEnd = Calendar.getInstance();
        cdateEnd.setTime(dateEnd);
        return getCalendarEvent(cdateStart, cdateEnd, user, description);
    }


    public CalendarPlugin getPlugin() {
        return plugin;
    }

    public void setPlugin(CalendarPlugin plugin) {
        this.plugin = plugin;
    }

    public Calendar getCalendar(long time) {
        Calendar cal = Calendar.getInstance(context.getResponse().getLocale());
        cal.setTime(new Date(time));
        return cal;
    }

    public Calendar getCalendar() {
        Calendar cal = Calendar.getInstance(context.getResponse().getLocale());
        cal.setTime(new Date());
        return cal;
    }

}
