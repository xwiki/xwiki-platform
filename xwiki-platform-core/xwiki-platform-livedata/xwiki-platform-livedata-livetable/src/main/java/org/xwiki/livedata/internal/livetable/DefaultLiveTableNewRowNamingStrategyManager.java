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

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategyManager;

/**
 * Default implementation of {@link LiveTableNewRowNamingStrategyManager}.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component
@Singleton
public class DefaultLiveTableNewRowNamingStrategyManager implements LiveTableNewRowNamingStrategyManager
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    @Override
    public Optional<LiveTableNewRowNamingStrategy> get(String hint) throws LiveDataException
    {
        ComponentManager componentManager = this.componentManagerProvider.get();
        if (StringUtils.isBlank(hint) || !componentManager.hasComponent(LiveTableNewRowNamingStrategy.class, hint)) {
            return Optional.empty();
        }
        try {
            return Optional.of(componentManager.getInstance(LiveTableNewRowNamingStrategy.class, hint));
        } catch (ComponentLookupException e) {
            throw new LiveDataException(String.format("Failed to load the row naming strategy [%s].", hint), e);
        }
    }

    @Override
    public Map<String, LiveTableNewRowNamingStrategy> getAll()
    {
        ComponentManager componentManager = this.componentManagerProvider.get();
        Map<String, LiveTableNewRowNamingStrategy> strategies = new TreeMap<>();
        for (ComponentDescriptor<LiveTableNewRowNamingStrategy> descriptor : componentManager
            .getComponentDescriptorList(LiveTableNewRowNamingStrategy.class)) {
            String hint = descriptor.getRoleHint();
            try {
                strategies.put(hint, componentManager.getInstance(LiveTableNewRowNamingStrategy.class, hint));
            } catch (ComponentLookupException e) {
                this.logger.warn("Failed to load the row naming strategy [{}]. Root cause is [{}].", hint,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return strategies;
    }
}
