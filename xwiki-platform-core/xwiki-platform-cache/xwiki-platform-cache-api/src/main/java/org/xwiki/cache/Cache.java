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
package org.xwiki.cache;

import org.xwiki.cache.event.CacheEntryListener;

/**
 * Cache interface. Used to add/get/remove value from cache which can be local or distributed, with a limited capacity
 * etc. depending of the implementation and configuration.
 * <p>
 * You can create a new cache using the {@link CacheFactory} component.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 */
public interface Cache<T>
{
    /**
     * Add a new value or overwrite the existing one associated with the provided key.
     * <p>
     * You can catch this events using
     * {@link CacheEntryListener#cacheEntryAdded(org.xwiki.cache.event.CacheEntryEvent)} or
     * {@link CacheEntryListener#cacheEntryModified(org.xwiki.cache.event.CacheEntryEvent)}
     * </p>
     * 
     * @param key the associated key used to access the value in the cache
     * @param value the value to store in the cache; if {@code null}, the cache entry is removed
     */
    void set(String key, T value);

    /**
     * @param key the key used to access the value in the cache.
     * @return the value associated with the provided key, or {@code null} if there is no value.
     */
    T get(String key);

    /**
     * Remove the entry associated with the provided key from the cache.
     * <p>
     * You can catch this events using
     * {@link CacheEntryListener#cacheEntryRemoved(org.xwiki.cache.event.CacheEntryEvent)}
     * </p>
     * 
     * @param key the key used to access the value in the cache.
     */
    void remove(String key);

    /**
     * Remove all the entries the cache contains.
     * 
     * @see #remove(String)
     */
    void removeAll();

    /**
     * Add the provided listener to the cache to catch events on entries like add, remove etc.
     * 
     * @param listener the implemented listener.
     */
    void addCacheEntryListener(CacheEntryListener<T> listener);

    /**
     * Remove the provided listener from the list of listeners.
     * 
     * @param listener the implemented listener.
     */
    void removeCacheEntryListener(CacheEntryListener<T> listener);

    /**
     * Release all the resources this cache use.
     */
    void dispose();
}
