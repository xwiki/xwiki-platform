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
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Plugin for manipulating dates from velocity scripts inside xwiki documents. It is based on the <a
 * href="http://joda-time.sourceforge.net/">JodaTime framework</a>, a quality replacement for the Java date and time
 * classes.
 * 
 * @see JodaTimePluginApi
 */
public class JodaTimePlugin extends XWikiDefaultPlugin
{
    
    /**
     * @param name the plugin name, usually ignored, since plugins have a fixed name
     * @param className the name of this class, ignored
     * @param context the current request context
     */
    public JodaTimePlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        init(context);
    }

    @Override
    public String getName()
    {
        return "jodatime";
    }

    @Override
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new JodaTimePluginApi((JodaTimePlugin) plugin, context);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
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
    public DateTime getDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour,
        int secondOfMinute, int millisOfSecond)
    {
        return new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond);
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
    public MutableDateTime getMutableDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay,
        int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        return new MutableDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
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
    public DateTimeFormatter getDateTimeFormatterForPattern(String pattern, XWikiContext context)
    {
        return DateTimeFormat.forPattern(pattern).withLocale((Locale) context.get("locale"));
    }

    /**
     * @see org.joda.time.format.DateTimeFormat#forStyle(String)
     */
    public DateTimeFormatter getDateTimeFormatterForStyle(String style, XWikiContext context)
    {
        return DateTimeFormat.forStyle(style).withLocale((Locale) context.get("locale"));
    }

    /**
     * @see org.joda.time.DateTimeZone#getDefault()
     */
    public DateTimeZone getServerTimezone()
    {
        return DateTimeZone.getDefault();
    }

    /**
     * @see org.joda.time.DateTimeZone#UTC
     */
    public DateTimeZone getUTCTimezone()
    {
        return DateTimeZone.UTC;
    }

    /**
     * @see org.joda.time.DateTimeZone#forID(String)
     */
    public DateTimeZone getTimezone(String locationOrOffset)
    {
        return DateTimeZone.forID(locationOrOffset);
    }

    /**
     * @see org.joda.time.DateTimeZone#forOffsetHours(int)
     */
    public DateTimeZone getTimezone(int offsetHours)
    {
        return DateTimeZone.forOffsetHours(offsetHours);
    }

    /**
     * @see org.joda.time.DateTimeZone#forOffsetHoursMinutes(int, int)
     */
    public DateTimeZone getTimezone(int offsetHours, int offsetMinutes)
    {
        return DateTimeZone.forOffsetHoursMinutes(offsetHours, offsetMinutes);
    }
}
