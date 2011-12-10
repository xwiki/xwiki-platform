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
package org.xwiki.cache.eviction;

/**
 * An helper for {@link EntryEvictionConfiguration.Algorithm#NONE} cache algorithm. Evicts the least recently used entry
 * when thresholds are hit.
 * 
 * @version $Id$
 */
public class LRUEvictionConfiguration extends EntryEvictionConfiguration
{
    /**
     * The key to access the maximum entries the cache can contain.
     */
    public static final String MAXENTRIES_ID = "maxentries";

    /**
     * Create a new EntryEvictionConfiguration based on LRU algorithm.
     */
    public LRUEvictionConfiguration()
    {
        setAlgorithm(Algorithm.LRU);
    }

    /**
     * @param maxEntries the maximum entries the cache can contain.
     */
    public void setMaxEntries(int maxEntries)
    {
        put(MAXENTRIES_ID, maxEntries);
    }

    /**
     * @return the maximum entries the cache can contain.
     */
    public int getMaxEntries()
    {
        Object obj = get(MAXENTRIES_ID);

        return obj == null ? 0 : (Integer) get(MAXENTRIES_ID);
    }
}
