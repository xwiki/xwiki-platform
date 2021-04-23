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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
 * @since 12.10
 */
@Component
@Named("liveTable")
@Singleton
public class LiveTableLiveDataConfigurationResolver implements LiveDataConfigurationResolver<LiveTableConfiguration>
{
    private static final String HTML = "html";

    private static final String LINK = "link";

    private static final String ACTIONS = "actions";

    private static final String TYPE = "type";

    private static final String HIDDEN = "hidden";

    private static final String CLASS_NAME = "className";

    private static final String QUERY_FILTERS = "queryFilters";

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

    /**
     * Used to add missing live data configuration values specific to the live table source.
     */
    @Inject
    @Named("liveTable")
    private LiveDataConfigurationResolver<LiveDataConfiguration> defaultConfigResolver;

    @Inject
    private PropertyTypeSupplier propertyTypeSupplier;

    @Inject
    private LiveTableResultsURLDocumentReferenceResolver urlDocumentReferenceResolver;

    private final URLQueryStringParser urlQueryStringParser = new URLQueryStringParser();

    @Override
    public LiveDataConfiguration resolve(LiveTableConfiguration liveTableConfig) throws LiveDataException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode columnPropertiesJSON = objectMapper.valueToTree(liveTableConfig.getColumnProperties());
        ObjectNode optionsJSON = objectMapper.valueToTree(liveTableConfig.getOptions());
        LiveDataConfiguration config =
            getConfig(liveTableConfig.getId(), liveTableConfig.getColumns(), columnPropertiesJSON, optionsJSON);
        return this.defaultConfigResolver.resolve(config);
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
        queryConfig.setSort(getSortConfig(options));

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
        for (String parameter : Arrays.asList(CLASS_NAME, "resultPage", "translationPrefix")) {
            if (options.path(parameter).isTextual()) {
                source.setParameter(parameter, options.path(parameter).asText());
            }
        }

