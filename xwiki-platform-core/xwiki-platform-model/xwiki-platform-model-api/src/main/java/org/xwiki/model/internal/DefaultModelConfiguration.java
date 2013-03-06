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
package org.xwiki.model.internal;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelConfiguration;

/**
 * Get configuration data from the XWiki configuration using a {@link ConfigurationSource}. If no
 * {@link ConfigurationSource} component is found in the system then default to default values:
 * <ul>
 *   <li>"xwiki" for the default wiki value</li>
 *   <li>"Main" for the default space value</li>
 *   <li>"WebHome" for the default page value</li>
 * </ul>
 *
 * @version $Id$
 * @since 2.2M1
 */
@Component
@Singleton
public class DefaultModelConfiguration implements ModelConfiguration
{
    /**
     * Prefix for configuration keys for the Model module.
     */
    private static final String PREFIX = "model.";

    /**
     * Default values for all the Entity types, see {@link #getDefaultReferenceValue(org.xwiki.model.EntityType)}.
     */
    private static final Map<EntityType, String> DEFAULT_VALUES = new HashMap<EntityType, String>()
    {
        {
            put(EntityType.WIKI, "xwiki");
            put(EntityType.SPACE, "Main");
            put(EntityType.DOCUMENT, "WebHome");
            put(EntityType.ATTACHMENT, "filename");
            put(EntityType.OBJECT, "object");
            put(EntityType.OBJECT_PROPERTY, "property");
            put(EntityType.CLASS_PROPERTY, get(EntityType.OBJECT_PROPERTY));
        }
    };

    /**
     * Default Model implementation to use.
     */
    private static final String DEFAULT_MODEL_HINT = "bridge";

    /**
     * We want to make sure this component can be loaded and used even if there's no ConfigurationSource available
     * in the system. This is why we lazy load the ConfigurationSource component.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public String getDefaultReferenceValue(EntityType type)
    {
        String name;
        ConfigurationSource configurationSource = getConfigurationSource();
        if (configurationSource != null) {
            name = configurationSource.getProperty(PREFIX + "reference.default." + type.toString().toLowerCase(),
                DEFAULT_VALUES.get(type));
        } else {
            name = DEFAULT_VALUES.get(type);
        }

        return name;
    }

    @Override
    public String getImplementationHint()
    {
        String hint;
        ConfigurationSource configurationSource = getConfigurationSource();
        if (configurationSource != null) {
            hint = configurationSource.getProperty(PREFIX + "implementation", DEFAULT_MODEL_HINT);
        } else {
            hint = DEFAULT_MODEL_HINT;
        }
        return hint;
    }

    /**
     * @return the configuration source to use to get configuration data. For the moment we only look in the XWiki
     *         properties file since otherwise looking into Wiki, Space and User preferences cause some cyclic
     *         dependencies (we'll be able to do that when all code has been migrated to use References instead of
     *         Strings).
     */
    private ConfigurationSource getConfigurationSource()
    {
        ConfigurationSource configurationSource;
        try {
            configurationSource = this.componentManager.getInstance(ConfigurationSource.class, "xwikiproperties");
        } catch (ComponentLookupException e) {
            this.logger.debug("Failed to load [%]. Using default Model values", ConfigurationSource.class.getName(), e);
            configurationSource = null;
        }

        return configurationSource;
    }
}
