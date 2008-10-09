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
package org.xwiki.cache.internal;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSourceCollection;

/**
 * The default implementation of CacheManager. It uses ConfigurationManager to find the cache an local cache hints to
 * use to lookup cache components.
 * 
 * @version $Id$
 * @since 1.7M1
 */
public class DefaultCacheManager implements CacheManager, Initializable, Composable
{
    /**
     * The component manager to use to find cache components.
     */
    private ComponentManager componentManager;

    /**
     * Injected by the Component Manager.
     */
    private ConfigurationManager configurationManager;

    /**
     * Injected by the Component Manager.
     */
    private ConfigurationSourceCollection sourceCollection;

    /**
     * The role hint of configured default cache component.
     */
    private String cacheHint;

    /**
     * The role hint of configured default local cache component.
     */
    private String localCacheHint;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Composable#compose(org.xwiki.component.manager.ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.configurationManager.initializeConfiguration(this, this.sourceCollection.getConfigurationSources(),
            "cache");
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheManager#getCacheFactory()
     */
    public CacheFactory getCacheFactory() throws ComponentLookupException
    {
        return getCacheFactory(this.cacheHint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheManager#getLocalCacheFactory()
     */
    public CacheFactory getLocalCacheFactory() throws ComponentLookupException
    {
        return getCacheFactory(this.localCacheHint);
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
        return (CacheFactory) this.componentManager.lookup(CacheFactory.ROLE, cacheHint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheManager#createNewCache(org.xwiki.cache.config.CacheConfiguration)
     */
    public <T> Cache<T> createNewCache(CacheConfiguration config) throws CacheException, ComponentLookupException
    {
        return createNewCache(config, this.cacheHint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheManager#createNewLocalCache(org.xwiki.cache.config.CacheConfiguration)
     */
    public <T> Cache<T> createNewLocalCache(CacheConfiguration config) throws CacheException, ComponentLookupException
    {
        return createNewCache(config, this.localCacheHint);
    }

    /**
     * Lookup the cache component with provided hint and create a new cache.
     * 
     * @param <T> the class of the data stored in the cache.
     * @param config the cache configuration.
     * @param cacheHint the role hint to lookup.
     * @return a new {@link Cache}.
     * @throws CacheException error when creating the cache.
     * @throws ComponentLookupException error when searching for cache component.
     */
    public <T> Cache<T> createNewCache(CacheConfiguration config, String cacheHint) throws CacheException,
        ComponentLookupException
    {
        CacheFactory cacheFactory = (CacheFactory) this.componentManager.lookup(CacheFactory.ROLE, cacheHint);

        return cacheFactory.newCache(config);
    }

    /**
     * @return the role hint of configured default cache component.
     */
    public String getDefaultCache()
    {
        return this.cacheHint;
    }

    /**
     * @param cacheHint the role hint of configured default cache component.
     */
    public void setDefaultCache(String cacheHint)
    {
        this.cacheHint = cacheHint;
    }

    /**
     * @return the role hint of configured default local cache component.
     */
    public String getLocalDefaultCache()
    {
        return this.localCacheHint;
    }

    /**
     * @param localCacheHint the role hint of configured default local cache component.
     */
    public void setLocalDefaultCache(String localCacheHint)
    {
        this.localCacheHint = localCacheHint;
    }
}
