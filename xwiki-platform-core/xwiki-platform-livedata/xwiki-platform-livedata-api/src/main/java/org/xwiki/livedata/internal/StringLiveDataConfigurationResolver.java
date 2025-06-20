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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveDataActionDescriptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataLayoutDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataQuery.Filter;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataQuery.Source;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Resolves the live data configuration from a JSON string input.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Singleton
public class StringLiveDataConfigurationResolver implements LiveDataConfigurationResolver<String>
{
    private static final String QUERY = "query";

    private static final String PROPERTIES = "properties";

    private static final String SOURCE = "source";

    private static final String SORT = "sort";

    private static final String PROPERTY = "property";

    private static final String LIMIT = "limit";

    private static final String OFFSET = "offset";

    private static final String META = "meta";

    private static final String ICON = "icon";

    private static final String DISPLAYER = "displayer";

    private static final String FILTER = "filter";

    private static final String DISPLAYERS = "displayers";

    private static final String FILTERS = "filters";

    private static final String LAYOUTS = "layouts";

    private static final String ACTIONS = "actions";

    private static final String CSS_CLASS = "cssClass";

    private static final String EXTRA_ICON_CLASSES = "extraIconClasses";

    @Inject
    private Logger logger;

    @Inject
    private IconManager iconManager;

