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

import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.LocaleUtils;
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
     * Default value for the available locales that support optimized indexing.
     * <p>
     * Old codes are used (<code>in</code> instead of <code>id</code> for example) because of
     * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6457127.
     */
    public static final List<String> DEFAULT_OPTIMIZABLE_LOCALES = Arrays.asList("ar", "bg", "ca", "cz", "da", "de",
        "en", "el", "es", "eu", "fa", "fi", "fr", "ga", "gl", "hi", "hu", "hy", "in", "it", "ja", "lv", "nl", "no",
        "pt", "ro", "ru", "sv", "th", "tr");

    /**
     * Default value for the locales that are have optimized indexing activated.
     */
    public static final List<String> DEFAULT_OPTIMIZED_LOCALES = DEFAULT_OPTIMIZABLE_LOCALES;

    /**
     * Default list of multilingual fields.
     */
    public static final List<String> DEFAULT_MULTILINGUAL_FIELDS = Arrays.asList("title", "doccontent", "comment",
        "objcontent", "propertyvalue", "attcontent");

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
     * Solr conf directory file names (including default locale resources).
     */
    public static final String[] CONF_DIRECTORY_FILE_NAMES = {"solrconfig.xml", "schema.xml", "elevate.xml",
        "protwords.txt", "stopwords.txt", "synonyms.txt"};

    /**
     * Solr locale analysis resource file names.
     */
    public static final String[] LOCALES_RESOURCE_FILE_NAME_PREFIXES = {"contractions", "hyphenations", "stemdict",
        "stoptags", "stopwords", "synonyms", "userdict"};

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
    public static final int SOLR_INDEXER_QUEUE_CAPACITY_DEFAULT = 10000;

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

    /**
     * @param localeStrings the locales as {@link String}s
     * @return the locales as {@link Locale}s
     */
    private List<Locale> toLocales(List<String> localeStrings)
    {
        List<Locale> locales = new ArrayList<Locale>(localeStrings.size());

        for (String localeString : localeStrings) {
            locales.add(LocaleUtils.toLocale(localeString));
        }

        return locales;
    }

    @Override
    public List<Locale> getOptimizableLocales()
    {
        // Note: To avoid hardcoding the DEFAULT_OPTIMIZABLE_LOCALES value, we could try to read the default
        // schema.xml file an look for "<fieldType name="text_XX"..." tags.
        return toLocales(this.configuration.getProperty("solr.multilingual.availableLocales",
            DEFAULT_OPTIMIZABLE_LOCALES));
    }

    @Override
    public List<Locale> getOptimizedLocales()
    {
        return toLocales(this.configuration.getProperty("solr.multilingual.activeLocales", DEFAULT_OPTIMIZED_LOCALES));
    }

    @Override
    public List<String> getMultilingualFields()
    {
        return this.configuration.getProperty("solr.multilingual.fields", DEFAULT_MULTILINGUAL_FIELDS);
    }

    @Override
    public Map<String, URL> getHomeDirectoryConfiguration()
    {
        // Build the result
        Map<String, URL> result = new HashMap<String, URL>();

        // Home directory.
        for (String file : HOME_DIRECTORY_FILE_NAMES) {
            result.put(file, this.getClass().getResource(String.format(CLASSPATH_LOCATION_PREFIX, file)));
        }

        // Conf directory
        for (String file : CONF_DIRECTORY_FILE_NAMES) {
            String fileName = String.format("%s/%s", CONF_DIRECTORY, file);
            String classPathLocation = String.format(CLASSPATH_LOCATION_PREFIX, fileName);
            URL classPathURL = this.getClass().getResource(classPathLocation);
            result.put(fileName, classPathURL);
        }

        // Locale resources. All combinations.
        for (Locale locale : this.getOptimizableLocales()) {
            for (String localeFileName : LOCALES_RESOURCE_FILE_NAME_PREFIXES) {
                String fileName = String.format("%s/lang/%s_%s.txt", CONF_DIRECTORY, localeFileName, locale);
                String classPathLocation = String.format(CLASSPATH_LOCATION_PREFIX, fileName);
                URL classPathURL = this.getClass().getResource(classPathLocation);
                try {
                    URLConnection testConnection = classPathURL.openConnection();
                    // Attempt a connection, since most of the combinations ("fileName_locale.txt") will not be valid.
                    testConnection.connect();
                    result.put(fileName, classPathURL);
                } catch (Exception e) {
                    // Ignore.
                }
            }
        }

        return result;
    }

    @Override
    public int getIndexerBatchSize()
    {
        return this.configuration.getProperty(SOLR_INDEXER_BATCH_SIZE_PROPERTY, SOLR_INDEXER_BATCH_SIZE_DEFAULT);
    }

    @Override
    public int getIndexerBatchMaxLengh()
    {
        return this.configuration.getProperty(SOLR_INDEXER_BATCH_SIZE_PROPERTY, SOLR_INDEXER_BATCH_SIZE_DEFAULT);
    }

    @Override
    public int getIndexerQueueCapacity()
    {
        return this.configuration
            .getProperty(SOLR_INDEXER_QUEUE_CAPACITY_PROPERTY, SOLR_INDEXER_QUEUE_CAPACITY_DEFAULT);
    }
}
