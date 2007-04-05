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
 * @author ludovic
 * @author sdumitriu
 */

package com.xpn.xwiki.plugin.calendar;

import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

public class CalendarPlugin extends XWikiDefaultPlugin implements XWikiPluginInterface {
    public CalendarPlugin(String name, String className, XWikiContext context) {
        super(name, className, context);
    }

    protected BaseClass getCalendarEventClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        XWiki xwiki = context.getWiki();
        boolean needsUpdate = false;

        try {
            doc = xwiki.getDocument("XWiki.CalendarEvent", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace("XWiki");
            doc.setName("CalendarEvent");
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        bclass.setName("XWiki.CalendarEvent");
        needsUpdate |= bclass.addTextField("user", "User", 30);
        needsUpdate |= bclass.addDateField("startDate", "Start Date", "dd/MM/yyyy");
        needsUpdate |= bclass.addDateField("endDate", "End Date", "dd/MM/yyyy");
        needsUpdate |= bclass.addTextField("title", "Title", 30);
        needsUpdate |= bclass.addTextAreaField("description", "Description", 40, 5);
        needsUpdate |= bclass.addTextField("url", "URL (optional)", 30);
        needsUpdate |= bclass.addTextField("location", "Location", 30);
        needsUpdate |= bclass.addStaticListField("category", "Category", 3, false, "");

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 CalendarEvent");
        }

        if (needsUpdate)
            xwiki.saveDocument(doc, context);
        return bclass;
    }

    public CalendarParams getCalendarParams(String month, String year, XWikiContext context) {
        CalendarParams cparams = new CalendarParams();
        cparams.put("month", month);
        cparams.put("year", year);
        return cparams;
    }

