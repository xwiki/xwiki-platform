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
     * @param timeToLive for how long cache entry will be valid (in seconds)
     */
    public LRUCacheConfiguration(String configurationId, int maxSize, int timeToLive)
    {
        super(configurationId);

        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(maxSize);
        lru.setTimeToLive(timeToLive);
        put(EntryEvictionConfiguration.CONFIGURATIONID, lru);
    }
}
