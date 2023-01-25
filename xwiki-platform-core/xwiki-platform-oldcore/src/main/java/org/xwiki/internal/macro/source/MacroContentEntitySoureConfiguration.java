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
package org.xwiki.internal.macro.source;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * Configuration related to the various entity sources for the code macro.
 * 
 * @version $Id$
 * @since 15.1RC1
 * @since 14.10.5
 */
@Component(roles = MacroContentEntitySoureConfiguration.class)
@Singleton
public class MacroContentEntitySoureConfiguration
{
    /**
     * Prefix for configuration keys for content macro related properties.
     */
    private static final String PREFIX = "rendering.macro.content.source.";

    /**
     * Use 1MB as maximum attachment size by default.
     */
    private static final int DEFAULT_MAXIMUM_ATTACHMENTSIZE = 1000000;

    /**
     * Defines from where to read the Pygments configuration data.
     */
    @Inject
    private ConfigurationSource configuration;

    /**
     * @return the maximum size of attachment to load
     */
    public int getMaximumAttachmentSize()
    {
        return this.configuration.getProperty(PREFIX + "attachmentMaximumSize", DEFAULT_MAXIMUM_ATTACHMENTSIZE);
    }
}
