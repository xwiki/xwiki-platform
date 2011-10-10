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
import org.infinispan.config.Configuration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.file.FileCacheStoreConfig;
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
     * @param configuration the XWiki cache configuration
     */
    public InfinispanConfigurationLoader(CacheConfiguration configuration)
    {
        super(configuration, null);
    }

    /**
     * Customize provided configuration based on XWiki cache configuration.
     * 
     * @param isConfiguration the Infinispan configuration
     * @return true if the provided configuration has been modified
     */
    public boolean customize(Configuration isConfiguration)
    {
        boolean configChanged = false;

        EntryEvictionConfiguration eec =
            (EntryEvictionConfiguration) getCacheConfiguration().get(EntryEvictionConfiguration.CONFIGURATIONID);

        if (eec != null && eec.getAlgorithm() == EntryEvictionConfiguration.Algorithm.LRU) {
            if (eec.containsKey(LRUEvictionConfiguration.MAXENTRIES_ID)) {
                isConfiguration.fluent().eviction().strategy(EvictionStrategy.LRU)
                    .maxEntries(((Number) eec.get(LRUEvictionConfiguration.MAXENTRIES_ID)).intValue());
                configChanged = true;
            }

            if (eec.getTimeToLive() > 0) {
                isConfiguration.fluent().expiration().maxIdle(eec.getTimeToLive() * 1000L);
                configChanged = true;
            }
        }

        for (CacheLoaderConfig cacheLoaderConfig : isConfiguration.getCacheLoaders()) {
            if (cacheLoaderConfig instanceof FileCacheStoreConfig) {
                FileCacheStoreConfig fileCacheLoaderConfig = (FileCacheStoreConfig) cacheLoaderConfig;
                String location = fileCacheLoaderConfig.getLocation();

                if (StringUtils.isBlank(location)) {
                    fileCacheLoaderConfig.location(createTempDir());
                }
            }
        }

        return configChanged;
    }
}
