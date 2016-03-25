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
     * @see #getLifespan()
     */
    public static final String LIFESPAN_ID = "lifespan";

    /**
     * Create a new EntryEvictionConfiguration based on LRU algorithm.
     */
    public LRUEvictionConfiguration()
    {
        setAlgorithm(Algorithm.LRU);
    }

    /**
     * Create a new EntryEvictionConfiguration with given capacity, eviction will be based on LRU algorithm.
     * 
     * @param maxEntries the maximum entries the cache can contain.
     * @since 4.3M1
     */
    public LRUEvictionConfiguration(int maxEntries)
    {
        this();

        setMaxEntries(maxEntries);
    }

    /**
     * @param maxEntries see {@link #getMaxEntries()}
     */
    public void setMaxEntries(int maxEntries)
    {
        put(MAXENTRIES_ID, maxEntries);
    }

    /**
     * @return the maximum entries the cache can contain. When the cache reaches that any element, the defined eviction
     *         algorithm kicks in to remove existing cache entries.
     */
    public int getMaxEntries()
    {
        Object obj = get(MAXENTRIES_ID);

        return obj == null ? 0 : (Integer) get(MAXENTRIES_ID);
    }

    /**
     * @param maxIdle see {@link #getMaxIdle()}
     * @since 7.4M2
     */
    public void setMaxIdle(int maxIdle)
    {
        setTimeToLive(maxIdle);
    }

    /**
     * @return the time a cache entry will continue to stay in the cache after being last accessed, in seconds. When
     *         the time is reached, the entry is expired and removed from the cache. In addition, when the cache
     *         reaches its maximum number of entries, the defined eviction algorithm is used (e.g. LRU) and thus an
     *         entry can stay less time in the cache than its maximum defined time.
     * @since 7.4M2
     */
    public int getMaxIdle()
    {
        return getTimeToLive();
    }

    /**
     * @param lifespan see {@link #getLifespan()}
     * @since 7.4M2
     */
    public void setLifespan(int lifespan)
    {
        put(LIFESPAN_ID, lifespan);
    }

    /**
     * @return the maximum lifespan of a cache entry, after which the entry is expired and removed from the cache, in
     *         seconds. In addition, when the cache reaches its maximum number of entries, the defined eviction
     *         algorithm is used (e.g. LRU) and thus an entry can stay less time in the cache than its maximum defined
     *         time.
     * @since 7.4M2
     */
    public int getLifespan()
    {
        Object obj = get(LIFESPAN_ID);

        return obj == null ? 0 : (Integer) get(LIFESPAN_ID);
    }
}
