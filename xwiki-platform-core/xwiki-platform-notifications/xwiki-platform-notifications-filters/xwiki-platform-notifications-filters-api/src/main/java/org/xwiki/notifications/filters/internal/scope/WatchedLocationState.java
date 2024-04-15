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

import java.util.Date;

/**
 * The state of a location (is it watched or not) associated with a starting date if the location is watched.
 *
 * @version $Id$
 * @since 10.9
 * @since 10.8.1
 */
public class WatchedLocationState
{
    public enum WatchedState
    {
        WATCHED,
        WATCHED_BY_ANCESTOR,
        BLOCKED,
        BLOCKED_BY_ANCESTOR,
        CUSTOM,
        NOT_SET
    }
    private final WatchedState state;

    private Date startingDate;

    /**
     * Construct a WatchedLocationState with no starting date.
     */
    public WatchedLocationState()
    {
        this.state = WatchedState.NOT_SET;
    }

    /**
     * Construct a WatchedLocationState.
     * @param startingDate since when the location is watched (can be null)
     */
    public WatchedLocationState(WatchedState state, Date startingDate)
    {
        this.state = state;
        if (startingDate != null) {
            this.startingDate = truncateMilliseconds(startingDate);
        }
    }

    private Date truncateMilliseconds(Date startingDate)
    {
        long dateValue = startingDate.getTime() / 1000;
        return new Date(dateValue * 1000);
    }

    public WatchedState getState()
    {
        return state;
    }

    /**
     * @return since when the location is watched (can be null)
     */
    public Date getStartingDate()
    {
        return startingDate;
    }
}
