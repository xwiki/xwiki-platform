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
package org.xwiki.cache.internal;

import org.xwiki.cache.Cache;
import org.xwiki.cache.event.CacheEntryListener;

/**
 * Default implementation of {@link Cache}. Does not really store anything and always return null when trying to get a
 * value associated with a key.
 * 
 * @param <T> the class of the data stored in the cache.
 * @version $Id$
 */
public class DefaultCache<T> implements Cache<T>
{
    @Override
    public void remove(String key)
    {

    }

    @Override
    public void set(String key, T obj)
    {

    }

    @Override
    public T get(String key)
    {
        return null;
    }

    @Override
    public void removeAll()
    {

    }

    @Override
    public void addCacheEntryListener(CacheEntryListener<T> listener)
    {

    }

    @Override
    public void removeCacheEntryListener(CacheEntryListener<T> listener)
    {

    }

    @Override
    public void dispose()
    {

    }
}
