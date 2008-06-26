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

package com.xpn.xwiki.plugin.calendar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.objects.LargeStringProperty;
import com.xpn.xwiki.objects.StringListProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.render.XWikiVelocityRenderer;

/**
 * CalendarData stores a list of events that can be displayed in an Event Calendar. The internal data is of type
 * {@link com.xpn.xwiki.plugin.calendar.CalendarEvent}. The list can be populated with CalendarEvent XWiki objects from
 * a document, with document names retrieved by a custom hibernate query, or with recently changed document names.
 */
public class CalendarData
{
    private Map cdata = new HashMap();

    /**
     * Default constructor. Leaves the list of events empty.
     */
    public CalendarData()
    {
    }

    /**
     * The most used constructor, which populates the list with CalendarEvent XWiki objects from the current document.
     * 
     * @param user The default user to be used.
     * @param context The request context
     * @throws XWikiException
     */
    public CalendarData(String user, XWikiContext context) throws XWikiException
    {
        addCalendarData(context.getDoc(), user);
    }

    public CalendarData(XWikiDocument doc, String user, XWikiContext context) throws XWikiException
    {
        addCalendarData(doc, user);
    }

    public CalendarData(String hql, String user, XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        List list = xwiki.getStore().searchDocumentsNames(hql, context);
        for (int i = 0; i < list.size(); i++) {
            String docname = (String) list.get(i);
            XWikiDocument doc = xwiki.getDocument(docname, context);
            addCalendarData(doc, user);
        }
    }

    public CalendarData(String hql, int nb, XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        List list = xwiki.getStore().searchDocumentsNames(hql, nb, 0, context);
        SimpleDateFormat df = new SimpleDateFormat("yyMMdd");
        for (int i = 0; i < list.size(); i++) {
            String docname = (String) list.get(i);
            XWikiDocument doc = xwiki.getDocument(docname, context);
            Date date = doc.getDate();
            Calendar cdate = Calendar.getInstance();
            cdate.setTime(date);
            this.cdata.put(df.format(date), new CalendarEvent(cdate, cdate, "", "[" + doc.getName() + ">"
                + doc.getFullName() + "] by " + context.getWiki().getLocalUserName(doc.getAuthor(), context)));
        }
    }

    public List getCalendarData()
    {
        List ldata = new ArrayList();
        List sortedKeys = new ArrayList(this.cdata.keySet());
        java.util.Collections.sort(sortedKeys);
        for (Iterator it = sortedKeys.iterator(); it.hasNext();) {
            ldata.addAll((List) this.cdata.get(it.next()));
        }
        return ldata;
    }

    public Map getMappedCalendarData()
    {
        return this.cdata;
    }

    public List getCalendarData(Calendar date)
    {
        Object result = this.cdata.get(new SimpleDateFormat("yyMMdd").format(date.getTime()));
        if (result == null) {
            result = new ArrayList();
        }
        return (List) result;
    }

    /**
     * List populating method. It iterates the CalendarEvent objects stored in the given document and creates Java
     * wrappers that can be used by the Calendar plugin.
     * 
     * @param doc The source document, populated with CalendarEvent objects.
     * @param defaultUser The username to be used ig objects do not have a User field.
     * @throws XWikiException
     */
    public void addCalendarData(XWikiDocument doc, String defaultUser) throws XWikiException
    {
        if (doc == null) {
            return;
        }
        if (defaultUser == null) {
            BaseObject bobj = doc.getObject("XWiki.XWikiUsers");
            if (bobj == null) {
                defaultUser = doc.getCreator();
            } else {
                defaultUser = doc.getFullName();
            }
        }

        String defaultDescription = "";
        defaultDescription = "[" + doc.getFullName() + "]";

        Vector bobjs = doc.getObjects("XWiki.CalendarEvent");
        if (bobjs != null) {
            for (int i = 0; i < bobjs.size(); ++i) {
                try {
                    BaseObject bobj = (BaseObject) bobjs.get(i);
                    String user = "";
                    try {
                        user = (String) ((StringProperty) bobj.get("user")).getValue();
                    } catch (Exception e) {
                    }

                    String description = "";
                    try {
                        description = (String) ((LargeStringProperty) bobj.get("description")).getValue();
                    } catch (Exception e) {
                    }

                    String title = "";
                    try {
                        title = (String) ((StringProperty) bobj.get("title")).getValue();
                    } catch (Exception e) {
                    }

                    String url = "";
                    try {
                        url = (String) ((StringProperty) bobj.get("url")).getValue();
                    } catch (Exception e) {
                    }

                    String location = "";
                    try {
                        location = (String) ((StringProperty) bobj.get("location")).getValue();
                    } catch (Exception e) {
                    }

                    List category = null;
                    try {
                        category = (List) ((StringListProperty) bobj.get("category")).getValue();
                    } catch (Exception e) {
                    }

                    Date dateStart = null;
                    try {
                        dateStart = (Date) ((DateProperty) bobj.get("startDate")).getValue();
                    } catch (Exception e) {
                    }

                    Date dateEnd = null;
                    try {
                        dateEnd = (Date) ((DateProperty) bobj.get("endDate")).getValue();
                    } catch (Exception e) {
                    }

                    if ((user == null) || user.equals("")) {
                        user = defaultUser;
                    }

                    if (dateStart == null) {
                        dateStart = dateEnd;
                    }
                    if (dateEnd == null) {
                        dateEnd = dateStart;
                    }

                    if ((dateStart == null) || (dateEnd == null)) {
                        continue;
                    }

                    if (dateStart.getTime() > dateEnd.getTime()) {
                        Date dateTemp = dateStart;
                        dateStart = dateEnd;
                        dateEnd = dateTemp;
                    }

                    if ((description == null) || description.equals("")) {
                        description = defaultDescription;
                    }

                    Calendar cdateStart = Calendar.getInstance();
                    cdateStart.setTime(dateStart);
                    Calendar cdateEnd = Calendar.getInstance();
                    cdateEnd.setTime(dateEnd);
                    addCalendarData(new CalendarEvent(cdateStart, cdateEnd, user, description, title, category, url,
                        location));
                } catch (Exception e) {
                    // Let's continue in case of failure
                    e.printStackTrace();
                }
            }
        }
    }

