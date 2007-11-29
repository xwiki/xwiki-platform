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
 * Helper factory class for creating Interval objects in velocity
 */
public class IntervalFactory
{
    /**
     * The interval that matches the entire list it is applied to.
     */
    public static final Interval ALL = createInterval(0, 0);

    /**
     * The interval that matches just the first element of the list it is applied to.
     */
    public static final Interval FIRST = createInterval(0, 1);

    /**
     * The interval that matches just the last element of the list it is applied to.
     */
    public static final Interval LAST = createInterval(0, -1);

    /**
     * This factory is implemented as a singleton. This is its only instance.
     */
    private static final IntervalFactory instance = new IntervalFactory();

    private IntervalFactory()
    {
    }

    /**
     * @return The only instance of this singleton factory
     */
    public static IntervalFactory getInstance()
    {
        return instance;
    }

    /**
     * @see Interval#Interval(int, int)
     */
    public static Interval createInterval(int start, int size)
    {
        return new Interval(start, size);
    }

    /**
     * Creates a new Interval starting from 0 and having the specified size. It matches the first
     * <code>size</code> elements of the list it is applied to.
     * 
     * @param size The size of the interval
     * @return A new Interval instance
     */
    public static Interval createHeadInterval(int size)
    {
        return createInterval(0, Math.abs(size));
    }

    /**
     * Creates a new Interval starting from the end of the list it is applied to and having the
     * specified size. It matches the last <code>size</code> elements of the list.
     * 
     * @param size The size of the interval
     * @return A new Interval instance
     */
    public static Interval createTailInterval(int size)
    {
        return createInterval(0, -Math.abs(size));
    }

    /**
     * Helper method for accessing {@link #ALL} static field in velocity
     */
    public static Interval getALL()
    {
        return ALL;
    }

    /**
     * Helper method for accessing {@link #FIRST} static field in velocity
     */
    public static Interval getFIRST()
    {
        return FIRST;
    }

    /**
     * Helper method for accessing {@link #LAST} static field in velocity
     */
    public static Interval getLAST()
    {
        return LAST;
    }
}
