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
 * Unit tests for the {@link Interval} class.
 */
public class IntervalTest extends TestCase
{
    /**
     * Test for {@link Interval#Interval(int, int)}
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
        Interval i = new Interval(start, size);
        assertEquals(i.getStart(), start);
        assertEquals(i.getSize(), size);
        assertEquals(i.getAbsoluteStart(), Math.abs(start));
        assertEquals(i.getAbsoluteSize(), Math.abs(size));
    }
}
