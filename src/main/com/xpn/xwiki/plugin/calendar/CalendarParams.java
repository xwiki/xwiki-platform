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
 * Time: 10:05:18
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
