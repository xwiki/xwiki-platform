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
package org.xwiki.rendering.internal.macro.formula;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.macro.formula.FormulaMacroConfiguration;

/**
 * Configuration options for the {@link org.xwiki.rendering.internal.macro.formula.FormulaMacro formula macro},
 * implemented using the {@link ConfigurationSource} component.
 * 
 * @version $Id$
 * @since 2.0M3
 */
@Component
public class DefaultFormulaMacroConfiguration implements FormulaMacroConfiguration
{
    /**
     * Prefix for configuration keys for this module.
     */
    private static final String PREFIX = "macro.formula.";

    /**
     * Default value for the renderer hint.
     * 
     * @see #getRenderer()
     */
    private static final String DEFAULT_RENDERER = "native";

    /**
     * The "safe" renderer hint.
     * 
     * @see #getSafeRenderer()
     */
    private static final String SAFE_RENDERER = "snuggletex";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Requirement
    private ConfigurationSource configuration;

    /**
     * {@inheritDoc}
     * 
     * @see FormulaMacroConfiguration#getRenderer()
     */
    public String getRenderer()
    {
        return this.configuration.getProperty(PREFIX + "renderer", DEFAULT_RENDERER);
    }

    /**
     * {@inheritDoc}
     * 
     * @see FormulaMacroConfiguration#getSafeRenderer()
     */
    public String getSafeRenderer()
    {
        return SAFE_RENDERER;
    }
}
