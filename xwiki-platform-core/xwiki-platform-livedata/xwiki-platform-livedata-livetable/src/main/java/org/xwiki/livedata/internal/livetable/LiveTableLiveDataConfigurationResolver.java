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

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPaginationConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.livetable.LiveTableConfiguration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Converts the Live Table configuration into Live Data configuration.
 * 
 * @version $Id$
 * @since 12.10RC1
 */
@Component
@Named("liveTable")
@Singleton
public class LiveTableLiveDataConfigurationResolver implements LiveDataConfigurationResolver<LiveTableConfiguration>
{
    private static final String UTF8 = "UTF-8";

    private static final String HTML = "html";

    private static final String LINK = "link";

    private static final String ACTIONS = "actions";

    private static final String TYPE = "type";

    private static final String HIDDEN = "hidden";

    @SuppressWarnings("serial")
    private static final Map<String, String> DEFAULT_OPERATOR = new HashMap<String, String>()
    {
        {
            put("exact", "equals");
            put("partial", "contains");
            put("prefix", "startsWith");
        }
    };

    @Inject
    private Logger logger;

    @Override
    public LiveDataConfiguration resolve(LiveTableConfiguration liveTableConfig) throws LiveDataException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode columnPropertiesJSON = objectMapper.valueToTree(liveTableConfig.getColumnProperties());
        ObjectNode optionsJSON = objectMapper.valueToTree(liveTableConfig.getOptions());
        return getConfig(liveTableConfig.getId(), liveTableConfig.getColumns(), columnPropertiesJSON, optionsJSON);
    }

    private LiveDataConfiguration getConfig(String id, List<String> columns, ObjectNode columnProperties,
        ObjectNode options)
    {
        LiveDataConfiguration config = new LiveDataConfiguration();
        config.setId(id);
        config.setQuery(getQueryConfig(columns, options));
        config.setMeta(getMetaConfig(columnProperties, options));
        return config;
    }

    private LiveDataQuery getQueryConfig(List<String> columns, ObjectNode options)
    {
        LiveDataQuery queryConfig = new LiveDataQuery();
        queryConfig.setProperties(columns);
        queryConfig.setSource(getSourceConfig(options));
        processExtraParams(options, queryConfig);
        queryConfig.setSort(getSortConfig(columns, options));

        JsonNode rowCount = options.path("rowCount");
        if (rowCount.isNumber()) {
            queryConfig.setLimit(rowCount.asInt());
        }

        return queryConfig;
    }

    private Source getSourceConfig(ObjectNode options)
    {
        Source source = new Source();
        source.setId("liveTable");
        for (String parameter : Arrays.asList("className", "resultPage", "queryFilters", "translationPrefix")) {
            if (options.path(parameter).isTextual()) {
                source.setParameter(parameter, options.path(parameter).asText());
            }
        }

        JsonNode urlNode = options.path("url");
        if (urlNode.isTextual()) {
            String url = urlNode.asText();
            try {
                addSourceParametersFromURL(source, url);
            } catch (Exception e) {
                this.logger.warn("Failed to extract live data source parameters from live table results URL [{}]. "
                    + "Root cause is [{}].", url, ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return source;
    }

    private void addSourceParametersFromURL(Source source, String url) throws Exception
    {
        Map<String, List<String>> parameters = getURLParameters(url);
        List<String> xpage = parameters.remove("xpage");
        if (xpage != null && !xpage.isEmpty() && !StringUtils.isEmpty(xpage.get(0)) && !"plain".equals(xpage.get(0))) {
            source.setParameter("template", xpage.get(0) + ".vm");
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                if (entry.getValue().size() > 1) {
                    source.setParameter(entry.getKey(), entry.getValue());
                } else {
                    source.setParameter(entry.getKey(), entry.getValue().get(0));
                }
            }
        }
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

    private void processExtraParams(ObjectNode options, LiveDataQuery queryConfig)
    {
        JsonNode extraParamsNode = options.path("extraParams");
        if (extraParamsNode.isTextual()) {
            String extraParams = extraParamsNode.asText();
            try {
                List<Filter> filters = new ArrayList<>();
                Map<String, List<String>> parameters = getURLParameters('?' + extraParams);
                for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                    if (queryConfig.getProperties().contains(entry.getKey())) {
                        // Convert to a live data property filter.
                        Filter filter = new Filter();
                        filter.setProperty(entry.getKey());
                        filter.getConstraints()
                            .addAll(entry.getValue().stream().map(Constraint::new).collect(Collectors.toList()));
                        filters.add(filter);
                    } else if (entry.getValue().size() == 1) {
                        // Convert to a live data source parameter.
                        queryConfig.getSource().setParameter(entry.getKey(), entry.getValue().get(0));
                    } else {
                        queryConfig.getSource().setParameter(entry.getKey(), entry.getValue());
                    }
                }
                if (!filters.isEmpty()) {
                    queryConfig.setFilters(filters);
                }
            } catch (Exception e) {
                this.logger.warn(
                    "Failed to extract live data source parameters and property filters"
                        + " from live table extra parameters [{}]." + " Root cause is [{}].",
                    extraParams, ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    private List<SortEntry> getSortConfig(List<String> columns, ObjectNode options)
    {
        SortEntry sortEntry = new SortEntry();
        JsonNode selectedColumn = options.path("selectedColumn");
        if (selectedColumn.isTextual()) {
            sortEntry.setProperty(selectedColumn.asText());
        } else {
            // Use the first non-special column.
            Optional<String> firstNonSpecialColumn =
                columns.stream().filter(column -> !column.startsWith("_")).findFirst();
            if (firstNonSpecialColumn.isPresent()) {
                sortEntry.setProperty(firstNonSpecialColumn.get());
            } else {
                return null;
            }
        }
        sortEntry.setDescending("desc".equals(options.path("defaultOrder").asText()));
        return Collections.singletonList(sortEntry);
    }

    private LiveDataMeta getMetaConfig(ObjectNode columnProperties, ObjectNode options)
    {
        LiveDataMeta metaConfig = new LiveDataMeta();
        metaConfig.setPropertyDescriptors(getPropertyDescriptorsConfig(columnProperties, options));
        metaConfig.setPagination(getPaginationConfig(options));
        return metaConfig;
    }

    private List<LiveDataPropertyDescriptor> getPropertyDescriptorsConfig(ObjectNode columnProperties,
        ObjectNode options)
    {
        List<LiveDataPropertyDescriptor> propertyDescriptors = new ArrayList<>();
        columnProperties.fields().forEachRemaining(field -> propertyDescriptors
            .add(getPropertyDescriptor(field.getKey(), (ObjectNode) field.getValue(), options)));
        return propertyDescriptors;
    }

    private LiveDataPropertyDescriptor getPropertyDescriptor(String column, ObjectNode columnProperties,
        ObjectNode options)
    {
        LiveDataPropertyDescriptor propertyDescriptor = new LiveDataPropertyDescriptor();
        propertyDescriptor.setId(column);

        JsonNode displayName = columnProperties.path("displayName");
        if (displayName.isTextual()) {
            propertyDescriptor.setName(displayName.asText());
        }

        // The live table macro considers all columns, except for "actions", as sortable by default.
        propertyDescriptor.setSortable(columnProperties.path("sortable").asBoolean(!columnProperties.has(ACTIONS)));

        if (HIDDEN.equals(columnProperties.path(TYPE).asText())) {
            propertyDescriptor.setVisible(false);
        }

        propertyDescriptor.setDisplayer(getDisplayerConfig(column, columnProperties));

        // The live table macro considers all columns, except for "actions", as filterable by default.
        propertyDescriptor.setFilterable(columnProperties.path("filterable").asBoolean(!columnProperties.has(ACTIONS)));

        propertyDescriptor.setFilter(getFilterConfig(columnProperties));

        propertyDescriptor.setStyleName(columnProperties.path("headerClass").asText(null));

        JsonNode className = columnProperties.path("class");
        if (className.isTextual()) {
            // TODO: Extract more information from the specified XWiki class (the property type).
        }

        return propertyDescriptor;
    }

    private DisplayerDescriptor getDisplayerConfig(String column, ObjectNode columnProperties)
    {
        DisplayerDescriptor displayerConfig = new DisplayerDescriptor();
        if (columnProperties.path(ACTIONS).isArray()) {
            displayerConfig.setId(ACTIONS);
            displayerConfig.setParameter(ACTIONS, columnProperties.get(ACTIONS));
        } else if (columnProperties.path(LINK).isTextual()) {
            displayerConfig.setId(LINK);
            Map<String, String[]> propertyHref = new HashMap<>();
            String docURL = "doc.url";
            String columnURL = column + "_url";
            propertyHref.put("auto", new String[] {columnURL, docURL});
            propertyHref.put("field", new String[] {columnURL});
            propertyHref.put("author", new String[] {"doc.author_url"});
            propertyHref.put("space", new String[] {"doc.space_url"});
            propertyHref.put("wiki", new String[] {"doc.wiki_url"});
            String linkType = columnProperties.get(LINK).asText();
            String[] values = propertyHref.getOrDefault(linkType, new String[] {docURL});
            displayerConfig.setParameter("propertyHref", values[0]);
        } else if (columnProperties.path(HTML).booleanValue()) {
            displayerConfig.setId(HTML);
        } else {
            displayerConfig = null;
        }
        return displayerConfig;
    }

    private FilterDescriptor getFilterConfig(ObjectNode columnProperties)
    {
        FilterDescriptor filterConfig = new FilterDescriptor();
        boolean hasFilter = false;

        JsonNode filterId = columnProperties.path(TYPE);
        if (filterId.isTextual() && !HIDDEN.equals(filterId.asText())) {
            filterConfig.setId(filterId.asText());
            hasFilter = true;
        }

        JsonNode match = columnProperties.path("match");
        if (match.isTextual()) {
            String defaultOperator = DEFAULT_OPERATOR.get(match.asText());
            if (defaultOperator != null) {
                filterConfig.setDefaultOperator(defaultOperator);
                hasFilter = true;
            }
        }

        return hasFilter ? filterConfig : null;
    }

    private LiveDataPaginationConfiguration getPaginationConfig(ObjectNode options)
    {
        LiveDataPaginationConfiguration pagination = new LiveDataPaginationConfiguration();
        pagination.setMaxShownPages(options.path("maxPages").asInt(10));
        boolean showPageSizeDropdown = options.path("pageSize").asBoolean(true);
        pagination.setShowPageSizeDropdown(showPageSizeDropdown);

        JsonNode pageSizeBounds = options.path("pageSizeBounds");
        int min = 10;
        int max = 100;
        int step = 10;
        if (pageSizeBounds.isArray() && pageSizeBounds.size() == 3) {
            min = pageSizeBounds.get(0).asInt();
            max = pageSizeBounds.get(1).asInt();
            step = pageSizeBounds.get(2).asInt();
        }
        if (showPageSizeDropdown && min <= max && step > 0) {
            List<Integer> pageSizes = new ArrayList<>();
            for (int i = min; i <= max; i += step) {
                pageSizes.add(i);
            }
            pagination.setPageSizes(pageSizes);
        }

        return pagination;
    }
}
