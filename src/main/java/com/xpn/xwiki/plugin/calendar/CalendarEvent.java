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

import org.apache.commons.lang.StringUtils;

public class CalendarEvent
{
    private Calendar dateStart;

    private Calendar dateEnd;

    private String title;

    private String description;

    private String user;

    private List category;

    private String url;

    private String location;

    public CalendarEvent()
    {
    }

    public CalendarEvent(Calendar date, String user, String description)
    {
        this(date, date, user, description, description, null, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description)
    {
        this(dateStart, dateEnd, user, description, description, null, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description, String title)
    {
        this(dateStart, dateEnd, user, description, title, null, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description, String title,
        List category)
    {
        this(dateStart, dateEnd, user, description, title, category, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description, String title,
        List category, String url)
    {
        this(dateStart, dateEnd, user, description, title, category, url, "");
    }

    public CalendarEvent(Calendar dateStart, Calendar dateEnd, String user, String description, String title,
        List category, String url, String location)
    {
        setDateStart(dateStart);
        setDateEnd(dateEnd);
        setUser(user);
        setDescription(description);
        setTitle(title);
        setCategory(category);
        setUrl(url);
        setLocation(location);
    }

    public Calendar getDateStart()
    {
        return this.dateStart;
    }

    public void setDateStart(Calendar dateStart)
    {
        this.dateStart = dateStart;
    }

    public Calendar getDateEnd()
    {
        return this.dateEnd;
    }

    public void setDateEnd(Calendar dateEnd)
    {
        this.dateEnd = dateEnd;
    }

    public String getUser()
    {
        return this.user;
    }

    public void setUser(String user)
    {
        this.user = StringUtils.trimToEmpty(user);
    }

    public String getDescription()
    {
        return this.description;
    }

    public void setDescription(String description)
    {
        this.description = StringUtils.trimToEmpty(description);
    }

    public String getTitle()
    {
        return this.title;
    }

    public void setTitle(String title)
    {
        this.title = StringUtils.trimToEmpty(title);
    }

    public List getCategory()
    {
        return this.category;
    }

    public void setCategory(List category)
    {
        this.category = category;
    }

    public String getUrl()
    {
        return this.url;
    }

    public void setUrl(String url)
    {
        this.url = StringUtils.trimToEmpty(url);
    }

    public String getLocation()
    {
        return this.location;
    }

    public void setLocation(String location)
    {
        this.location = StringUtils.trimToEmpty(location);
    }
}
