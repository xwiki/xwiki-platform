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
package org.xwiki.records.internal.macro;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.livetable.LiveTableNewRowNamingStrategy;

/**
 * Gives the properties a row naming strategy needs to create entries.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = RecordsCreationStrategies.class)
@Singleton
public class RecordsCreationStrategies
{
    /**
     * Used to find the installed row naming strategies.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    /**
     * @param hint the strategy hint
     * @param sourceParameters the Live Data source parameters
     * @return the properties the strategy needs, or {@link Optional#empty()} when it cannot be loaded
     */
    public Optional<Set<String>> getMandatoryProperties(String hint, Map<String, Object> sourceParameters)
    {
        ComponentManager componentManager = this.componentManagerProvider.get();
        if (!componentManager.hasComponent(LiveTableNewRowNamingStrategy.class, hint)) {
            return Optional.empty();
        }
        try {
            LiveTableNewRowNamingStrategy strategy =
                componentManager.getInstance(LiveTableNewRowNamingStrategy.class, hint);
            return Optional.of(strategy.getMandatoryProperties(sourceParameters));
        } catch (ComponentLookupException e) {
            this.logger.warn("Failed to load the new row naming strategy [{}]. Root cause is [{}].", hint,
                ExceptionUtils.getRootCauseMessage(e));
            return Optional.empty();
        }
    }
}
