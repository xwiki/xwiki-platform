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
package org.xwiki.cache.jcache.internal.event;

import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.jcache.internal.JCacheCache;
import org.xwiki.cache.jcache.internal.JCacheCacheEntry;

/**
 * Implements {@link org.xwiki.cache.event.CacheEntryEvent} based on JCache.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 * @since 7.0M1
 */
public class JCacheCacheEntryEvent<T> extends JCacheCacheEvent<T> implements
    org.xwiki.cache.event.CacheEntryEvent<T>
{
    /**
     * The cache entry associated with the event.
     */
    private JCacheCacheEntry<T> entry;

    /**
     * @param entry the cache entry associated with the event.
     */
    public JCacheCacheEntryEvent(JCacheCacheEntry<T> entry)
    {
        super((JCacheCache<T>) entry.getCache());

        this.entry = entry;
    }

    @Override
    public CacheEntry<T> getEntry()
    {
        return entry;
    }
}
