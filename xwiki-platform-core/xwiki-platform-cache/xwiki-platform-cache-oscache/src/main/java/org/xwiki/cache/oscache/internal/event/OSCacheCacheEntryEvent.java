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

import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.oscache.internal.OSCacheCache;
import org.xwiki.cache.oscache.internal.OSCacheCacheEntry;

import com.opensymphony.oscache.base.events.CacheEntryEvent;

/**
 * Implements {@link org.xwiki.cache.event.CacheEntryEvent} based on OSCache.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 */
public class OSCacheCacheEntryEvent<T> extends OSCacheCacheEvent<T> implements org.xwiki.cache.event.CacheEntryEvent<T>
{
    /**
     * The OSCache event object.
     */
    private CacheEntryEvent event;

    /**
     * @param cache the cache for which this event has been sent.
     * @param event the OSCaceh event object.
     */
    public OSCacheCacheEntryEvent(OSCacheCache<T> cache, CacheEntryEvent event)
    {
        super(cache);

        this.event = event;
    }

    @Override
    public CacheEntry<T> getEntry()
    {
        return new OSCacheCacheEntry<T>(this.cache, this.event.getEntry());
    }
}
