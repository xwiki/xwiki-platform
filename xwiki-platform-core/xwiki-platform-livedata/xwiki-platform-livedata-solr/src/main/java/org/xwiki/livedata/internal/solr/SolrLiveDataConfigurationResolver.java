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
package org.xwiki.livedata.internal.solr;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.AbstractLiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.WithParameters;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Provides the default live data configuration for the {@code solr} source. The most important value it adds is the
 * {@code meta.entryDescriptor.idProperty} ({@code doc.id}): without it the live data widget cannot identify the rows
 * and renders an empty table. It also fills the property descriptors from the property store so that the column types
 * and displayers are known at macro evaluation time.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@Component
@Named(SolrLiveDataEntryStore.ROLE_HINT)
@Singleton
public class SolrLiveDataConfigurationResolver extends AbstractLiveDataConfigurationResolver
{
    private static final String DEFAULT_CONFIG_RESOURCE = "/solrLiveDataConfiguration.json";

    /**
     * The source parameter through which the caller specifies the translation key prefix used to localize the column
     * headers (same contract as the live table source).
     */
    private static final String TRANSLATION_PREFIX_PARAMETER = "translationPrefix";

    /**
     * The translation key suffix used to localize the column description (the hint displayed below the header).
     */
    private static final String HINT_SUFFIX = ".hint";

    /**
     * Used to parse the default configuration JSON.
     */
    @Inject
    private LiveDataConfigurationResolver<String> stringLiveDataConfigResolver;

    @Inject
    @Named(SolrLiveDataPropertyStore.ROLE_HINT)
    private Provider<LiveDataPropertyDescriptorStore> propertyStoreProvider;

    /**
     * Used to localize the column headers from the translation prefix specified by the caller.
     */
    @Inject
    private ContextualLocalizationManager localizationManager;

    private String defaultConfigJSON;

    @Override
    protected LiveDataConfiguration getDefaultConfiguration(LiveDataConfiguration input) throws LiveDataException
    {
        LiveDataConfiguration defaultConfig = this.stringLiveDataConfigResolver.resolve(getDefaultConfigJSON());

        // Provide the column descriptors (types, displayers) from the property store so that the macro-time meta is
        // complete, mirroring what the live table source does.
        Source source = input.getQuery() == null ? null : input.getQuery().getSource();
        defaultConfig.getMeta().setPropertyDescriptors(new ArrayList<>(getPropertyStore(source).get()));

        // Make sure every property requested by the caller has a descriptor: a requested property unknown to this
        // source (e.g. an unsupported column) would otherwise have no descriptor and crash the live data widget. We
        // add a minimal default descriptor for it (mirrors the live table source), so the column renders empty
        // instead of breaking the table.
        if (input.getQuery() != null) {
            addMissingPropertyDescriptors(defaultConfig, input.getQuery().getProperties());
        }

        // Localize the column headers using the caller-provided translation prefix (the property store leaves the
        // names unset). The names are set on the default configuration, so a name explicitly provided by the user in
        // the macro still wins through the merge performed by the parent resolver.
        translatePropertyDescriptors(defaultConfig, source);

        return defaultConfig;
    }

    /**
     * Adds a minimal default descriptor (id, {@code String} type, visible) for every requested property that does not
     * already have a descriptor, so that the live data widget does not fail on an unknown column.
     */
    private void addMissingPropertyDescriptors(LiveDataConfiguration defaultConfig, List<String> requestedProperties)
    {
        if (requestedProperties == null) {
            return;
        }
        Collection<LiveDataPropertyDescriptor> descriptors = defaultConfig.getMeta().getPropertyDescriptors();
        Set<String> knownIds =
            descriptors.stream().filter(Objects::nonNull).map(LiveDataPropertyDescriptor::getId)
                .collect(Collectors.toSet());
        List<LiveDataPropertyDescriptor> missing = requestedProperties.stream()
            .filter(property -> property != null && !knownIds.contains(property))
            .distinct()
            .map(this::getDefaultPropertyDescriptor)
            .toList();
        if (!missing.isEmpty()) {
            List<LiveDataPropertyDescriptor> merged = new ArrayList<>(descriptors);
            merged.addAll(missing);
            defaultConfig.getMeta().setPropertyDescriptors(merged);
        }
    }

    private LiveDataPropertyDescriptor getDefaultPropertyDescriptor(String property)
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setId(property);
        descriptor.setType("String");
        descriptor.setVisible(true);
        return descriptor;
    }

    /**
     * Resolves the localized name (and description) of each column from the {@code translationPrefix} source parameter
     * supplied by the caller, mirroring the live table source. The translation key is {@code translationPrefix} +
     * property id; when no prefix is set or no translation is found, the column header falls back to the property id.
     */
    private void translatePropertyDescriptors(LiveDataConfiguration defaultConfig, Source source)
    {
        String translationPrefix = source == null ? null
            : StringUtils.defaultString((String) source.getParameters().get(TRANSLATION_PREFIX_PARAMETER));
        for (LiveDataPropertyDescriptor property : defaultConfig.getMeta().getPropertyDescriptors()) {
            if (property.getName() == null) {
                String name = this.localizationManager.getTranslationPlain(translationPrefix + property.getId());
                property.setName(name != null ? name : property.getId());
            }
            if (property.getDescription() == null) {
                property.setDescription(
                    this.localizationManager.getTranslationPlain(translationPrefix + property.getId() + HINT_SUFFIX));
            }
        }
    }

    private LiveDataPropertyDescriptorStore getPropertyStore(Source sourceConfig)
    {
        LiveDataPropertyDescriptorStore propertyStore = this.propertyStoreProvider.get();
        if (propertyStore instanceof WithParameters && sourceConfig != null) {
            ((WithParameters) propertyStore).getParameters().putAll(sourceConfig.getParameters());
        }
        return propertyStore;
    }

    private String getDefaultConfigJSON()
    {
        if (this.defaultConfigJSON == null) {
            try (InputStream inputStream = getClass().getResourceAsStream(DEFAULT_CONFIG_RESOURCE)) {
                this.defaultConfigJSON = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to read the default live data configuration for the Solr source.", e);
            }
        }
        return this.defaultConfigJSON;
    }
}
