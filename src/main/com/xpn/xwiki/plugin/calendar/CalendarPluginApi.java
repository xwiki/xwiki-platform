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
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.*;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.text.SimpleDateFormat;

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

    public net.fortuna.ical4j.model.Calendar getCalendar(String surl) throws ParserException, IOException {
        net.fortuna.ical4j.model.Calendar calendar = plugin.getCalendar(surl, context);
        return calendar;
    }


    public net.fortuna.ical4j.model.Calendar getCalendar(String surl, String username, String password) throws ParserException, IOException {
        net.fortuna.ical4j.model.Calendar calendar = plugin.getCalendar(surl, username, password, context);
        return calendar;
    }

    public CalendarData getCalendarEvents(String surl, String user) throws ParserException, IOException {
        net.fortuna.ical4j.model.Calendar calendar = plugin.getCalendar(surl, context);
        return fromCalendar(calendar, user);
    }

    public CalendarData getCalendarEvents(String surl, String user, String username, String password) throws ParserException, IOException {
        net.fortuna.ical4j.model.Calendar calendar = plugin.getCalendar(surl, username, password, context);
        return fromCalendar(calendar, user);
    }

    public CalendarData fromCalendar(net.fortuna.ical4j.model.Calendar calendar, String user) {
        CalendarData data = new CalendarData();
        Iterator it = calendar.getComponents().iterator();
        while (it.hasNext()) {
            Component component = (Component) it.next();
            if (component instanceof VEvent) {
                VEvent event = (VEvent) component;
                Calendar sdate = Calendar.getInstance();
                sdate.setTime(event.getStartDate().getTime());
                Calendar edate = Calendar.getInstance();
                edate.setTime(event.getEndDate().getTime());
                PropertyList prop = event.getProperties();
                Property summary = prop.getProperty("SUMMARY");
                StringBuffer newsummary = new StringBuffer((summary!=null) ? summary.getValue() : "");
                if ((sdate.get(Calendar.HOUR)!=0)||(sdate.get(Calendar.MINUTE)!=0)
                        ||(edate.get(Calendar.HOUR)!=0)||(edate.get(Calendar.MINUTE)!=0)) {
                           SimpleDateFormat sformat = new SimpleDateFormat("HH:mm");
                           newsummary.append(" ");
                           newsummary.append(sformat.format(sdate.getTime()));
                           newsummary.append("-");
                           newsummary.append(sformat.format(edate.getTime()));
                        }

                CalendarEvent cevent = new CalendarEvent(sdate, edate, user, newsummary.toString());
                data.addCalendarData(cevent);
            }
        }
        return data;
    }

    public net.fortuna.ical4j.model.Calendar toCalendar(CalendarData data) {
        Iterator it = data.getCalendarData().iterator();
        net.fortuna.ical4j.model.Calendar cal = new net.fortuna.ical4j.model.Calendar();
        while (it.hasNext()) {
            CalendarEvent cevent = (CalendarEvent) it.next();
            Calendar datestart = cevent.getDateStart();
            Calendar dateend = cevent.getDateEnd();

            VEvent vevent = new VEvent();
            float duration = (dateend.getTimeInMillis() - datestart.getTimeInMillis())/1000/60/60/24;
            if (duration>=1) {
              dateend = ((Calendar)datestart.clone());
              dateend.add(Calendar.HOUR, 24);
              RRule rule = new RRule(new Recur("DAILY", Math.round(duration)));
              vevent.getProperties().add(rule);
            }
            DtStart dtstart = new DtStart(cevent.getDateStart().getTime());
            DtEnd dtend = new DtEnd(cevent.getDateEnd().getTime());
            dtstart.getParameters().add(Value.DATE);
            dtend.getParameters().add(Value.DATE);
            vevent.getProperties().add(dtstart);
            vevent.getProperties().add(dtend);
            vevent.getProperties().add(new Summary(cevent.getDescription()));
            cal.getComponents().add(vevent);
            }
        return cal;
    }

    public String toICal(CalendarData data) {
        return toCalendar(data).toString();
    }

    public String toICal(Document doc, String user) throws XWikiException {
      CalendarData cData = new CalendarData(doc.getDocument(), user, context);
      return toICal(cData);
    }

    public String toICal(String user) throws XWikiException {
      CalendarData cData = new CalendarData(context.getDoc(), user, context);
      return toICal(cData);
    }

    public String getHTMLCalendarFromICal(CalendarParams calendarParams, String surl, String user) throws XWikiException, ParserException, IOException {
        return getPlugin().getHTMLCalendar(calendarParams, getCalendarEvents(surl, user), context);
    }

    public String getHTMLCalendarFromICal(CalendarParams calendarParams, String surl, String user, String username, String password) throws XWikiException, ParserException, IOException {
        return getPlugin().getHTMLCalendar(calendarParams, getCalendarEvents(surl, user, username, password), context);
    }

}
