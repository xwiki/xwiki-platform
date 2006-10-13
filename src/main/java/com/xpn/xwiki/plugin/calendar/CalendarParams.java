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

package com.xpn.xwiki.plugin.calendar;

import java.util.*;

public class CalendarParams {
    private Map map = new HashMap();

    public CalendarParams() {
    }

    public CalendarParams(Map map) {
        this.map = map;
    }

    public Object get(Object key) {
        return map.get(key);
    }

    public void put(Object key, Object value) {
        map.put(key, value);
    }

    public Calendar getCalendar(Locale locale) {
        Calendar cal = Calendar.getInstance(locale);
        cal.setTime(new Date());
        String smonth = (String)get("month");
        if (smonth!=null) {
         if (smonth.indexOf("+")!=-1) {
            cal.add(Calendar.MONTH, Integer.parseInt(smonth.substring(1)));
         } else if (smonth.indexOf("-")!=-1) {
            cal.add(Calendar.MONTH, -Integer.parseInt(smonth.substring(1)));
         } else {
            cal.set(Calendar.MONTH, Integer.parseInt(smonth));
         }
        }
        String syear = (String)get("year");
        if (syear!=null) {
         if (syear.indexOf("+")!=-1) {
            cal.add(Calendar.YEAR, Integer.parseInt(syear));
         } else if (syear.indexOf("-")!=-1) {
            cal.add(Calendar.YEAR, Integer.parseInt(syear));
         } else {
            cal.set(Calendar.YEAR, Integer.parseInt(syear));
         }
        }
        return cal;
    }
}
