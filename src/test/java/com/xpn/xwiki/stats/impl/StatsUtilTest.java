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
package com.xpn.xwiki.stats.impl;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.xpn.xwiki.stats.impl.StatsUtil.PeriodType;

import junit.framework.TestCase;

/**
 * Unit tests for the {@link StatsUtil} class.
 */
public class StatsUtilTest extends TestCase
{
    /**
     * Test for the {@link StatsUtil#getPeriodAsInt(java.util.Date, PeriodType)}.
     */
    public void testGetPeriodAsInt()
    {
        Calendar cal = Calendar.getInstance();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMM");
        String a = sdf.format(cal.getTime());
        String b = StatsUtil.getPeriodAsInt(cal.getTime(), PeriodType.MONTH) + "";
        assertEquals("Wrong month period format", a, b);

        sdf = new SimpleDateFormat("yyyyMMdd");
        a = sdf.format(cal.getTime());
        b = StatsUtil.getPeriodAsInt(cal.getTime(), PeriodType.DAY) + "";
        assertEquals("Wrong day period format", a, b);
    }
}
