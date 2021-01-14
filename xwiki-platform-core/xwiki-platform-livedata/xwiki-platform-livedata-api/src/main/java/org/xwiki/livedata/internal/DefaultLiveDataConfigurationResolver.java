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
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataLayoutDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.OperatorDescriptor;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Adds default values to a live data configuration.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Singleton
public class DefaultLiveDataConfigurationResolver implements LiveDataConfigurationResolver<LiveDataConfiguration>
{
    @Inject
    private LiveDataConfigurationResolver<String> stringLiveDataConfigResolver;

    @Inject
    private LiveDataSourceManager sourceManager;

    @Inject
    private ContextualLocalizationManager l10n;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private JSONMerge jsonMerge = new JSONMerge();

    @Override
    public LiveDataConfiguration resolve(LiveDataConfiguration config) throws LiveDataException
    {
        try {
            Source source = config.getQuery() != null ? config.getQuery().getSource() : null;

            LiveDataConfiguration baseConfig = getBaseConfig(source);
            // Make sure both configurations have the same id so that they are properly merged.
            baseConfig.setId(config.getId());

            LiveDataConfiguration mergedConfig = this.jsonMerge.merge(baseConfig, config);
            // Prevent null values (make the configuration explicit).
            mergedConfig.initialize();

            // Add default configuration values that are specific to the live data source being used.
            mergedConfig = addSourceSpecificDefaults(mergedConfig);

            // Translate using the context locale.
            return translate(mergedConfig);
        } catch (IOException e) {
            throw new LiveDataException(e);
        }
    }

    private LiveDataConfiguration getBaseConfig(Source sourceConfig) throws LiveDataException, IOException
    {
        InputStream configInputStream = getClass().getResourceAsStream("/liveDataConfiguration.json");
        String configJSON = IOUtils.toString(configInputStream, "UTF-8");
        LiveDataConfiguration baseConfig = this.stringLiveDataConfigResolver.resolve(configJSON);

        Source actualSourceConfig = sourceConfig;
        if (actualSourceConfig == null) {
            actualSourceConfig = baseConfig.getQuery() != null ? baseConfig.getQuery().getSource() : null;
        }
        if (actualSourceConfig != null) {
            Optional<LiveDataSource> source = this.sourceManager.get(actualSourceConfig);
            if (source.isPresent()) {
                LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
                entryDescriptor.setIdProperty(source.get().getEntries().getIdProperty());
                baseConfig.getMeta().setEntryDescriptor(entryDescriptor);
                baseConfig.getMeta().setPropertyDescriptors(source.get().getProperties().get());
                baseConfig.getMeta().setPropertyTypes(source.get().getPropertyTypes().get());
            }
        }

        return baseConfig;
    }

    private LiveDataConfiguration addSourceSpecificDefaults(LiveDataConfiguration config) throws LiveDataException
    {
        Type role =
            new DefaultParameterizedType(null, LiveDataConfigurationResolver.class, LiveDataConfiguration.class);
        String hint = config.getQuery().getSource().getId();
        ComponentManager componentManager = this.componentManagerProvider.get();
        if (hint != null && componentManager.hasComponent(role, hint)) {
            try {
                LiveDataConfigurationResolver<LiveDataConfiguration> defaultConfigResolver =
                    componentManager.getInstance(role, hint);
                return defaultConfigResolver.resolve(config);
            } catch (ComponentLookupException e) {
                // Nothing to do.
            }
        }
        return config;
    }

    private LiveDataConfiguration translate(LiveDataConfiguration config)
    {
        config.getMeta().getLayouts().stream().filter(Objects::nonNull).forEach(this::translate);
        config.getMeta().getFilters().stream().filter(Objects::nonNull).forEach(this::translate);
        return config;
    }

    private void translate(LiveDataLayoutDescriptor layout)
    {
        if (layout.getName() == null && layout.getId() != null) {
            layout.setName(this.l10n.getTranslationPlain("liveData.layout." + layout.getId()));
            if (layout.getName() == null) {
                layout.setName(layout.getId());
            }
        }
    }

    private void translate(FilterDescriptor filter)
    {
        filter.getOperators().stream().filter(Objects::nonNull).forEach(this::translate);
    }

    private void translate(OperatorDescriptor operator)
    {
        if (operator.getName() == null && operator.getId() != null) {
            operator.setName(this.l10n.getTranslationPlain("liveData.operator." + operator.getId()));
            if (operator.getName() == null) {
                operator.setName(operator.getId());
            }
        }
    }
}
