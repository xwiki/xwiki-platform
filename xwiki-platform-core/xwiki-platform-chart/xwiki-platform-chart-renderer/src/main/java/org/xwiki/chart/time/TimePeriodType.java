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
package org.xwiki.chart.time;

/**
 * Enumeration of supported time period types.
 * 
 * @version $Id$
 * @since 4.2M1
 */
public enum TimePeriodType
{
    /** Millisecond. */
    MILLISECOND("millisecond"),
    /** Second. */
    SECOND("second"),
    /** Minute. */
    MINUTE("minute"),
    /** Hour. */
    HOUR("hour"),
    /** A day. */
    DAY("day"),
    /** Week. */
    WEEK("week"),
    /** Month. */
    MONTH("month"),
    /** Qarter of a year. */
    QUARTER("quarter"),
    /** Year. */
    YEAR("year"),
    /** A simple (irregular) time period. */
    SIMPLE("simple");

    /** The name. */
    private final String name;

    /**
     * @param name the name.
     */
    TimePeriodType(String name)
    {
        this.name = name;
    }

    /** @return the name. */
    public String getName()
    {
        return name;
    }

    /**
     * @param name A plot type.
     * @return the axis type corresponding to the name, or {@code null}.
     */
    public static TimePeriodType forName(String name)
    {
        for (TimePeriodType type : values())
        {
            if (name.equals(type.getName())) {
                return type;
            }
        }

        return null;
    }

}
