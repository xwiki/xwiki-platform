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

import java.util.Properties;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.transformation.macro.MacroTransformationConfiguration;

/**
 * Basic default implementation to be used when using the XWiki Rendering system standalone.
 *
 * @version $Id$
 * @since 2.6RC1
 */
@Component
public class DefaultMacroTransformationConfiguration implements MacroTransformationConfiguration
{
    /**
     * @see #getCategories()
     */
    private Properties macroCategories = new Properties();

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.transformation.macro.MacroTransformationConfiguration#getCategories()
     */
    public Properties getCategories()
    {
        return this.macroCategories;
    }

    /**
     * @param macroId the id of the macro for which to set a category
     * @param category the category name to set
     */
    public void addCategory(MacroId macroId, String category)
    {
        // This method is useful for those using the XWiki Rendering in standalone mode since it allows the rendering
        // to work even without a configuration store.
        this.macroCategories.setProperty(macroId.toString(), category);
    }
}
