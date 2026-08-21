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

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link com.xpn.xwiki.criteria.impl.Range}.
 *
 * @version $Id$
 */
class RangeTest
{
    /**
     * Test for {@link com.xpn.xwiki.criteria.impl.Range#Range(int, int)}
     */
    @Test
    void constructor()
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
        assertEquals(start, i.getStart());
        assertEquals(size, i.getSize());
        assertEquals(Math.abs(start), i.getAbsoluteStart());
        assertEquals(Math.abs(size), i.getAbsoluteSize());
    }

    public static final List<String> zeroToHeight =
        Arrays.asList(new String[]{"0", "1", "2", "3", "4", "5", "6", "7", "8"});

    @Test
    void subListWithStartZeroAndSizeZero()
    {
        Range range = new Range(0, 0);
        assertEquals(zeroToHeight, range.subList(zeroToHeight));
    }

    @Test
    void subListWithStartZeroAndSizeOneThousand()
    {
        Range range = new Range(0, 1000);
        assertEquals(zeroToHeight, range.subList(zeroToHeight));
    }

    @Test
    void subListWithHeadIntervalOne()
    {
        Range range = RangeFactory.createHeadRange(1);
        List<String> zero = Arrays.asList(new String[]{"0"});
        assertEquals(zero, range.subList(zeroToHeight));
    }

    @Test
    void subListWithTailIntervalOne()
    {
        Range range = RangeFactory.createTailRange(1);
        List<String> height = Arrays.asList(new String[]{"8"});
        assertEquals(height, range.subList(zeroToHeight));
    }

    @Test
    void subListWithStartZeroAndPositiveSize()
    {
        Range range = new Range(0, 4);
        List<String> zeroToThree = Arrays.asList(new String[]{"0", "1", "2", "3"});
        assertEquals(zeroToThree, range.subList(zeroToHeight));
    }

    @Test
    void subListWithStartZeroAndNegativeSize()
    {
        Range range = new Range(0, -4);
        List<String> fiveToHeight = Arrays.asList(new String[]{"5", "6", "7", "8"});
        assertEquals(fiveToHeight, range.subList(zeroToHeight));
    }

    @Test
    void subListWithStartMinusTwoAndSizeFour()
    {
        Range range = new Range(-2, 4);
        List<String> sevenToHeight = Arrays.asList(new String[]{"7", "8"});
        assertEquals(sevenToHeight, range.subList(zeroToHeight));
    }

    @Test
    void subListWithStartMinusTwoAndSizeMinusFour()
    {
        Range range = new Range(-2, -4);
        List<String> threeToSix = Arrays.asList(new String[]{"3", "4", "5", "6"});
        assertEquals(threeToSix, range.subList(zeroToHeight));
    }

    @Test
    void subListWithStartTwoAndSizeMinusFour()
    {
        Range range = new Range(2, -4);
        List<String> zeroToOne = Arrays.asList(new String[]{"0", "1"});
        assertEquals(zeroToOne, range.subList(zeroToHeight));
    }
}
