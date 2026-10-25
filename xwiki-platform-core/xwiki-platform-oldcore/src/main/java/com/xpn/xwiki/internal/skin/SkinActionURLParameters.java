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
package com.xpn.xwiki.internal.skin;

import java.util.Map;
import java.util.TreeMap;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.skin.SkinActionURLParametersProvider;

/**
 * Collects the query parameters of all the {@link SkinActionURLParametersProvider} components, to add them to the URL
 * of the skin resources evaluated by the skin action.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@Component(roles = SkinActionURLParameters.class)
@Singleton
public class SkinActionURLParameters
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private Logger logger;

    /**
     * Add the parameters of all the {@link SkinActionURLParametersProvider} components to the passed map. The
     * providers are called in the order of their hints so that the generated URLs are stable.
     *
     * @param parameters the URL query parameters to complete
     */
    public void addParameters(Map<String, Object> parameters)
    {
        Map<String, SkinActionURLParametersProvider> providers;
        try {
            providers = new TreeMap<>(
                this.componentManagerProvider.get().getInstanceMap(SkinActionURLParametersProvider.class));
        } catch (ComponentLookupException e) {
            this.logger.warn("Failed to lookup the skin action URL parameters providers. Root cause: [{}]",
                ExceptionUtils.getRootCauseMessage(e));
            return;
        }

        for (Map.Entry<String, SkinActionURLParametersProvider> entry : providers.entrySet()) {
            try {
                Map<String, Object> providerParameters = entry.getValue().getParameters();
                if (providerParameters != null) {
                    parameters.putAll(providerParameters);
                }
            } catch (Exception e) {
                this.logger.warn("Failed to get the skin action URL parameters from provider [{}]. Root cause: [{}]",
                    entry.getKey(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }
}
