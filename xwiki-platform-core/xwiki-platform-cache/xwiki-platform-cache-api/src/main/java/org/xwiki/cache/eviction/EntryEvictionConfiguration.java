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

import java.util.HashMap;

/**
 * This configuration class is used to add constraints in the configuration of the cache to create.
 * <p>
 * <code>
 * CacheFactory factory = (CacheFactory) getComponentManager().lookup(CacheFactory.class, this.roleHint);
 *
 * CacheConfiguration conf = new CacheConfiguration();
 * LRUEvictionConfiguration lec = new LRUEvictionConfiguration();
 * lec.setMaxEntries(1);
 * conf.put(LRUEvictionConfiguration.CONFIGURATIONID, lec);
 * </code>
 * 
 * @version $Id$
 */
public class EntryEvictionConfiguration extends HashMap<String, Object>
{
    /**
     * The key use to access eviction configuration in the {@link org.xwiki.cache.config.CacheConfiguration}.
     */
    public static final String CONFIGURATIONID = "eviction";

    /**
     * The key used to access time to live duration in the {@link EntryEvictionConfiguration}.
     */
    public static final String TIMETOLIVE_ID = "timetolive";

    /**
     * The ordering/storing algorithm used by the cache.
     * 
     * @version $Id$
     */
    public enum Algorithm
    {
        /**
         * Unlimited cache. Depends of the implementation own limitations.
         * <p>
         * No specific configuration.
         */
        NONE,
        /**
         * Evicts the least recently used entry when thresholds are hit.
         * <p>
         * Support <code>maxentries</code> property. See {@link LRUEvictionConfiguration}.
         */
        LRU
    }

    /**
     * The ordering/storing algorithm used by the cache.
     */
    private Algorithm mode;

    /**
     * @param mode the ordering/storing algorithm used by the cache.
     */
    public void setAlgorithm(Algorithm mode)
    {
        this.mode = mode;
    }

    /**
     * @return the ordering/storing algorithm used by the cache.
     */
    public Algorithm getAlgorithm()
    {
        return mode;
    }

    /**
     * @param timeToLive the maximum time to live in seconds of a cache entry.
     */
    public void setTimeToLive(int timeToLive)
    {
        put(TIMETOLIVE_ID, timeToLive);
    }

    /**
     * @return the maximum time to live in seconds of a cache entry.
     */
    public int getTimeToLive()
    {
        Object obj = get(TIMETOLIVE_ID);

        return obj == null ? 0 : (Integer) get(TIMETOLIVE_ID);
    }
}
