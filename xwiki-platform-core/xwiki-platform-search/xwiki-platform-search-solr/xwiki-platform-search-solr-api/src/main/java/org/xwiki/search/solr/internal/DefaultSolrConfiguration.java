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
package org.xwiki.search.solr.internal;

import java.io.InputStream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.search.solr.internal.api.SolrConfiguration;

/**
 * Default implementation for {@link SolrConfiguration} that uses the xwiki.properties file.
 * 
 * @version $Id$
 * @since 4.5M1
 */
@Component
@Singleton
public class DefaultSolrConfiguration implements SolrConfiguration
{
    /**
     * Default component type.
     */
    public static final String DEFAULT_SOLR_TYPE = "embedded";

    /**
     * The classpath location prefix to use when looking for the default solr configuration files.
     */
    public static final String CLASSPATH_LOCATION_PREFIX = "/solr/%s";

    /**
     * Name of the classpath folder where the default configuration files are located.
     */
    public static final String CONF_DIRECTORY = "conf";

    /**
     * Classpath location pattern for the default configuration files.
     */
    public static final String CONF_FILE_LOCATION_PATTERN = "/%s/%s/%s";

    /**
     * Solr home directory file names.
     */
    public static final String[] HOME_DIRECTORY_FILE_NAMES = {"solr.xml"};

    /**
     * The package containing the Solr core configuration files.
     */
    public static final String HOME_DIRECTORY_CORE_PACKAGE = "solr.xwiki";

    /**
     * The prefix of the solr resources.
     */
    public static final String HOME_DIRECTORY_PREFIX = "solr/";

    /**
     * The name of the configuration property containing the batch size.
     */
    public static final String SOLR_INDEXER_BATCH_SIZE_PROPERTY = "solr.indexer.batch.size";

    /**
     * The default size of the batch.
     */
    public static final int SOLR_INDEXER_BATCH_SIZE_DEFAULT = 50;

    /**
     * The name of the configuration property containing the batch maximum length.
     */
    public static final String SOLR_INDEXER_BATCH_MAXLENGH_PROPERTY = "solr.indexer.batch.maxLength";

    /**
     * The default length of the data above which the batch is sent without waiting.
     */
    public static final int SOLR_INDEXER_BATCH_MAXLENGH_DEFAULT = 10000;

    /**
     * The name of the configuration property containing the batch size.
     */
    public static final String SOLR_INDEXER_QUEUE_CAPACITY_PROPERTY = "solr.indexer.queue.capacity";

    /**
     * The default size of the batch.
     */
    public static final int SOLR_INDEXER_QUEUE_CAPACITY_DEFAULT = 100000;

    /**
     * The name of the configuration property indicating if a synchronization should be run at startup.
     */
    public static final String SOLR_SYNCHRONIZE_AT_STARTUP = "solr.synchronizeAtStartup";

    /**
     * Indicate if a synchronization should be run at startup by default.
     */
    public static final boolean SOLR_SYNCHRONIZE_AT_STARTUP_DEFAULT = true;

    /**
     * The Solr configuration source.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public String getServerType()
    {
        return this.configuration.getProperty("solr.type", DEFAULT_SOLR_TYPE);
    }

    @Override
    public <T> T getInstanceConfiguration(String instanceType, String propertyName, T defaultValue)
    {
        String actualPropertyName = String.format("%s.%s.%s", "solr", instanceType, propertyName);
        return this.configuration.getProperty(actualPropertyName, defaultValue);
    }

    @Override
    public InputStream getHomeDirectoryConfiguration()
    {
        return getClass().getResourceAsStream("/xwiki-platform-search-solr-server-data.zip");
    }

    @Override
    public int getIndexerBatchSize()
    {
        return this.configuration.getProperty(SOLR_INDEXER_BATCH_SIZE_PROPERTY, SOLR_INDEXER_BATCH_SIZE_DEFAULT);
    }

    @Override
    public int getIndexerBatchMaxLengh()
    {
        return this.configuration
            .getProperty(SOLR_INDEXER_BATCH_MAXLENGH_PROPERTY, SOLR_INDEXER_BATCH_MAXLENGH_DEFAULT);
    }

    @Override
    public int getIndexerQueueCapacity()
    {
        return this.configuration
            .getProperty(SOLR_INDEXER_QUEUE_CAPACITY_PROPERTY, SOLR_INDEXER_QUEUE_CAPACITY_DEFAULT);
    }

    @Override
    public boolean synchronizeAtStartup()
    {
        return this.configuration.getProperty(SOLR_SYNCHRONIZE_AT_STARTUP, SOLR_SYNCHRONIZE_AT_STARTUP_DEFAULT);
    }
}
