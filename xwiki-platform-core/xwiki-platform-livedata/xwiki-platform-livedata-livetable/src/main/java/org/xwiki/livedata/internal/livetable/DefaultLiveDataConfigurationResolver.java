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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Adds missing live data configuration values specific to the live table source.
 * 
 * @version $Id$
 * @since 12.10.3
 * @since 13.0
 */
@Component
@Named("liveTable")
@Singleton
public class DefaultLiveDataConfigurationResolver implements LiveDataConfigurationResolver<LiveDataConfiguration>
{
    /**
     * Used to translate the live data property names using the translation prefix specified by the live date source.
     */
    @Inject
    private ContextualLocalizationManager localization;

    @Override
    public LiveDataConfiguration resolve(LiveDataConfiguration config) throws LiveDataException
    {
        setDefaultSort(config.getQuery());
        addMissingPropertyDescriptors(config);
        setPropertyDescriptorDefaults(config);
        return config;
    }

    private void setDefaultSort(LiveDataQuery query)
    {
        if (query.getProperties() != null) {
            // Sort by default using the first non-special property (replicate the live table behavior).
            Optional<String> firstNonSpecialProperty = query.getProperties().stream().filter(Objects::nonNull)
                .filter(property -> !property.startsWith("_")).findFirst();
            if (firstNonSpecialProperty.isPresent()) {
                if (query.getSort() == null) {
                    query.setSort(new ArrayList<SortEntry>());
                }
                if (query.getSort().isEmpty()) {
                    // The sort is not specified.
                    query.getSort().add(new SortEntry(firstNonSpecialProperty.get()));
                } else if (query.getSort().size() == 1 && query.getSort().get(0).getProperty() == null) {
                    // The sort property was not specified (only the sort direction was specified).
                    query.getSort().get(0).setProperty(firstNonSpecialProperty.get());
                }
            }
        }

        if (query.getSort() != null) {
            // Remove the sort entry if we couldn't find a default sort property.
            query.setSort(query.getSort().stream().filter(Objects::nonNull)
                .filter(sortEntry -> sortEntry.getProperty() != null).collect(Collectors.toList()));
        }
    }

    private void addMissingPropertyDescriptors(LiveDataConfiguration config)
    {
        List<String> properties = config.getQuery().getProperties();
        if (properties != null) {
            Collection<LiveDataPropertyDescriptor> propertyDescriptors = config.getMeta().getPropertyDescriptors();
            Set<String> propertiesWithDescriptor = propertyDescriptors.stream().filter(Objects::nonNull)
                .map(propertyDescriptor -> propertyDescriptor.getId()).collect(Collectors.toSet());
            propertyDescriptors
                .addAll(properties.stream().filter(property -> !propertiesWithDescriptor.contains(property))
                    .map(this::getDefaultPropertyDescriptor).collect(Collectors.toList()));
        }
    }

    private LiveDataPropertyDescriptor getDefaultPropertyDescriptor(String property)
    {
        LiveDataPropertyDescriptor propertyDescriptor = new LiveDataPropertyDescriptor();
        propertyDescriptor.setId(property);
        return propertyDescriptor;
    }

    private void setPropertyDescriptorDefaults(LiveDataConfiguration config)
    {
        String translationPrefix = (String) config.getQuery().getSource().getParameters().get("translationPrefix");
        for (LiveDataPropertyDescriptor propertyDescriptor : config.getMeta().getPropertyDescriptors()) {
            // Prevent null values.
            propertyDescriptor.initialize();

            // Set the default property name.
            if (propertyDescriptor.getName() != null) {
                // All good.
            } else if (translationPrefix != null) {
                // The property name remains null if the translation key is missing so that it can fall-back on the
                // value specified by the live data source (property store).
                propertyDescriptor
                    .setName(this.localization.getTranslationPlain(translationPrefix + propertyDescriptor.getId()));
            } else {
                // Fall back on the property id.
                propertyDescriptor.setName(propertyDescriptor.getId());
            }

            // Set the default property type.
            if (propertyDescriptor.getType() == null) {
                propertyDescriptor.setType("String");
            }
        }
    }
}
