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
 * Date: 10 oct. 2004
 * Time: 00:07:29
 */
package com.xpn.xwiki.plugin.calendar;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.StringProperty;

import java.util.*;

public class CalendarData {
    private List cdata = new ArrayList();

    public CalendarData() {
    }

    public CalendarData(String user, XWikiContext context) throws XWikiException {
        addCalendarData(context.getDoc(), user);
    }

    public CalendarData(XWikiDocument doc, String user, XWikiContext context) throws XWikiException {
        addCalendarData(doc, user);
    }

    public CalendarData(String hql, String user, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        List list = xwiki.getStore().searchDocumentsNames(hql, context);
        for (int i=0;i<list.size();i++) {
            String docname = (String) list.get(i);
            XWikiDocument doc = xwiki.getDocument(docname, context);
            addCalendarData(doc, user);
        }
    }

    public CalendarData(String hql, int nb, XWikiContext context) throws XWikiException {
        XWiki xwiki = context.getWiki();
        List list = xwiki.getStore().searchDocumentsNames(hql, nb, 0, context);
        for (int i=0;i<list.size();i++) {
            String docname = (String) list.get(i);
            XWikiDocument doc = xwiki.getDocument(docname, context);
            Date date = doc.getDate();
            Calendar cdate = Calendar.getInstance();
            cdate.setTime(date);
            cdata.add(new CalendarEvent(cdate, cdate, "",
                    "[" + doc.getName() + ">" + doc.getFullName() + "] by " + context.getWiki().getLocalUserName(doc.getAuthor(), context) ));
        }
    }

    public List getCalendarData() {
        return cdata;
    }

    public void addCalendarData(XWikiDocument doc, String defaultUser) throws XWikiException {
        if (defaultUser==null) {
            BaseObject bobj = doc.getObject("XWiki.XWikiUsers");
            if (bobj==null) {
                defaultUser = doc.getCreator();
            } else {
                defaultUser = doc.getFullName();
            }
        }
        String defaultDescription = "[" + doc.getFullName() + "]";

        Vector bobjs = doc.getObjects("XWiki.CalendarEvent");
        if (bobjs!=null) {
        for (int i=0;i<bobjs.size();i++) {
            try {
            BaseObject bobj = (BaseObject) bobjs.get(i);
            String user = "";

            try {
                user = (String) ((StringProperty)bobj.get("user")).getValue();
            } catch (Exception e) {}
            String description = "";

            try {
                description = (String) ((StringProperty)bobj.get("description")).getValue();
            } catch (Exception e) {}

            Date dateStart = null;
            try {
                dateStart = (Date) ((DateProperty) bobj.get("startDate")).getValue();
            } catch (Exception e) {}


            Date dateEnd = null;
            try {
                dateEnd = (Date) ((DateProperty) bobj.get("endDate")).getValue();
            } catch (Exception e) {}

            if ((user==null)||user.equals("")) {
                user = defaultUser;
            }

            if ((dateStart==null)&&(dateEnd==null))
                continue;

            if (dateStart==null)
             dateStart = dateEnd;
            if (dateEnd==null)
             dateEnd = dateStart;

            if (dateStart.getTime()>dateEnd.getTime()) {
                Date dateTemp = dateStart;
                dateStart = dateEnd;
                dateEnd = dateTemp;
            }

            if ((description==null)||description.equals("")) {
                description = defaultDescription;
            }

            Calendar cdateStart = Calendar.getInstance();
            cdateStart.setTime(dateStart);
            Calendar cdateEnd = Calendar.getInstance();
            cdateEnd.setTime(dateEnd);
            cdata.add(new CalendarEvent(cdateStart, cdateEnd, user, description));
            } catch (Exception e) {
                // Let's continue in case of failure
                e.printStackTrace();
            }

        }
        }
    }

    public String getContent(Calendar tddate, XWikiContext context) {
        StringBuffer result = new StringBuffer();
        for (int i=0;i<cdata.size();i++) {
            CalendarEvent event = (CalendarEvent) cdata.get(i);
            int idate = tddate.get(Calendar.YEAR) * 1000 + tddate.get(Calendar.DAY_OF_YEAR);
            int isdate = event.getDateStart().get(Calendar.YEAR) * 1000 + event.getDateStart().get(Calendar.DAY_OF_YEAR);
            Calendar dtend = (Calendar) event.getDateEnd().clone();
            // dtend.add(Calendar.SECOND, -1);
            int iedate = dtend.get(Calendar.YEAR) * 1000 + dtend.get(Calendar.DAY_OF_YEAR);
            if ((idate>= isdate)&&(idate<=iedate)) {
                    StringBuffer message = new StringBuffer();
                    String user = event.getUser();
                    if ((user!=null)&&(!user.equals("")))
                     message.append(context.getWiki().getLocalUserName(event.getUser(), context));
                    String desc = event.getDescription();
                    if ((desc!=null)&&(!desc.trim().equals(""))&&(!message.toString().trim().equals("")))
                      message.append(": ");
                    message.append(desc);
                    message.append("<br />");
                    result.append(message);
                }
        }
        return result.toString();
    }

    public void addCalendarData(CalendarEvent event) {
        cdata.add(event);
    }

    public void addCalendarData(Calendar dateStart, Calendar dateEnd, String user, String description) {
        addCalendarData(new CalendarEvent(dateStart, dateEnd, user, description));
    }

    public void addCalendarData(Date dateStart, Date dateEnd, String user, String description) {
     Calendar cdateStart = Calendar.getInstance();
     cdateStart.setTime(dateStart);
     Calendar cdateEnd = Calendar.getInstance();
     cdateEnd.setTime(dateEnd);
     addCalendarData(cdateStart, cdateEnd, user, description);
    }
 }
