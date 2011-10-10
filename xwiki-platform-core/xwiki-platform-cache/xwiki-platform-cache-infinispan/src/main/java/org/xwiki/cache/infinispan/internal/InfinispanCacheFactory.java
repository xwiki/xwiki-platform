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
package org.xwiki.cache.infinispan.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.infinispan.config.Configuration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.Container;

/**
 * Implements {@link org.xwiki.cache.CacheFactory} based on Infinispan.
 * 
 * @version $Id$
 * @since 3.3M1
 */
@Component
@Named("infinispan")
@Singleton
public class InfinispanCacheFactory implements CacheFactory, Initializable
{
    /**
     * The folder containing Infinispan properties files.
     */
    private static final String DEFAULT_CONFIGURATION_FILE = "/WEB-INF/cache/infinispan/default.xml";

    /**
     * The container used to access configuration files.
     */
    @Inject
    private Container container;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The original default configuration (generally coming from the configuration file).
     */
    private Configuration defaultConfiguration;

    /**
     * Used to create Infinispan caches.
     */
    private EmbeddedCacheManager cacheManager;

    @Override
    public void initialize() throws InitializationException
    {
        InputStream configurationStream = getConfigurationFileAsStream();

        if (configurationStream != null) {
            try {
                this.cacheManager = new DefaultCacheManager(configurationStream);
            } catch (IOException e) {
                throw new InitializationException("Failed to create Infinispan cache manager", e);
            } finally {
                try {
                    configurationStream.close();
                } catch (IOException e) {
                    logger.error("Failed to close configuration file stream", e);
                }
            }
        } else {
            this.cacheManager = new DefaultCacheManager();
        }

        // save the real default configuration to be able to restore it

        this.defaultConfiguration = this.cacheManager.getDefaultConfiguration().clone();
    }

    /**
     * @return the default Infinispan configuration file in the container as stream
     */
    private InputStream getConfigurationFileAsStream()
    {
        InputStream is;

        if (this.container != null && this.container.getApplicationContext() != null) {
            is = this.container.getApplicationContext().getResourceAsStream(DEFAULT_CONFIGURATION_FILE);
        } else {
            is = null;
        }

        return is;
    }

    @Override
    public <T> org.xwiki.cache.Cache<T> newCache(CacheConfiguration configuration) throws CacheException
    {
        // set custom configuration

        EntryEvictionConfiguration eec =
            (EntryEvictionConfiguration) configuration.get(EntryEvictionConfiguration.CONFIGURATIONID);

        boolean configChanged = false;
        if (eec != null && eec.getAlgorithm() == EntryEvictionConfiguration.Algorithm.LRU) {
            Configuration isDefaultConfiguration = this.cacheManager.getDefaultConfiguration();
            if (eec.containsKey(LRUEvictionConfiguration.MAXENTRIES_ID)) {
                isDefaultConfiguration.fluent().eviction().strategy(EvictionStrategy.LRU)
                    .maxEntries(((Number) eec.get(LRUEvictionConfiguration.MAXENTRIES_ID)).intValue());
                configChanged = true;
            }

            if (eec.getTimeToLive() > 0) {
                isDefaultConfiguration.fluent().expiration().maxIdle(eec.getTimeToLive() * 1000L);
                configChanged = true;
            }
        }

        // create cache

        try {
            String cacheName = configuration.getConfigurationId();
            if (cacheName == null) {
                // Infinispan require a name for the cache
                cacheName = UUID.randomUUID().toString();
            }

            return new InfinispanCache<T>(this.cacheManager.<String, T> getCache(cacheName), configuration);
        } finally {
            // restore default configuration
            if (configChanged) {
                this.cacheManager.getDefaultConfiguration().applyOverrides(this.defaultConfiguration);
            }
        }
    }
}
