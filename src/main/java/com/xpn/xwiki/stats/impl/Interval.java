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
 *
 */

package com.xpn.xwiki.stats.impl;

/**
 * Immutable continuous integer interval. It can be used for pagination
 */
public class Interval
{
    /**
     * The start of the interval. It can be both positive and negative. A negative start is usually
     * associated with an interval which is relative to the end of the list it is applied to.
     */
    private int start;

    /**
     * The size of the interval. It can be both positive and negative. A negative size is usually
     * associated with an interval which is relative to the end of the list it is applied to.
     */
    private int size;

    /**
     * Creates a new interval having the specified start and size.
     * 
     * @param start The start of the interval
     * @param size The size of the interval
     */
    public Interval(int start, int size)
    {
        this.start = start;
        this.size = size;
    }

    /**
     * @see #start
     */
    public int getStart()
    {
        return start;
    }

    /**
     * @return The absolute value (nonnegative) of this interval's start
     */
    public int getAbsoluteStart()
    {
        return Math.abs(start);
    }

    /**
     * @see #size
     */
    public int getSize()
    {
        return size;
    }

    /**
     * @return The absolute value (nonnegative) of this interval's size
     */
    public int getAbsoluteSize()
    {
        return Math.abs(size);
    }
}
