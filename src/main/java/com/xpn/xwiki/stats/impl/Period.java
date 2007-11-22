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

package com.xpn.xwiki.stats.impl;

import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Immutable period for retrieving statistics. A period of time is uniquely identified by its
 * bounds, and not by its span.
 */
public class Period
{
    private static final DateTimeFormatter DAY_PERIOD_FORMATTER =
        DateTimeFormat.forPattern("yyyyMMdd");

    private static final DateTimeFormatter MONTH_PERIOD_FORMATTER =
        DateTimeFormat.forPattern("yyyyMM");

    private Interval interval;

    public Period(long start, long end)
    {
        interval = new Interval(start, end);
    }

    public long getStart()
    {
        return interval.getStartMillis();
    }

    public long getEnd()
    {
        return interval.getEndMillis();
    }

    public int getStartCode()
    {
        return Integer.parseInt(getFormatter().print(interval.getStart()));
    }

    public int getEndCode()
    {
        return Integer.parseInt(getFormatter().print(interval.getEnd()));
    }

    private DateTimeFormatter getFormatter()
    {
        if (interval.toPeriod().getMonths() >= 1) {
            return MONTH_PERIOD_FORMATTER;
        }
        return DAY_PERIOD_FORMATTER;
    }
}
