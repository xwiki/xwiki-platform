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
package org.xwiki.cache.jcache.internal;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.cache.expiry.AccessedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.environment.Environment;

/**
 * Implements {@link org.xwiki.cache.CacheFactory} based on JCache.
 * 
 * @version $Id$
 * @since 7.0M1
 */
@Component
@Named("jache")
@Singleton
public class JCacheCacheFactory implements CacheFactory, Initializable, Disposable
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to lookup the container.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * Optional Environment used to access configuration files.
     */
    private Environment environment;

    private CacheManager cacheManager;

    @Override
    public void initialize() throws InitializationException
    {
        this.cacheManager = Caching.getCachingProvider().getCacheManager();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.cacheManager.close();
    }

    @Override
    public <T> org.xwiki.cache.Cache<T> newCache(CacheConfiguration configuration) throws CacheException
    {
        JCacheConfigurationLoader loader = new JCacheConfigurationLoader(configuration);

        String cacheName = configuration.getConfigurationId();

        if (cacheName == null) {
            cacheName = UUID.randomUUID().toString();
        }

        Configuration<String, T> jcacheConfiguration = new XWikiConfiguration<T>(cacheName);

        // Eviction
        EntryEvictionConfiguration eec =
            (EntryEvictionConfiguration) configuration.get(EntryEvictionConfiguration.CONFIGURATIONID);

        if (eec != null && eec.getAlgorithm() == EntryEvictionConfiguration.Algorithm.LRU) {
            if (eec.containsKey(LRUEvictionConfiguration.MAXENTRIES_ID)) {
                int maxEntries = ((Number) eec.get(LRUEvictionConfiguration.MAXENTRIES_ID)).intValue();
                if (configuration.eviction() == null || configuration.eviction().strategy() != EvictionStrategy.LRU
                    || configuration.eviction().maxEntries() != maxEntries) {
                    builder = builder(builder, null);
                    builder.eviction().strategy(EvictionStrategy.LRU);
                    builder.eviction().maxEntries(maxEntries);
                }
            }

            if (eec.getTimeToLive() > 0) {
                AccessedExpiryPolicy policy =
                    new AccessedExpiryPolicy(new Duration(TimeUnit.SECONDS, eec.getTimeToLive()));
            }
        }

        // Filesystem cache

        // create cache

        return new JCacheCache<T>(this.cacheManager, jcacheConfiguration);
    }
}
