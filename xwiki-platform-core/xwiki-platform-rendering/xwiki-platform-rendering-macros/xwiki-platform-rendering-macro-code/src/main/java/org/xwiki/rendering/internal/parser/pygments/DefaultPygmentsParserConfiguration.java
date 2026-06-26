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
package org.xwiki.rendering.internal.parser.pygments;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;

/**
 * All configuration options for the Pygments based highlight parser macro.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Singleton
public class DefaultPygmentsParserConfiguration implements PygmentsParserConfiguration
{
    /**
     * Prefix for configuration keys for the Velocity Macro module.
     */
    private static final String PREFIX = "rendering.macro.code.pygments.";

    /**
     * Default value of style provided to {@code code.py}.
     * 
     * @since 16.10.0RC1
     */
    private static final String DEFAULT_STYLE = "default";

    /**
     * Defines from where to read the Pygments configuration data.
     */
    @Inject
    private ConfigurationSource configuration;

    @Override
    public String getStyle()
    {
        String style = this.configuration.getProperty(PREFIX + "style", String.class);
        if (style == null || style.isEmpty()) {
            return DEFAULT_STYLE;
        } else {
            return style;
        }
    }
}
