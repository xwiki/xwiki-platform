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

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;

import org.xwiki.cache.jcache.internal.JCacheCache;

/**
 * XWiki implementation of {@link CacheEntryListenerConfiguration}.
 * 
 * @param <T> the type of the cache stored in cache
 * @version $Id$
 * @since 7.0M1
 */
public class XWikiCacheEntryListenerConfiguration<T> implements CacheEntryListenerConfiguration<String, T>,
    Factory<CacheEntryListener<? super String, ? super T>>
{
    JCacheCache<T> listener;

    public XWikiCacheEntryListenerConfiguration(JCacheCache<T> listener)
    {
        this.listener = listener;
    }

    // CacheEntryListenerConfiguration

    @Override
    public Factory<CacheEntryListener<? super String, ? super T>> getCacheEntryListenerFactory()
    {
        return this;
    }

    @Override
    public boolean isOldValueRequired()
    {
        return true;
    }

    @Override
    public Factory<CacheEntryEventFilter<? super String, ? super T>> getCacheEntryEventFilterFactory()
    {
        return null;
    }

    @Override
    public boolean isSynchronous()
    {
        return false;
    }

    // Factory

    @Override
    public CacheEntryListener<? super String, ? super T> create()
    {
        return this.listener;
    }
}
