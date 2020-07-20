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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Default implementation of {@link LiveDataConfigHelper}.
 * 
 * @version $Id$
 * @since 12.6RC1
 */
@Component(roles = LiveDataConfigHelper.class)
@Singleton
public class LiveDataConfigHelper
{
    private static final String QUERY = "query";

    private static final String PROPERTIES = "properties";

    private static final String SOURCE = "source";

    private static final String ID = "id";

    private static final String SORT = "sort";

    private static final String PROPERTY = "property";

    private static final String DESCENDING = "descending";

    private static final String FILTERS = "filters";

    private static final String LIMIT = "limit";

    private static final String OFFSET = "offset";

    private static final String META = "meta";

    private static final String PROPERTY_DESCRIPTORS = "propertyDescriptors";

    private static final String ICON = "icon";

    private static final String DISPLAYER = "displayer";

    private static final String FILTER = "filter";

    private static final String PROPERTY_TYPES = "propertyTypes";

    @Inject
    private Logger logger;

    @Inject
    private LiveDataSourceManager sourceManager;

    @Inject
    private IconManager iconManager;

    /**
     * Creates a live data query based on the given configuration.
     * 
     * @param queryConfig the live data query configuration
     * @return the live data query instance
     * @throws IOException if the given query configuration cannot be serialized as JSON
     */
    public LiveDataQuery createQuery(Map<String, Object> queryConfig) throws IOException
    {
        return createQuery(new ObjectMapper().writeValueAsString(queryConfig));
    }

