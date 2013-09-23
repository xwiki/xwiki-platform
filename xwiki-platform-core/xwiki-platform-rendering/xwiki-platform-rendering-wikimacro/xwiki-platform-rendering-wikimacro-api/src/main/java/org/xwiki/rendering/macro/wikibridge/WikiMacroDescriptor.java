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
package org.xwiki.rendering.macro.wikibridge;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;
import org.xwiki.rendering.macro.descriptor.MacroDescriptor;
import org.xwiki.rendering.macro.descriptor.ParameterDescriptor;

/**
 * A {@link MacroDescriptor} for describing wiki macros.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class WikiMacroDescriptor implements MacroDescriptor
{
    /**
     * Macro id.
     */
    private MacroId id;

    /**
     * Macro name.
     */
    private String name;

    /**
     * Macro description.
     */
    private String description;

    /**
     * Default category under which this macro should be listed.
     */
    private String defaultCategory;

    /**
     * Whether the macro is visible in the current wiki, for the current user or global.
     */
    private WikiMacroVisibility visibility;

    /**
     * Macro content description.
     */
    private ContentDescriptor contentDescriptor;

    /**
     * Parameter descriptors.
     */
    private List<WikiMacroParameterDescriptor> parameterDescriptors;

    /**
     * Creates a new {@link WikiMacroDescriptor} instance.
     * 
     * @param id the macro id
     * @param name the macro name
     * @param description macro description
     * @param defaultCategory default category under which this macro should be listed.
     * @param visibility the macro visibility (only visible in the current wiki, for the current user or global)
     * @param contentDescriptor macro content description.
     * @param parameterDescriptors parameter descriptors.
     * @since 2.3M1
     */
    public WikiMacroDescriptor(MacroId id, String name, String description, String defaultCategory,
        WikiMacroVisibility visibility, ContentDescriptor contentDescriptor,
        List<WikiMacroParameterDescriptor> parameterDescriptors)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.contentDescriptor = contentDescriptor;
        this.parameterDescriptors = parameterDescriptors;
        this.defaultCategory = defaultCategory;
        this.visibility = visibility;
    }

    /**
     * Creates a new {@link WikiMacroDescriptor} instance.
     * 
     * @param name the macro name
     * @param description macro description
     * @param defaultCategory default category under which this macro should be listed.
     * @param visibility the macro visibility (only visible in the current wiki, for the current user or global)
     * @param contentDescriptor macro content description.
     * @param parameterDescriptors parameter descriptors.
     * @since 2.2M1
     * @deprecated since 2.3M1 use {@link #WikiMacroDescriptor(MacroId, String, String, String, WikiMacroVisibility, ContentDescriptor, List)} instead
     */
    @Deprecated
    public WikiMacroDescriptor(String name, String description, String defaultCategory, WikiMacroVisibility visibility,
        ContentDescriptor contentDescriptor, List<WikiMacroParameterDescriptor> parameterDescriptors)
    {
        this.name = name;
        this.description = description;
        this.contentDescriptor = contentDescriptor;
        this.parameterDescriptors = parameterDescriptors;
        this.defaultCategory = defaultCategory;
        this.visibility = visibility;
    }

    @Override
    public MacroId getId()
    {
        return this.id;
    }

    @Override
    public String getName()
    {
        return this.name;
    }

    @Override
    public String getDescription()
    {
        return this.description;
    }

    @Override
    public ContentDescriptor getContentDescriptor()
    {
        return this.contentDescriptor;
    }

    @Override
    public Class< ? > getParametersBeanClass()
    {
        return WikiMacroParameters.class;
    }

    @Override
    public Map<String, ParameterDescriptor> getParameterDescriptorMap()
    {
        // Note: use a linked hash map to preserve the parameter order.
        // TODO: Replace saving the descriptors as a list and use a map instead so that this method becomes performant
        // (as it should be).
        Map<String, ParameterDescriptor> descriptors = new LinkedHashMap<String, ParameterDescriptor>();

        for (WikiMacroParameterDescriptor descriptor : this.parameterDescriptors) {
            descriptors.put(descriptor.getId(), descriptor);
        }

        return descriptors;
    }

    @Override
    public String getDefaultCategory()
    {
        return this.defaultCategory;
    }

    /**
     * @return the visibility of the macro (ie whether the macro is visible in the current wiki, for the current user or
     *         global)
     * @since 2.2M1
     */
    public WikiMacroVisibility getVisibility()
    {
        return this.visibility;
    }
}
