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

import junit.framework.TestCase;
import com.xpn.xwiki.criteria.impl.Duration;
import com.xpn.xwiki.criteria.impl.DurationFactory;

/**
 * Unit tests for the {@link DurationFactory} class.
 */
public class DurationFactoryTest extends TestCase
{
    /**
     * Test for {@link DurationFactory#DAY}
     */
    public void testDay()
    {
        doDurationTest(DurationFactory.DAY, 0, 0, 0, 1);
    }

    /**
     * Test for {@link DurationFactory#WEEK}
     */
    public void testWeek()
    {
        doDurationTest(DurationFactory.WEEK, 0, 0, 1, 0);
    }

    /**
     * Test for {@link DurationFactory#MONTH}
     */
    public void testMonth()
    {
        doDurationTest(DurationFactory.MONTH, 0, 1, 0, 0);
    }

    /**
     * Test for {@link DurationFactory#YEAR}
     */
    public void testYear()
    {
        doDurationTest(DurationFactory.YEAR, 1, 0, 0, 0);
    }

    /**
     * Test for {@link DurationFactory#createDuration(int, int, int, int)}
     */
    public void testCreateDuration()
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
    public void testCreateDays()
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
    public void testCreateWeeks()
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
    public void testCreateMonths()
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
    public void testCreateYears()
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
