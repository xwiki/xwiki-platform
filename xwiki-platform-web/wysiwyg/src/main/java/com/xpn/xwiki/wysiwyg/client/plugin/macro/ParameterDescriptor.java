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

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Describes a macro parameter.
 * 
 * @version $Id$
 */
public class ParameterDescriptor implements IsSerializable
{
    /**
     * @see #getName()
     */
    private String name;

    /**
     * @see #getDescription()
     */
    private String description;

    /**
     * @see #getType()
     */
    private String type;

    /**
     * Flag indicating if this parameter is required or not.
     */
    private boolean mandatory;

    /**
     * @return the parameter name
     */
    public String getName()
    {
        return name;
    }

    /**
     * Sets the name of this parameter.
     * 
     * @param name the name of the parameter
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
     * The parameter type is a fully qualified Java class name or the name of a primitive Java type. Common types are
     * {@code java.lang.String} or {@code int}.
     * 
     * @return the parameter type
     */
    public String getType()
    {
        return type;
    }

    /**
     * Sets the type of this parameter.
     * 
     * @param type the type of this parameter
     */
    public void setType(String type)
    {
        this.type = type;
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
