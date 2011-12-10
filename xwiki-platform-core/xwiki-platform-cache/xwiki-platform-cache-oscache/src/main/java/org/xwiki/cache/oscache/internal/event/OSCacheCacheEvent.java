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
package org.xwiki.cache.oscache.internal.event;

import org.xwiki.cache.Cache;
import org.xwiki.cache.event.CacheEvent;
import org.xwiki.cache.oscache.internal.OSCacheCache;

/**
 * Implements {@link CacheEvent} based on OSCache.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 */
public class OSCacheCacheEvent<T> implements CacheEvent<T>
{
    /**
     * The cache which generated this event.
     */
    protected OSCacheCache<T> cache;

    /**
     * @param cache the cache for which this event has been sent.
     */
    public OSCacheCacheEvent(OSCacheCache<T> cache)
    {
        this.cache = cache;
    }

    @Override
    public Cache<T> getCache()
    {
        return cache;
    }
}
