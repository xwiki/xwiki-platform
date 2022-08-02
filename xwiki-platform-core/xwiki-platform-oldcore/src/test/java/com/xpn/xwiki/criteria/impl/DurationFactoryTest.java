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
package com.xpn.xwiki.criteria.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@link DurationFactory} class.
 */
class DurationFactoryTest
{
    /**
     * Test for {@link DurationFactory#DAY}
     */
    @Test
    void day()
    {
        doDurationTest(DurationFactory.DAY, 0, 0, 0, 1);
    }

    /**
     * Test for {@link DurationFactory#WEEK}
     */
    @Test
    void week()
    {
        doDurationTest(DurationFactory.WEEK, 0, 0, 1, 0);
    }

    /**
     * Test for {@link DurationFactory#MONTH}
     */
    @Test
    void month()
    {
        doDurationTest(DurationFactory.MONTH, 0, 1, 0, 0);
    }

    /**
     * Test for {@link DurationFactory#YEAR}
     */
    @Test
    void year()
    {
        doDurationTest(DurationFactory.YEAR, 1, 0, 0, 0);
    }

    /**
     * Test for {@link DurationFactory#createDuration(int, int, int, int)}
     */
    @Test
    void createDuration()
    {
        doCreateDurationTest(0, 0, 0, 0);
        doCreateDurationTest(1, 2, 5, 4);
        doCreateDurationTest(-1, -2, -3, -14);
        doCreateDurationTest(0, -2, 3, 0);
    }

    private void doCreateDurationTest(int years, int months, int weeks, int days)
    {
        doDurationTest(DurationFactory.createDuration(years, months, weeks, days), years, months,
            weeks, days);
    }

    /**
     * Test for {@link DurationFactory#createDays(int)}
     */
    @Test
    void createDays()
    {
        doCreateDaysTest(0);
        doCreateDaysTest(-3);
        doCreateDaysTest(15);
    }

    private void doCreateDaysTest(int days)
    {
        doDurationTest(DurationFactory.createDays(days), 0, 0, 0, days);
    }

    /**
     * Test for {@link DurationFactory#createWeeks(int)}
     */
    @Test
    void createWeeks()
    {
        doCreateWeeksTest(0);
        doCreateWeeksTest(-1);
        doCreateWeeksTest(5);
    }

    private void doCreateWeeksTest(int weeks)
    {
        doDurationTest(DurationFactory.createWeeks(weeks), 0, 0, weeks, 0);
    }

    /**
     * Test for {@link DurationFactory#createMonths(int)}
     */
    @Test
    void createMonths()
    {
        doCreateMonthsTest(0);
        doCreateMonthsTest(-13);
        doCreateMonthsTest(1);
    }

    private void doCreateMonthsTest(int months)
    {
        doDurationTest(DurationFactory.createMonths(months), 0, months, 0, 0);
    }

    /**
     * Test for {@link DurationFactory#createYears(int)}
     */
    @Test
    void createYears()
    {
        doCreateYearsTest(0);
        doCreateYearsTest(-2);
        doCreateYearsTest(103);
    }

    private void doCreateYearsTest(int years)
    {
        doDurationTest(DurationFactory.createYears(years), years, 0, 0, 0);
    }

    private void doDurationTest(Duration d, int years, int months, int weeks, int days)
    {
        assertEquals(d.getDays(), days);
        assertEquals(d.getWeeks(), weeks);
        assertEquals(d.getMonths(), months);
        assertEquals(d.getYears(), years);
    }
}
