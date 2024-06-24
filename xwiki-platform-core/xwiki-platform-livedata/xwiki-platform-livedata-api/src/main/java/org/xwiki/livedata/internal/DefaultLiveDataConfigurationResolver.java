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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.livedata.LiveDataActionDescriptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataConfigurationResolver;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataLayoutDescriptor;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPaginationConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor.FilterDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.OperatorDescriptor;
import org.xwiki.livedata.LiveDataQuery.Source;
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
    private ContextualLocalizationManager l10n;

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    private JSONMerge jsonMerge = new JSONMerge();

    @Override
    public LiveDataConfiguration resolve(LiveDataConfiguration config) throws LiveDataException
    {
        try {
            return mergeBaseConfig(mergeSourceConfig(config));
        } catch (IOException e) {
            throw new LiveDataException(e);
        }
    }

    private LiveDataConfiguration mergeSourceConfig(LiveDataConfiguration config) throws LiveDataException
    {
        Source source = config.getQuery() != null ? config.getQuery().getSource() : null;
        String hint = source != null ? source.getId() : null;
        Type role =
            new DefaultParameterizedType(null, LiveDataConfigurationResolver.class, LiveDataConfiguration.class);
        ComponentManager componentManager = this.componentManagerProvider.get();
        if (hint != null && !"".equals(hint) && !"default".equals(hint) && componentManager.hasComponent(role, hint)) {
            try {
                LiveDataConfigurationResolver<LiveDataConfiguration> sourceConfigResolver =
                    componentManager.getInstance(role, hint);
                return sourceConfigResolver.resolve(config);
            } catch (ComponentLookupException e) {
                // Nothing to do.
            }
        }
        return config;
    }

    private LiveDataConfiguration mergeBaseConfig(LiveDataConfiguration config) throws LiveDataException, IOException
    {
        InputStream baseConfigInputStream = getClass().getResourceAsStream("/liveDataConfiguration.json");
        String baseConfigJSON = IOUtils.toString(baseConfigInputStream, "UTF-8");
        LiveDataConfiguration baseConfig = this.stringLiveDataConfigResolver.resolve(baseConfigJSON);

        // Make sure both configurations have the same id so that they are properly merged.
        baseConfig.setId(config.getId());

        LiveDataConfiguration mergedConfig = this.jsonMerge.merge(baseConfig, config);

        // Prevent null values (make the configuration explicit).
        mergedConfig.initialize();

        handleLayouts(config.getMeta().getLayouts(), mergedConfig.getMeta());
        handlePageSizes(mergedConfig);

        // Translate using the context locale.
        return translate(mergedConfig);
    }

    /**
     * If the pagination sizes are missing the limit define in the query, add it to the allowed page limits.
     *
     * @param mergedConfiguration the live data configuration
     */
    private void handlePageSizes(LiveDataConfiguration mergedConfiguration)
    {
        Integer limit = mergedConfiguration.getQuery().getLimit();
        if (limit != null) {
            LiveDataMeta meta = mergedConfiguration.getMeta();
            if (meta == null) {
                meta = new LiveDataMeta();
                mergedConfiguration.setMeta(meta);
            }
            LiveDataPaginationConfiguration pagination = meta.getPagination();
            if (pagination == null) {
                pagination = new LiveDataPaginationConfiguration();
                meta.setPagination(pagination);
            }
            List<Integer> pageSizes = pagination.getPageSizes();
            if (pageSizes == null) {
                pageSizes = new ArrayList<>();
                pagination.setPageSizes(pageSizes);
            }
            if (!pageSizes.contains(limit)) {
                pageSizes.add(limit);
                Collections.sort(pageSizes);
            }
        }
    }

    /**
     * Filters and updates the layouts in the merged configuration based on the layout descriptors provided by the
     * initial configuration.
     *
     * @param configuredLayouts the collection of layout descriptors to be matched against the merged configuration
     *     layouts
     * @param mergedConfigMeta the merged configuration meta-object containing the layouts to be used to have access
     *     to the full descriptors
     */
    private void handleLayouts(Collection<LiveDataLayoutDescriptor> configuredLayouts, LiveDataMeta mergedConfigMeta)
    {
        // Skip this if no layouts are configured explicitly.
        if (configuredLayouts != null && !configuredLayouts.isEmpty())
        {
            // Only keep the layout matching the ids provided by the higher level configuration, preserving the order.
            mergedConfigMeta.setLayouts(configuredLayouts.stream().map(configLayout -> mergedConfigMeta.getLayouts()
                    .stream()
                    .filter(baseConfigLayout -> Objects.equals(baseConfigLayout.getId(), configLayout.getId()))
                    .findFirst()
                    .orElse(configLayout))
                .collect(Collectors.toList()));
        }
    }

    private LiveDataConfiguration translate(LiveDataConfiguration config)
    {
        config.getMeta().getLayouts().stream().filter(Objects::nonNull).forEach(this::translate);
        config.getMeta().getFilters().stream().filter(Objects::nonNull).forEach(this::translate);
        config.getMeta().getActions().stream().filter(Objects::nonNull).forEach(this::translate);
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

    private void translate(LiveDataActionDescriptor action)
    {
        if (action.getName() == null && action.getId() != null) {
            action.setName(this.l10n.getTranslationPlain("liveData.action." + action.getId()));
            if (action.getName() == null) {
                action.setName(action.getId());
            }
        }
        if (action.getDescription() == null && action.getId() != null) {
            action.setDescription(
                this.l10n.getTranslationPlain(String.format("liveData.action.%s.hint", action.getId())));
        }
    }
}
