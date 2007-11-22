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
    public static final Interval ALL = createInterval(0, 0);

    public static final Interval FIRST = createInterval(0, 1);

    public static final Interval LAST = createInterval(0, -1);

    private static final IntervalFactory instance = new IntervalFactory();

    private IntervalFactory()
    {
    }

    public static IntervalFactory getInstance()
    {
        return instance;
    }

    public static Interval createInterval(int start, int size)
    {
        return new Interval(start, size);
    }

    public static Interval createHeadInterval(int size)
    {
        return createInterval(0, size);
    }

    public static Interval createTailInterval(int size)
    {
        return createInterval(0, -size);
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
