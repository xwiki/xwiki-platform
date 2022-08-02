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

package org.xwiki.security.authorization.cache.internal;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheEntry;
import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * A Mock for the Cache<?> interface that ensure the same function than a cache without any
 * unexpected eviction.
 *
 * @param <T> Type of values stored in the cache.
 *
 * @version $Id$
 * @since 5.0M2
 */
public class TestCache<T> implements Cache<T>
{
    private Map<String, T> cache = new HashMap<String, T>();
    private CacheEntryListener<T> listener;
    private String lastInsertedKey;

    class TestCacheEntry implements CacheEntry<T>
    {
        private final String key;
        private final T value;

        TestCacheEntry(String key, T value)
        {
            this.key = key;
            this.value = value;
        }

        @Override
        public Cache<T> getCache()
        {
            return TestCache.this;
        }

        @Override
        public String getKey()
        {
            return key;
        }

        @Override
        public T getValue()
        {
            return value;
        }
    }

    private CacheEntryEvent<T> getEvent(final String key, final T value)
    {
        return new CacheEntryEvent<T>()
        {
            @Override
            public CacheEntry<T> getEntry()
            {
                return new TestCacheEntry(key, value);
            }

            @Override
            public Cache<T> getCache()
            {
                return TestCache.this;
            }
        };
    }


    @Override
    public void set(String key, T value)
    {
        T old = cache.put(key, value);
        if (listener != null && old == null) {
            listener.cacheEntryAdded(getEvent(key, value));
        } else {
            if (old != value) {
                disposeCacheValue(old);
            }

            if (listener != null) {
                listener.cacheEntryModified(getEvent(key, value));
            }
        }
        lastInsertedKey = key;
    }

    @Override
    public T get(String key)
    {
        return cache.get(key);
    }

    @Override
    public void remove(String key)
    {
        T value = cache.remove(key);
        if (listener != null) {
            listener.cacheEntryRemoved(getEvent(key, value));
        }
        disposeCacheValue(value);
    }

    @Override
    public void removeAll()
    {
        cache.clear();
    }

    @Override
    public void addCacheEntryListener(CacheEntryListener<T> tCacheEntryListener)
    {
        listener = tCacheEntryListener;
    }

    @Override
    public void removeCacheEntryListener(CacheEntryListener<T> tCacheEntryListener)
    {
        assertThat(tCacheEntryListener, equalTo(listener));
        listener = null;
    }

    @Override
    public void dispose()
    {
        listener = null;
    }

    private void disposeCacheValue(T value)
    {
        if (value instanceof DisposableCacheValue) {
            try {
                ((DisposableCacheValue) value).dispose();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public String getLastInsertedKey()
    {
        return lastInsertedKey;
    }
}
