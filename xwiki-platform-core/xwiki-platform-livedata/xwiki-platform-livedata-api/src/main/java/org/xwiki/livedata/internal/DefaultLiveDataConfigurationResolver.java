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
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
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

    private JSONMerge jsonMerge = new JSONMerge();

    @Override
    public LiveDataConfiguration resolve(LiveDataConfiguration config) throws LiveDataException
    {
        try {
            Source source = config.getQuery() != null ? config.getQuery().getSource() : null;
            LiveDataConfiguration defaultConfig = getDefaultConfig(source);
            // Make sure both configurations have the same id so that they are properly merged.
            defaultConfig.setId(config.getId());
            return translate(this.jsonMerge.merge(defaultConfig, config));
        } catch (IOException e) {
            throw new LiveDataException(e);
        }
    }

    private LiveDataConfiguration getDefaultConfig(Source sourceConfig) throws LiveDataException, IOException
    {
        InputStream configInputStream = getClass().getResourceAsStream("/liveDataConfiguration.json");
        String configJSON = IOUtils.toString(configInputStream, "UTF-8");
        LiveDataConfiguration config = this.stringLiveDataConfigResolver.resolve(configJSON);

        config.initialize();

        Source actualSourceConfig = sourceConfig;
        if (actualSourceConfig == null) {
            actualSourceConfig = config.getQuery() != null ? config.getQuery().getSource() : null;
        }
        if (actualSourceConfig != null) {
            Optional<LiveDataSource> source = this.sourceManager.get(actualSourceConfig);
            if (source.isPresent()) {
                LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
                entryDescriptor.setIdProperty(source.get().getEntries().getIdProperty());
                config.getMeta().setEntryDescriptor(entryDescriptor);
                config.getMeta().setPropertyDescriptors(source.get().getProperties().get());
                config.getMeta().setPropertyTypes(source.get().getPropertyTypes().get());
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
