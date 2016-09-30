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

import java.util.ArrayList;
import java.util.List;

/**
 * Immutable continuous integer range. It can be used for pagination
 */
public class Range
{
    /**
     * The start of the interval. It can be both positive and negative. A negative start is usually associated with an
     * interval which is relative to the end of the list it is applied to.
     */
    private int start;

    /**
     * The size of the interval. It can be both positive and negative. A negative size is usually associated with an
     * interval which is relative to the end of the list it is applied to.
     */
    private int size;

    /**
     * Creates a new interval having the specified start and size.
     *
     * @param start The start of the interval
     * @param size The size of the interval
     */
    public Range(int start, int size)
    {
        this.start = start;
        this.size = size;
    }

    /**
     * @see #start
     */
    public int getStart()
    {
        return this.start;
    }

    /**
     * @return The absolute value (nonnegative) of this interval's start
     */
    public int getAbsoluteStart()
    {
        return Math.abs(this.start);
    }

    /**
     * @see #size
     */
    public int getSize()
    {
        return this.size;
    }

    /**
     * @return The absolute value (nonnegative) of this interval's size
     */
    public int getAbsoluteSize()
    {
        return Math.abs(this.size);
    }

    /**
     * Copy the given list and return a (smart) subList corresponding to this range. If the absolute size of this range
     * is 0 (ALL) it returns an unmodified copy.
     * <p>
     * Considering this 9 elements list : [0, 1, 2, 3, 4, 5, 6, 7, 8]
     * <ul>
     * <li>Range 0 / 4 : will return [0, 1, 2, 3]</li>
     * <li>Range -2 / 4 : will return [7, 8] (not enough elements for the given size)</li>
     * <li>Range -2 / -4 : will return [3, 4, 5, 6]</li>
     * <li>Range 2 / -4 : will return [0, 1] (not enough elements for the given size)</li>
     * <li>Range 0 / -4 : will return [5, 6, 7, 8]</li>
     * </ul>
     *
     * @param list the list from which the sublist will be extracted
     * @return a sublist of the given list computed from this range
     */
    public List<String> subList(List<String> list)
    {
        List<String> results = new ArrayList<String>();
        results.addAll(list);

        if (getAbsoluteSize() > 0) {
            int min = 0;
            int max = 0;

            min = this.start;

            // negative start : relative to the end of the list
            if (min < 0) {
                min = (list.size()) + this.start;
            }

            max = min + this.size;

            // negative size with start 0 : 0 represents to the end of the list
            if (min == 0 && max < 0) {
                min = list.size() + max;
                max = list.size();
            }

            // with both start and size negative the maximum becomes the mininum & vice versa
            if (min > max) {
                int oldmax = max;
                max = min;
                min = oldmax;
            }

            // out of bounds "sanitization"
            if (min < 0) {
                min = 0;
            }
            if (max > list.size()) {
                max = list.size();
            }

            results = results.subList(min, max);
        }
        return results;
    }
}
