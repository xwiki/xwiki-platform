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

import javax.cache.configuration.Configuration;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.cache.util.AbstractCacheConfigurationLoader;

import com.google.common.collect.ImmutableBiMap.Builder;

/**
 * Customize JCache configuration based on XWiki Cache configuration.
 * 
 * @version $Id$
 * @since 7.0M1
 */
public class JCacheConfigurationLoader extends AbstractCacheConfigurationLoader
{
    /**
     * @param configuration the XWiki cache configuration
     */
    public JCacheConfigurationLoader(CacheConfiguration configuration)
    {
        super(configuration, null);
    }

    /**
     * @param isconfiguration the configuration to check
     * @return true if one of the loader is an incomplete {@link FileCacheStoreConfiguration}
     */
    private boolean containsIncompleteFileLoader(Configuration isconfiguration)
    {
        // TODO

        return false;
    }

    /**
     * Customize the eviction configuration.
     * 
     * @param currentBuilder the configuration builder
     * @param configuration the configuration
     * @return the configuration builder
     */
    private ConfigurationBuilder customizeEviction(ConfigurationBuilder currentBuilder, Configuration configuration)
    {
        ConfigurationBuilder builder = currentBuilder;

        EntryEvictionConfiguration eec =
            (EntryEvictionConfiguration) getCacheConfiguration().get(EntryEvictionConfiguration.CONFIGURATIONID);

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
                long maxIdle = eec.getTimeToLive() * 1000L;
                if (configuration.expiration() == null || configuration.expiration().maxIdle() != maxIdle) {
                    builder = builder(builder, null);
                    builder.expiration().maxIdle(eec.getTimeToLive() * 1000L);
                }
            }
        }

        return builder;
    }

    /**
     * Add missing location for filesystem based cache.
     * 
     * @param currentBuilder the configuration builder
     * @param configuration the configuration
     * @return the configuration builder
     */
    private ConfigurationBuilder completeFilesystem(ConfigurationBuilder currentBuilder, Configuration configuration)
    {
        ConfigurationBuilder builder = currentBuilder;

        if (containsIncompleteFileLoader(configuration)) {
            builder = builder(builder, configuration);

            PersistenceConfigurationBuilder persistence = builder.persistence();

            persistence.clearStores();

            for (StoreConfiguration storeConfiguration : configuration.persistence().stores()) {
                if (storeConfiguration instanceof SingleFileStoreConfiguration) {
                    String location = fileCacheLoaderConfig.location();
                    // "JCache-FileCacheStore" is the default location...
                    if (StringUtils.isBlank(location) || location.equals(DEFAULT_FILECACHESTORE_LOCATION)) {
                        loaderBuilder.location(createTempDir());
                    }
                } else {
                    // Copy the loader as it is
                    Class<? extends StoreConfigurationBuilder<?, ?>> storeBuilderClass =
                        (Class<? extends StoreConfigurationBuilder<?, ?>>) ConfigurationUtils
                            .<StoreConfiguration>builderFor(storeConfiguration);
                    Builder<StoreConfiguration> storeBuilder =
                        (Builder<StoreConfiguration>) persistence.addStore(storeBuilderClass);
                    storeBuilder.read(storeConfiguration);
                }
            }
        }

        return builder;
    }

    /**
     * Customize provided configuration based on XWiki cache configuration.
     * 
     * @return the new configuration or the passed one if nothing changed
     */
    public <T> Configuration<String, T> customize()
    {
        // Set custom configuration

        ConfigurationBuilder builder = null;

        if (namedConfiguration == null) {
            builder = customizeEviction(builder, defaultConfiguration);
        }

        // Make sure filesystem based caches have a proper location

        if (namedConfiguration != null) {
            builder = completeFilesystem(builder, namedConfiguration);
        }

        return builder != null ? builder.build() : null;
    }
}
