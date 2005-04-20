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
 * Time: 00:18:59
 */
package com.xpn.xwiki.plugin.calendar;

import java.util.Calendar;

public class CalendarEvent {
    private Calendar dateStart;
    private Calendar dateEnd;
    private String user;
    private String description;

    public CalendarEvent() {
    }

    public CalendarEvent(Calendar date, String user, String description) {
        this.dateStart = date;
        this.dateEnd = date;
        this.user = user;
        this.description = description;
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.user = user;
        this.description = description;
    }

    public Calendar getDateStart() {
        return dateStart;
    }

    public void setDateStart(Calendar dateStart) {
        this.dateStart = dateStart;
    }

    public Calendar getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Calendar dateEnd) {
        this.dateEnd = dateEnd;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


}
