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
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.LiveDataQuery.SortEntry;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.WithParameters;
import org.xwiki.livedata.internal.JSONMerge;
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
    private ContextualLocalizationManager l10n;

    /**
     * Used to retrieve the property descriptors. Document properties are static but class properties are not so they
     * need to be retrieved from the database (users can add or remove class properties).
     */
    @Inject
    @Named("liveTable")
    private Provider<LiveDataPropertyDescriptorStore> propertyStoreProvider;

    @Inject
    @Named("liveTable")
    private Provider<LiveDataConfiguration> defaultConfigProvider;

    /**
     * Used to merge the default configuration with the provided configuration.
     */
    private JSONMerge jsonMerge = new JSONMerge();

    @Override
    public LiveDataConfiguration resolve(LiveDataConfiguration config) throws LiveDataException
    {
        LiveDataConfiguration defaultConfig = getDefaultConfiguration(config);

        // Make sure both configurations have the same id so that they are properly merged.
        defaultConfig.setId(config.getId());

        LiveDataConfiguration mergedConfig = this.jsonMerge.merge(defaultConfig, config);

        // We don't set the default sort on the default configuration (before the merge) because the sort is not easy to
        // merge automatically (the sort entry doesn't have an "id" property). We need to do the merge manually because
        // the given configuration can have a sort entry that specifies only the sort order and not the sort property
        // (e.g. when you want to sort on the default property using a specified sort order).
        setDefaultSort(mergedConfig);

        // Translate using the context locale.
        return translate(mergedConfig);
    }

    private LiveDataConfiguration getDefaultConfiguration(LiveDataConfiguration config) throws LiveDataException
    {
        LiveDataConfiguration defaultConfig = this.defaultConfigProvider.get();

        // We overwrite the property descriptors from the default configuration because they don't include the class
        // properties which are dynamic (retrieved from the database). The property store returns both the static
        // document properties and the dynamic class properties.
        defaultConfig.getMeta().setPropertyDescriptors(getPropertyStore(config.getQuery().getSource()).get());

        List<String> properties = config.getQuery().getProperties();
        if (properties != null) {
            // Make sure all the properties listed in the query have a descriptor.
            addMissingPropertyDescriptors(defaultConfig, properties);
        }

        return defaultConfig;
    }

    private LiveDataPropertyDescriptorStore getPropertyStore(Source sourceConfig)
    {
        LiveDataPropertyDescriptorStore propertyStore = this.propertyStoreProvider.get();
        if (propertyStore instanceof WithParameters && sourceConfig != null) {
            ((WithParameters) propertyStore).getParameters().putAll(sourceConfig.getParameters());
        }
        return propertyStore;
    }

    private void setDefaultSort(LiveDataConfiguration config)
    {
        LiveDataQuery query = config.getQuery();
        if (query.getProperties() != null) {
            // Replicate the current live table behavior: look for the first non-special property (whose name doesn't
            // start with underscore) and use it if its descriptor says we can sort on it.
            Optional<String> firstNonSpecialProperty = query.getProperties().stream().filter(Objects::nonNull)
                .filter(property -> !property.startsWith("_")).findFirst();
            if (firstNonSpecialProperty.isPresent()
                && isPropertySortable(firstNonSpecialProperty.get(), config.getMeta())) {
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
                .filter(sortEntry -> !StringUtils.isEmpty(sortEntry.getProperty())).collect(Collectors.toList()));
        }
    }

    private boolean isPropertySortable(String property, LiveDataMeta meta)
    {
        Optional<LiveDataPropertyDescriptor> propertyDescriptor = meta.getPropertyDescriptors().stream()
            .filter(descriptor -> Objects.equals(property, descriptor.getId())).findFirst();
        if (!propertyDescriptor.isPresent()) {
            return false;
        } else if (propertyDescriptor.get().isSortable() != null) {
            return propertyDescriptor.get().isSortable();
        } else {
            String propertyType = propertyDescriptor.get().getType();
            Optional<LiveDataPropertyDescriptor> propertyTypeDescriptor = meta.getPropertyTypes().stream()
                .filter(descriptor -> Objects.equals(descriptor.getId(), propertyType)).findFirst();
            return propertyTypeDescriptor.isPresent() && Boolean.TRUE.equals(propertyTypeDescriptor.get().isSortable());
        }
    }

    private void addMissingPropertyDescriptors(LiveDataConfiguration config, List<String> properties)
    {
        Collection<LiveDataPropertyDescriptor> propertyDescriptors = config.getMeta().getPropertyDescriptors();
        Set<String> propertiesWithDescriptor = propertyDescriptors.stream().filter(Objects::nonNull)
            .map(propertyDescriptor -> propertyDescriptor.getId()).collect(Collectors.toSet());
        List<LiveDataPropertyDescriptor> missingDescriptors =
            properties.stream().filter(property -> !propertiesWithDescriptor.contains(property))
                .map(this::getDefaultPropertyDescriptor).collect(Collectors.toList());
        if (!missingDescriptors.isEmpty()) {
            propertyDescriptors = new ArrayList<>(propertyDescriptors);
            propertyDescriptors.addAll(missingDescriptors);
            config.getMeta().setPropertyDescriptors(propertyDescriptors);
        }
    }

    private LiveDataPropertyDescriptor getDefaultPropertyDescriptor(String property)
    {
        LiveDataPropertyDescriptor propertyDescriptor = new LiveDataPropertyDescriptor();
        propertyDescriptor.setId(property);
        propertyDescriptor.setType("String");
        propertyDescriptor.setVisible(true);
        return propertyDescriptor;
    }

    private LiveDataConfiguration translate(LiveDataConfiguration config)
    {
        String translationPrefix = (String) config.getQuery().getSource().getParameters().get("translationPrefix");
        for (LiveDataPropertyDescriptor property : config.getMeta().getPropertyDescriptors()) {
            if (property.getName() == null) {
                property.setName(this.l10n.getTranslationPlain(translationPrefix + property.getId()));
                if (property.getName() == null) {
                    property.setName(property.getId());
                }
            }
            if (property.getDescription() == null) {
                property.setDescription(this.l10n.getTranslationPlain(translationPrefix + property.getId() + ".hint"));
            }
        }
        return config;
    }
}
