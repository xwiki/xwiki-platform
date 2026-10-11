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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormatter;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * Api for the JodaTimePlugin.
 *
 * @version $Id$
 */
public class JodaTimePluginApi extends PluginApi<JodaTimePlugin>
{
    /**
     * @param plugin the wrapped plugin
     * @param context the current request context
     */
    public JodaTimePluginApi(JodaTimePlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * @return the current date and time
     * @see org.joda.time.DateTime#DateTime()
     * @see JodaTimePlugin#getDateTime()
     */
    public DateTime getDateTime()
    {
        return getProtectedPlugin().getDateTime();
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
     * @see JodaTimePlugin#getDateTime(int, int, int, int, int, int, int)
     */
    public DateTime getDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay, int minuteOfHour,
        int secondOfMinute, int millisOfSecond)
    {
        return getProtectedPlugin().getDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour, secondOfMinute,
            millisOfSecond);
    }

    /**
     * @param instant the number of milliseconds since 1970-01-01T00:00:00Z
     * @return the date and time corresponding to the passed instant
     * @see org.joda.time.DateTime#DateTime(long)
     * @see JodaTimePlugin#getDateTime(long)
     */
    public DateTime getDateTime(long instant)
    {
        return getProtectedPlugin().getDateTime(instant);
    }

    /**
     * @return the current date and time as a mutable instance
     * @see org.joda.time.MutableDateTime#MutableDateTime()
     * @see JodaTimePlugin#getMutableDateTime()
     */
    public MutableDateTime getMutableDateTime()
    {
        return getProtectedPlugin().getMutableDateTime();
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
     * @see JodaTimePlugin#getMutableDateTime(int, int, int, int, int, int, int)
     */
    public MutableDateTime getMutableDateTime(int year, int monthOfYear, int dayOfMonth, int hourOfDay,
        int minuteOfHour, int secondOfMinute, int millisOfSecond)
    {
        return getProtectedPlugin().getMutableDateTime(year, monthOfYear, dayOfMonth, hourOfDay, minuteOfHour,
            secondOfMinute, millisOfSecond);
    }

    /**
     * @param instant the number of milliseconds since 1970-01-01T00:00:00Z
     * @return the mutable date and time corresponding to the passed instant
     * @see org.joda.time.MutableDateTime#MutableDateTime(long)
     * @see JodaTimePlugin#getMutableDateTime(long)
     */
    public MutableDateTime getMutableDateTime(long instant)
    {
        return getProtectedPlugin().getMutableDateTime(instant);
    }

    /**
     * @param pattern the pattern describing the date and time format
     * @return a formatter for the passed pattern
     * @see org.joda.time.format.DateTimeFormat#forPattern(String)
     * @see JodaTimePlugin#getDateTimeFormatterForPattern(String, XWikiContext)
     */
    public DateTimeFormatter getDateTimeFormatterForPattern(String pattern)
    {
        return getProtectedPlugin().getDateTimeFormatterForPattern(pattern, getXWikiContext());
    }

    /**
     * @param style the style describing the date and time format
     * @return a formatter for the passed style
     * @see org.joda.time.format.DateTimeFormat#forStyle(String)
     * @see JodaTimePlugin#getDateTimeFormatterForStyle(String, XWikiContext)
     */
    public DateTimeFormatter getDateTimeFormatterForStyle(String style)
    {
        return getProtectedPlugin().getDateTimeFormatterForStyle(style, getXWikiContext());
    }

    /**
     * @return the timezone of the server
     * @see org.joda.time.DateTimeZone#getDefault()
     * @see JodaTimePlugin#getServerTimezone()
     */
    public DateTimeZone getServerTimezone()
    {
        return getProtectedPlugin().getServerTimezone();
    }

    /**
     * @return the UTC timezone
     * @see org.joda.time.DateTimeZone#UTC
     * @see JodaTimePlugin#getUTCTimezone()
     */
    public DateTimeZone getUTCTimezone()
    {
        return getProtectedPlugin().getUTCTimezone();
    }

    /**
     * @param locationOrOffset the identifier of the location or offset of the timezone
     * @return the timezone corresponding to the passed identifier
     * @see org.joda.time.DateTimeZone#forID(String)
     * @see JodaTimePlugin#getTimezone(String)
     */
    public DateTimeZone getTimezone(String locationOrOffset)
    {
        return getProtectedPlugin().getTimezone(locationOrOffset);
    }

    /**
     * @param offsetHours the offset from UTC in hours
     * @return the timezone corresponding to the passed offset
     * @see org.joda.time.DateTimeZone#forOffsetHours(int)
     * @see JodaTimePlugin#getTimezone(int)
     */
    public DateTimeZone getTimezone(int offsetHours)
    {
        return getProtectedPlugin().getTimezone(offsetHours);
    }

    /**
     * @param offsetHours the hours part of the offset from UTC
     * @param offsetMinutes the minutes part of the offset from UTC
     * @return the timezone corresponding to the passed offset
     * @see org.joda.time.DateTimeZone#forOffsetHoursMinutes(int, int)
     * @see JodaTimePlugin#getTimezone(int, int)
     */
    public DateTimeZone getTimezone(int offsetHours, int offsetMinutes)
    {
        return getProtectedPlugin().getTimezone(offsetHours, offsetMinutes);
    }

    /**
     * @param millis the duration in milliseconds
     * @return the duration corresponding to the passed number of milliseconds
     * @see org.joda.time.Duration#Duration(long)
     * @see JodaTimePlugin#getDuration(long)
     */
    public Duration getDuration(long millis)
    {
        return getProtectedPlugin().getDuration(millis);
    }

    /**
     * @param from the start instant of the duration
     * @param to the end instant of the duration
     * @return the duration between the two passed instants
     * @see org.joda.time.Duration#Duration(ReadableInstant, ReadableInstant)
     * @see JodaTimePlugin#getDuration(ReadableInstant, ReadableInstant)
     */
    public Duration getDuration(ReadableInstant from, ReadableInstant to)
    {
        return getProtectedPlugin().getDuration(from, to);
    }

    /**
     * @return an ISO8601 date time formatter
     * @since 5.2RC1
     */
    public DateTimeFormatter getISODateTimeFormatter()
    {
        return getProtectedPlugin().getISODateTimeFormatter();
    }
}
