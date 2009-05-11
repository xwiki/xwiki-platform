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
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.config.CacheLoaderConfig;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.EvictionConfig;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.config.CacheLoaderConfig.IndividualCacheLoaderConfig;
import org.jboss.cache.eviction.LRUConfiguration;
import org.jboss.cache.eviction.LRUPolicy;
import org.jboss.cache.factories.XmlConfigurationParser;
import org.jboss.cache.loader.FileCacheLoaderConfig;
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
    private static final Log LOG = LogFactory.getLog(JBossCacheCacheConfiguration.class);

    /**
     * The folder containing JBossCache properties files.
     */
    private static final String PROPS_PATH = "cache/jbosscache/";

    /**
     * The extension of JBossCache properties files.
     */
    private static final String PROPS_EXT = ".xml";

    /**
     * The default value of the wakeup interval to set to {@link EvictionConfig}.
     */
    private static final int DEFAULT_WAKEUPINTERVAL = 5;

    /**
     * The JBossCache configuration.
     */
    private Configuration jbossConfiguration;

    /**
     * The container used to access configuration files.
     */
    private Container container;

    /**
     * @param container the container used to access configuration files.
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

        if (this.jbossConfiguration == null) {
            this.jbossConfiguration = getDefaultConfig();

            if (this.jbossConfiguration == null) {
                this.jbossConfiguration = new Configuration();
            }

            EntryEvictionConfiguration eec =
                (EntryEvictionConfiguration) getCacheConfiguration().get(EntryEvictionConfiguration.CONFIGURATIONID);

            if (eec != null && eec.getAlgorithm() == EntryEvictionConfiguration.Algorithm.LRU) {
                EvictionConfig ec = this.jbossConfiguration.getEvictionConfig();

                if (ec == null) {
                    ec = new EvictionConfig();
                    this.jbossConfiguration.setEvictionConfig(ec);
                }

                ec.setDefaultEvictionPolicyClass(LRUPolicy.class.getName());

                if (eec.containsKey(CONFX_EVICTION_WAKEUPINTERVAL)) {
                    ec.setWakeupIntervalSeconds(((Number) eec.get(CONFX_EVICTION_WAKEUPINTERVAL)).intValue());
                } else {
                    ec.setWakeupIntervalSeconds(DEFAULT_WAKEUPINTERVAL);
                }

                List<EvictionRegionConfig> ercList = ec.getEvictionRegionConfigs();

                EvictionRegionConfig erc = null;
                if (ercList != null && ercList.size() > 0) {
                    erc = ercList.get(0);
                } else {
                    erc = new EvictionRegionConfig();
                    ec.setEvictionRegionConfigs(Collections.singletonList(erc));
                }
                erc.setRegionFqn(JBossCacheCache.ROOT_FQN);

                setLRUConfiguration(erc, eec);
            }
        }

        this.jbossConfiguration.setClusterName(getCacheConfiguration().getConfigurationId());

        completeCacheLoaderConfiguration();
    }

    /**
     * Add or update the {@link LRUConfiguration}.
     * 
     * @param erc the JBoss eviction configuration.
     * @param eec the XWiki eviction configuration.
     */
    private void setLRUConfiguration(EvictionRegionConfig erc, EntryEvictionConfiguration eec)
    {
        LRUConfiguration lru = null;

        if (erc.getEvictionPolicyConfig() instanceof LRUConfiguration) {
            lru = (LRUConfiguration) erc.getEvictionPolicyConfig();
        } else {
            lru = new LRUConfiguration();
            lru.setTimeToLiveSeconds(0);
        }

        if (eec.containsKey(LRUEvictionConfiguration.MAXENTRIES_ID)) {
            lru.setMaxNodes(((Number) eec.get(LRUEvictionConfiguration.MAXENTRIES_ID)).intValue());
        }

        if (eec.getTimeToLive() > 0) {
            lru.setTimeToLiveSeconds(eec.getTimeToLive());
        }

        erc.setEvictionPolicyConfig(lru);
    }

    /**
     * Add missing configuration needed by some JBossCache {@link CacheLoader} implementations.
     */
    private void completeCacheLoaderConfiguration()
    {
        CacheLoaderConfig config = this.jbossConfiguration.getCacheLoaderConfig();

        if (config != null) {
            for (IndividualCacheLoaderConfig iconfig : config.getIndividualCacheLoaderConfigs()) {
                if (iconfig instanceof FileCacheLoaderConfig) {
                    FileCacheLoaderConfig ficonfig = (FileCacheLoaderConfig) iconfig;

                    if (ficonfig.getLocation() == null || ficonfig.getLocation().trim().length() == 0) {
                        ficonfig.setLocation(createTempDir());
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

        try {
            if (file.exists()) {
                is = new FileInputStream(file);
            } else {
                is =
                    this.container.getApplicationContext().getResourceAsStream(
                        "/WEB-INF/" + PROPS_PATH + propertiesFilename);
            }

            if (is == null) {
                throw new PropertiesLoadingCacheException("Can't find any configuration file for" + propertiesId);
            }

            XmlConfigurationParser parser = new XmlConfigurationParser();
            config = parser.parseStream(is);

            if (LOG.isInfoEnabled()) {
                LOG.info("Properties loaded: " + propertiesFilename);
            }
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Failed to load configuration file " + propertiesId, e);
            }
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Failed t close properties file", e);
                    }
                }
            }
        }

        return config;
    }
}
