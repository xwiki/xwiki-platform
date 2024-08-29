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
package org.xwiki.rendering.internal.transformation.macro;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.transformation.macro.MacroTransformationConfiguration;

import static org.xwiki.rendering.macro.AbstractMacro.DEFAULT_CATEGORY_DEPRECATED;
import static org.xwiki.rendering.macro.AbstractMacro.DEFAULT_CATEGORY_INTERNAL;

/**
 * All configuration options for the Macro Transformation subsystem.
 *
 * @version $Id$
 * @since 2.6RC1
 */
@Component
@Singleton
public class XWikiMacroTransformationConfiguration implements MacroTransformationConfiguration
{
    /**
     * Prefix for configuration keys for the Rendering module.
     */
    private static final String PREFIX = "rendering.transformation.macro.";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    private ConfigurationSource configuration;

    @Override
    public Properties getCategories()
    {
        return this.configuration.getProperty(PREFIX + "categories", Properties.class);
    }

    @Override
    public Set<String> getHiddenCategories()
    {
        List<?> hiddenCategories = this.configuration.getProperty(PREFIX + "hiddenCategories", List.class,
            List.of(DEFAULT_CATEGORY_INTERNAL, DEFAULT_CATEGORY_DEPRECATED));
        return hiddenCategories.stream().map(String::valueOf).collect(Collectors.toSet());
    }
}
