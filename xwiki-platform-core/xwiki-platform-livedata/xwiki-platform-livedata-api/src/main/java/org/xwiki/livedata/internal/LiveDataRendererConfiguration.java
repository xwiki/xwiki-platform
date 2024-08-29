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
package org.xwiki.livedata.internal;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.BaseDescriptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataLayoutDescriptor;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPaginationConfiguration;
import org.xwiki.livedata.LiveDataQuery;

/**
 * Provides services to manipulate configurations for the {@link LiveDataRendererConfiguration}.
 *
 * @version $Id$
 * @since 14.9
 * @since 14.4.7
 * @since 13.10.10
 */
@Component(roles = LiveDataRendererConfiguration.class)
@Singleton
public class LiveDataRendererConfiguration
{
    private static final String UTF8 = "UTF-8";

    /**
     * Used to read the Live Data configuration from the macro content.
     */
    @Inject
    private LiveDataConfigurationResolver<String> stringLiveDataConfigResolver;

    /**
     * Used to merge the Live Data configuration built from the macro parameters with the live data configuration read
     * from the macro content.
     */
    private JSONMerge jsonMerge = new JSONMerge();

    /**
     * Resolve a complete Live Data configuration from a json advanced configuration (the content) and a set of macro
     * parameters.
     *
     * @param content the string representation of the json live data advanced configuration
     * @param parameters the Live Data macro parameters
     * @return the complete Live Data configuration
     * @throws Exception in case of error when resolving the configuration
     */
    public LiveDataConfiguration getLiveDataConfiguration(String content, LiveDataRendererParameters parameters)
        throws Exception
    {
        String json = StringUtils.defaultIfBlank(content, "{}");
        LiveDataConfiguration advancedConfig = this.stringLiveDataConfigResolver.resolve(json);
        LiveDataConfiguration basicConfig = getLiveDataConfiguration(parameters);
        // Make sure both configurations have the same id so that they are properly merged.
        advancedConfig.setId(basicConfig.getId());
        return this.jsonMerge.merge(advancedConfig, basicConfig);
    }

    private LiveDataConfiguration getLiveDataConfiguration(LiveDataRendererParameters parameters) throws Exception
    {
        LiveDataConfiguration liveDataConfig = new LiveDataConfiguration();
        liveDataConfig.setId(parameters.getId());
        liveDataConfig.setQuery(getQuery(parameters));
        liveDataConfig.setMeta(getMeta(parameters));
        return liveDataConfig;
    }

    private LiveDataQuery getQuery(LiveDataRendererParameters parameters) throws Exception
    {
        LiveDataQuery query = new LiveDataQuery();
        query.setProperties(getProperties(parameters.getProperties()));
        query.setSource(new LiveDataQuery.Source(parameters.getSource()));
        query.getSource().getParameters().putAll(getSourceParameters(parameters.getSourceParameters()));
        query.setSort(getSortEntries(parameters.getSort()));
        query.setFilters(getFilters(parameters.getFilters()));
        query.setLimit(parameters.getLimit());
        query.setOffset(parameters.getOffset());
        return query;
    }

    private List<String> getProperties(String properties)
    {
        if (properties == null) {
            return null;
        } else {
            return getSplitStringStream(properties).collect(Collectors.toList());
        }
    }

