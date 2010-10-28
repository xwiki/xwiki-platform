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
package org.xwiki.rendering.macro.descriptor;

import org.xwiki.properties.BeanDescriptor;
import org.xwiki.rendering.macro.MacroId;

/**
 * Describe a macro with no parameters.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class DefaultMacroDescriptor extends AbstractMacroDescriptor
{
    /**
     * @param id the id of the macro
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @since 2.3M1
     */
    public DefaultMacroDescriptor(MacroId id, String name)
    {
        this(id, name, null);
    }

    /**
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @since 2.0M3
     * @deprecated since 2.3M1 use {@link #DefaultMacroDescriptor(MacroId, String)} instead
     */
    @Deprecated
    public DefaultMacroDescriptor(String name)
    {
        this(name, null);
    }

    /**
     * @param id the id of the macro
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description the description of the macro.
     * @since 2.3M1
     */
    public DefaultMacroDescriptor(MacroId id, String name, String description)
    {
        super(id, name, description, new DefaultContentDescriptor(), null);
    }

    /**
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description the description of the macro.
     * @since 2.0M3
     * @deprecated since 2.3M1 use {@link #DefaultMacroDescriptor(MacroId, String, String)} instead
     */
    @Deprecated
    public DefaultMacroDescriptor(String name, String description)
    {
        super(name, description, new DefaultContentDescriptor(), null);
    }

    /**
     * @param id the id of the macro
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description the description of the macro.
     * @param contentDescriptor description of the macro content.
     * @since 2.3M1
     */
    public DefaultMacroDescriptor(MacroId id, String name, String description, ContentDescriptor contentDescriptor)
    {
        super(id, name, description, contentDescriptor, null);
    }

    /**
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description the description of the macro.
     * @param contentDescriptor description of the macro content.
     * @since 2.0M3
     * @deprecated since 2.3M1 use {@link #DefaultMacroDescriptor(MacroId, String, String, ContentDescriptor)} instead
     */
    @Deprecated
    public DefaultMacroDescriptor(String name, String description, ContentDescriptor contentDescriptor)
    {
        super(name, description, contentDescriptor, null);
    }

    /**
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description the description of the macro.
     * @param contentDescriptor the description of the macro content. null indicate macro does not support content.
     * @param parametersBeanDescriptor the description of the parameters bean.
     * @since 2.0M3
     * @deprecated since 2.3M1 use
     *             {@link #DefaultMacroDescriptor(MacroId, String, String, ContentDescriptor, BeanDescriptor)} instead
     */
    @Deprecated
    public DefaultMacroDescriptor(String name, String description, ContentDescriptor contentDescriptor,
        BeanDescriptor parametersBeanDescriptor)
    {
        super(name, description, contentDescriptor, parametersBeanDescriptor);

        extractParameterDescriptorMap();
    }

    /**
     * @param id the id of the macro
     * @param name the name of the macro (eg "Table Of Contents" for the TOC macro)
     * @param description the description of the macro.
     * @param contentDescriptor the description of the macro content. null indicate macro does not support content.
     * @param parametersBeanDescriptor the description of the parameters bean.
     * @since 2.3M1
     */
    public DefaultMacroDescriptor(MacroId id, String name, String description, ContentDescriptor contentDescriptor,
        BeanDescriptor parametersBeanDescriptor)
    {
        super(id, name, description, contentDescriptor, parametersBeanDescriptor);

        extractParameterDescriptorMap();
    }
}
