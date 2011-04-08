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

/**
 * Unit tests for the {@link com.xpn.xwiki.criteria.impl.Duration} class.
 */
public class DurationTest extends TestCase
{
    /**
     * Test for {@link com.xpn.xwiki.criteria.impl.Duration#Duration(int, int, int, int)}
     */
    public void testConstructor()
    {
        doConstructorTest(0, 0, 0, 0);
        doConstructorTest(1, 2, 3, 4);
        doConstructorTest(-1, -2, -3, -4);
        doConstructorTest(0, -2, 3, 0);
    }

    private void doConstructorTest(int years, int months, int weeks, int days)
    {
        Duration d = new Duration(years, months, weeks, days);
        assertEquals(years, d.getYears());
        assertEquals(months, d.getMonths());
        assertEquals(weeks, d.getWeeks());
        assertEquals(days, d.getDays());
    }
}