    private Map<String, Object> getSourceParameters(String sourceParametersString) throws Exception
    {
        if (StringUtils.isEmpty(sourceParametersString)) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> urlParams = getURLParameters('?' + sourceParametersString);
        Map<String, Object> sourceParams = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : urlParams.entrySet()) {
            if (entry.getValue().size() > 1) {
                sourceParams.put(entry.getKey(), entry.getValue());
            } else {
                sourceParams.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return sourceParams;
    }

    private List<LiveDataQuery.SortEntry> getSortEntries(String sort)
    {
        if (sort == null) {
            return null;
        } else {
            return getSplitStringStream(sort)
                .filter(StringUtils::isNotEmpty)
                .map(this::getSortEntry)
                .collect(Collectors.toList());
        }
    }

    private LiveDataQuery.SortEntry getSortEntry(String sortEntryString)
    {
        LiveDataQuery.SortEntry sortEntry = new LiveDataQuery.SortEntry();
        sortEntry.setDescending(sortEntryString.endsWith(":desc"));
        if (sortEntry.isDescending() || sortEntryString.endsWith(":asc")) {
            sortEntry.setProperty(StringUtils.substringBeforeLast(sortEntryString, ":"));
        } else {
            sortEntry.setProperty(sortEntryString);
        }
        return sortEntry;
    }

    private List<LiveDataQuery.Filter> getFilters(String filtersString) throws Exception
    {
        List<LiveDataQuery.Filter> filters =
            getURLParameters('?' + StringUtils.defaultString(filtersString)).entrySet().stream()
                .map(this::getFilter).collect(Collectors.toList());
        return filters.isEmpty() ? null : filters;
    }

    private LiveDataQuery.Filter getFilter(Map.Entry<String, List<String>> entry)
    {
        LiveDataQuery.Filter filter = new LiveDataQuery.Filter();
        filter.setProperty(entry.getKey());
        filter.getConstraints()
            .addAll(entry.getValue().stream().map(LiveDataQuery.Constraint::new).collect(Collectors.toList()));
        return filter;
    }

    private LiveDataMeta getMeta(LiveDataRendererParameters parameters)
    {
        LiveDataMeta meta = new LiveDataMeta();
        List<LiveDataLayoutDescriptor> layouts = getLayouts(parameters);
        meta.setLayouts(layouts);
        // If it exists, use the id of the first layout as the default layout.
        Optional.ofNullable(layouts)
            .flatMap(ls -> ls.stream().findFirst().map(BaseDescriptor::getId))
            .ifPresent(meta::setDefaultLayout);
        meta.setPagination(getPagination(parameters));
        String description = parameters.getDescription();
        if (StringUtils.isNoneEmpty(description)) {
            meta.setDescription(description);
        }
        return meta;
    }

    private List<LiveDataLayoutDescriptor> getLayouts(LiveDataRendererParameters parameters)
    {
        if (parameters.getLayouts() == null) {
            return null;
        } else {
            return getSplitStringStream(parameters.getLayouts())
                .map(LiveDataLayoutDescriptor::new)
                .collect(Collectors.toList());
        }
    }

    private LiveDataPaginationConfiguration getPagination(LiveDataRendererParameters parameters)
    {
        LiveDataPaginationConfiguration pagination = new LiveDataPaginationConfiguration();
        pagination.setShowPageSizeDropdown(parameters.getShowPageSizeDropdown());
        if (parameters.getPageSizes() != null) {
            pagination.setPageSizes(getSplitStringStream(parameters.getPageSizes())
                .map(Integer::parseInt)
                .collect(Collectors.toList()));
        }
        return pagination;
    }

    private Map<String, List<String>> getURLParameters(String url) throws Exception
    {
        URL baseURL = new URL("http://www.xwiki.org");
        String queryString = new URL(baseURL, url).getQuery();
        Map<String, List<String>> parameters = new HashMap<>();
        for (String entry : queryString.split("&")) {
            String[] parts = entry.split("=", 2);
            String key = URLDecoder.decode(parts[0], UTF8);
            if (key.isEmpty()) {
                continue;
            }
            String value = parts.length == 2 ? URLDecoder.decode(parts[1], UTF8) : "";
            List<String> values = parameters.get(key);
            if (values == null) {
                values = new ArrayList<>();
                parameters.put(key, values);
            }
            values.add(value);
        }
        return parameters;
    }

    private Stream<String> getSplitStringStream(String commaListAsString)
    {
        return Stream.of(commaListAsString.split(",")).map(StringUtils::trim);
    }
}
