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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Default value for the available languages that support optimized indexing.
     */
    public static final List<String> DEFAULT_OPTIMIZABLE_LANGUAGES = Arrays.asList("ar", "bg", "ca", "cz", "da", "de",
        "en", "el", "es", "eu", "fa", "fi", "fr", "ga", "gl", "hi", "hu", "hy", "id", "it", "ja", "lv", "nl", "no",
        "pt", "ro", "ru", "sv", "th", "tr");

    /**
     * Default value for the languages that are have optimized indexing activated.
     */
    public static final List<String> DEFAULT_OPTIMIZED_LANGUAGES = DEFAULT_OPTIMIZABLE_LANGUAGES;

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
     * Solr conf directory file names (including default language resources).
     */
    public static final String[] CONF_DIRECTORY_FILE_NAMES = {"solrconfig.xml", "schema.xml", "elevate.xml",
        "protwords.txt", "stopwords.txt", "synonyms.txt"};

    /**
     * Solr language analysis resource file names.
     */
    public static final String[] LANGUAGE_RESOURCE_FILE_NAME_PREFIXES = {"contractions", "hyphenations", "stemdict",
        "stoptags", "stopwords", "synonyms", "userdict"};

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
    public List<String> getOptimizableLanguages()
    {
        // Note: To avoid hardcoding the DEFAULT_OPTIMIZABLE_LANGUAGES value, we could try to read the default
        // schema.xml file an look for "<fieldType name="text_XX"..." tags.
        return this.configuration.getProperty("solr.multilingual.availableLanguages", DEFAULT_OPTIMIZABLE_LANGUAGES);
    }

    @Override
    public List<String> getOptimizedLanguages()
    {
        return this.configuration.getProperty("solr.multilingual.activeLanguages", DEFAULT_OPTIMIZED_LANGUAGES);
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

        // Language resources. All combinations.
        for (String language : this.getOptimizableLanguages()) {
            for (String languageFileName : LANGUAGE_RESOURCE_FILE_NAME_PREFIXES) {
                String fileName = String.format("%s/lang/%s_%s.txt", CONF_DIRECTORY, languageFileName, language);
                String classPathLocation = String.format(CLASSPATH_LOCATION_PREFIX, fileName);
                URL classPathURL = this.getClass().getResource(classPathLocation);
                try {
                    URLConnection testConnection = classPathURL.openConnection();
                    // Attempt a connection, since most of the combinations ("fileName_language.txt") will not be valid.
                    testConnection.connect();
                    result.put(fileName, classPathURL);
                } catch (Exception e) {
                    // Ignore.
                }
            }
        }

        return result;
    }
}
