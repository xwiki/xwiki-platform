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
package org.xwiki.rendering.internal.transformation.icon;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * All configuration options for the Icon transformation.
 *
 * @version $Id$
 * @since 2.6RC1
 */
@Component
@Singleton
public class XWikiIconTransformationConfiguration extends DefaultIconTransformationConfiguration
{
    /**
     * Prefix for configuration keys for the Icon transformation module.
     */
    private static final String PREFIX = "rendering.transformation.icon.";

    /**
     * Configuration source from where to read configuration data from.
     */
    @Inject
    private ConfigurationSource configuration;

    @Override
    public Properties getMappings()
    {
        // Merge default properties and properties defined in the configuration
        Properties props = super.getMappings();
        props.putAll(this.configuration.getProperty(PREFIX + "mappings", Properties.class));
        return props;
    }
}
