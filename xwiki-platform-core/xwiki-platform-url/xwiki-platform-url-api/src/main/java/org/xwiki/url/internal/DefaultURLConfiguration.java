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
package org.xwiki.url.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.url.URLConfiguration;

/**
 * @version $Id$
 * @since 2.3M1
 */
@Component
@Singleton
public class DefaultURLConfiguration implements URLConfiguration
{
    /**
     * Prefix for configuration keys for the Core module.
     */
    private static final String PREFIX = "url.";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    @Named("xwikiproperties")
    private ConfigurationSource configuration;

    @Override
    public String getURLFormatId()
    {
        // Note: the format corresponds to the component hint for the XWikiURLFactory implementation to use.
        return this.configuration.getProperty(PREFIX + "format", "standard");
    }
}
