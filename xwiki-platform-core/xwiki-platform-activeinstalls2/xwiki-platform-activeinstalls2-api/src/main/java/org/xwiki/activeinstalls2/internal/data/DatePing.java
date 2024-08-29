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
package org.xwiki.activeinstalls2.internal.data;

import java.util.Date;

/**
 * Represents date-related Ping data.
 *
 * @version $Id$
 * @since 14.4RC1
 */
public class DatePing
{
    /**
     * Filled automatically by an ingest pipeline when left empty.
     */
    private Date first;

    private long since;

    /**
     * Filled automatically by an ingest pipeline (will overwrite any value).
     */
    private Date current;

    /**
     * @return the first time a given XWiki instance has sent a ping
     */
    public Date getFirst()
    {
        return this.first;
    }

    /**
     * @param first see {@link #getFirst()}
     */
    public void setFirst(Date first)
    {
        this.first = first;
    }

    /**
     * @return the number of days that have passed since the first ping
     */
    public long getSince()
    {
        return this.since;
    }

    /**
     * @param since see {@link #getSince()}
     */
    public void setSince(long since)
    {
        this.since = since;
    }

    /**
     * @return the date for the current ping
     */
    public Date getCurrent()
    {
        return this.current;
    }

    /**
     * @param current see {@link #getCurrent()}
     */
    public void setCurrent(Date current)
    {
        this.current = current;
    }
}
