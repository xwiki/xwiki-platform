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

import junit.framework.TestCase;

/**
 * Unit tests for the {@link IntervalFactory} class.
 */
public class IntervalFactoryTest extends TestCase
{
    /**
     * Test for {@link IntervalFactory#ALL}
     */
    public void testAll()
    {
        doIntervalTest(IntervalFactory.ALL, 0, 0);
    }

    /**
     * Test for {@link IntervalFactory#FIRST}
     */
    public void testFirst()
    {
        doIntervalTest(IntervalFactory.FIRST, 0, 1);
    }

    /**
     * Test for {@link IntervalFactory#LAST}
     */
    public void testLast()
    {
        doIntervalTest(IntervalFactory.LAST, 0, -1);
    }

    /**
     * Test for {@link IntervalFactory#createInterval(int, int)}
     */
    public void testCreateInterval()
    {
        doCreateIntervalTest(0, 0);
        doCreateIntervalTest(0, 3);
        doCreateIntervalTest(0, -4);
        doCreateIntervalTest(-10, 0);
        doCreateIntervalTest(-10, 5);
        doCreateIntervalTest(-10, -9);
    }

    private void doCreateIntervalTest(int start, int size)
    {
        doIntervalTest(IntervalFactory.createInterval(start, size), start, size);
    }

    /**
     * Test for {@link IntervalFactory#createHeadInterval(int)}
     */
    public void testCreateHeadInterval()
    {
        doCreateHeadIntervalTest(0);
        doCreateHeadIntervalTest(3);
        doCreateHeadIntervalTest(-7);
    }

    private void doCreateHeadIntervalTest(int size)
    {
        doIntervalTest(IntervalFactory.createHeadInterval(size), 0, Math.abs(size));
    }

    /**
     * Test for {@link IntervalFactory#createTailInterval(int)}
     */
    public void testCreateTailInterval()
    {
        doCreateTailIntervalTest(0);
        doCreateTailIntervalTest(3);
        doCreateTailIntervalTest(-7);
    }

    private void doCreateTailIntervalTest(int size)
    {
        doIntervalTest(IntervalFactory.createTailInterval(size), 0, -Math.abs(size));
    }

    private void doIntervalTest(Interval i, int start, int size)
    {
        assertEquals(i.getStart(), start);
        assertEquals(i.getSize(), size);
        assertEquals(i.getAbsoluteStart(), Math.abs(start));
        assertEquals(i.getAbsoluteSize(), Math.abs(size));
    }
}
