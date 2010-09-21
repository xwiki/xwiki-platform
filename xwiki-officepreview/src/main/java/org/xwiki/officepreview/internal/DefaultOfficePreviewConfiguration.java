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
package org.xwiki.officepreview.internal;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.officepreview.OfficePreviewConfiguration;

/**
 * Default implementation of {@link OfficePreviewConfiguration}.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@Component
public class DefaultOfficePreviewConfiguration implements OfficePreviewConfiguration
{
    /**
     * Prefix for configuration keys for the office-preview module.
     */
    private static final String PREFIX = "officepreview.";

    /**
     * Default size of the previews cache.
     */
    private static final int DEFAULT_CACHE_SIZE = 10;

    /**
     * used to access configuration data.
     */
    @Requirement
    private ConfigurationSource configuration;

    /**
     * {@inheritDoc}
     */
    public int getCacheSize()
    {
        return configuration.getProperty(PREFIX + "cacheSize", DEFAULT_CACHE_SIZE);
    }
}
