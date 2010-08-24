/*
 * Copyright 2010 Andreas Jonsson
 * 
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
 *
 */
package org.xwiki.security.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.logging.AbstractLogEnabled;
import static org.xwiki.component.descriptor.ComponentInstantiationStrategy.PER_LOOKUP;

import org.xwiki.configuration.ConfigurationSource;

import org.xwiki.security.RightServiceConfigurationManager;
import org.xwiki.security.RightService;
import org.xwiki.security.RightResolver;

/**
 * Default factory for right service.
 * @version $Id$
 */
@Component
@InstantiationStrategy(PER_LOOKUP)
public class DefaultRightServiceConfigurationManager extends AbstractLogEnabled
    implements RightServiceConfigurationManager
{
    /** Prefix for configuration keys. */
    private static final String CONFIGURATION_PREFIX = "security.";

    /** Prefix for right resolver configuration keys. */
    private static final String RIGHTRESOLVER_PREFIX = CONFIGURATION_PREFIX + "rightservice.resolver.";

    /** Default hint for component manager. */
    private static final String DEFAULT_HINT = "default";

    /** The component manager. */
    @Requirement private ComponentManager componentManager;

    /** Obtain configuration from the xwiki.properties file. */
    @Requirement("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * @param name Name of the property.
     * @param defaultValue A default value to use if none could be
     * found in the configuration.
     * @return a configured property, or the given default value. 
     */
    private String getRightResolverProperty(String name, String defaultValue)
    {
        return configuration.getProperty(RIGHTRESOLVER_PREFIX + name, defaultValue);
    }

    /** Configure the right resolver instance. */
    private void configureRightResolver()
    {
        String hint = getRightResolverProperty("type", DEFAULT_HINT);
        RightResolver resolver;
        try {
            resolver = componentManager.lookup(RightResolver.class, hint);
        } catch (ComponentLookupException e) {
            getLogger().error("Failed to lookup component for RightResolver of type '" + hint + "'", e);
            throw new RuntimeException(e);
        }

        if (!hint.equals(DEFAULT_HINT)) {
            try {
                ComponentDescriptor<RightResolver> descriptor
                    = componentManager.getComponentDescriptor(RightResolver.class, DEFAULT_HINT);
                componentManager.registerComponent(descriptor, resolver);
            } catch (ComponentRepositoryException e) {
                getLogger().error("Failed to register default right resolver instance.", e);
                throw new RuntimeException(e);
            }
        }

        getLogger().info("Successfully configured right resolver of type "
                         + resolver.getClass().getName());
    }

    @Override
    public RightService getConfiguredRightService()
    {
        configureRightResolver();

        try {
            return componentManager.lookup(RightService.class);
        } catch (ComponentLookupException e) {
            getLogger().error("Failed to lookup component for RightService.");
            throw new RuntimeException(e);
        }
    }
}