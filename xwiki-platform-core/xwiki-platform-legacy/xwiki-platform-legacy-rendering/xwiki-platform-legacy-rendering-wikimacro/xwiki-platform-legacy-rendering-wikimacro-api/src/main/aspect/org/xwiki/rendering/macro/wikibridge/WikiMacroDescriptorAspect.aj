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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.xwiki.rendering.macro.MacroId;
import org.xwiki.rendering.macro.descriptor.ContentDescriptor;

/**
 * Legacy aspect for {@link WikiMacroDescriptor}.
 *
 * @version $Id$
 * @since 14.6RC1
 */
public privileged aspect WikiMacroDescriptorAspect
{
    /**
     * @param defaultCategory the macro default category
     * @return this builder
     * @deprecated since 14.6RC1, use {@link WikiMacroDescriptor.Builder#defaultCategories(Collection)} instead
     */
    @Deprecated(since = "14.6RC1")
    public WikiMacroDescriptor.Builder WikiMacroDescriptor.Builder.defaultCategory(String defaultCategory)
    {
        this.descriptor.defaultCategories = Set.of(defaultCategory);
        return this;
    }

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
     * @deprecated since 10.10RC1 use the {@link WikiMacroDescriptor.Builder} instead
     */
    @Deprecated(since = "10.10RC1")
    public WikiMacroDescriptor.new(MacroId id, String name, String description, String defaultCategory,
        WikiMacroVisibility visibility, ContentDescriptor contentDescriptor,
        List<WikiMacroParameterDescriptor> parameterDescriptors)
    {
        super();
        this.id = id;
        this.name = name;
        this.description = description;
        this.contentDescriptor = contentDescriptor;
        this.parameterDescriptors = parameterDescriptors;
        this.defaultCategories = Set.of(defaultCategory);
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
     * @deprecated since 2.3M1 use
     *             {@link #WikiMacroDescriptor(MacroId, String, String, String, WikiMacroVisibility, ContentDescriptor, List)}
     *             instead
     */
    @Deprecated(since = "2.3M1")
    public WikiMacroDescriptor.new(String name, String description, String defaultCategory, WikiMacroVisibility visibility,
        ContentDescriptor contentDescriptor, List<WikiMacroParameterDescriptor> parameterDescriptors)
    {
        super();
        this.name = name;
        this.description = description;
        this.contentDescriptor = contentDescriptor;
        this.parameterDescriptors = parameterDescriptors;
        this.defaultCategories = Set.of(defaultCategory);
        this.visibility = visibility;
    }

    public String WikiMacroDescriptor.getDefaultCategory()
    {
        return this.getDefaultCategories().stream().findFirst().orElse(null);
    }
}
