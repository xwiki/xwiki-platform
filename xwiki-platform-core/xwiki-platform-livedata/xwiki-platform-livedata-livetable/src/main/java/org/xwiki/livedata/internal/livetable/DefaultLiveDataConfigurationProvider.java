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
package org.xwiki.livedata.internal.livetable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;

/**
 * Provides the default live data configuration for the live table source.
 * 
 * @version $Id$
 * @since 12.10.4
 * @since 13.0
 */
@Component
@Named("liveTable")
@Singleton
public class DefaultLiveDataConfigurationProvider implements Provider<LiveDataConfiguration>
{
    private static final List<String> USER_DOC_PROPS = Arrays.asList("doc.creator", "doc.author");

    /**
     * Used to parse the default configuration JSON.
     */
    @Inject
    private LiveDataConfigurationResolver<String> stringLiveDataConfigResolver;

    @Inject
    @Named("wiki")
    private ConfigurationSource wikiConfig;

    /**
     * Cache the static default configuration JSON.
     */
    private String defaultConfigJSON;

    @Override
    public LiveDataConfiguration get()
    {
        if (this.defaultConfigJSON == null) {
            try {
                InputStream defaultConfigInputStream =
                    getClass().getResourceAsStream("/liveTableLiveDataConfiguration.json");
                this.defaultConfigJSON = IOUtils.toString(defaultConfigInputStream, "UTF-8");
            } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to read the default live data configuration for the live table source.", e);
            }
        }

        try {
            LiveDataConfiguration defaultConfig = this.stringLiveDataConfigResolver.resolve(this.defaultConfigJSON);
            maybeSetDateFormat(defaultConfig.getMeta());
            setSearchURLForUserFilter(defaultConfig.getMeta());
            return defaultConfig;
        } catch (LiveDataException e) {
            throw new RuntimeException("Failed to parse the default live data configuration for the live table source.",
                e);
        }
    }

    private void maybeSetDateFormat(LiveDataMeta meta)
    {
        String dateFormat = this.wikiConfig.getProperty("dateformat");
        if (!StringUtils.isEmpty(dateFormat)) {
            Optional<FilterDescriptor> dateFilter =
                meta.getFilters().stream().filter(filter -> "date".equals(filter.getId())).findFirst();
            // We expect the date filter to be present in liveTableLiveDataConfiguration.json
            dateFilter.get().setParameter("dateFormat", dateFormat);
        }
    }

    private void setSearchURLForUserFilter(LiveDataMeta meta)
    {
        meta.getPropertyDescriptors().stream().filter(property -> USER_DOC_PROPS.contains(property.getId()))
            .forEach(this::setSearchURLForUserFilter);
    }

    private void setSearchURLForUserFilter(LiveDataPropertyDescriptor property)
    {
        if (property.getFilter() == null) {
            property.setFilter(new FilterDescriptor("list"));
        }
        // TODO: Take into account the user scope. See suggestUsersAndGroups.js for an example.
        property.getFilter().setParameter("searchURL", "?xpage=uorgsuggest&uorg=user&input={encodedQuery}&media=json");
    }
}
