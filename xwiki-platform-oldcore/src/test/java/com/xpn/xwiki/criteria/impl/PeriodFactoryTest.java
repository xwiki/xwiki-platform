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

import java.text.ParseException;
import java.text.SimpleDateFormat;

import junit.framework.TestCase;

import com.xpn.xwiki.stats.impl.xwiki.XWikiStatsReader;

/**
 * Unit tests for {@link PeriodFactory}.
 */
public class PeriodFactoryTest extends TestCase
{
    /**
     * Tests if the start code of a day period is strictly less than its end code. This is needed because the
     * {@link XWikiStatsReader} retrieves the statistics that have been recorded between the start code, including it,
     * and the end code, excluding it.
     * 
     * @throws ParseException
     */
    public void testDayPeriodStartEndCode() throws ParseException
    {
        Period day = PeriodFactory.createDayPeriod(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        long start = sdf.parse(String.valueOf(day.getStartCode())).getTime();
        long end = sdf.parse(String.valueOf(day.getEndCode())).getTime();
        assertTrue("Start code must be less than end code", start < end);
    }

    /**
     * Tests if the start code of a month period is strictly less than its end code. This is needed because the
     * {@link XWikiStatsReader} retrieves the statistics that have been recorded between the start code, including it,
     * and the end code, excluding it.
     * 
     * @throws ParseException
     */
    public void testMonthPeriodStartEndCode() throws ParseException
    {
        Period month = PeriodFactory.createMonthPeriod(System.currentTimeMillis());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        long start = sdf.parse(String.valueOf(month.getStartCode())).getTime();
        long end = sdf.parse(String.valueOf(month.getEndCode())).getTime();
        assertTrue("Start code must be less than end code", start < end);
    }
}
