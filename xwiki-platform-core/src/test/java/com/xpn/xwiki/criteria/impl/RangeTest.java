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

import java.util.List;
import java.util.Arrays;

/**
 * Unit tests for {@link com.xpn.xwiki.criteria.impl.Range}.
 *
 * @version $Id$
 */
public class RangeTest extends TestCase
{
    /**
     * Test for {@link com.xpn.xwiki.criteria.impl.Range#Range(int, int)}
     */
    public void testConstructor()
    {
        doConstructorTest(0, 0);
        doConstructorTest(0, 5);
        doConstructorTest(0, -7);
        doConstructorTest(-10, 0);
        doConstructorTest(-10, 4);
        doConstructorTest(-10, -1);
    }

    private void doConstructorTest(int start, int size)
    {
        Range i = new Range(start, size);
        assertEquals(i.getStart(), start);
        assertEquals(i.getSize(), size);
        assertEquals(i.getAbsoluteStart(), Math.abs(start));
        assertEquals(i.getAbsoluteSize(), Math.abs(size));
    }

    public static final List<String> zeroToHeight =
        Arrays.asList(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"});

    public void testSubListWithStartZeroAndSizeZero()
    {
        Range range = new Range(0, 0);
        assertEquals(range.subList(zeroToHeight), zeroToHeight);
    }

    public void testSubListWithStartZeroAndSizeOneThousand()
    {
        Range range = new Range(0, 1000);
        assertEquals(range.subList(zeroToHeight), zeroToHeight);
    }

    public void testSubListWithHeadIntervalOne()
    {
        Range range = RangeFactory.createHeadRange(1);
        List<String> zero = Arrays.asList(new String[]{"0"});
        assertEquals(range.subList(zeroToHeight), zero);
    }

    public void testSubListWithTailIntervalOne()
    {
        Range range = RangeFactory.createTailRange(1);
        List<String> height = Arrays.asList(new String[]{"8"});
        assertEquals(range.subList(zeroToHeight), height);
    }

    public void testSubListWithStartZeroAndPositiveSize()
    {
        Range range = new Range(0, 4);
        List<String> zeroToThree = Arrays.asList(new String[]{"0", "1", "2", "3"});
        assertEquals(range.subList(zeroToHeight), zeroToThree);
    }

    public void testSubListWithStartZeroAndNegativeSize()
    {
        Range range = new Range(0, -4);
        List<String> fiveToHeight = Arrays.asList(new String[]{"5", "6", "7", "8"});
        assertEquals(range.subList(zeroToHeight), fiveToHeight);
    }

    public void testSubListWithStartMinusTwoAndSizeFour()
    {
        Range range = new Range(-2, 4);
        List<String> sevenToHeight = Arrays.asList(new String[]{"7", "8"});
        assertEquals(range.subList(zeroToHeight), sevenToHeight);
    }

    public void testSubListWithStartMinusTwoAndSizeMinusFour()
    {
        Range range = new Range(-2, -4);
        List<String> threeToSix = Arrays.asList(new String[]{"3", "4", "5", "6"});
        assertEquals(range.subList(zeroToHeight), threeToSix);
    }

    public void testSubListWithStartTwoAndSizeMinusFour()
    {
        Range range = new Range(2, -4);
        List<String> zeroToOne = Arrays.asList(new String[]{"0", "1"});
        assertEquals(range.subList(zeroToHeight), zeroToOne);
    }
}
