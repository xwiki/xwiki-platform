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
package org.xwiki.cache.event;

import org.xwiki.stability.Unstable;

/**
 * Empty implementations of the {@link CacheEntryListener} interface to make it easy for code wanting to listen to
 * events to only override the method(s) corresponding to the event(s) listened to.
 *
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 * @since 8.1M1
 */
@Unstable
public abstract class AbstractCacheEntryListener<T> implements CacheEntryListener<T>
{
    @Override
    public void cacheEntryAdded(CacheEntryEvent<T> event)
    {
        // Do nothing by default
    }

    @Override
    public void cacheEntryRemoved(CacheEntryEvent<T> event)
    {
        // Do nothing by default
    }

    @Override
    public void cacheEntryModified(CacheEntryEvent<T> event)
    {
        // Do nothing by default
    }
}
