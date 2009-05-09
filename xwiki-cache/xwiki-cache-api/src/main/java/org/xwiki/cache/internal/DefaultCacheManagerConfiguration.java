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

import org.xwiki.cache.CacheManagerConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.configuration.ConfigurationManager;
import org.xwiki.configuration.ConfigurationSourceCollection;

/**
 * Default implementation of {@link CacheManagerConfiguration}.
 * 
 * @version $Id$
 */
@Component
public class DefaultCacheManagerConfiguration implements CacheManagerConfiguration, Initializable
{
    /**
     * The default cache implementation.
     */
    private static final String DEFAULT_CACHE_HINT = "jbosscache";

    /**
     * The default local cache implementation.
     */
    private static final String DEFAULT_LOCALCACHE_HINT = "jbosscache/local";

    /**
     * Injected by the Component Manager.
     */
    @Requirement
    private ConfigurationManager configurationManager;

    /**
     * Injected by the Component Manager.
     */
    @Requirement
    private ConfigurationSourceCollection sourceCollection;

    /**
     * The role hint of configured default cache component.
     */
    private String defaultCache = DEFAULT_CACHE_HINT;

    /**
     * The role hint of configured default local cache component.
     */
    private String defaultLocalCache = DEFAULT_LOCALCACHE_HINT;

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
     * @see org.xwiki.cache.CacheManagerConfiguration#getDefaultCache()
     */
    public String getDefaultCache()
    {
        return this.defaultCache;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.CacheManagerConfiguration#getDefaultLocalCache()
     */
    public String getDefaultLocalCache()
    {
        return this.defaultLocalCache;
    }

    /**
     * @param cacheHint the role hint of configured default cache component.
     */
    public void setDefaultCache(String cacheHint)
    {
        this.defaultCache = cacheHint;
    }

    /**
     * @param localCacheHint the role hint of configured default local cache component.
     */
    public void setLocalDefaultCache(String localCacheHint)
    {
        this.defaultLocalCache = localCacheHint;
    }
}
