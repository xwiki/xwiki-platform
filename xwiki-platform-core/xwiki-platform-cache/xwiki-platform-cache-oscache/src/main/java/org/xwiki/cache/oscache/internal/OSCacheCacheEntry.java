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

import org.xwiki.cache.Cache;

import com.opensymphony.oscache.base.CacheEntry;

/**
 * Implements {@link org.xwiki.cache.CacheEntry} based on OSCache.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 */
public class OSCacheCacheEntry<T> implements org.xwiki.cache.CacheEntry<T>
{
    /**
     * The cache where this entry comes from.
     */
    private OSCacheCache<T> cache;

    /**
     * The OSCache cache entry.
     */
    private CacheEntry entry;

    /**
     * @param cache the cache where this entry comes from.
     * @param entry the OSCache cache entry.
     */
    public OSCacheCacheEntry(OSCacheCache<T> cache, CacheEntry entry)
    {
        this.cache = cache;
        this.entry = entry;
    }

    @Override
    public Cache<T> getCache()
    {
        return this.cache;
    }

    @Override
    public T getValue()
    {
        return (T) entry.getContent();
    }

    @Override
    public String getKey()
    {
        return this.cache.apiKey(entry.getKey());
    }
}
