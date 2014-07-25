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
package org.xwiki.cache.oscache.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.oscache.internal.event.OSCacheCacheEntryEvent;
import org.xwiki.cache.util.AbstractCache;

import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.base.events.CacheEntryEvent;
import com.opensymphony.oscache.base.events.CacheEntryEventListener;
import com.opensymphony.oscache.base.events.CacheGroupEvent;
import com.opensymphony.oscache.base.events.CachePatternEvent;
import com.opensymphony.oscache.base.events.CachewideEvent;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;

/**
 * Implements {@link org.xwiki.cache.Cache} based on OSCache.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 */
public class OSCacheCache<T> extends AbstractCache<T> implements CacheEntryEventListener
{
    /**
     * Logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OSCacheCache.class);

    /**
     * The OSCache cache configuration.
     */
    private OSCacheCacheConfiguration oscacheConfiguration;

    /**
     * The OSCache cache management tool.
     */
    private GeneralCacheAdministrator cacheAdmin;

    /**
     * Create and initialize the cache.
     * 
     * @param oscacheConfiguration the configuration to use to create the cache.
     */
    public void initialize(OSCacheCacheConfiguration oscacheConfiguration)
    {
        this.oscacheConfiguration = oscacheConfiguration;

        this.configuration = this.oscacheConfiguration.getCacheConfiguration();

        this.cacheAdmin = new GeneralCacheAdministrator(this.oscacheConfiguration.getOSCacheProperties());

        this.cacheAdmin.getCache().addCacheEventListener(this);
    }

    /**
     * @return the cache name used to make cache entries keys unique between all the caches.
     */
    private String getName()
    {
        return this.oscacheConfiguration.getName();
    }

    /**
     * Get the real cache key from the API cache key.
     * 
     * @param apiKey the API cache key.
     * @return the real cache.
     */
    private String cacheKey(String apiKey)
    {
        return getName() != null ? getName() + apiKey : apiKey;
    }

    /**
     * Get the API cache key from the real cache key.
     * 
     * @param cacheKey the real cache.
     * @return the API cache key.
     */
    public String apiKey(String cacheKey)
    {
        return getName() != null ? cacheKey.substring(getName().length()) : cacheKey;
    }

    @Override
    public void remove(String key)
    {
        this.cacheAdmin.flushEntry(cacheKey(key));
    }

    @Override
    public void set(String key, T obj)
    {
        this.cacheAdmin.putInCache(cacheKey(key), obj);
    }

    @Override
    public T get(String key)
    {
        T value = null;

        String cacheKey = cacheKey(key);
        try {
            value = (T) this.cacheAdmin.getFromCache(cacheKey, oscacheConfiguration.getTimeToLive());
        } catch (NeedsRefreshException e) {
            this.cacheAdmin.cancelUpdate(cacheKey);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Failed to get the value from the cache", e);
            }
        }

        return value;
    }

    @Override
    public void removeAll()
    {
        this.cacheAdmin.flushAll();
    }

    @Override
    public void dispose()
    {
        super.dispose();

        this.cacheAdmin.destroy();
    }

    // ////////////////////////////////////////////////////////////////
    // Events
    // ////////////////////////////////////////////////////////////////

    @Override
    public void cacheEntryAdded(CacheEntryEvent event)
    {
        sendEntryAddedEvent(new OSCacheCacheEntryEvent<T>(this, event));
    }

    @Override
    public void cacheEntryFlushed(CacheEntryEvent event)
    {
        sendEntryRemovedEvent(new OSCacheCacheEntryEvent<T>(this, event));
    }

    @Override
    public void cacheEntryRemoved(CacheEntryEvent event)
    {
        sendEntryRemovedEvent(new OSCacheCacheEntryEvent<T>(this, event));
    }

    @Override
    public void cacheEntryUpdated(CacheEntryEvent event)
    {
        sendEntryModifiedEvent(new OSCacheCacheEntryEvent<T>(this, event));
    }

    @Override
    public void cacheGroupFlushed(CacheGroupEvent event)
    {
    }

    @Override
    public void cachePatternFlushed(CachePatternEvent event)
    {
    }

    @Override
    public void cacheFlushed(CachewideEvent event)
    {
    }
}
