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

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Describes a macro parameter.
 * 
 * @version $Id$
 */
public class ParameterDescriptor implements IsSerializable
{
    /**
     * @see #getId()
     * @since 2.1M1
     */
    private String id;

    /**
     * @see #getName()
     * @since 2.1M1
     */
    private String name;

    /**
     * @see #getDescription()
     */
    private String description;

    /**
     * @see #getType()
     */
    private ParameterType type;

    /**
     * The {@link String} representation of this parameter's default value.
     */
    private String defaultValue;

    /**
     * Flag indicating if this parameter is required or not.
     */
    private boolean mandatory;

    /**
     * @return the identifier of this parameter
     * @since 2.1M1
     */
    public String getId()
    {
        return id;
    }

    /**
     * Sets the identifier of this parameter.
     * 
     * @param id the identifier of the parameter
     * @since 2.1M1
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the display name of this parameter
     * @since 2.1M1
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the display name of this parameter.
     * 
     * @param name the display name of the parameter
     * @since 2.1M1
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the parameter description
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * Sets the description of this parameter.
     * 
     * @param description the description of this parameter
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @return the parameter type
     */
    public ParameterType getType()
    {
        return type;
    }

    /**
     * Sets the type of this parameter.
     * 
     * @param type the type of this parameter
     */
    public void setType(ParameterType type)
    {
        this.type = type;
    }

    /**
     * @return the default value of the parameter
     */
    public String getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Sets the default value of the parameter.
     * 
     * @param defaultValue a {@link String} representing the default value of the parameter
     */
    public void setDefaultValue(String defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * @return {@code true} if this parameter is required, {@code false} if not
     */
    public boolean isMandatory()
    {
        return mandatory;
    }

    /**
     * Specify if this parameter is required or not.
     * 
     * @param mandatory {@code true} to make this parameter required, {@code false} otherwise
     */
    public void setMandatory(boolean mandatory)
    {
        this.mandatory = mandatory;
    }
}
