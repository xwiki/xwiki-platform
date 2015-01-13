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
package org.xwiki.cache.infinispan.internal;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntriesEvicted;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntriesEvictedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.infinispan.internal.event.InfinispanCacheEntryEvent;
import org.xwiki.cache.util.AbstractCache;

/**
 * Implements {@link org.xwiki.cache.Cache} based on Infinispan.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 * @since 3.3M1
 */
@Listener
public class InfinispanCache<T> extends AbstractCache<T>
{
    /**
     * The Infinispan cache.
     */
    private Cache<String, T> cache;

    /**
     * The state of the node before modification.
     */
    private ConcurrentMap<String, T> preEventData = new ConcurrentHashMap<String, T>();

    /**
     * The Infinispan cache manager.
     */
    private EmbeddedCacheManager cacheManager;

    /**
     * @param cacheManager the Infinispan cache manager
     * @param configuration the XWiki Cache configuration
     */
    InfinispanCache(EmbeddedCacheManager cacheManager, CacheConfiguration configuration)
    {
        this.cacheManager = cacheManager;
        this.cache = cacheManager.<String, T>getCache(configuration.getConfigurationId());

        this.cache.addListener(this);
    }

    @Override
    public void remove(String key)
    {
        this.cache.remove(key);
    }

    @Override
    public void set(String key, T obj)
    {
        if (obj != null) {
            this.cache.put(key, obj);
        } else {
            this.cache.remove(key);
        }
    }

    @Override
    public T get(String key)
    {
        return this.cache.get(key);
    }

    @Override
    public void removeAll()
    {
        this.cache.clear();
    }

    @Override
    public void dispose()
    {
        super.dispose();

        this.cacheManager.removeCache(this.cache.getName());
    }

    // ////////////////////////////////////////////////////////////////
    // Events
    // ////////////////////////////////////////////////////////////////

    /**
     * @param event the eviction event.
     */
    @CacheEntriesEvicted
    public void nodeEvicted(CacheEntriesEvictedEvent<String, T> event)
    {
        for (Map.Entry<String, T> entry : event.getEntries().entrySet()) {
            String key = entry.getKey();
            T value = entry.getValue();

            // Looks like eviction does not produce any pre event
            cacheEntryRemoved(key, value);
        }
    }

    /**
     * @param event the eviction event.
     */
    @CacheEntryRemoved
    public void nodeRemoved(CacheEntryRemovedEvent<String, T> event)
    {
        String key = event.getKey();
        T value = event.getValue();

        if (event.isPre()) {
            if (value != null) {
                this.preEventData.put(key, value);
            }
        } else {
            cacheEntryRemoved(event.getKey(), this.preEventData.get(key));

            this.preEventData.remove(key);
        }
    }

    /**
     * @param event the modification event.
     */
    @CacheEntryCreated
    public void nodeCreated(CacheEntryCreatedEvent<String, T> event)
    {
        String key = event.getKey();

        if (!event.isPre()) {
            cacheEntryInserted(key, event.getValue());
        }
    }

    /**
     * @param event the modification event.
     */
    @CacheEntryModified
    public void nodeModified(CacheEntryModifiedEvent<String, T> event)
    {
        String key = event.getKey();
        T value = event.getValue();

        if (event.isPre()) {
            if (value != null) {
                this.preEventData.put(key, value);
            }
        } else {
            cacheEntryInserted(key, value);

            this.preEventData.remove(key);
        }
    }

    /**
     * Dispatch data insertion event.
     * 
     * @param key the entry key.
     * @param value the entry value.
     */
    private void cacheEntryInserted(String key, T value)
    {
        InfinispanCacheEntryEvent<T> event =
            new InfinispanCacheEntryEvent<T>(new InfinispanCacheEntry<T>(this, key, value));

        T previousValue = this.preEventData.get(key);

        if (previousValue != null) {
            if (previousValue != value) {
                disposeCacheValue(previousValue);
            }

            sendEntryModifiedEvent(event);
        } else {
            sendEntryAddedEvent(event);
        }
    }

    /**
     * Dispatch data remove event.
     * 
     * @param key the entry key.
     * @param value the entry value.
     */
    private void cacheEntryRemoved(String key, T value)
    {
        InfinispanCacheEntryEvent<T> event =
            new InfinispanCacheEntryEvent<T>(new InfinispanCacheEntry<T>(this, key, value));

        sendEntryRemovedEvent(event);
    }
}