    public String getHTMLCalendar(CalendarParams calendarParams, String user, XWikiContext context) throws XWikiException {
        CalendarData cData = new CalendarData(user, context);
        return getHTMLCalendar(calendarParams, cData, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, XWikiDocument doc, String user, XWikiContext context) throws XWikiException {
        CalendarData cData = new CalendarData(doc, user, context);
        return getHTMLCalendar(calendarParams, cData, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, String hql, String user, XWikiContext context) throws XWikiException {
        CalendarData cData = new CalendarData(hql, user, context);
        return getHTMLCalendar(calendarParams, cData, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, String hql, int nb, XWikiContext context) throws XWikiException {
        CalendarData cData = new CalendarData(hql, nb, context);
        return getHTMLCalendar(calendarParams, cData, context);
    }

    public String getHTMLCalendar(CalendarParams calendarParams, CalendarData calendarData, XWikiContext context) throws XWikiException {
        StringBuffer output = new StringBuffer();
        Calendar cal; // For iterating through days of month
        Calendar dayCal; // First day of the displayed month
        Calendar todayCal; // Today

        Locale locale = context.getResponse().getLocale();
        todayCal = Calendar.getInstance(locale);
        // JDK bug: Romanian weeks start on monday, not sunday. This should be fixed in future JDK-s.
        fixJDKLocaleBugs(todayCal, locale);
        todayCal.setTime(new Date());

        String id = (String) calendarParams.get("id");
        if (id == null) {
            id = (String) context.get("calendarId");
            if (id == null)
                id = "1";
            else
                id = "" + (Integer.parseInt(id.trim()) + 1);
            context.put("calendarId", id);
        }
        // formatter Month-Year title of calendar
        String dateFormat = (String) calendarParams.get("dateformat");
        if (dateFormat == null)
            dateFormat = "MMMM yyyy";
        SimpleDateFormat formatTitle = new SimpleDateFormat(dateFormat, locale);

        // build week day names
        String[] mDayNames = buildDayNames(locale);

        // go back to first day in month
        dayCal = calendarParams.getCalendar(locale);
        // JDK bug: Romanian weeks start on monday, not sunday. This should be fixed in future JDK-s.
        fixJDKLocaleBugs(dayCal, locale);
        cal = (Calendar) dayCal.clone();
        cal.set(Calendar.DAY_OF_MONTH, cal.getMinimum(Calendar.DAY_OF_MONTH));
        // JDK bug: Romanian weeks start on monday, not sunday. This should be fixed in future JDK-s.
        fixJDKLocaleBugs(cal, locale);

        // go back to sunday before that: the first sunday in the calendar
        while (cal.get(Calendar.DAY_OF_WEEK) != dayCal.getFirstDayOfWeek()) {
            cal.add(Calendar.DATE, -1);
        }

        // -------------------------
        // --- draw the calendar ---
        // -------------------------
        output.append("<table summary=\"Event calendar\"");
        output.append(" id=\"wiki-calendar-table-" + id + "\"");
        output.append(" class=\"wiki-calendar-table\">");

        output.append("<tr class=\"wiki-calendar-month\">");
        output.append("<th class=\"wiki-calendar-month-nav prev-month\"><a href=\"" + calendarParams.computePrevMonthURL() + "\">&lt;</a></th>");
        output.append("<th colspan=\"5\" " + "class=\"wiki-calendar-monthyearrow\">");
        output.append(formatTitle.format(dayCal.getTime()));
        output.append("</th>");
        output.append("<th class=\"wiki-calendar-month-nav next-month\"><a href=\"" + calendarParams.computeNextMonthURL() + "\">&gt;</a></th>");
        output.append("</tr>");

        // emit the HTML calendar
        output.append("<tr class=\"wiki-calendar-daynamerow\">");
        for (int d = 0; d < 7; d++) {
            output.append("<th>");
            output.append(mDayNames[d]);
            output.append("</th>");
            continue;
        }
        output.append("</tr>");
        int weeks = (int) Math.ceil((dayCal.getActualMaximum(Calendar.DAY_OF_MONTH) + (dayCal.get(Calendar.DAY_OF_WEEK) - dayCal.getFirstDayOfWeek() + 7) % 7 ) / 7.0);
        for (int w = 0; w < weeks; w++) {
            output.append("<tr>");
            for (int d = 0; d < 7; d++) {
                String content;
                String script = (String) calendarParams.get("script");

                if (script == null)
                    content = calendarData.getContent(cal, (String) calendarParams.get("user"), (String) calendarParams.get("location"), (List) calendarParams.get("categories"), context);
                else
                    content = calendarData.getContent(cal, script, context);

                if // day is today then use today style
                ((cal.get(Calendar.DAY_OF_MONTH) == todayCal.get(Calendar.DAY_OF_MONTH))
                        && (cal.get(Calendar.MONTH) == todayCal.get(Calendar.MONTH))
                        && (cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR))) {
                    printDay(output, cal, null, content, true, cal.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH));
                } else if // day is in calendar month
                ((cal.get(Calendar.MONTH) == dayCal.get(Calendar.MONTH)) && (cal.get(Calendar.YEAR) == dayCal.get(Calendar.YEAR))) {
                    //printDayInThisMonth(output, cal, null, content);
                    printDay(output, cal, null, content, false, true);
                } else // apply day-not-in-month style ;-)
                {
                    //printDayNotInMonth(output, cal);
                    printDay(output, cal, null, content, false, false);
                }

                // increment calendar by one day
                cal.add(Calendar.DATE, 1);
            }
            output.append("</tr>");
        }

        /*
         output.append("<tr class=\"wiki-calendar-nextprev\">");
         output.append("<td colspan=\"7\" align=\"center\">");
         output.append("<a href=\""+ calendarParams.computeTodayMonthUrl()
         +"\" class=\"wiki-calendar-navbar\">"
         + msg.get("calendar.today")
         +"</a>");
         output.append("</td>");
         output.append("</tr>");
         */

        output.append("</table>");
        return output.toString();
    }

    private void printDay(StringBuffer output, Calendar cal, String url, String content, boolean today, boolean inThisMonth) {
        if (inThisMonth) {
            if (today) {
                output.append("<td class=\"wiki-calendar-today\">");
            } else {
                output.append("<td class=\"wiki-calendar-dayinmonth\">");
            }
        }   
        else {
            if (today) {
                output.append("<td class=\"wiki-calendar-today-notinmonth\">");
            } else {
                output.append("<td class=\"wiki-calendar-daynotinmonth\">");
            }
        }
        if (url != null) {
            output.append("<div class=\"wiki-calendar-daytitle\">");
            if (content != null && content.length() > 0)
                output.append("<div class=\"wiki-calendar-daytitle-hasevent\">");
            output.append("<a href=\"" + url + "\">");
            output.append(cal.get(Calendar.DAY_OF_MONTH));
            if (content != null && content.length() > 0) {
                output.append(content);
                output.append("</div>");
            }
            output.append("</a></div>");
        } else {
            output.append("<div class=\"wiki-calendar-daytitle\">");
            output.append(cal.get(Calendar.DAY_OF_MONTH));
            output.append("</div>");
            if (content != null && content.length() > 0) {
                output.append(content);
            }
        }
        output.append("</td>");
    }

    /**
     * Helper method to build the names of the weekdays. This
     * used to take place in the <code>CalendarTag</code> constructor,
     * but there, <code>mLocale</code> doesn't have the correct value yet.
     */
    private String[] buildDayNames(Locale locale) {
        // build array of names of days of week
        String[] mDayNames = new String[7];
        Calendar dayNameCal = Calendar.getInstance(locale);
        fixJDKLocaleBugs(dayNameCal, locale);
        SimpleDateFormat dayFormatter = new SimpleDateFormat("EEE", locale);
        dayNameCal.set(Calendar.DAY_OF_WEEK, dayNameCal.getFirstDayOfWeek());
        for (int dnum = 0; dnum < 7; dnum++) {
            mDayNames[dnum] = dayFormatter.format(dayNameCal.getTime());
            dayNameCal.add(Calendar.DATE, 1);
        }
        return mDayNames;
    }

    public String getName() {
        return "calendar";
    }

    public void init(XWikiContext context) {
        try {
            getCalendarEventClass(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void virtualInit(XWikiContext context) {
        try {
            getCalendarEventClass(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context) {
        return new CalendarPluginApi((CalendarPlugin) plugin, context);
    }

    public net.fortuna.ical4j.model.Calendar getCalendar(String surl, XWikiContext context) throws ParserException, IOException {
        CalendarBuilder builder = new CalendarBuilder();
        String sical = context.getWiki().getURLContent(surl);
        StringReader reader = new StringReader(sical);
        net.fortuna.ical4j.model.Calendar calendar = builder.build(reader);
        return calendar;
    }

    public net.fortuna.ical4j.model.Calendar getCalendar(String surl, String username, String password, XWikiContext context) throws ParserException,
            IOException {
        CalendarBuilder builder = new CalendarBuilder();
        String sical = context.getWiki().getURLContent(surl, username, password);
        StringReader reader = new StringReader(sical);
        net.fortuna.ical4j.model.Calendar calendar = builder.build(reader);
        return calendar;
    }

    // JDK bug: Romanian weeks start on monday, not sunday. This should be fixed in future JDK-s.
    private void fixJDKLocaleBugs(Calendar c, Locale l) {
        if("ro".equals(l.getLanguage())) {
            c.setFirstDayOfWeek(Calendar.MONDAY);
        }
    }

    /*
    private void printDayNotInMonth(StringBuffer output, Calendar cal) {
        output.append("<td class=\"wiki-calendar-daynotinmonth\">");
        output.append(cal.get(Calendar.DAY_OF_MONTH));
        output.append("&nbsp;");
        output.append("</td>");
    }

    private void printDayInThisMonth(StringBuffer output, Calendar cal, String url, String content, boolean today) {
        if (today)
            output.append("<td class=\"wiki-calendar-today\">");
        else
            output.append("<td class=\"wiki-calendar-dayinmonth\">");
        if (url != null) {
            output.append("<div class=\"wiki-calendar-daytitle\">");
            if (content != null && content.length() > 0)
                output.append("<div class=\"wiki-calendar-daytitle-hasevent\">");
            output.append("<a href=\"" + url + "\">");
            output.append(cal.get(Calendar.DAY_OF_MONTH));
            if (content != null && content.length() > 0) {
                output.append("<br />");
                output.append(content);
                output.append("</div>");
            }
            output.append("</a></div>");
        } else {
            output.append("<div class=\"wiki-calendar-daytitle\">");
            if (content != null && content.length() > 0)
                output.append("<div class=\"wiki-calendar-daytitle-hasevent\">");
            output.append(cal.get(Calendar.DAY_OF_MONTH));
            if (content != null && content.length() > 0) {
                output.append("<br />");
                output.append(content);
                output.append("</div>");
            }
            output.append("</div>");
        }
        output.append("</td>");
    }

    private void printDayInThisMonth(StringBuffer output, Calendar cal, String url, String content) {
        //printDayInThisMonth(output, cal, url, content, false);
        printDay(output, cal, url, content, false, true);
    }

    private void printToday(StringBuffer output, Calendar cal, String url, String content) {
        //printDayInThisMonth(output, cal, url, content, true);
        printDay(output, cal, url, content, true, true);
    }
    */
}
