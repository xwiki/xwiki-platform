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
package org.xwiki.whatsnew;

import java.util.Date;

import org.xwiki.stability.Unstable;

/**
 * Represents a range of News items (a time period or a number of items starting at a given offset).
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Unstable
public class NewsRange
{
    private int offset;

    private int count;

    private Date startDate;

    private Date endDate;

    /**
     * Constructs a range based on a number of items starting at a given offset.
     *
     * @param offset the starting index for the items
     * @param count the number of items from the starting index included
     */
    public NewsRange(int offset, int count)
    {
        this.offset = offset;
        this.count = count;
    }

    /**
     * Constructs a range based on a time period.
     *
     * @param startDate the news item must be published after that start date
     * @param endDate the news items must be published after that end date
     */
    public NewsRange(Date startDate, Date endDate)
    {
        this.startDate = startDate;
        this.endDate = endDate;
    }
}
