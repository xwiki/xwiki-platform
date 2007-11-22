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

/**
 * Immutable duration for retrieving statistics. A duration of time is uniquely identified by its
 * span. It does not depend on the bounds used for its creation.
 */
public class Duration
{
    private org.joda.time.Period span;

    public Duration(int years, int months, int weeks, int days)
    {
        span = new org.joda.time.Period(years, months, weeks, days, 0, 0, 0, 0);
    }

    public int getYears()
    {
        return span.getYears();
    }

    public int getMonths()
    {
        return span.getMonths();
    }

    public int getWeeks()
    {
        return span.getWeeks();
    }

    public int getDays()
    {
        return span.getDays();
    }
}
