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
package org.xwiki.lesscss.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Get the configuration options concerning the LESS compiler.
 *
 * @version $Id$
 * @since 7.4.2 - 8.0M2
 */
@Component(roles = LESSConfiguration.class)
@Singleton
public class LESSConfiguration
{
    private static final String CONFIGURATION_PREFIX = "lesscss.";

    @Inject
    private ConfigurationSource configurationSource;

    /**
     * @return the number of maximum compilations that can be done simultaneously
     */
    public int getMaximumSimultaneousCompilations()
    {
        return configurationSource.getProperty(CONFIGURATION_PREFIX + "maximumSimultaneousCompilations", 4);
    }

    /**
     *
     * @return whether the LESS compiler should generate inline sourcemaps.
     */
    public boolean isGenerateInlineSourceMaps()
    {
        return configurationSource.getProperty(CONFIGURATION_PREFIX + "generateInlineSourceMaps", false);
    }
}
