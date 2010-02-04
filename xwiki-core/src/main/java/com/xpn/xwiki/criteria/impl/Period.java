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

package com.xpn.xwiki.criteria.impl;

import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Immutable period for retrieving statistics. A period of time is uniquely identified by its bounds, and not by its
 * span. Two periods of time with the same span are different if they don't start at the same time.
 */
public class Period
{
    /**
     * Formatter associated with periods that have less than a month
     */
    private static final DateTimeFormatter DAY_PERIOD_FORMATTER =
        DateTimeFormat.forPattern("yyyyMMdd");

    /**
     * Formatter associated with periods that have at least one month
     */
    private static final DateTimeFormatter MONTH_PERIOD_FORMATTER =
        DateTimeFormat.forPattern("yyyyMM");

    private Interval interval;

    /**
     * Creates a new time Period from the specified start time to the specified end time. Both ends of the period are
     * given as time stamps (the number of milliseconds from 1970-01-01T00:00:00Z)
     * 
     * @param start The period start as the number of milliseconds from 1970-01-01T00:00:00Z
     * @param end The period end as the number of milliseconds from 1970-01-01T00:00:00Z
     */
    public Period(long start, long end)
    {
        this.interval = new Interval(start, end);
    }

    /**
     * @return The period start as the number of milliseconds from 1970-01-01T00:00:00Z
     */
    public long getStart()
    {
        return this.interval.getStartMillis();
    }

    /**
     * @return The period end as the number of milliseconds from 1970-01-01T00:00:00Z
     */
    public long getEnd()
    {
        return this.interval.getEndMillis();
    }

    /**
     * @return The start of the period formatted with the associated formatter. Usually this is how the start of the
     *         period is stored in the database.
     * @see #getFormatter()
     */
    public int getStartCode()
    {
        return Integer.parseInt(getFormatter().print(this.interval.getStart()));
    }

    /**
     * @return The end of the period formatted with the associated formatter. Usually this is how the end of the period
     *         is stored in the database.
     * @see #getFormatter()
     */
    public int getEndCode()
    {
        return Integer.parseInt(getFormatter().print(this.interval.getEnd()));
    }

    /**
     * @return The formatter associated with this Period instance
     * @see #DAY_PERIOD_FORMATTER
     * @see #MONTH_PERIOD_FORMATTER
     */
    private DateTimeFormatter getFormatter()
    {
        if (this.interval.toPeriod().getMonths() >= 1) {
            return MONTH_PERIOD_FORMATTER;
        }
        return DAY_PERIOD_FORMATTER;
    }
}
