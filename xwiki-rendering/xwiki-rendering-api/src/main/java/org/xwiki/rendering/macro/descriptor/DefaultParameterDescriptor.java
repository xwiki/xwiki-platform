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

/**
 * The default implementation of {@link ParameterDescriptor}.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class DefaultParameterDescriptor implements ParameterDescriptor
{
    /**
     * The name of the parameter.
     */
    private String name;

    /**
     * The description of the parameter.
     */
    private String description;

    /**
     * The type of the parameter.
     */
    private Class< ? > type;

    /**
     * Indicate if the parameter is mandatory.
     * 
     * @since 1.7
     */
    private boolean mandatory;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the name of the parameter.
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#getDescription()
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description the description of the parameter.
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#getType()
     */
    public Class< ? > getType()
    {
        return this.type;
    }

    /**
     * @param type the type of the parameter.
     */
    public void setType(Class< ? > type)
    {
        this.type = type;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#isMandatory()
     * @since 1.7
     */
    public boolean isMandatory()
    {
        return this.mandatory;
    }

    /**
     * @param mandatory indicate if the parameter is mandatory.
     * @since 1.7
     */
    public void setMandatory(boolean mandatory)
    {
        this.mandatory = mandatory;
    }
}
