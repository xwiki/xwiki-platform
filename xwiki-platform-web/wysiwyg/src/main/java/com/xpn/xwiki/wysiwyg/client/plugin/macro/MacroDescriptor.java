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
package com.xpn.xwiki.wysiwyg.client.plugin.macro;

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
     * The description of the macro.
     */
    private String description;

    /**
     * A {@link Map} with descriptors for each parameter supported by the macro. The key is the parameter name.
     */
    private Map<String, ParameterDescriptor> parameterDescriptorMap;

    /**
     * Describes the content of the macro.
     */
    private ParameterDescriptor contentDescriptor;

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
}
