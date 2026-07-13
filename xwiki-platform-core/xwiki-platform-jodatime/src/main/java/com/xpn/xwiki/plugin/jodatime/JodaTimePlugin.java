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
import org.joda.time.Duration;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Plugin for manipulating dates from velocity scripts inside xwiki documents. It is based on the <a
 * href="http://joda-time.sourceforge.net/">JodaTime framework</a>, a quality replacement for the Java date and time
 * classes.
 *
 * @version $Id$
 * @see JodaTimePluginApi
 */
public class JodaTimePlugin extends XWikiDefaultPlugin
{
    /**
     * ISO8601 date time formatter.
     */
    private static final DateTimeFormatter ISO_DATE_FORMATTER = ISODateTimeFormat.dateTime().withZone(DateTimeZone.UTC);

    /**
     * The name of the context key holding the current locale.
     */
    private static final String LOCALE = "locale";

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

    @Override
    public void init(XWikiContext context)
    {
        super.init(context);
    }

    /**
     * @return the current date and time
     * @see org.joda.time.DateTime#DateTime()
     */
    public DateTime getDateTime()
    {
        return new DateTime();
    }

    /**
     * @param year the year
     * @param monthOfYear the month of the year
     * @param dayOfMonth the day of the month
     * @param hourOfDay the hour of the day
     * @param minuteOfHour the minute of the hour
     * @param secondOfMinute the second of the minute
     * @param millisOfSecond the millisecond of the second
     * @return the date and time corresponding to the passed values
     * @see org.joda.time.DateTime#DateTime(int, int, int, int, int, int, int)
     */
    public DateTime getDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour,
        int secondOfMinute, int millisOfSecond)
    {
        return new DateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute, millisOfSecond);
    }

    /**
     * @param instant the number of milliseconds since 1970-01-01T00:00:00Z
     * @return the date and time corresponding to the passed instant
     * @see org.joda.time.DateTime#DateTime(long)
     */
    public DateTime getDateTime(long instant)
    {
        return new DateTime(instant);
    }

    /**
     * @return the current date and time as a mutable instance
     * @see org.joda.time.MutableDateTime#MutableDateTime()
     */
    public MutableDateTime getMutableDateTime()
    {
        return new MutableDateTime();
    }

    /**
     * @param year the year
     * @param monthOfYear the month of the year
     * @param dayOfMonth the day of the month
     * @param hourOfDay the hour of the day
     * @param minuteOfHour the minute of the hour
     * @param secondOfMinute the second of the minute
     * @param millisOfSecond the millisecond of the second
     * @return the mutable date and time corresponding to the passed values
     * @see org.joda.time.MutableDateTime#MutableDateTime(int, int, int, int, int, int, int)
     */
    public MutableDateTime getMutableDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay,
        int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        return new MutableDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
            millisOfSecond);
    }

    /**
     * @param instant the number of milliseconds since 1970-01-01T00:00:00Z
     * @return the mutable date and time corresponding to the passed instant
     * @see org.joda.time.MutableDateTime#MutableDateTime(long)
     */
    public MutableDateTime getMutableDateTime(long instant)
    {
        return new MutableDateTime(instant);
    }

    /**
     * @param pattern the pattern describing the date and time format
     * @param context the current request context, used to retrieve the locale
     * @return a formatter for the passed pattern
     * @see org.joda.time.format.DateTimeFormat#forPattern(String)
     */
    public DateTimeFormatter getDateTimeFormatterForPattern(String pattern, XWikiContext context)
    {
        return DateTimeFormat.forPattern(pattern).withLocale((Locale) context.get(LOCALE));
    }

    /**
     * @param style the style describing the date and time format
     * @param context the current request context, used to retrieve the locale
     * @return a formatter for the passed style
     * @see org.joda.time.format.DateTimeFormat#forStyle(String)
     */
    public DateTimeFormatter getDateTimeFormatterForStyle(String style, XWikiContext context)
    {
        return DateTimeFormat.forStyle(style).withLocale((Locale) context.get(LOCALE));
    }

    /**
     * @return the timezone of the server
     * @see org.joda.time.DateTimeZone#getDefault()
     */
    public DateTimeZone getServerTimezone()
    {
        return DateTimeZone.getDefault();
    }

    /**
     * @return the UTC timezone
     * @see org.joda.time.DateTimeZone#UTC
     */
    public DateTimeZone getUTCTimezone()
    {
        return DateTimeZone.UTC;
    }

    /**
     * @param locationOrOffset the identifier of the location or offset of the timezone
     * @return the timezone corresponding to the passed identifier
     * @see org.joda.time.DateTimeZone#forID(String)
     */
    public DateTimeZone getTimezone(String locationOrOffset)
    {
        return DateTimeZone.forID(locationOrOffset);
    }

    /**
     * @param offsetHours the offset from UTC in hours
     * @return the timezone corresponding to the passed offset
     * @see org.joda.time.DateTimeZone#forOffsetHours(int)
     */
    public DateTimeZone getTimezone(int offsetHours)
    {
        return DateTimeZone.forOffsetHours(offsetHours);
    }

    /**
     * @param offsetHours the hours part of the offset from UTC
     * @param offsetMinutes the minutes part of the offset from UTC
     * @return the timezone corresponding to the passed offset
     * @see org.joda.time.DateTimeZone#forOffsetHoursMinutes(int, int)
     */
    public DateTimeZone getTimezone(int offsetHours, int offsetMinutes)
    {
        return DateTimeZone.forOffsetHoursMinutes(offsetHours, offsetMinutes);
    }

    /**
     * @param millis the duration in milliseconds
     * @return the duration corresponding to the passed number of milliseconds
     * @see org.joda.time.Duration#Duration(long)
     */
    public Duration getDuration(long millis)
    {
        return new Duration(millis);
    }

    /**
     * @param from the start instant of the duration
     * @param to the end instant of the duration
     * @return the duration between the two passed instants
     * @see org.joda.time.Duration#Duration(ReadableInstant, ReadableInstant)
     */
    public Duration getDuration(ReadableInstant from, ReadableInstant to)
    {
        return new Duration(from, to);
    }

    /**
     * @return an ISO8601 date time formatter
     * @since 5.2RC1
     */
    public DateTimeFormatter getISODateTimeFormatter()
    {
        return ISO_DATE_FORMATTER;
    }
}