    @Override
    public LiveDataConfiguration resolve(String liveDataConfigJSON) throws LiveDataException
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        try {
            JsonNode liveDataConfig = objectMapper.readTree(liveDataConfigJSON);
            liveDataConfig = normalizeConfig(liveDataConfig, objectMapper);

            return objectMapper.readerFor(LiveDataConfiguration.class).readValue(liveDataConfig);
        } catch (IOException e) {
            throw new LiveDataException(e);
        }
    }

    private ObjectNode normalizeConfig(JsonNode liveDataConfig, ObjectMapper objectMapper)
    {
        if (liveDataConfig.isObject()) {
            ObjectNode liveDataConfigObj = (ObjectNode) liveDataConfig;
            normalizeQueryConfig(liveDataConfigObj, objectMapper);
            normalizeMetaConfig(liveDataConfigObj, objectMapper);
            return liveDataConfigObj;
        } else {
            return objectMapper.createObjectNode();
        }
    }

    private void normalizeQueryConfig(ObjectNode liveDataConfig, ObjectMapper objectMapper)
    {
        JsonNode queryConfig = liveDataConfig.path(QUERY);
        if (queryConfig.isObject()) {
            ObjectNode queryConfigObj = (ObjectNode) queryConfig;

            if (!queryConfig.path(PROPERTIES).isArray()) {
                queryConfigObj.remove(PROPERTIES);
            }

            normalizeSourceConfig(queryConfigObj, objectMapper);
            normalizeSortConfig(queryConfigObj, objectMapper);
            normalizeQueryFiltersConfig("hiddenFilters", queryConfigObj, objectMapper);
            normalizeQueryFiltersConfig(FILTERS, queryConfigObj, objectMapper);

            if (!queryConfig.path(LIMIT).isNumber()) {
                queryConfigObj.remove(LIMIT);
            }

            if (!queryConfig.path(OFFSET).isNumber()) {
                queryConfigObj.remove(OFFSET);
            }
        } else {
            liveDataConfig.remove(QUERY);
        }
    }

    private void normalizeSourceConfig(ObjectNode queryConfig, ObjectMapper objectMapper)
    {
        JsonNode sourceConfig = queryConfig.path(SOURCE);
        if (sourceConfig.isTextual()) {
            Source source = new Source(sourceConfig.asText());
            queryConfig.set(SOURCE, objectMapper.valueToTree(source));
        } else if (!sourceConfig.isObject()) {
            queryConfig.remove(SOURCE);
        }
    }

    private void normalizeSortConfig(ObjectNode queryConfig, ObjectMapper objectMapper)
    {
        JsonNode sortConfig = queryConfig.path(SORT);
        if (sortConfig.isTextual()) {
            SortEntry sortEntry = new SortEntry(sortConfig.asText());
            queryConfig.set(SORT, objectMapper.valueToTree(new SortEntry[] {sortEntry}));
        } else if (sortConfig.isObject() && sortConfig.has(PROPERTY)) {
            queryConfig.putArray(SORT).add(sortConfig);
        } else if (sortConfig.isArray()) {
            ArrayNode sortList = queryConfig.putArray(SORT);
            for (JsonNode sortEntry : ((ArrayNode) sortConfig)) {
                if (sortEntry.isTextual()) {
                    sortList.add(objectMapper.valueToTree(new SortEntry(sortEntry.asText())));
                } else if (sortEntry.isObject() && sortEntry.has(PROPERTY)) {
                    sortList.add(sortEntry);
                }
            }
        } else {
            queryConfig.remove(SORT);
        }
    }

    private void normalizeQueryFiltersConfig(String filtersProperty, ObjectNode queryConfig, ObjectMapper objectMapper)
    {
        JsonNode filtersConfig = queryConfig.path(filtersProperty);
        if (filtersConfig.isObject()) {
            // Convert to array.
            Iterator<Entry<String, JsonNode>> iterator = filtersConfig.fields();
            ArrayNode filters = queryConfig.putArray(filtersProperty);
            while (iterator.hasNext()) {
                Entry<String, JsonNode> entry = iterator.next();
                Filter filter = new Filter(entry.getKey(), entry.getValue());
                filters.add(objectMapper.valueToTree(filter));
            }
        } else if (!filtersConfig.isArray()) {
            queryConfig.remove(filtersProperty);
        }
    }

    private void normalizeMetaConfig(ObjectNode liveDataConfig, ObjectMapper objectMapper)
    {
        JsonNode metaConfig = liveDataConfig.path(META);
        if (metaConfig.isObject()) {
            ObjectNode metaConfigObj = (ObjectNode) metaConfig;

            normalizePropertyDescriptors("propertyDescriptors", metaConfigObj, objectMapper);
            normalizePropertyDescriptors("propertyTypes", metaConfigObj, objectMapper);
            normalizeLayouts(metaConfigObj, objectMapper);
            normalizeFilters(metaConfigObj, objectMapper);
            normalizeDisplayers(metaConfigObj, objectMapper);
            normalizeActions(metaConfigObj, objectMapper);
        } else {
            liveDataConfig.remove(META);
        }
    }

    private void normalizePropertyDescriptors(String fieldName, ObjectNode metaConfig, ObjectMapper objectMapper)
    {
        JsonNode propertyDescriptorsConfig = metaConfig.path(fieldName);
        if (propertyDescriptorsConfig.isArray()) {
            for (JsonNode entry : propertyDescriptorsConfig) {
                if (entry.isObject()) {
                    normalizePropertyDescriptor((ObjectNode) entry, objectMapper);
                }
            }
        } else {
            metaConfig.remove(fieldName);
        }
    }

    private void normalizePropertyDescriptor(ObjectNode propertyDescriptor, ObjectMapper objectMapper)
    {
        normalizeIcon(propertyDescriptor, objectMapper);
        propertyDescriptor.set(FILTER, normalizeFilter(propertyDescriptor.path(FILTER), objectMapper));
        propertyDescriptor.set(DISPLAYER, normalizeDisplayer(propertyDescriptor.path(DISPLAYER), objectMapper));
    }

    private void normalizeIcon(ObjectNode descriptor, ObjectMapper objectMapper)
    {
        JsonNode icon = descriptor.path(ICON);
        if (icon.isTextual()) {
            try {
                Map<String, Object> iconMetaData = this.iconManager.getMetaData(icon.asText());
                descriptor.set(ICON, objectMapper.valueToTree(iconMetaData));
            } catch (IconException e) {
                this.logger.warn("Failed to get icon meta data for [{}]. Root cause is [{}].", icon.asText(),
                    ExceptionUtils.getRootCauseMessage(e));
                descriptor.remove(ICON);
            }
        } else if (!icon.isObject()) {
            descriptor.remove(ICON);
        }
        normalizeIconClasses(descriptor);
    }

    /**
     * Adds the {@link #EXTRA_ICON_CLASSES} to the icon's CSS classes. It is done by looking for an
     * {@link #EXTRA_ICON_CLASSES} field on the descriptor. If the {@link #EXTRA_ICON_CLASSES} field is not found,
     * nothing happen. If it is found, it is concatenated at the end of the {@link #CSS_CLASS} field of the
     * {@link #ICON} object. If the {@link #CSS_CLASS} is not present, it is initialized with the value of
     * {@link #EXTRA_ICON_CLASSES}. The {@link #EXTRA_ICON_CLASSES} field is removed for the descriptor in all cases.
     *
     * @param descriptor the descriptor to normalize
     */
    private static void normalizeIconClasses(ObjectNode descriptor)
    {
        JsonNode icon = descriptor.path(ICON);
        if (icon.isObject()) {
            JsonNode extraClasses = descriptor.path(EXTRA_ICON_CLASSES);
            if (extraClasses.isTextual()) {
                String cssClasses = extraClasses.textValue().trim();
                if (icon.path(CSS_CLASS).isTextual()) {
                    cssClasses = icon.path(CSS_CLASS).textValue().trim() + " " + cssClasses;
                }
                ((ObjectNode) icon).set(CSS_CLASS, new TextNode(cssClasses));
            }
            // Does not need to be preserved once the icon is fully resolved.
            descriptor.remove(EXTRA_ICON_CLASSES);
        }
    }

    private void normalizeLayouts(ObjectNode metaConfig, ObjectMapper objectMapper)
    {
        JsonNode layoutsConfig = metaConfig.path(LAYOUTS);
        if (layoutsConfig.isArray()) {
            ArrayNode layouts = metaConfig.putArray(LAYOUTS);
            for (JsonNode layout : layoutsConfig) {
                layouts.add(normalizeLayout(layout, objectMapper));
            }
        } else {
            metaConfig.remove(LAYOUTS);
        }
    }

    private JsonNode normalizeLayout(JsonNode layoutConfig, ObjectMapper objectMapper)
    {
        if (layoutConfig.isTextual()) {
            return objectMapper.valueToTree(new LiveDataLayoutDescriptor(layoutConfig.asText()));
        } else if (layoutConfig.isObject()) {
            normalizeIcon((ObjectNode) layoutConfig, objectMapper);
        }
        return layoutConfig;
    }

    private void normalizeFilters(ObjectNode metaConfig, ObjectMapper objectMapper)
    {
        JsonNode filtersConfig = metaConfig.path(FILTERS);
        if (filtersConfig.isArray()) {
            ArrayNode filters = metaConfig.putArray(FILTERS);
            for (JsonNode filter : filtersConfig) {
                ObjectNode filterConfig = normalizeFilter(filter, objectMapper);
                if (filterConfig != null) {
                    filters.add(filterConfig);
                }
            }
        } else {
            metaConfig.remove(FILTERS);
        }
    }

    private ObjectNode normalizeFilter(JsonNode filterConfig, ObjectMapper objectMapper)
    {
        if (filterConfig.isTextual()) {
            return objectMapper.valueToTree(new FilterDescriptor(filterConfig.asText()));
        } else if (filterConfig.isObject()) {
            JsonNode operators = filterConfig.path("operators");
            if (operators.isArray()) {
                for (int i = 0; i < operators.size(); i++) {
                    JsonNode operator = operators.get(i);
                    if (operator.isTextual()) {
                        ObjectNode operatorObj = objectMapper.createObjectNode();
                        operatorObj.set("id", operator);
                        ((ArrayNode) operators).set(i, operatorObj);
                    }
                }
            }
            return (ObjectNode) filterConfig;
        } else {
            return null;
        }
    }

    private void normalizeDisplayers(ObjectNode metaConfig, ObjectMapper objectMapper)
    {
        JsonNode displayersConfig = metaConfig.path(DISPLAYERS);
        if (displayersConfig.isArray()) {
            ArrayNode displayers = metaConfig.putArray(DISPLAYERS);
            for (JsonNode displayer : displayersConfig) {
                ObjectNode displayerConfig = normalizeDisplayer(displayer, objectMapper);
                if (displayerConfig != null) {
                    displayers.add(displayerConfig);
                }
            }
        } else {
            metaConfig.remove(DISPLAYERS);
        }
    }

    private ObjectNode normalizeDisplayer(JsonNode displayerConfig, ObjectMapper objectMapper)
    {
        if (displayerConfig.isTextual()) {
            return objectMapper.valueToTree(new DisplayerDescriptor(displayerConfig.asText()));
        } else if (displayerConfig.isObject()) {
            return (ObjectNode) displayerConfig;
        } else {
            return null;
        }
    }

    private void normalizeActions(ObjectNode metaConfig, ObjectMapper objectMapper)
    {
        JsonNode actionsConfig = metaConfig.path(ACTIONS);
        if (actionsConfig.isArray()) {
            ArrayNode actions = metaConfig.putArray(ACTIONS);
            for (JsonNode action : actionsConfig) {
                actions.add(normalizeAction(action, objectMapper));
            }
        } else {
            metaConfig.remove(ACTIONS);
        }
    }

    private JsonNode normalizeAction(JsonNode actionConfig, ObjectMapper objectMapper)
    {
        if (actionConfig.isTextual()) {
            return objectMapper.valueToTree(new LiveDataActionDescriptor(actionConfig.asText()));
        } else if (actionConfig.isObject()) {
            normalizeIcon((ObjectNode) actionConfig, objectMapper);
        }
        return actionConfig;
    }
}
