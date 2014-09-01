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

import java.util.Date;

/**
 * Add a backward compatibility layer to the {@link StatsUtil} class.
 * 
 * @version $Id$
 */
public privileged aspect StatsUtilCompatibilityAspect
{
    /**
     * @deprecated Use {@link PeriodType} since 1.4M1.
     */
    @Deprecated
    public static int StatsUtil.PERIOD_MONTH = 0;

    /**
     * @deprecated Use {@link PeriodType} since 1.4M1.
     */
    @Deprecated
    public static int StatsUtil.PERIOD_DAY = 1;

    /**
     * Computes an integer representation of the passed date using the following format:
     * <ul>
     * <li>"yyyMMdd" for {@link PERIOD_DAY}</li>
     * <li>"yyyMM" for {@link PERIOD_MONTH}</li>
     * </ul>
     * 
     * @param date the date for which to return an integer representation
     * @param type the date type. It can be {@link PERIOD_DAY} or {@link PERIOD_MONTH}
     * @return the integer representation of the specified date
     * @see java.text.SimpleDateFormat
     * @deprecated Use
     *             {@link StatsUtil#getPeriodAsInt(Date, com.xpn.xwiki.stats.impl.StatsUtil.PeriodType)}
     *             since 1.4M1.
     */
    @Deprecated
    public static int StatsUtil.getPeriodAsInt(Date date, int type)
    {
        return StatsUtil.getPeriodAsInt(date, PeriodType.values()[type]);
    }
}
