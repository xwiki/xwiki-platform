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

import org.xwiki.properties.PropertyDescriptor;

/**
 * The default implementation of {@link ParameterDescriptor}.
 * 
 * @version $Id$
 * @since 1.7M2
 */
public class DefaultParameterDescriptor implements ParameterDescriptor
{
    /**
     * The description of the parameter.
     */
    private PropertyDescriptor propertyDescriptor;

    /**
     * Creates a new {@link DefaultParameterDescriptor} instance using the given {@link PropertyDescriptor}.
     * 
     * @param propertyDescriptor The {@link PropertyDescriptor} instance.
     */
    public DefaultParameterDescriptor(PropertyDescriptor propertyDescriptor)
    {
        this.propertyDescriptor = propertyDescriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#getId()
     */
    public String getId()
    {
        return this.propertyDescriptor.getId();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#getName()
     */
    public String getName()
    {
        return this.propertyDescriptor.getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#getDescription()
     */
    public String getDescription()
    {
        return propertyDescriptor.getDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#getType()
     */
    public Class< ? > getType()
    {
        return propertyDescriptor.getPropertyClass();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#getDefaultValue()
     */
    public Object getDefaultValue()
    {
        return propertyDescriptor.getDefaultValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.descriptor.ParameterDescriptor#isMandatory()
     * @since 1.7
     */
    public boolean isMandatory()
    {
        return propertyDescriptor.isMandatory();
    }
}
