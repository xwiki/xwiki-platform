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
package org.xwiki.gwt.wysiwyg.client.plugin.macro;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Describes a macro and its parameters.
 * <p>
 * NOTE: This class is a serializable, GWT-supported <em>clone</em> of the MacroDescriptor from the rendering module.
 * 
 * @version $Id$
 */
public class MacroDescriptor implements IsSerializable
{
    /**
     * The macro identifier.
     */
    private String id;

    /**
     * The human-readable name of the macro (e.g. Table of Contents for ToC macro).
     */
    private String name;

    /**
     * The description of the macro.
     */
    private String description;

    /**
     * The category of the macro.
     */
    private String category;

    /**
     * A {@link Map} with descriptors for each parameter supported by the macro. The key is the parameter name.
     */
    private Map<String, ParameterDescriptor> parameterDescriptorMap;

    /**
     * Describes the content of the macro.
     */
    private ParameterDescriptor contentDescriptor;

    /**
     * Flag indicating if this macro supports in-line mode.
     */
    private boolean supportingInlineMode;

    /**
     * @return the macro identifier
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the identifier of the macro.
     * 
     * @param id a macro identifier
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the human-readable name of the macro (e.g. Table of Contents for ToC macro)
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the human-readable name of the macro (e.g. Table of Contents for ToC macro).
     * 
     * @param name the macro name
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the description of the macro
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the macro description.
     * 
     * @param description a {@link String} representing the macro description
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the category of the macro
     */
    public String getCategory()
    {
        return category;
    }

    /**
     * Sets the macro category.
     * 
     * @param category the macro category
     */
    public void setCategory(String category)
    {
        this.category = category;
    }

    /**
     * @return a {@link Map} with parameter descriptors
     */
    public Map<String, ParameterDescriptor> getParameterDescriptorMap()
    {
        return parameterDescriptorMap;
    }

    /**
     * Sets the {@link Map} of parameter descriptors.
     * 
     * @param parameterDescriptorMap a {@link Map} of parameter descriptors
     */
    public void setParameterDescriptorMap(Map<String, ParameterDescriptor> parameterDescriptorMap)
    {
        this.parameterDescriptorMap = parameterDescriptorMap;
    }

    /**
     * @return the content descriptor
     */
    public ParameterDescriptor getContentDescriptor()
    {
        return contentDescriptor;
    }

    /**
     * Sets the content descriptor.
     * 
     * @param contentDescriptor the object describing the content of the macro
     */
    public void setContentDescriptor(ParameterDescriptor contentDescriptor)
    {
        this.contentDescriptor = contentDescriptor;
    }

    /**
     * @return {@code true} if this macro supports in-line mode, {@code false} otherwise
     */
    public boolean isSupportingInlineMode()
    {
        return supportingInlineMode;
    }

    /**
     * Sets the flag which indicates if this macro supports in-line mode.
     * 
     * @param supportingInlineMode {@code true} if this macro is allowed in-line, {@code false} otherwise
     */
    public void setSupportingInlineMode(boolean supportingInlineMode)
    {
        this.supportingInlineMode = supportingInlineMode;
    }
}
