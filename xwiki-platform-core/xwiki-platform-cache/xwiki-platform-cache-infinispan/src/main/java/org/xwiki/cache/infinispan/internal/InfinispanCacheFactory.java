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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.infinispan.config.Configuration;
import org.infinispan.config.InfinispanConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.slf4j.Logger;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.container.ApplicationContext;
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
    private static final String DEFAULT_CONFIGURATION_FILE = "/WEB-INF/cache/infinispan/config.xml";

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
     * Optional container used to access configuration files.
     */
    private Container container;

    /**
     * Used to create Infinispan caches.
     */
    private EmbeddedCacheManager cacheManager;

    /**
     * Configurations assigned to specific cache identifiers.
     */
    private final ConcurrentMap<String, Configuration> namedConfigurations =
        new ConcurrentHashMap<String, Configuration>();

    @Override
    public void initialize() throws InitializationException
    {
        // Container
        // Note that the reason we lazy load the container is because we want to be able to use the Cache in
        // environments when there's no container.

        try {
            this.container = this.componentManager.lookup(Container.class);
        } catch (ComponentLookupException e) {
            this.logger.debug("Can't find any Container", e);
        }

        InputStream configurationStream = getConfigurationFileAsStream();

        if (configurationStream != null) {
            // Configuration file loading

            try {
                InfinispanConfiguration configuration =
                    InfinispanConfiguration.newInfinispanConfiguration(configurationStream,
                        InfinispanConfiguration.findSchemaInputStream());

                Configuration defaultConfiguration = configuration.parseDefaultConfiguration();

                for (Map.Entry<String, Configuration> entry : configuration.parseNamedConfigurations().entrySet()) {
                    Configuration c = defaultConfiguration.clone();
                    c.applyOverrides(entry.getValue());
                    this.namedConfigurations.put(entry.getKey(), c);
                }
            } catch (IOException e) {
                this.logger.error("Failed to load Infinispan configuration file", e);
            } finally {
                IOUtils.closeQuietly(configurationStream);
            }

            // CacheManager initialization

            configurationStream = getConfigurationFileAsStream();

            try {
                this.cacheManager = new DefaultCacheManager(configurationStream);
            } catch (IOException e) {
                throw new InitializationException("Failed to create Infinispan cache manager", e);
            } finally {
                IOUtils.closeQuietly(configurationStream);
            }
        } else {
            this.cacheManager = new DefaultCacheManager();
        }
    }

    /**
     * @return the default Infinispan configuration file in the container as stream
     */
    private InputStream getConfigurationFileAsStream()
    {
        InputStream is = null;

        if (this.container != null) {
            ApplicationContext applicationContext = this.container.getApplicationContext();
            if (applicationContext != null) {
                is = applicationContext.getResourceAsStream(DEFAULT_CONFIGURATION_FILE);
            }
        }

        return is;
    }

    @Override
    public <T> org.xwiki.cache.Cache<T> newCache(CacheConfiguration configuration) throws CacheException
    {
        Configuration customizedConfiguration = null;

        String cacheName = configuration.getConfigurationId();
        if (cacheName == null) {
            // Infinispan require a name for the cache
            cacheName = UUID.randomUUID().toString();
            configuration.setConfigurationId(cacheName);
        } else {
            customizedConfiguration = this.namedConfigurations.get(cacheName);
        }

        // Apply XWiki cache configuration

        InfinispanConfigurationLoader loader = new InfinispanConfigurationLoader(configuration);

        if (customizedConfiguration == null) {
            customizedConfiguration = this.cacheManager.getDefaultConfiguration().clone();
            loader.customize(customizedConfiguration, true);
        } else {
            loader.customize(customizedConfiguration, false);
        }

        // Set custom configuration

        this.cacheManager.defineConfiguration(cacheName, customizedConfiguration);

        // create cache

        return new InfinispanCache<T>(this.cacheManager, loader.getCacheConfiguration());
    }
}
