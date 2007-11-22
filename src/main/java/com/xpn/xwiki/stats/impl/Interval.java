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
    private int start;

    private int size;

    public Interval(int start, int size)
    {
        this.start = start;
        this.size = size;
    }

    public int getStart()
    {
        return start;
    }

    public int getAbsoluteStart()
    {
        return Math.abs(start);
    }

    public int getSize()
    {
        return size;
    }

    public int getAbsoluteSize()
    {
        return Math.abs(size);
    }
}
