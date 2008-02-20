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

package com.xpn.xwiki.plugin.jodatime;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;

/**
 * Api for the JodaTimePlugin
 */
public class JodaTimePluginApi extends Api
{
    /**
     * the plugin instance
     */
    private JodaTimePlugin plugin;

    public JodaTimePluginApi(JodaTimePlugin plugin, XWikiContext context)
    {
        super(context);
        setPlugin(plugin);
    }

    /**
     * @see #plugin
     */
    public JodaTimePlugin getPlugin()
    {
        if (hasProgrammingRights()) {
            return plugin;
        }
        return null;
    }

    /**
     * @see #plugin
     */
    public void setPlugin(JodaTimePlugin plugin)
    {
        this.plugin = plugin;
    }

    /**
     * @see org.joda.time.DateTime#DateTime()
     */
    public DateTime getDateTime()
    {
        return new DateTime();
    }

    /**
     * @see org.joda.time.DateTime#DateTime(int, int, int, int, int, int, int)
     */
    public DateTime getDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay,
        int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        return new DateTime(year,
            monthOfYear,
            dayOfMonth,
            hourOfDay,
            minuteOfHour,
            secondOfMinute,
            millisOfSecond);
    }

    /**
     * @see org.joda.time.DateTime#DateTime(long)
     */
    public DateTime getDateTime(long instant)
    {
        return new DateTime(instant);
    }

    /**
     * @see org.joda.time.MutableDateTime#MutableDateTime()
     */
    public MutableDateTime getMutableDateTime()
    {
        return new MutableDateTime();
    }

    /**
     * @see org.joda.time.MutableDateTime#MutableDateTime(int, int, int, int, int, int, int)
     */
    public MutableDateTime getMutableDateTime(int year, int monthOfYear, int dayOfMonth,
        int hourOfDay, int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        return new MutableDateTime(year,
            monthOfYear,
            dayOfMonth,
            hourOfDay,
            minuteOfHour,
            secondOfMinute,
            millisOfSecond);
    }

    /**
     * @see org.joda.time.MutableDateTime#MutableDateTime(long)
     */
    public MutableDateTime getMutableDateTime(long instant)
    {
        return new MutableDateTime(instant);
    }

    /**
     * @see org.joda.time.format.DateTimeFormat#forPattern(String)
     */
    public DateTimeFormatter getDateTimeFormatterForPattern(String pattern)
    {
        return DateTimeFormat.forPattern(pattern).withLocale(
            (Locale) getXWikiContext().get("locale"));
    }

    /**
     * @see org.joda.time.format.DateTimeFormat#forStyle(String)
     */
    public DateTimeFormatter getDateTimeFormatterForStyle(String style)
    {
        return DateTimeFormat.forStyle(style)
            .withLocale((Locale) getXWikiContext().get("locale"));
    }
}
