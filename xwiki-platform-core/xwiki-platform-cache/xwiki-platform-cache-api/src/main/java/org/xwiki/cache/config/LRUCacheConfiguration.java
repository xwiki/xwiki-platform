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
package org.xwiki.cache.config;

import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

/**
 * Cache configuration using LRU eviction method.
 * 
 * @version $Id$
 * @since 4.3M1
 */
public class LRUCacheConfiguration extends CacheConfiguration
{
    /**
     * Unique id for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates new LRUCacheConfiguration instance with empty (null) configurationId, default size of 100 and time to
     * live 0.
     */
    public LRUCacheConfiguration()
    {
        this(null);
    }

    /**
     * Creates new LRUCacheConfiguration instance with given configurationId, default max size of 100 and time to live
     * 0.
     * 
     * @param configurationId configuration identifier
     */
    public LRUCacheConfiguration(String configurationId)
    {
        super(configurationId);

        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        put(EntryEvictionConfiguration.CONFIGURATIONID, lru);
    }

    /**
     * Creates new LRUCacheConfiguration instance with given configurationId and max size, and default time to live 0.
     * 
     * @param configurationId configuration identifier
     * @param maxSize maximum cache capacity
     */
    public LRUCacheConfiguration(String configurationId, int maxSize)
    {
        super(configurationId);

        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(maxSize);
        put(EntryEvictionConfiguration.CONFIGURATIONID, lru);
    }

    /**
     * Creates new LRUCacheConfiguration instance with given configurationId, max size and time to live.
     * 
     * @param configurationId configuration identifier
     * @param maxSize maximum cache capacity
     * @param maxIdle for how long cache entry will be valid (in seconds) since the last time it was used
     */
    public LRUCacheConfiguration(String configurationId, int maxSize, int maxIdle)
    {
        super(configurationId);

        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(maxSize);
        lru.setMaxIdle(maxIdle);
        put(EntryEvictionConfiguration.CONFIGURATIONID, lru);
    }

    /**
     * @return the eviction configuration as a {@link LRUEvictionConfiguration} instance
     * @since 7.4M2
     */
    public LRUEvictionConfiguration getLRUEvictionConfiguration()
    {
        return (LRUEvictionConfiguration) get(EntryEvictionConfiguration.CONFIGURATIONID);
    }
}
