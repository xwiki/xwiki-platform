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
package org.xwiki.livedata;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * The live data to display.
 * 
 * @version $Id$
 * @since 12.10
 */
public class LiveData
{
    /**
     * The total number of entries available. This is used for pagination.
     */
    private long count;

    /**
     * The live data entries. Could be all or just a subset. Each entry is a mapping between property names and property
     * values. Property names are specific to each live data source.
     */
    private final List<Map<String, Object>> entries = new LinkedList<>();

    /**
     * @return the total number of entries available
     */
    public long getCount()
    {
        return count;
    }

    /**
     * Set the total number of entries available.
     * 
     * @param count the new entry count
     */
    public void setCount(long count)
    {
        this.count = count;
    }

    /**
     * @return the live data entries
     */
    public List<Map<String, Object>> getEntries()
    {
        return entries;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder().append(getCount()).append(getEntries()).build();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) {
            return true;
        }

        if (obj instanceof LiveData) {
            LiveData liveData = (LiveData) obj;
            return new EqualsBuilder().append(getCount(), liveData.getCount())
                .append(getEntries(), liveData.getEntries()).build();
        }

        return false;
    }

    @Override
    public String toString()
    {
        return getCount() + ", " + getEntries().toString();
    }
}
