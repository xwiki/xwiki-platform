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
package org.xwiki.whatsnew.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.whatsnew.NewsConfiguration;
import org.xwiki.whatsnew.NewsSourceDescriptor;

import static org.apache.commons.lang3.StringUtils.substringAfter;

/**
 * Implementation for configuration data for the What's New extension, looking in the {@code xwiki.properties} file.
 * <p>
 * Format example:
 * <code><pre>
 * whatsnew.sources = xwikiorg = xwikiblog
 * whatsnew.source.xwikiorg.rssURL = https://extensions.xwiki.org/news
 * whatsnew.sources = xwikisas = xwikiblog
 * whatsnew.sources.xwikisas.rssURL = https://xwiki.com/news
 * </pre></code>
 *
 * @version $Id$
 * @since 15.1RC1
 */
@Component
@Singleton
public class DefaultNewsConfiguration implements NewsConfiguration
{
    /**
     * Prefix for configuration keys for the What's New module.
     */
    private static final String PREFIX = "whatsnew";

    private static final String XWIKIBLOG_HINT = "xwikiblog";

    private static final String XWIKIBLOG_RSSURL_KEY = "rssURL";

    private static final String XWIKIORG_RSS_URL = "https://extensions.xwiki.org/news";

    private static final String XWIKISAS_RSS_URL = "https://xwiki.com/news";

    private static final String SOURCES_CONFIG_NAME = "sources";

    private static final String SOURCE_CONFIG_NAME = "source";

    private static final long DAY = 1 * 60L * 60L * 24;

    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configurationSource;

    @Override
    public List<NewsSourceDescriptor> getNewsSourceDescriptors()
    {
        // Format example:
        // whatsnew.sources = xwikiorg = xwikiblog
        // whatsnew.source.xwikiorg.rssURL = https://extensions.xwiki.org/news
        // whatsnew.sources = xwikisas = xwikiblog
        // whatsnew.sources.xwikisas.rssURL = https://xwiki.com/news
        List<NewsSourceDescriptor> descriptors;
        Properties sources = getConfiguredSources();
        // If there's no configuration set by the user the use a default configuration.
        if (sources != null) {
            descriptors = getConfiguredNewsSourceDescriptors(sources);
        } else {
            // Define default news source configuration
            descriptors = getDefaultNewsSourceDescriptors();
        }
        return descriptors;
    }

    @Override
    public long getNewsRefreshRate()
    {
        return this.configurationSource.getProperty(getFullKeyName("refreshRate"), DAY);
    }

    @Override
    public int getNewsDisplayCount()
    {
        return this.configurationSource.getProperty(getFullKeyName("displayCount"), 10);
    }

    @Override
    public boolean isActive()
    {
        Properties sources = getConfiguredSources();
        return sources == null || !sources.isEmpty();
    }

    private Properties getConfiguredSources()
    {
        Properties result;
        if (this.configurationSource.containsKey(getFullKeyName(SOURCES_CONFIG_NAME))) {
            result = this.configurationSource.getProperty(getFullKeyName(SOURCES_CONFIG_NAME), Properties.class);
        } else {
            result = null;
        }
        return result;
    }

    private List<NewsSourceDescriptor> getConfiguredNewsSourceDescriptors(Properties sources)
    {
        List<NewsSourceDescriptor> descriptors = new ArrayList<>();
        // Only keep the keys related to configuring news sources, for performance.
        List<String> keys = new ArrayList<>();
        String sourceKeyNamePrefix = String.format("%s.", getFullKeyName(SOURCE_CONFIG_NAME));
        for (String key : this.configurationSource.getKeys()) {
            if (key.startsWith(sourceKeyNamePrefix)) {
                keys.add(key);
            }
        }
        for (Map.Entry<Object, Object> entry : sources.entrySet()) {
            // Find all parameter properties for the defined source
            Map<String, String> parameters = new HashMap<>();
            String prefix = String.format("%s%s.", sourceKeyNamePrefix, entry.getKey());
            for (String key : keys) {
                if (key.startsWith(prefix)) {
                    String parameterKey = substringAfter(key, prefix);
                    parameters.put(parameterKey, this.configurationSource.getProperty(key, String.class));
                }
            }
            NewsSourceDescriptor descriptor =
                new NewsSourceDescriptor((String) entry.getKey(), (String) entry.getValue(), parameters);
            descriptors.add(descriptor);
        }
        return descriptors;
    }

    private List<NewsSourceDescriptor> getDefaultNewsSourceDescriptors()
    {
        // The xwiki.org XWiki Blog
        NewsSourceDescriptor xwikiOrgBlogDescriptor = new NewsSourceDescriptor("xwikiorg", XWIKIBLOG_HINT,
            Collections.singletonMap(XWIKIBLOG_RSSURL_KEY, XWIKIORG_RSS_URL));
        // The top sponsoring company XWiki Blog
        NewsSourceDescriptor xwikiSASBlogDescriptor = new NewsSourceDescriptor("xwikisas", XWIKIBLOG_HINT,
            Collections.singletonMap(XWIKIBLOG_RSSURL_KEY, XWIKISAS_RSS_URL));

        return List.of(xwikiOrgBlogDescriptor, xwikiSASBlogDescriptor);
    }

    private String getFullKeyName(String shortKeyName)
    {
        return String.format("%s.%s", PREFIX, shortKeyName);
    }
}
