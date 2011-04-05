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

package com.xpn.xwiki.criteria.impl;

/**
 * Helper factory class for creating Range objects in velocity
 */
public class RangeFactory
{
    /**
     * The interval that matches the entire list it is applied to.
     */
    public static final Range ALL = createRange(0, 0);

    /**
     * The interval that matches just the first element of the list it is applied to.
     */
    public static final Range FIRST = createRange(0, 1);

    /**
     * The interval that matches just the last element of the list it is applied to.
     */
    public static final Range LAST = createRange(0, -1);

    public RangeFactory()
    {
    }

    /**
     * @see Range#Range(int, int)
     */
    public static Range createRange(int start, int size)
    {
        return new Range(start, size);
    }

    /**
     * Creates a new Range which matches all the elements of the list it is applied to.
     * 
     * @return A new Range instance
     */
    public static Range createAllRange()
    {
        return new Range(0, 0);
    }

    /**
     * Creates a new Range starting from 0 and having the specified size. It matches the first <code>size</code>
     * elements of the list it is applied to.
     * 
     * @param size The size of the interval
     * @return A new Range instance
     */
    public static Range createHeadRange(int size)
    {
        return createRange(0, Math.abs(size));
    }

    /**
     * Creates a new Range starting from the end of the list it is applied to and having the specified size. It matches
     * the last <code>size</code> elements of the list.
     * 
     * @param size The size of the interval
     * @return A new Range instance
     */
    public static Range createTailRange(int size)
    {
        return createRange(0, -Math.abs(size));
    }

    /**
     * Helper method for accessing {@link #ALL} static field in velocity
     */
    public static Range getALL()
    {
        return ALL;
    }

    /**
     * Helper method for accessing {@link #FIRST} static field in velocity
     */
    public static Range getFIRST()
    {
        return FIRST;
    }

    /**
     * Helper method for accessing {@link #LAST} static field in velocity
     */
    public static Range getLAST()
    {
        return LAST;
    }
}
