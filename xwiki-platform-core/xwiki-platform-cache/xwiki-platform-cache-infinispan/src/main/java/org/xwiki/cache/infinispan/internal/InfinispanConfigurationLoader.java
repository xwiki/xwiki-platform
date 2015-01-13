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

import org.apache.commons.lang3.StringUtils;
import org.infinispan.commons.configuration.Builder;
import org.infinispan.commons.configuration.ConfigurationUtils;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfiguration;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfiguration;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.configuration.cache.StoreConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.cache.util.AbstractCacheConfigurationLoader;

/**
 * Customize Infinispan configuration based on XWiki Cache configuration.
 * 
 * @version $Id$
 */
public class InfinispanConfigurationLoader extends AbstractCacheConfigurationLoader
{
    /**
     * The default location of a filesystem based cache loader when not provided in the xml configuration file.
     */
    private static final String DEFAULT_SINGLEFILESTORE_LOCATION = "Infinispan-SingleFileStore";

    /**
     * @param configuration the XWiki cache configuration
     */
    public InfinispanConfigurationLoader(CacheConfiguration configuration)
    {
        super(configuration, null);
    }

    /**
     * @param builder the current builder
     * @param isconfiguration the configuration to customize
     * @return the new builder based on provided configuration if the current one is null
     */
    private ConfigurationBuilder builder(ConfigurationBuilder builder, Configuration isconfiguration)
    {
        if (builder != null) {
            return builder;
        }

        ConfigurationBuilder newBuilder = new ConfigurationBuilder();

        if (isconfiguration != null) {
            newBuilder.read(isconfiguration);
        }

        return newBuilder;
    }

    /**
     * @param isconfiguration the configuration to check
     * @return true if one of the loader is an incomplete {@link FileCacheStoreConfiguration}
     */
    private boolean containsIncompleteFileLoader(Configuration isconfiguration)
    {
        PersistenceConfiguration persistenceConfiguration = isconfiguration.persistence();

        for (StoreConfiguration storeConfiguration : persistenceConfiguration.stores()) {
            if (storeConfiguration instanceof SingleFileStoreConfiguration) {
                SingleFileStoreConfiguration singleFileStoreConfiguration =
                    (SingleFileStoreConfiguration) storeConfiguration;

                String location = singleFileStoreConfiguration.location();

                // "Infinispan-SingleFileStore" is the default location...
                if (StringUtils.isBlank(location) || location.equals(DEFAULT_SINGLEFILESTORE_LOCATION)) {
                    return true;
                }
            }
        }

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
                    SingleFileStoreConfiguration singleFileStoreConfiguration =
                        (SingleFileStoreConfiguration) storeConfiguration;

                    String location = singleFileStoreConfiguration.location();

                    // "Infinispan-SingleFileStore" is the default location...
                    if (StringUtils.isBlank(location) || location.equals(DEFAULT_SINGLEFILESTORE_LOCATION)) {
                        SingleFileStoreConfigurationBuilder singleFileStoreConfigurationBuilder =
                            persistence.addSingleFileStore();
                        singleFileStoreConfigurationBuilder.read(singleFileStoreConfiguration);
                        singleFileStoreConfigurationBuilder.location(createTempDir());
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
     * @param defaultConfiguration the default Infinispan configuration
     * @param namedConfiguration the named default Infinispan configuration
     * @return the new configuration or the passed one if nothing changed
     */
    public Configuration customize(Configuration defaultConfiguration, Configuration namedConfiguration)
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
