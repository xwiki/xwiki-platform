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
package org.xwiki.cache.jbosscache.internal;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import org.jboss.cache.config.CacheLoaderConfig;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.EvictionConfig;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.config.parsing.XmlConfigurationParser;
import org.jboss.cache.eviction.LRUAlgorithmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.cache.util.AbstractCacheConfigurationLoader;
import org.xwiki.container.Container;

/**
 * Convert XWiki cache configuration into JBossCache configuration.
 * 
 * @version $Id$
 */
public class JBossCacheCacheConfiguration extends AbstractCacheConfigurationLoader
{
    /**
     * The name of the field containing the wakeup interval to set to {@link EvictionConfig}.
     */
    public static final String CONFX_EVICTION_WAKEUPINTERVAL = "wakeupinterval";

    /**
     * the logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JBossCacheCacheConfiguration.class);

    /**
     * The folder containing JBossCache properties files.
     */
    private static final String PROPS_PATH = "cache/jbosscache/";

    /**
     * The extension of JBossCache properties files.
     */
    private static final String PROPS_EXT = ".xml";
    
    /**
     * Name of the JBossCache properties containing filesystem cache location.
     */
    private static final String JBCPROP_LOCATION = "location";

    /**
     * The JBossCache configuration.
     */
    private Configuration jbossConfiguration;

    /**
     * The optional container used to access configuration files (can be null).
     */
    private Container container;

    /**
     * @param container the container used to access configuration files, can be null if there's no container
     * @param configuration the XWiki cache API configuration.
     * @param defaultPropsId the default configuration identifier used to load cache configuration file.
     */
    public JBossCacheCacheConfiguration(Container container, CacheConfiguration configuration, String defaultPropsId)
    {
        super(configuration, defaultPropsId);

        this.container = container;

        load();
    }

    /**
     * @return the JBossCache configuration.
     */
    public Configuration getJBossCacheConfiguration()
    {
        return this.jbossConfiguration;
    }

    /**
     * Load JBossCache properties file.
     */
    private void load()
    {
        this.jbossConfiguration = loadConfig(getCacheConfiguration().getConfigurationId());

        // If no custom configuration is set, use the provided CacheConfiguration to set one
        if (this.jbossConfiguration == null) {
            this.jbossConfiguration = getDefaultConfig();

            if (this.jbossConfiguration == null) {
                this.jbossConfiguration = new Configuration();
            }

            // Set eviction configuration

            EntryEvictionConfiguration eec =
                (EntryEvictionConfiguration) getCacheConfiguration().get(EntryEvictionConfiguration.CONFIGURATIONID);

            if (eec != null && eec.getAlgorithm() == EntryEvictionConfiguration.Algorithm.LRU) {
                EvictionConfig ec = this.jbossConfiguration.getEvictionConfig();

                if (ec == null) {
                    ec = new EvictionConfig();
                    this.jbossConfiguration.setEvictionConfig(ec);
                }

                // handle eviction thread timer
                if (eec.containsKey(CONFX_EVICTION_WAKEUPINTERVAL)) {
                    ec.setWakeupInterval(((Number) eec.get(CONFX_EVICTION_WAKEUPINTERVAL)).longValue());
                }

                setLRUConfiguration(ec.getDefaultEvictionRegionConfig(), eec);
            }
        }

        // set unique name of the cache
        this.jbossConfiguration.setClusterName(getCacheConfiguration().getConfigurationId());

        completeCacheLoaderConfiguration();
    }

    /**
     * Add or update the {@link LRUAlgorithmConfig}.
     * 
     * @param erc the JBoss eviction configuration.
     * @param eec the XWiki eviction configuration.
     */
    private void setLRUConfiguration(EvictionRegionConfig erc, EntryEvictionConfiguration eec)
    {
        LRUAlgorithmConfig lruc;

        if (erc.getEvictionAlgorithmConfig() instanceof LRUAlgorithmConfig) {
            lruc = (LRUAlgorithmConfig) erc.getEvictionAlgorithmConfig();
        } else {
            lruc = new LRUAlgorithmConfig();
        }

        if (eec.containsKey(LRUEvictionConfiguration.MAXENTRIES_ID)) {
            lruc.setMaxNodes(((Number) eec.get(LRUEvictionConfiguration.MAXENTRIES_ID)).intValue());
        }

        if (eec.getTimeToLive() > 0) {
            lruc.setTimeToLive(eec.getTimeToLive(), TimeUnit.SECONDS);
        }

        erc.setEvictionAlgorithmConfig(lruc);
    }

    /**
     * Add missing configuration needed by some JBossCache {@link org.jboss.cache.loader.CacheLoader} implementations.
     */
    private void completeCacheLoaderConfiguration()
    {
        CacheLoaderConfig config = this.jbossConfiguration.getCacheLoaderConfig();

        if (config != null) {
            for (IndividualCacheLoaderConfig iconfig : config.getIndividualCacheLoaderConfigs()) {
                if (iconfig.getClassName().equals("org.jboss.cache.loader.FileCacheLoader")) {
                    String location = (String) iconfig.getProperties().get(JBCPROP_LOCATION);

                    if (location == null || location.trim().length() == 0) {
                        iconfig.getProperties().put(JBCPROP_LOCATION, createTempDir());
                    }
                }
            }
        }
    }

    /**
     * @return the default JBossCache configuration.
     */
    protected Configuration getDefaultConfig()
    {
        return loadConfig(getDefaultPropsId());
    }

    /**
     * Load JBossCache configuration file based on configuration identifier.
     * 
     * @param propertiesId the configuration identifier.s
     * @return the JBossCache configuration.
     */
    private Configuration loadConfig(String propertiesId)
    {
        Configuration config = null;

        String propertiesFilename = propertiesId + PROPS_EXT;

        File file = new File(PROPS_PATH + propertiesFilename);

        InputStream is = null;

        // Note: We look into the container only if it exists and if it has its application context specified since
        // we want to allow usage of JBoss Cache even in environments where there's no container or no application
        // context.
        try {
            if (file.exists()) {
                is = new FileInputStream(file);
            } else if (this.container != null && this.container.getApplicationContext() != null) {
                is = this.container.getApplicationContext().getResourceAsStream(
                    "/WEB-INF/" + PROPS_PATH + propertiesFilename);
            }

            if (is == null) {
                LOGGER.debug("Can't find any configuration file for [" + propertiesId
                    + "]. Default JBoss Cache configuration will be used");
            } else {
                XmlConfigurationParser parser = new XmlConfigurationParser();
                config = parser.parseStream(is);

                LOGGER.debug("Properties [{}] loaded", propertiesFilename);
            }
        } catch (Exception e) {
            // TODO: Raise a runtime exception to stop the application since the configuration that the user has
            // specified couldn't be loaded for some reason.
            LOGGER.error("Failed to load configuration file [" + propertiesId + "]. Default JBoss Cache configuration "
                + "will be used.", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.debug("Failed to close properties file", e);
                }
            }
        }

        return config;
    }
}
