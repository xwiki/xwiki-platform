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
 * Unit tests for the {@link com.xpn.xwiki.criteria.impl.RangeFactory} class.
 */
class RangeFactoryTest
{
    /**
     * Test for {@link RangeFactory#ALL}
     */
    @Test
    void all()
    {
        doRangeTest(RangeFactory.ALL, 0, 0);
    }

    /**
     * Test for {@link RangeFactory#FIRST}
     */
    @Test
    void first()
    {
        doRangeTest(RangeFactory.FIRST, 0, 1);
    }

    /**
     * Test for {@link RangeFactory#LAST}
     */
    @Test
    void last()
    {
        doRangeTest(RangeFactory.LAST, 0, -1);
    }

    /**
     * Test for {@link RangeFactory#createRange(int, int)}
     */
    @Test
    void createRange()
    {
        doCreateRangeTest(0, 0);
        doCreateRangeTest(0, 3);
        doCreateRangeTest(0, -4);
        doCreateRangeTest(-10, 0);
        doCreateRangeTest(-10, 5);
        doCreateRangeTest(-10, -9);
    }

    private void doCreateRangeTest(int start, int size)
    {
        doRangeTest(RangeFactory.createRange(start, size), start, size);
    }

    /**
     * Test for {@link RangeFactory#createHeadRange(int)}
     */
    @Test
    void createHeadRange()
    {
        doCreateHeadRangeTest(0);
        doCreateHeadRangeTest(3);
        doCreateHeadRangeTest(-7);
    }

    private void doCreateHeadRangeTest(int size)
    {
        doRangeTest(RangeFactory.createHeadRange(size), 0, Math.abs(size));
    }

    /**
     * Test for {@link RangeFactory#createTailRange(int)}
     */
    @Test
    void createTailRange()
    {
        doCreateTailRangeTest(0);
        doCreateTailRangeTest(3);
        doCreateTailRangeTest(-7);
    }

    private void doCreateTailRangeTest(int size)
    {
        doRangeTest(RangeFactory.createTailRange(size), 0, -Math.abs(size));
    }

    private void doRangeTest(Range i, int start, int size)
    {
        assertEquals(i.getStart(), start);
        assertEquals(i.getSize(), size);
        assertEquals(i.getAbsoluteStart(), Math.abs(start));
        assertEquals(i.getAbsoluteSize(), Math.abs(size));
    }
}
