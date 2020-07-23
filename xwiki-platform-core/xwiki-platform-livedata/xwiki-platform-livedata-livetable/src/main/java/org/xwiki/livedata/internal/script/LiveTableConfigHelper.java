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
package org.xwiki.livedata.internal.script;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery.Constraint;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataQuery.Source;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper component for converting the Live Table configuration into Live Data configuration.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@Component(roles = LiveTableConfigHelper.class)
@Singleton
public class LiveTableConfigHelper
{
    private static final String ID = "id";

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

    /**
     * Converts the Live Table configuration into Live Data configuration.
     * 
     * @param id the live table id
     * @param columns the list of live table columns
     * @param columnProperties the column properties
     * @param options the live table options
     * @return the live data configuration
     */
    public Map<String, Object> getConfig(String id, List<String> columns, Map<String, Object> columnProperties,
        Map<String, Object> options) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode columnPropertiesJSON = objectMapper.valueToTree(columnProperties);
        ObjectNode optionsJSON = objectMapper.valueToTree(options);
        ObjectNode config = getConfig(id, columns, columnPropertiesJSON, optionsJSON, objectMapper);
        return objectMapper.readerForMapOf(Object.class).readValue(config);
    }

    /**
     * Converts the Live Table configuration into Live Data configuration.
     * 
     * @param id the live table id
     * @param columns the list of live table columns
     * @param columnProperties the column properties
     * @param options the live table options
     * @return the live data configuration JSON
     */
    public String getConfigJSON(String id, List<String> columns, Map<String, Object> columnProperties,
        Map<String, Object> options) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_DEFAULT);
        ObjectNode columnPropertiesJSON = objectMapper.valueToTree(columnProperties);
        ObjectNode optionsJSON = objectMapper.valueToTree(options);
        ObjectNode config = getConfig(id, columns, columnPropertiesJSON, optionsJSON, objectMapper);
        return objectMapper.writeValueAsString(config);
    }

    private ObjectNode getConfig(String id, List<String> columns, ObjectNode columnProperties, ObjectNode options,
        ObjectMapper objectMapper)
    {
        ObjectNode config = objectMapper.createObjectNode();
        config.put(ID, id);
        config.set("query", getQueryConfig(columns, options, objectMapper));
        config.set("meta", getMetaConfig(columnProperties, options, objectMapper));
        return config;
    }

    private ObjectNode getQueryConfig(List<String> columns, ObjectNode options, ObjectMapper objectMapper)
    {
        ObjectNode queryConfig = objectMapper.createObjectNode();

        if (!columns.isEmpty()) {
            queryConfig.set("properties", objectMapper.valueToTree(columns));
        }

        queryConfig.set("source", getSourceConfig(options, objectMapper));

        ArrayNode hiddenFilters = getHiddenFiltersConfig(options, objectMapper);
        if (!hiddenFilters.isEmpty()) {
            queryConfig.set("hiddenFilters", hiddenFilters);
        }

        ArrayNode sortConfig = getSortConfig(columns, options, objectMapper);
        if (!sortConfig.isEmpty()) {
            queryConfig.set("sort", sortConfig);
        }

        JsonNode rowCount = options.path("rowCount");
        if (rowCount.isNumber()) {
            queryConfig.put("limit", rowCount.asInt());
        }

        return queryConfig;
    }

    private ObjectNode getSourceConfig(ObjectNode options, ObjectMapper objectMapper)
    {
        Source source = new Source();
        source.setId("liveTable");
        for (String parameter : Arrays.asList("className", "resultPage", "queryFilters", "translationPrefix")) {
            if (options.path(parameter).isTextual()) {
                source.put(parameter, options.path(parameter).asText());
            }
        }

        JsonNode urlNode = options.path("url");
        if (urlNode.isTextual()) {
            String url = urlNode.asText();
            try {
                addSourceParametersFromURL(source, url);
            } catch (MalformedURLException | UnsupportedEncodingException e) {
                this.logger.warn("Failed to extract live data source parameters from live table results URL [{}]. "
                    + "Root cause is [{}].", url, ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return objectMapper.valueToTree(source);
    }

    private void addSourceParametersFromURL(Source source, String url)
        throws MalformedURLException, UnsupportedEncodingException
    {
        Map<String, List<String>> parameters = getURLParameters(url);
        List<String> xpage = parameters.remove("xpage");
        if (xpage != null && !xpage.isEmpty() && !StringUtils.isEmpty(xpage.get(0)) && !"plain".equals(xpage.get(0))) {
            source.put("template", xpage.get(0));
            // Make sure we don't overwrite the source id.
            parameters.remove(ID);
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                if (entry.getValue().size() > 1) {
                    source.put(entry.getKey(), entry.getValue());
                } else {
                    source.put(entry.getKey(), entry.getValue().get(0));
                }
            }
        }
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

    private ArrayNode getHiddenFiltersConfig(ObjectNode options, ObjectMapper objectMapper)
    {
        List<Filter> filters = new ArrayList<>();
        JsonNode extraParamsNode = options.path("extraParams");
        if (extraParamsNode.isTextual()) {
            String extraParams = extraParamsNode.asText();
            try {
                Map<String, List<String>> parameters = getURLParameters('?' + extraParams);
                for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                    Filter filter = new Filter();
                    filter.setProperty(entry.getKey());
                    filter.getConstraints()
                        .addAll(entry.getValue().stream().map(Constraint::new).collect(Collectors.toList()));
                    filters.add(filter);
                }
            } catch (MalformedURLException | UnsupportedEncodingException e) {
                this.logger.warn("Failed to extract live data hidden filters from live table extra parameters [{}]."
                    + " Root cause is [{}].", extraParams, ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return objectMapper.valueToTree(filters);
    }

    private ArrayNode getSortConfig(List<String> columns, ObjectNode options, ObjectMapper objectMapper)
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
                return objectMapper.createArrayNode();
            }
        }
        sortEntry.setDescending("desc".equals(options.path("defaultOrder").asText()));
        return objectMapper.valueToTree(new SortEntry[] {sortEntry});
    }

    private ObjectNode getMetaConfig(ObjectNode columnProperties, ObjectNode options, ObjectMapper objectMapper)
    {
        ObjectNode metaConfig = objectMapper.createObjectNode();

        ArrayNode propertyDescriptors = getPropertyDescriptorsConfig(columnProperties, options, objectMapper);
        if (!propertyDescriptors.isEmpty()) {
            metaConfig.set("propertyDescriptors", propertyDescriptors);
        }

        ObjectNode pagination = getPaginationConfig(options, objectMapper);
        if (!pagination.isEmpty()) {
            metaConfig.set("pagination", pagination);
        }

        return metaConfig;
    }

    private ArrayNode getPropertyDescriptorsConfig(ObjectNode columnProperties, ObjectNode options,
        ObjectMapper objectMapper)
    {
        ArrayNode propertyDescriptors = objectMapper.createArrayNode();
        columnProperties.fields().forEachRemaining(field -> propertyDescriptors
            .add(getPropertyDescriptor(field.getKey(), (ObjectNode) field.getValue(), options, objectMapper)));
        return propertyDescriptors;
    }

    private ObjectNode getPropertyDescriptor(String column, ObjectNode columnProperties, ObjectNode options,
        ObjectMapper objectMapper)
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
            propertyDescriptor.setHidden(true);
        }

        ObjectNode displayer = getDisplayerConfig(column, columnProperties, objectMapper);
        try {
            propertyDescriptor.getDisplayer().putAll(objectMapper.readerForMapOf(Object.class).readValue(displayer));
        } catch (IOException e) {
            this.logger.warn("Failed to extract displayer information from column properties [{}]. Root cause is [{}].",
                columnProperties, ExceptionUtils.getRootCauseMessage(e));
        }

        // The live table macro considers all columns, except for "actions", as filterable by default.
        propertyDescriptor.setFilterable(columnProperties.path("filterable").asBoolean(!columnProperties.has(ACTIONS)));

        ObjectNode filter = getFilterConfig(columnProperties, objectMapper);
        try {
            propertyDescriptor.getFilter().putAll(objectMapper.readerForMapOf(Object.class).readValue(filter));
        } catch (IOException e) {
            this.logger.warn("Failed to extract filter information from column properties [{}]. Root cause is [{}].",
                columnProperties, ExceptionUtils.getRootCauseMessage(e));
        }

        propertyDescriptor.setStyleName(columnProperties.path("headerClass").asText(null));

        JsonNode className = columnProperties.path("class");
        if (className.isTextual()) {
            // TODO: Extract more information from the specified XWiki class (the property type).
        }

        return objectMapper.valueToTree(propertyDescriptor);
    }

    private ObjectNode getDisplayerConfig(String column, ObjectNode columnProperties, ObjectMapper objectMapper)
    {
        ObjectNode displayerConfig = objectMapper.createObjectNode();
        if (columnProperties.path(ACTIONS).isArray()) {
            displayerConfig.put(ID, ACTIONS);
            displayerConfig.set(ACTIONS, columnProperties.get(ACTIONS));
        } else if (columnProperties.path(LINK).isTextual()) {
            displayerConfig.put(ID, LINK);
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
            displayerConfig.put("propertyHref", values[0]);
        } else if (columnProperties.path(HTML).booleanValue()) {
            displayerConfig.put(ID, HTML);
        }
        return displayerConfig;
    }

    private ObjectNode getFilterConfig(ObjectNode columnProperties, ObjectMapper objectMapper)
    {
        ObjectNode filterConfig = objectMapper.createObjectNode();

        JsonNode filterId = columnProperties.path(TYPE);
        if (filterId.isTextual() && !HIDDEN.equals(filterId.asText())) {
            filterConfig.put(ID, filterId.asText());
        }

        JsonNode match = columnProperties.path("match");
        if (match.isTextual()) {
            String defaultOperator = DEFAULT_OPERATOR.get(match.asText());
            if (defaultOperator != null) {
                filterConfig.put("defaultOperator", defaultOperator);
            }
        }

        return filterConfig;
    }

    private ObjectNode getPaginationConfig(ObjectNode options, ObjectMapper objectMapper)
    {
        ObjectNode pagination = objectMapper.createObjectNode();
        pagination.put("maxShownPages", options.path("maxPages").asInt(10));
        boolean showPageSizeDropdown = options.path("pageSize").asBoolean(true);
        pagination.put("showPageSizeDropdown", showPageSizeDropdown);

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
            pagination.set("pageSizes", objectMapper.valueToTree(pageSizes));
        }

        return pagination;
    }
}
