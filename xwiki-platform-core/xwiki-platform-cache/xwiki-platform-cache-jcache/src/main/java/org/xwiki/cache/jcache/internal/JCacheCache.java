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
package org.xwiki.cache.jcache.internal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.event.CacheEntryCreatedListener;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;
import javax.cache.event.CacheEntryUpdatedListener;

import org.xwiki.cache.jcache.internal.event.JCacheCacheEntryEvent;
import org.xwiki.cache.jcache.internal.event.XWikiCacheEntryListenerConfiguration;
import org.xwiki.cache.util.AbstractCache;

/**
 * Implements {@link org.xwiki.cache.Cache} based on JCache.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 * @since 7.0M1
 */
public class JCacheCache<T> extends AbstractCache<T> implements CacheEntryRemovedListener<String, T>,
    CacheEntryExpiredListener<String, T>, CacheEntryCreatedListener<String, T>, CacheEntryUpdatedListener<String, T>
{
    /**
     * The JCache cache.
     */
    private Cache<String, T> cache;

    /**
     * The state of the node before modification.
     */
    private ConcurrentMap<String, T> preEventData = new ConcurrentHashMap<String, T>();

    /**
     * The JCache cache manager.
     */
    private CacheManager cacheManager;

    /**
     * @param cacheManager the JCache cache manager
     * @param configuration the XWiki Cache configuration
     */
    JCacheCache(CacheManager cacheManager, XWikiConfiguration<T> configuration)
    {
        this.cacheManager = cacheManager;
        this.cacheManager.createCache(configuration.getName(), configuration);
        this.cache = cacheManager.<String, T>getCache(configuration.getName());

        this.cache.registerCacheEntryListener(new XWikiCacheEntryListenerConfiguration<T>(this));
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
        this.cache.removeAll();
    }

    @Override
    public void dispose()
    {
        super.dispose();

        this.cacheManager.destroyCache(this.cache.getName());
    }

    // ////////////////////////////////////////////////////////////////
    // Events
    // ////////////////////////////////////////////////////////////////

    /**
     * Dispatch data insertion event.
     * 
     * @param event the event
     */
    private void cacheEntryInserted(CacheEntryEvent<? extends String, ? extends T> event)
    {
        JCacheCacheEntryEvent<T> xwikiEvent =
            new JCacheCacheEntryEvent<T>(new JCacheCacheEntry<T>(this, event.getKey(), event.getValue()));

        if (event.getOldValue() != null) {
            if (event.getOldValue() != event.getValue()) {
                disposeCacheValue(event.getOldValue());
            }

            sendEntryModifiedEvent(xwikiEvent);
        } else {
            sendEntryAddedEvent(xwikiEvent);
        }
    }

    /**
     * Dispatch data remove event.
     * 
     * @param event the event
     */
    private void cacheEntryRemoved(CacheEntryEvent<? extends String, ? extends T> event)
    {
        JCacheCacheEntryEvent<T> xwikiEvent =
            new JCacheCacheEntryEvent<T>(new JCacheCacheEntry<T>(this, event.getKey(), event.getOldValue()));

        sendEntryRemovedEvent(xwikiEvent);
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends String, ? extends T>> events)
        throws CacheEntryListenerException
    {
        for (CacheEntryEvent<? extends String, ? extends T> event : events) {
            cacheEntryInserted(event);
        }
    }

    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends String, ? extends T>> events)
        throws CacheEntryListenerException
    {
        for (CacheEntryEvent<? extends String, ? extends T> event : events) {
            cacheEntryInserted(event);
        }
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends String, ? extends T>> events)
        throws CacheEntryListenerException
    {
        for (CacheEntryEvent<? extends String, ? extends T> event : events) {
            cacheEntryRemoved(event);
        }
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends String, ? extends T>> events)
        throws CacheEntryListenerException
    {
        for (CacheEntryEvent<? extends String, ? extends T> event : events) {
            cacheEntryRemoved(event);
        }
    }
}
