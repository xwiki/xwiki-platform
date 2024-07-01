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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveDataQuery.Source;
import org.xwiki.livedata.LiveDataSource;
import org.xwiki.livedata.LiveDataSourceManager;
import org.xwiki.livedata.WithParameters;

/**
 * Default {@link LiveDataSourceManager} implementation.
 * 
 * @version $Id$
 * @since 12.10
 */
@Component
@Singleton
public class DefaultLiveDataSourceManager implements LiveDataSourceManager
{
    /**
     * Used to look for {@link LiveDataSource} implementations in the current namespace.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * Used to look for {@link LiveDataSource} implementations in a given namespace.
     */
    @Inject
    private ComponentManagerManager componentManagerManager;

    @Inject
    private Logger logger;

    @Override
    public Optional<LiveDataSource> get(Source sourceConfig, String namespace)
    {
        ComponentManager cm = getComponentManager(namespace);
        if (cm != null && cm.hasComponent(LiveDataSource.class, sourceConfig.getId())) {
            try {
                LiveDataSource liveDataSource = cm.getInstance(LiveDataSource.class, sourceConfig.getId());
                if (liveDataSource instanceof WithParameters) {
                    ((WithParameters) liveDataSource).getParameters().putAll(sourceConfig.getParameters());
                }
                return Optional.of(liveDataSource);
            } catch (ComponentLookupException e) {
                this.logger.error("Error when initializing LiveDataSource with hint [{}]", sourceConfig.getId(), e);
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<Collection<String>> getAvailableSources(String namespace)
    {
        ComponentManager cm = getComponentManager(namespace);
        if (cm == null) {
            return Optional.empty();
        } else {
            return Optional.of(cm.getComponentDescriptorList((Type) LiveDataSource.class).stream()
                .map(descriptor -> descriptor.getRoleHint()).collect(Collectors.toSet()));
        }
    }

    private ComponentManager getComponentManager(String namespace)
    {
        return "".equals(namespace) ? this.contextComponentManagerProvider.get()
            : this.componentManagerManager.getComponentManager(namespace, false);
    }
}
