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

package org.xwiki.notifications.filters.internal.scope;

import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WatchedLocationStateTest
{
    @Test
    void getStartingDate()
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(0);
        calendar.set(2024, Calendar.FEBRUARY, 16, 15, 52, 53);
        long timeInMillis = calendar.getTimeInMillis();

        Date expectedDate = calendar.getTime();

        for (int i = 1; i < 999; i++) {
            Date startingDate = new Date(timeInMillis + i);
            assertNotEquals(expectedDate, startingDate);
            Date obtainedStartingDate =
                new WatchedLocationState(WatchedLocationState.WatchedState.NOT_SET, startingDate).getStartingDate();
            assertEquals(expectedDate, obtainedStartingDate,
                String.format("Expected [%s] and got [%s]", expectedDate.getTime(), obtainedStartingDate.getTime()));
        }
    }
}