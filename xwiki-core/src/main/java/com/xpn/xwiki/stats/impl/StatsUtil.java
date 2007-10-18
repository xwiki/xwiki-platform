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

import java.util.Calendar;
import java.util.Date;

/**
 * Utility class for statistics
 * 
 * @version $Id: $
 */
public class StatsUtil
{
    public static int PERIOD_MONTH = 0;

    public static int PERIOD_DAY = 1;

    /**
     * Computes an integer representation of the passed date using the following format:
     * <ul>
     *   <li>"yyyMMdd" for {@link #PERIOD_DAY}</li>
     *   <li>"yyyMM" for {@link #PERIOD_MONTH}</li>
     * </ul>
     * 
     * @param date the date for which to return an integer representation
     * @param type the date type. It can be {@link #PERIOD_DAY} or {@link #PERIOD_MONTH}
     * @return the integer representation of the specified date
     * @see java.text.SimpleDateFormat
     */
    public static int getPeriodAsInt(Date date, int type)
    {
        Calendar cal = Calendar.getInstance();
        if (date != null) {
            cal.setTime(date);
        }
        if (type == PERIOD_MONTH) {
            // The first month of the year is JANUARY which is 0
            return cal.get(Calendar.YEAR) * 100 + (cal.get(Calendar.MONTH) + 1);
        } else {
            // The first day of the month has value 1
            return cal.get(Calendar.YEAR) * 10000 + (cal.get(Calendar.MONTH) + 1) * 100
                + cal.get(Calendar.DAY_OF_MONTH);
        }
    }
}
