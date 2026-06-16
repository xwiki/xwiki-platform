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

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.AbstractLiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.WithParameters;

/**
 * Provides the default live data configuration for the {@code documentSolrSearch} source. The most important value it
 * adds is the {@code meta.entryDescriptor.idProperty} ({@code doc.id}): without it the live data widget cannot identify
 * the rows and renders an empty table. It also fills the property descriptors from the property store so that the
 * column types and displayers are known at macro evaluation time.
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
     * Used to parse the default configuration JSON.
     */
    @Inject
    private LiveDataConfigurationResolver<String> stringLiveDataConfigResolver;

    @Inject
    @Named(SolrLiveDataPropertyStore.ROLE_HINT)
    private Provider<LiveDataPropertyDescriptorStore> propertyStoreProvider;

    private String defaultConfigJSON;

    @Override
    protected LiveDataConfiguration getDefaultConfiguration(LiveDataConfiguration input) throws LiveDataException
    {
        LiveDataConfiguration defaultConfig = this.stringLiveDataConfigResolver.resolve(getDefaultConfigJSON());

        // Provide the column descriptors (types, displayers) from the property store so that the macro-time meta is
        // complete, mirroring what the live table source does.
        Source source = input.getQuery() == null ? null : input.getQuery().getSource();
        defaultConfig.getMeta().setPropertyDescriptors(new ArrayList<>(getPropertyStore(source).get()));

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
