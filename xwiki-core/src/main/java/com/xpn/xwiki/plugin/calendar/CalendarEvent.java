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

import java.util.Calendar;
import java.util.List;

public class CalendarEvent {
    private Calendar dateStart;
    private Calendar dateEnd;
    private String title;
    private String description;
    private String user;
    private List category;
    private String url;
    private String location;

    public CalendarEvent() {
    }

    public CalendarEvent(Calendar date, String user, String description) {
        this(date, date, user, description, description, null, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description) {
        this(dateStart, dateEnd, user, description, description, null, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description, String title) {
        this(dateStart, dateEnd, user, description, title, null, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description, String title, List category) {
        this(dateStart, dateEnd, user, description, title, category, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description, String title, List category, String url) {
        this(dateStart, dateEnd, user, description, title, category, url, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description, String title, List category, String url, String location) {
        this.dateStart = dateStart;
        this.dateEnd = dateEnd;
        this.user = user;
        this.description = description;
        this.title = title;
        this.category = category;
        this.url = url;
        this.location = location;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List getCategory() {
        return category;
    }

    public void setCategory(List category) {
        this.category = category;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
