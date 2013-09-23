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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.CacheManagerConfiguration;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * The default implementation of CacheManager. It uses ConfigurationManager to find the cache an local cache hints to
 * use to lookup cache components.
 * 
 * @version $Id$
 * @since 1.7M1
 */
@Component
@Singleton
public class DefaultCacheManager implements CacheManager
{
    /**
     * The component manager to use to find cache components.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The configuration component for {@link CacheManager}.
     */
    @Inject
    private CacheManagerConfiguration configuration;

    @Override
    public CacheFactory getCacheFactory() throws ComponentLookupException
    {
        return getCacheFactory(this.configuration.getDefaultCache());
    }

    @Override
    public CacheFactory getLocalCacheFactory() throws ComponentLookupException
    {
        return getCacheFactory(this.configuration.getDefaultLocalCache());
    }

    /**
     * Lookup the cache creation component with provided hint return it.
     * 
     * @param cacheHint the role hint to lookup.
     * @return a cache creation service.
     * @throws ComponentLookupException error when searching for cache component.
     */
    public CacheFactory getCacheFactory(String cacheHint) throws ComponentLookupException
    {
        return this.componentManager.getInstance(CacheFactory.class, cacheHint);
    }

    @Override
    public <T> Cache<T> createNewCache(CacheConfiguration config) throws CacheException
    {
        return createNewCache(config, this.configuration.getDefaultCache());
    }

    @Override
    public <T> Cache<T> createNewLocalCache(CacheConfiguration config) throws CacheException
    {
        return createNewCache(config, this.configuration.getDefaultLocalCache());
    }

    /**
     * Lookup the cache component with provided hint and create a new cache.
     * 
     * @param <T> the class of the data stored in the cache.
     * @param config the cache configuration.
     * @param cacheHint the role hint to lookup.
     * @return a new {@link Cache}.
     * @throws CacheException error when creating the cache.
     */
    public <T> Cache<T> createNewCache(CacheConfiguration config, String cacheHint) throws CacheException
    {
        CacheFactory cacheFactory;
        try {
            cacheFactory = this.componentManager.getInstance(CacheFactory.class, cacheHint);
        } catch (ComponentLookupException e) {
            throw new CacheException("Failed to get cache factory for role hint [" + cacheHint + "]", e);
        }

        return cacheFactory.newCache(config);
    }
}
