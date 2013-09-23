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

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheEntry;

/**
 * Implements {@link org.xwiki.cache.CacheEntry} based on Infinispan.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 * @since 3.3M1
 */
public class InfinispanCacheEntry<T> implements CacheEntry<T>
{
    /**
     * The cache.
     */
    private Cache<T> cache;

    /**
     * The entry key.
     */
    private String key;

    /**
     * the entry data.
     */
    private T data;

    /**
     * @param cache the cache where this entry comes from.
     * @param key the entry key.
     * @param data the entry data.
     */
    public InfinispanCacheEntry(Cache<T> cache, String key, T data)
    {
        this.cache = cache;
        this.key = key;
        this.data = data;
    }

    @Override
    public Cache<T> getCache()
    {
        return cache;
    }

    @Override
    public T getValue()
    {
        return this.data;
    }

    @Override
    public String getKey()
    {
        return this.key;
    }
}