    public String getContent(Calendar tddate, XWikiContext context)
    {
        return getContent(tddate, null, null, null, context);
    }

    public String getContent(Calendar tddate, String filteredUser, String filteredLocation, List filteredCategories,
        XWikiContext context)
    {
        StringBuffer result = new StringBuffer();
        for (Iterator it = getCalendarData(tddate).iterator(); it.hasNext();) {
            CalendarEvent event = (CalendarEvent) it.next();
            String user = event.getUser();
            if (!StringUtils.isBlank(filteredUser) && (!filteredUser.trim().equals(user))) {
                continue;
            }
            String location = event.getLocation();
            if (!StringUtils.isBlank(filteredLocation) && (!filteredLocation.trim().equals(location))) {
                continue;
            }
            List categories;
            if (filteredCategories != null && filteredCategories.size() > 0) {
                categories = new ArrayList(event.getCategory());
                categories.retainAll(filteredCategories);
                if (categories.size() <= 0) {
                    continue;
                }
            }
            categories = event.getCategory();
            String title = event.getTitle();
            String url = event.getUrl();
            result.append("<div class=\"event");
            if (categories != null && categories.size() > 0) {
                for (Iterator cit = categories.iterator(); cit.hasNext();) {
                    result.append(" " + cit.next());
                }
            }
            result.append("\">");
            if (!StringUtils.isBlank(user)) {
                result.append("<span class=\"username\">"
                    + context.getWiki().getLocalUserName(event.getUser(), context) + "</span>");
            }
            if (!StringUtils.isBlank(user) && !StringUtils.isBlank(title)) {
                result.append(": ");
            }
            if (!StringUtils.isBlank(url)) {
                result.append("<a href=\"" + url + "\">");
            }
            result.append(title);
            if (!StringUtils.isBlank(url)) {
                result.append("</a>");
            }
            result.append("</div>");
        }
        return result.toString();
    }

    public String getContent(Calendar tddate, String velocityScript, XWikiContext context)
    {
        VelocityContext vcontext = new VelocityContext();
        vcontext.put("date", tddate);
        List events = getCalendarData(tddate);
        vcontext.put("events", events);
        return XWikiVelocityRenderer.evaluate(velocityScript, "<calendar displaying code>", vcontext, context);
    }

    public void addCalendarData(CalendarEvent event)
    {
        SimpleDateFormat df = new SimpleDateFormat("yyMMdd");
        Calendar cdateStart = event.getDateStart();
        String dateEnd = df.format(event.getDateEnd().getTime());
        Calendar crtDate = (Calendar) cdateStart.clone();
        List evtList;
        do {
            evtList = (List) this.cdata.get(df.format(crtDate.getTime()));
            if (evtList == null) {
                evtList = new ArrayList();
            }
            evtList.add(event);
            this.cdata.put(df.format(crtDate.getTime()), evtList);
            crtDate.add(Calendar.DATE, 1);
        } while (df.format(crtDate.getTime()).compareTo(dateEnd) <= 0);
    }

    public void addCalendarData(Calendar dateStart, Calendar dateEnd, String user, String description)
    {
        addCalendarData(new CalendarEvent(dateStart, dateEnd, user, description));
    }

    public void addCalendarData(Date dateStart, Date dateEnd, String user, String description)
    {
        Calendar cdateStart = Calendar.getInstance();
        cdateStart.setTime(dateStart);
        Calendar cdateEnd = Calendar.getInstance();
        cdateEnd.setTime(dateEnd);
        addCalendarData(cdateStart, cdateEnd, user, description);
    }
}
