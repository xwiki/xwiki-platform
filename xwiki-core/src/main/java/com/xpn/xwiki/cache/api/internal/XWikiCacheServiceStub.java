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
 *
 */

package com.xpn.xwiki.cache.api.internal;

import java.util.Properties;

import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheService;

@Deprecated
public class XWikiCacheServiceStub implements XWikiCacheService
{
    CacheFactory cacheFactory;

    CacheFactory localCacheFactory;

    public XWikiCacheServiceStub(CacheFactory cacheFactory, CacheFactory localCacheFactory)
    {
        this.cacheFactory = cacheFactory;
        this.localCacheFactory = localCacheFactory;
    }

    public void init(XWiki context)
    {
    }

    public XWikiCache newCache(String cacheName) throws XWikiException
    {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setConfigurationId(cacheName);

        try {
            return new XWikiCacheStub(this.cacheFactory.newCache(configuration));
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to create new cache", e);
        }
    }

    public XWikiCache newCache(String cacheName, int capacity) throws XWikiException
    {
        CacheConfiguration configuration = new CacheConfiguration();
        configuration.setConfigurationId(cacheName);
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(capacity);
        configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

        try {
            return new XWikiCacheStub(this.cacheFactory.newCache(configuration));
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to create new cache", e);
        }
    }

    public XWikiCache newCache(String cacheName, Properties props) throws XWikiException
    {
        return newCache(cacheName);
    }

    public XWikiCache newCache(String cacheName, Properties props, int capacity) throws XWikiException
    {
        return newCache(cacheName, capacity);
    }

    public XWikiCache newLocalCache() throws XWikiException
    {
        try {
            return new XWikiCacheStub(this.localCacheFactory.newCache(new CacheConfiguration()));
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to create new cache", e);
        }
    }

    public XWikiCache newLocalCache(int capacity) throws XWikiException
    {
        CacheConfiguration configuration = new CacheConfiguration();
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(capacity);
        configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

        try {
            return new XWikiCacheStub(this.localCacheFactory.newCache(configuration));
        } catch (CacheException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE, XWikiException.ERROR_CACHE_INITIALIZING,
                "Failed to create new cache", e);
        }
    }

    public XWikiCache newLocalCache(Properties props) throws XWikiException
    {
        return newLocalCache();
    }

    public XWikiCache newLocalCache(Properties props, int capacity) throws XWikiException
    {
        return newLocalCache(capacity);
    }

}
