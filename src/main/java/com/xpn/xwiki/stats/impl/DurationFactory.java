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
 * Helper factory class for creating Duration objects in velocity
 */
public class DurationFactory
{
    public static final Duration DAY = createDuration(0, 0, 0, 1);

    public static final Duration WEEK = createDuration(0, 0, 1, 0);

    public static final Duration MONTH = createDuration(0, 1, 0, 0);

    public static final Duration YEAR = createDuration(1, 0, 0, 0);

    private static final DurationFactory instance = new DurationFactory();

    private DurationFactory()
    {
    }

    public static DurationFactory getInstance()
    {
        return instance;
    }

    public static Duration createDuration(int years, int months, int weeks, int days)
    {
        return new Duration(years, months, weeks, days);
    }

    public static Duration createDays(int days)
    {
        return createDuration(0, 0, 0, days);
    }

    public static Duration createWeeks(int weeks)
    {
        return createDuration(0, 0, weeks, 0);
    }

    public static Duration createMonths(int months)
    {
        return createDuration(0, months, 0, 0);
    }

    public static Duration createYears(int years)
    {
        return createDuration(years, 0, 0, 0);
    }

    /**
     * Helper method for accessing {@link #DAY} static field in velocity
     */
    public static Duration getDAY()
    {
        return DAY;
    }

    /**
     * Helper method for accessing {@link #WEEK} static field in velocity
     */
    public static Duration getWEEK()
    {
        return WEEK;
    }

    /**
     * Helper method for accessing {@link #MONTH} static field in velocity
     */
    public static Duration getMONTH()
    {
        return MONTH;
    }

    /**
     * Helper method for accessing {@link #YEAR} static field in velocity
     */
    public static Duration getYEAR()
    {
        return YEAR;
    }
}
