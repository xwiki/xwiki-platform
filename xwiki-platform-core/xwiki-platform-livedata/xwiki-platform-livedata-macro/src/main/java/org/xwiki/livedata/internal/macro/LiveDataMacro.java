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
package org.xwiki.livedata.internal.macro;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataLayoutDescriptor;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPaginationConfiguration;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.macro.LiveDataMacroParameters;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Display dynamic lists of data.
 * 
 * @version $Id$
 * @since 12.9
 */
@Component
@Named("liveData")
@Singleton
public class LiveDataMacro extends AbstractMacro<LiveDataMacroParameters>
{
    private static final Pattern PATTERN_COMMA = Pattern.compile("\\s*,\\s*");

    private static final String UTF8 = "UTF-8";

    @Inject
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultLiveDataConfigurationResolver;

    /**
     * Default constructor.
     */
    public LiveDataMacro()
    {
        super("Live Data", "Display dynamic lists of data.", LiveDataMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    @Override
    public List<Block> execute(LiveDataMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // TODO: Load the JavaScript and CSS.
        GroupBlock output = new GroupBlock();
        output.setParameter("class", "liveData");
        if (parameters.getId() != null) {
            output.setParameter("id", parameters.getId());
        }
        try {
            // Compute the live data configuration based on the macro parameters.
            LiveDataConfiguration liveDataConfig = getLiveDataConfiguration(parameters, content);
            // Add the default values.
            liveDataConfig = this.defaultLiveDataConfigurationResolver.resolve(liveDataConfig);
            // Serialize as JSON.
            ObjectMapper objectMapper = new ObjectMapper();
            output.setParameter("data-config", objectMapper.writeValueAsString(liveDataConfig));
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to generate live data configuration from macro parameters.", e);
        }
        return Collections.singletonList(output);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }

    private LiveDataConfiguration getLiveDataConfiguration(LiveDataMacroParameters parameters, String content)
        throws MalformedURLException, UnsupportedEncodingException
    {
        LiveDataConfiguration liveDataConfig = new LiveDataConfiguration();
        liveDataConfig.setId(parameters.getId());
        liveDataConfig.setQuery(getQuery(parameters));
        liveDataConfig.setData(getData(content));
        liveDataConfig.setMeta(getMeta(parameters));
        return liveDataConfig;
    }

    private LiveDataQuery getQuery(LiveDataMacroParameters parameters)
        throws MalformedURLException, UnsupportedEncodingException
    {
        LiveDataQuery query = new LiveDataQuery();
        query.setProperties(getProperties(parameters.getProperties()));
        query.setSource(new Source(parameters.getSource()));
        query.getSource().getParameters().putAll(getSourceParameters(parameters.getSourceParameters()));
        query.setSort(getSortEntries(parameters.getSort()));
        query.setFilters(getFilters(parameters));
        query.setLimit(parameters.getLimit());
        query.setOffset(parameters.getOffset());
        return query;
    }

    private List<String> getProperties(String properties)
    {
        if (properties == null) {
            return null;
        } else {
            return Stream.of(PATTERN_COMMA.split(properties)).collect(Collectors.toList());
        }
    }

    private Map<String, Object> getSourceParameters(String sourceParametersString)
        throws MalformedURLException, UnsupportedEncodingException
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

    private List<SortEntry> getSortEntries(String sort)
    {
        if (sort == null) {
            return null;
        } else {
            return Stream.of(PATTERN_COMMA.split(sort)).filter(StringUtils::isNotEmpty).map(this::getSortEntry)
                .collect(Collectors.toList());
        }
    }

    private SortEntry getSortEntry(String sortEntryString)
    {
        SortEntry sortEntry = new SortEntry();
        sortEntry.setDescending(sortEntryString.endsWith(":desc"));
        if (sortEntry.isDescending() || sortEntryString.endsWith(":asc")) {
            sortEntry.setProperty(StringUtils.substringBeforeLast(sortEntryString, ":"));
        } else {
            sortEntry.setProperty(sortEntryString);
        }
        return sortEntry;
    }

    private List<Filter> getFilters(String filtersString) throws MalformedURLException, UnsupportedEncodingException
    {
        return getURLParameters('?' + StringUtils.defaultString(filtersString)).entrySet().stream().map(this::getFilter)
            .collect(Collectors.toList());
    }

    private List<Filter> getFilters(LiveDataMacroParameters parameters)
        throws MalformedURLException, UnsupportedEncodingException
    {
        List<Filter> filters = new ArrayList<>();
        // Add hidden filters that the user cannot change.
        filters.addAll(getFilters(parameters.getHiddenFilters()));
        filters.forEach(filter -> filter.setReadOnly(true));
        // Add visible filters.
        filters.addAll(getFilters(parameters.getFilters()));
        return filters.isEmpty() ? null : filters;
    }

    private Filter getFilter(Map.Entry<String, List<String>> entry)
    {
        Filter filter = new Filter();
        filter.setProperty(entry.getKey());
        filter.getConstraints().addAll(entry.getValue().stream().map(Constraint::new).collect(Collectors.toList()));
        return filter;
    }

    private LiveData getData(String content)
    {
        // TODO: Read live data entries from the macro content.
        return null;
    }

    private LiveDataMeta getMeta(LiveDataMacroParameters parameters)
    {
        LiveDataMeta meta = new LiveDataMeta();
        meta.setLayouts(getLayouts(parameters));
        meta.setPagination(getPagination(parameters));
        return meta;
    }

    private List<LiveDataLayoutDescriptor> getLayouts(LiveDataMacroParameters parameters)
    {
        if (parameters.getLayouts() == null) {
            return null;
        } else {
            return Stream.of(PATTERN_COMMA.split(parameters.getLayouts())).map(LiveDataLayoutDescriptor::new)
                .collect(Collectors.toList());
        }
    }

    private LiveDataPaginationConfiguration getPagination(LiveDataMacroParameters parameters)
    {
        LiveDataPaginationConfiguration pagination = new LiveDataPaginationConfiguration();
        pagination.setShowPageSizeDropdown(parameters.getShowPageSizeDropdown());
        if (parameters.getPageSizes() != null) {
            pagination.setPageSizes(Stream.of(PATTERN_COMMA.split(parameters.getPageSizes())).map(Integer::parseInt)
                .collect(Collectors.toList()));
        }
        return pagination;
    }

    private Map<String, List<String>> getURLParameters(String url)
        throws MalformedURLException, UnsupportedEncodingException
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
}
