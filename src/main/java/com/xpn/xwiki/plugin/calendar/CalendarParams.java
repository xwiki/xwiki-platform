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

package com.xpn.xwiki.plugin.calendar;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.xpn.xwiki.XWikiContext;

public class CalendarParams
{
    public static final String CONFIG_LOWER_BOUND = "xwiki.calendar.bound.prev";

    public static final int CONFIG_DEFAULT_LOWER_BOUND = 6;

    public static final String CONFIG_UPPER_BOUND = "xwiki.calendar.bound.next";

    public static final int CONFIG_DEFAULT_UPPER_BOUND = 12;

    private Map map = new HashMap();

    public CalendarParams()
    {
    }

    public CalendarParams(Map map)
    {
        this.map = map;
    }

    public Object get(Object key)
    {
        return map.get(key);
    }

    public void put(Object key, Object value)
    {
        map.put(key, value);
    }

    public Calendar getCalendar(Locale locale)
    {
        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(new Date());
        String smonth = (String) get("month");
        cal.set(Calendar.DAY_OF_MONTH, 1);
        try {
            if (smonth != null && !smonth.trim().equals("")) {
                if (smonth.indexOf("+") != -1) {
                    cal.add(Calendar.MONTH, Integer.parseInt(smonth.substring(1)));
                } else if (smonth.indexOf("-") != -1) {
                    cal.add(Calendar.MONTH, -Integer.parseInt(smonth.substring(1)));
                } else {
                    cal.set(Calendar.MONTH, Integer.parseInt(smonth));
                }
            }
            String syear = (String) get("year");
            if (syear != null && !syear.trim().equals("")) {
                if (syear.indexOf("+") != -1) {
                    cal.add(Calendar.YEAR, Integer.parseInt(syear));
                } else if (syear.indexOf("-") != -1) {
                    cal.add(Calendar.YEAR, Integer.parseInt(syear));
                } else {
                    cal.set(Calendar.YEAR, Integer.parseInt(syear));
                }
            }
        } catch (NumberFormatException ex) {
        }
        return cal;
    }

    public String computePrevMonthURL(XWikiContext context)
    {
        Calendar c = this.getCalendar(Locale.getDefault());

        int prevBound =
            (int) context.getWiki().ParamAsLong(CONFIG_LOWER_BOUND, CONFIG_DEFAULT_LOWER_BOUND);
        if (prevBound <= 0
            || Calendar.getInstance().get(Calendar.MONTH) - c.get(Calendar.MONTH) + 12
                * (Calendar.getInstance().get(Calendar.YEAR) - c.get(Calendar.YEAR)) < prevBound) {
            c.add(Calendar.MONTH, -1);
            return getQueryString(c);
        }
        return "";
    }

    public String computeNextMonthURL(XWikiContext context)
    {
        Calendar c = this.getCalendar(Locale.getDefault());

        int nextBound =
            (int) context.getWiki().ParamAsLong(CONFIG_UPPER_BOUND, CONFIG_DEFAULT_UPPER_BOUND);
        if (nextBound <= 0
            || c.get(Calendar.MONTH) - Calendar.getInstance().get(Calendar.MONTH) + 12
                * (c.get(Calendar.YEAR) - Calendar.getInstance().get(Calendar.YEAR)) < nextBound) {
            c.add(Calendar.MONTH, 1);
            return getQueryString(c);
        }
        return "";
    }

    public String computePrevMonthURL()
    {
        Calendar c = this.getCalendar(Locale.getDefault());
        c.add(Calendar.MONTH, -1);
        return getQueryString(c);
    }

    public String computeNextMonthURL()
    {
        Calendar c = this.getCalendar(Locale.getDefault());
        c.add(Calendar.MONTH, 1);
        return getQueryString(c);
    }

    protected String getQueryString(Calendar c)
    {
        if (c.get(Calendar.YEAR) != Calendar.getInstance().get(Calendar.YEAR)) {
            return "?year=" + c.get(Calendar.YEAR) + "&amp;month=" + c.get(Calendar.MONTH);
        }
        return "?month=" + c.get(Calendar.MONTH);
    }
}