        // We handle the query filters separately because they can be specified either as a string (comma-separated) or
        // as an array.
        JsonNode queryFilters = options.path(QUERY_FILTERS);
        if (queryFilters.isTextual()) {
            source.setParameter(QUERY_FILTERS, queryFilters.asText());
        } else if (queryFilters.isArray()) {
            List<String> values = new ArrayList<>();
            queryFilters.forEach(queryFilter -> values.add(queryFilter.asText()));
            source.setParameter(QUERY_FILTERS, StringUtils.join(values, ','));
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
        Map<String, List<String>> parameters = this.urlQueryStringParser.parse(url);
        List<String> xpage = parameters.remove("xpage");
        if (xpage != null && !xpage.isEmpty() && !StringUtils.isEmpty(xpage.get(0)) && !"plain".equals(xpage.get(0))) {
            source.setParameter("template", xpage.get(0) + ".vm");
            String documentReference = this.urlDocumentReferenceResolver.resolve(url);
            if (documentReference != null) {
                source.setParameter("$doc", documentReference);
            }
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                if (entry.getValue().size() > 1) {
                    source.setParameter(entry.getKey(), entry.getValue());
                } else {
                    source.setParameter(entry.getKey(), entry.getValue().get(0));
                }
            }
        }
    }

    private void processExtraParams(ObjectNode options, LiveDataQuery queryConfig)
    {
        JsonNode extraParamsNode = options.path("extraParams");
        if (extraParamsNode.isTextual()) {
            String extraParams = extraParamsNode.asText();
            try {
                List<Filter> filters = new ArrayList<>();
                Map<String, List<String>> parameters = this.urlQueryStringParser.parse('?' + extraParams);
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

    private List<SortEntry> getSortConfig(ObjectNode options)
    {
        JsonNode selectedColumn = options.path("selectedColumn");
        JsonNode defaultOrder = options.path("defaultOrder");
        if (selectedColumn.isTextual() || defaultOrder.isTextual()) {
            SortEntry sortEntry = new SortEntry(selectedColumn.asText(), "desc".equals(defaultOrder.asText()));
            return Collections.singletonList(sortEntry);
        }
        return null;
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
        propertyDescriptor.setName(getPropertyName(columnProperties));
        propertyDescriptor.setType(getPropertyType(column, columnProperties, options));

        // The live table macro considers all columns, except for "actions", as sortable by default.
        propertyDescriptor.setSortable(columnProperties.path("sortable").asBoolean(!columnProperties.has(ACTIONS)));

        // All columns are visible by default, unless explicitly marked as hidden.
        propertyDescriptor.setVisible(!HIDDEN.equals(columnProperties.path(TYPE).asText()));
        propertyDescriptor.setDisplayer(getDisplayerConfig(column, columnProperties, options));

        // The live table macro considers all columns, except for "actions", as filterable by default.
        propertyDescriptor.setFilterable(columnProperties.path("filterable").asBoolean(!columnProperties.has(ACTIONS)));
        propertyDescriptor.setFilter(getFilterConfig(columnProperties));

        propertyDescriptor.setStyleName(columnProperties.path("headerClass").asText(null));

        return propertyDescriptor;
    }

    private String getPropertyName(ObjectNode columnProperties)
    {
        JsonNode displayName = columnProperties.path("displayName");
        return displayName.isTextual() ? displayName.asText() : null;
    }

    private String getPropertyType(String column, ObjectNode columnProperties, ObjectNode options)
    {
        // The property type is specified by the class that owns the property, and the class name can be specified
        // either in the column configuration, for each column, or in the live table configuration, for all live table
        // columns.
        JsonNode className = columnProperties.path("class");
        if (!className.isTextual()) {
            className = options.path(CLASS_NAME);
            if (!className.isTextual()) {
                // We cannot determine the property type without the class name.
                return null;
            }
        }
        return this.propertyTypeSupplier.getPropertyType(column, className.asText());
    }

    /**
     * Identifies the column's displayer according to the column's livetable properties and the livetable's options.
     *
     * @param column the column to analyse
     * @param columnProperties the properties of the column
     * @param options the properties of the livetable
     * @return the displayer descriptor selected for the column
     */
    private DisplayerDescriptor getDisplayerConfig(String column, ObjectNode columnProperties,
        ObjectNode options)
    {
        DisplayerDescriptor displayerConfig = new DisplayerDescriptor();
        if (columnProperties.path(ACTIONS).isArray()) {
            displayerConfig.setId(ACTIONS);
            displayerConfig.setParameter(ACTIONS, columnProperties.get(ACTIONS));
        } else if (columnProperties.path(LINK).isTextual()) {
            displayerConfig.setId(LINK);
            displayerConfig.setParameter("propertyHref", getLinkTarget(column, columnProperties.get(LINK).asText()));
            displayerConfig.setParameter(HTML, columnProperties.path(HTML).booleanValue());
        } else if (columnProperties.path(HTML).booleanValue()) {
            if (options.get(CLASS_NAME) != null && StringUtils.isNotEmpty(options.get(CLASS_NAME).textValue())) {
                displayerConfig.setId("xClassProperty");
            } else {
                displayerConfig.setId(HTML);
            }
        } else if (Objects.equals(columnProperties.path(TYPE).textValue(), "list")) {
            displayerConfig.setId(HTML);
        } else {
            displayerConfig = null;
        }
        return displayerConfig;
    }

    private Object getLinkTarget(String column, String linkType)
    {
        String docURL = "doc.url";
        String columnURL = column + "_url";
        if ("auto".equals(linkType)) {
            return new String[] {columnURL, docURL};
        } else if ("field".equals(linkType)) {
            return columnURL;
        } else {
            String linkTypeURL = String.format("doc.%s_url", linkType);
            return new String[] {linkTypeURL, docURL};
        }
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
