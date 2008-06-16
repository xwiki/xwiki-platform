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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.config.Configuration;
import org.jboss.cache.config.EvictionConfig;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.eviction.LRUConfiguration;
import org.jboss.cache.eviction.LRUPolicy;
import org.jboss.cache.factories.XmlConfigurationParser;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.container.Container;

/**
 * Convert XWiki cache configuration into JBossCache configuration.
 * 
 * @version $Id: $
 */
public class JBossCacheCacheConfiguration
{
    /**
     * the logging tool.
     */
    private static final Log LOG = LogFactory.getLog(JBossCacheCacheConfiguration.class);

    /**
     * The folder containing JBossCache properties files.
     */
    private static final String PROPS_PATH = "cache/jboss/";

    /**
     * The extension of JBossCache properties files.
     */
    private static final String PROPS_EXT = ".xml";

    /**
     * The default configuration identifier used to load cache configuration file.
     */
    protected String defaultPropsId = "default";

    /**
     * The XWiki cache API configuration.
     */
    private CacheConfiguration configuration;

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
        this.container = container;
        this.configuration = configuration;
        this.defaultPropsId = defaultPropsId;

        load();
    }

    /**
     * @return the XWiki cache API configuration.
     */
    public CacheConfiguration getCacheConfiguration()
    {
        return this.configuration;
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
        this.jbossConfiguration = loadConfig(configuration.getConfigurationId());

        if (this.jbossConfiguration == null) {
            this.jbossConfiguration = getDefaultConfig();

            if (this.jbossConfiguration == null) {
                this.jbossConfiguration = new Configuration();
            }

            EntryEvictionConfiguration eec =
                (EntryEvictionConfiguration) this.configuration.get(EntryEvictionConfiguration.CONFIGURATIONID);

            if (eec != null && eec.getAlgorithm() == EntryEvictionConfiguration.Algorithm.LRU) {
                EvictionConfig ec = this.jbossConfiguration.getEvictionConfig();

                if (ec == null) {
                    ec = new EvictionConfig();
                    this.jbossConfiguration.setEvictionConfig(ec);
                }

                int maxEntries = ((Number) eec.get(LRUEvictionConfiguration.MAXENTRIES_ID)).intValue();

                ec.setDefaultEvictionPolicyClass(LRUPolicy.class.getName());

                LRUConfiguration lru = new LRUConfiguration();

                lru.setMaxNodes(maxEntries);

                if (eec.getTimeToLive() > 0) {
                    lru.setTimeToLiveSeconds(eec.getTimeToLive());
                }

                EvictionRegionConfig erc = new EvictionRegionConfig();

                erc.setRegionFqn(JBossCacheCache.ROOT_FQN);

                erc.setEvictionPolicyConfig(lru);

                ec.setEvictionRegionConfigs(Collections.singletonList(erc));
            }
        }

        this.jbossConfiguration.setClusterName(this.configuration.getConfigurationId());
    }

    /**
     * @return the default JBossCache configuration.
     */
    protected Configuration getDefaultConfig()
    {
        return loadConfig(defaultPropsId);
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
