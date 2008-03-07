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

/**
 * Immutable duration for retrieving statistics. A duration of time is uniquely identified by its
 * span. For instance, a duration of 3 minutes doesn't imply a specific start time. It can start at
 * any time, but it takes just 3 minutes. A Duration can be used to sample a Period of time. For
 * instance the period between November 1th 2007 and December 1th 2007 can be divided in samples
 * each having a duration of 3 days.
 */
public class Duration
{
    private org.joda.time.Period span;

    /**
     * Creates a duration by specifying a value for each of its fields.
     *
     * @param years The number of years
     * @param months The number of months
     * @param weeks The number of weeks
     * @param days The number of days
     */
    public Duration(int years, int months, int weeks, int days)
    {
        span = new org.joda.time.Period(years, months, weeks, days, 0, 0, 0, 0);
    }

    /**
     * Creates a duration by specifying a value for each of its fields.
     *
     * @param years The number of years
     * @param months The number of months
     * @param weeks The number of weeks
     * @param days The number of days
     */
    public Duration(int years, int months, int weeks, int days, int hours)
    {
        span = new org.joda.time.Period(years, months, weeks, days, hours, 0, 0, 0);
    }

    /**
     * @return The number of years this duration has
     */
    public int getYears()
    {
        return span.getYears();
    }

    /**
     * @return The number of months this duration has
     */
    public int getMonths()
    {
        return span.getMonths();
    }

    /**
     * @return The number of weeks this duration has
     */
    public int getWeeks()
    {
        return span.getWeeks();
    }

    /**
     * @return The number of days this duration has
     */
    public int getDays()
    {
        return span.getDays();
    }

    /**
     * @return The number of hours this duration has
     */
    public int getHours()
    {
        return span.getHours();
    }
}