    /**
     * Creates a live data query based on the given configuration.
     * 
     * @param queryConfigJSON the live data query configuration
     * @return the live data query instance
     * @throws IOException if the given configuration cannot be parsed as JSON or if it cannot be converted into a
     *             {@link LiveDataQuery} instance
     */
    public LiveDataQuery createQuery(String queryConfigJSON) throws IOException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode queryConfig = objectMapper.readTree(queryConfigJSON);
        queryConfig = normalizeQueryConfig(queryConfig, objectMapper);
        Optional<LiveDataSource> source = getSource(queryConfig.path(SOURCE), objectMapper);
        if (source.isPresent()) {
            queryConfig = addDefaultQueryConfig(queryConfig, source.get(), objectMapper);
        }
        return objectMapper.readerFor(LiveDataQuery.class).readValue(queryConfig);
    }

    /**
     * Computes the effective live data configuration by normalizing the given configuration (i.e. transforming it to
     * match the format expected by the live data widget) and adding the (missing) default values.
     * 
     * @param liveDataConfig the live data configuration to start with
     * @return the effective live data configuration, using the standard format and containing the default values
     * @throws IOException if the given live data configuration cannot be serialized as JSON
     */
    public Map<String, Object> effectiveConfig(Map<String, Object> liveDataConfig) throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        String effectiveConfigJSON = effectiveConfig(objectMapper.writeValueAsString(liveDataConfig));
        return objectMapper.readerForMapOf(Object.class).readValue(effectiveConfigJSON);
    }

    /**
     * Computes the effective live data configuration by normalizing the given configuration (i.e. transforming it to
     * match the format expected by the live data widget) and adding the (missing) default values.
     * 
     * @param liveDataConfigJSON the live data configuration to start with
     * @return the effective live data configuration, using the standard format and containing the default values
     * @throws IOException if it fails to parse the live data configuration JSON
     */
    public String effectiveConfig(String liveDataConfigJSON) throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode liveDataConfig = objectMapper.readTree(liveDataConfigJSON);
        liveDataConfig = normalizeConfig(liveDataConfig, objectMapper);
        liveDataConfig = addDefaultConfig(liveDataConfig, objectMapper);
        return objectMapper.writeValueAsString(liveDataConfig);
    }

    private JsonNode normalizeConfig(JsonNode liveDataConfig, ObjectMapper objectMapper)
    {
        if (!liveDataConfig.isObject()) {
            return objectMapper.createObjectNode();
        }

        ObjectNode liveDataConfigObj = (ObjectNode) liveDataConfig;
        liveDataConfigObj.set(QUERY, normalizeQueryConfig(liveDataConfig.get(QUERY), objectMapper));
        liveDataConfigObj.set(META, normalizeMetaConfig(liveDataConfig.get(META), objectMapper));

        return liveDataConfig;
    }

    private JsonNode normalizeQueryConfig(JsonNode queryConfig, ObjectMapper objectMapper)
    {
        if (queryConfig == null || !queryConfig.isObject()) {
            return objectMapper.createObjectNode();
        }
        ObjectNode queryConfigObj = (ObjectNode) queryConfig;

        JsonNode propertiesConfig = queryConfig.path(PROPERTIES);
        if (!propertiesConfig.isArray()) {
            queryConfigObj.remove(PROPERTIES);
        }

        JsonNode sourceConfig = queryConfig.path(SOURCE);
        if (sourceConfig.isTextual()) {
            ObjectNode sourceConfigObj = objectMapper.createObjectNode();
            sourceConfigObj.put(ID, sourceConfig.asText());
            queryConfigObj.set(SOURCE, sourceConfigObj);
        } else if (!sourceConfig.isObject()) {
            queryConfigObj.remove(SOURCE);
        }

        normalizeSortConfig(queryConfigObj, objectMapper);
        normalizeQueryFiltersConfig(queryConfigObj, objectMapper);

        JsonNode limitConfig = queryConfig.path(LIMIT);
        if (!limitConfig.isNumber()) {
            queryConfigObj.remove(LIMIT);
        }

        JsonNode offsetConfig = queryConfig.path(OFFSET);
        if (!offsetConfig.isNumber()) {
            queryConfigObj.remove(OFFSET);
        }

        return queryConfig;
    }

    private void normalizeSortConfig(ObjectNode queryConfig, ObjectMapper objectMapper)
    {
        JsonNode sortConfig = queryConfig.path(SORT);
        if (sortConfig.isTextual()) {
            ArrayNode sortList = objectMapper.createArrayNode();
            ObjectNode sortEntry = objectMapper.createObjectNode();
            sortEntry.set(PROPERTY, sortConfig);
            sortEntry.put(DESCENDING, false);
            sortList.add(sortEntry);
            queryConfig.set(SORT, sortList);
        } else if (sortConfig.isObject() && sortConfig.has(PROPERTY)) {
            ArrayNode sortList = objectMapper.createArrayNode();
            sortList.add(sortConfig);
            queryConfig.set(SORT, sortList);
        } else if (sortConfig.isArray()) {
            ArrayNode sortList = objectMapper.createArrayNode();
            for (JsonNode sortEntry : ((ArrayNode) sortConfig)) {
                if (sortEntry.isTextual()) {
                    ObjectNode sortEntryObj = objectMapper.createObjectNode();
                    sortEntryObj.set(PROPERTY, sortEntry);
                    sortEntryObj.put(DESCENDING, false);
                    sortList.add(sortEntryObj);
                } else if (sortEntry.isObject() && sortEntry.has(PROPERTY)) {
                    sortList.add(sortEntry);
                }
            }
            queryConfig.set(SORT, sortList);
        } else {
            queryConfig.remove(SORT);
        }
    }

    private void normalizeQueryFiltersConfig(ObjectNode queryConfig, ObjectMapper objectMapper)
    {
        JsonNode filtersConfig = queryConfig.path(FILTERS);
        if (filtersConfig.isObject()) {
            // Convert to array.
            Iterator<Entry<String, JsonNode>> iterator = filtersConfig.fields();
            ArrayNode filters = objectMapper.createArrayNode();
            while (iterator.hasNext()) {
                Entry<String, JsonNode> entry = iterator.next();
                ObjectNode filter = objectMapper.createObjectNode();
                filter.put(PROPERTY, entry.getKey());
                filter.set("value", entry.getValue());
                filters.add(filter);
            }
            queryConfig.set(FILTERS, filters);
        } else if (!filtersConfig.isArray()) {
            queryConfig.remove(FILTERS);
        }
    }

    private JsonNode normalizeMetaConfig(JsonNode metaConfig, ObjectMapper objectMapper)
    {
        if (metaConfig == null || !metaConfig.isObject()) {
            return objectMapper.createObjectNode();
        }

        ObjectNode metaConfigObj = (ObjectNode) metaConfig;
        metaConfigObj.set(PROPERTY_DESCRIPTORS,
            normalizePropertyDescriptors(metaConfig.path(PROPERTY_DESCRIPTORS), objectMapper));
        metaConfigObj.set(PROPERTY_TYPES, normalizePropertyDescriptors(metaConfig.path(PROPERTY_TYPES), objectMapper));

        return metaConfig;
    }

    private JsonNode normalizePropertyDescriptors(JsonNode propertyDescriptorsConfig, ObjectMapper objectMapper)
    {
        if (propertyDescriptorsConfig.isArray()) {
            ArrayNode propertyDescriptors = objectMapper.createArrayNode();
            for (JsonNode entry : propertyDescriptorsConfig) {
                JsonNode propertyDescriptor = normalizePropertyDescriptor(entry, objectMapper);
                if (!propertyDescriptor.isEmpty()) {
                    propertyDescriptors.add(propertyDescriptor);
                }
            }
            return propertyDescriptors;
        } else {
            return objectMapper.createArrayNode();
        }
    }

    private JsonNode normalizePropertyDescriptor(JsonNode propertyDescriptorConfig, ObjectMapper objectMapper)
    {
        if (propertyDescriptorConfig.isObject()) {
            ObjectNode propertyDescriptor = (ObjectNode) propertyDescriptorConfig;

            normalizeIcon(propertyDescriptor, objectMapper);

            JsonNode displayer = propertyDescriptor.path(DISPLAYER);
            if (displayer.isTextual()) {
                ObjectNode displayerObj = objectMapper.createObjectNode();
                displayerObj.set(ID, displayer);
                propertyDescriptor.set(DISPLAYER, displayerObj);
            } else if (!displayer.isObject()) {
                propertyDescriptor.remove(DISPLAYER);
            }

            JsonNode filter = propertyDescriptor.path(FILTER);
            if (filter.isBoolean() && !filter.booleanValue()) {
                ObjectNode filterObj = objectMapper.createObjectNode();
                filterObj.put(ID, "none");
                propertyDescriptor.set(FILTER, filterObj);
            } else if (filter.isTextual()) {
                ObjectNode filterObj = objectMapper.createObjectNode();
                filterObj.set(ID, filter);
                propertyDescriptor.set(FILTER, filterObj);
            } else if (!filter.isObject()) {
                propertyDescriptor.remove(FILTER);
            }

            return propertyDescriptor;
        } else {
            return objectMapper.createObjectNode();
        }
    }

    private void normalizeIcon(ObjectNode propertyDescriptor, ObjectMapper objectMapper)
    {
        JsonNode icon = propertyDescriptor.path(ICON);
        if (icon.isTextual()) {
            try {
                Map<String, Object> iconMetaData = this.iconManager.getMetaData(icon.asText());
                propertyDescriptor.set(ICON, objectMapper.valueToTree(iconMetaData));
            } catch (IconException e) {
                this.logger.warn("Failed to get icon meta data for [{}]. Root cause is [{}].", icon.asText(),
                    ExceptionUtils.getRootCauseMessage(e));
                propertyDescriptor.remove(ICON);
            }
        } else if (!icon.isObject()) {
            propertyDescriptor.remove(ICON);
        }
    }

    private JsonNode addDefaultConfig(JsonNode liveDataConfig, ObjectMapper objectMapper) throws Exception
    {
        JsonNode sourceConfig = liveDataConfig.path(QUERY).path(SOURCE);
        Optional<LiveDataSource> source = getSource(sourceConfig, objectMapper);

        if (source.isPresent()) {
            addDefaultQueryConfig(liveDataConfig.path(QUERY), source.get(), objectMapper);
            addDefaultMetaConfig(liveDataConfig.path(META), source.get(), objectMapper);
        }

        return liveDataConfig;
    }

    private JsonNode addDefaultQueryConfig(JsonNode queryConfig, LiveDataSource source, ObjectMapper objectMapper)
    {
        // Nothing to do for now.
        return queryConfig;
    }

    private JsonNode addDefaultMetaConfig(JsonNode metaConfig, LiveDataSource source, ObjectMapper objectMapper)
        throws LiveDataException
    {
        JsonNode defaultPropertyDescriptors = objectMapper.valueToTree(source.getProperties().get());
        mergePropertyDescriptors((ArrayNode) metaConfig.get(PROPERTY_DESCRIPTORS),
            (ArrayNode) defaultPropertyDescriptors);

        JsonNode defaultPropertyTypes = objectMapper.valueToTree(source.getPropertyTypes().get());
        mergePropertyDescriptors((ArrayNode) metaConfig.get(PROPERTY_TYPES), (ArrayNode) defaultPropertyTypes);

        return metaConfig;
    }

    private void mergePropertyDescriptors(ArrayNode left, ArrayNode right)
    {
        List<JsonNode> missing = new ArrayList<>();
        for (JsonNode rightEntry : right) {
            JsonNode foundEntry = null;
            for (JsonNode leftEntry : left) {
                if (Objects.equals(rightEntry.path(ID).asText(), leftEntry.path(ID).asText())) {
                    foundEntry = leftEntry;
                    break;
                }
            }
            if (foundEntry != null) {
                // Merge property descriptors.
                mergePropertyDescriptor((ObjectNode) foundEntry, (ObjectNode) rightEntry);
            } else {
                missing.add(rightEntry);
            }
        }
        // Add the missing property descriptors.
        for (JsonNode propertyDescriptor : missing) {
            left.add(propertyDescriptor);
        }
    }

    private void mergePropertyDescriptor(ObjectNode left, ObjectNode right)
    {
        Iterator<Entry<String, JsonNode>> rightFieldsIterator = right.fields();
        while (rightFieldsIterator.hasNext()) {
            Entry<String, JsonNode> rightField = rightFieldsIterator.next();
            if (!left.has(rightField.getKey()) && !rightField.getValue().isNull()) {
                left.set(rightField.getKey(), rightField.getValue());
            } else if (DISPLAYER.equals(rightField.getKey()) || FILTER.equals(rightField.getKey())) {
                maybeMergeDisplayerOrFilter((ObjectNode) left.get(rightField.getKey()),
                    (ObjectNode) rightField.getValue());
            }
        }
    }

    private void maybeMergeDisplayerOrFilter(ObjectNode left, ObjectNode right)
    {
        if (left.has(ID) && !Objects.equals(left.get(ID).asText(), right.get(ID).asText())) {
            return;
        }

        Iterator<Entry<String, JsonNode>> rightFieldsIterator = right.fields();
        while (rightFieldsIterator.hasNext()) {
            Entry<String, JsonNode> rightField = rightFieldsIterator.next();
            if (!left.has(rightField.getKey()) && !rightField.getValue().isNull()) {
                left.set(rightField.getKey(), rightField.getValue());
            }
        }
    }

    private Optional<LiveDataSource> getSource(JsonNode sourceConfig, ObjectMapper objectMapper) throws IOException
    {
        Source sourceConfigObj = new Source();
        if (sourceConfig != null && !sourceConfig.isNull() && !sourceConfig.isMissingNode()) {
            sourceConfigObj = objectMapper.readerFor(Source.class).readValue(sourceConfig);
        }

        return this.sourceManager.get(sourceConfigObj);
    }
}
