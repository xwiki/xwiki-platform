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
package org.xwiki.wikistream.descriptor;

import java.lang.reflect.Type;

import org.xwiki.properties.PropertyDescriptor;
import org.xwiki.stability.Unstable;

/**
 * The default implementation of {@link WikiStreamPropertyDescriptor}.
 * 
 * @version $Id$
 */
@Unstable
public class DefaultWikiStreamBeanParameterDescriptor<T> implements WikiStreamPropertyDescriptor<T>
{
    /**
     * The description of the property.
     */
    private PropertyDescriptor propertyDescriptor;

    /**
     * @param propertyDescriptor The description of the property
     */
    public DefaultWikiStreamBeanParameterDescriptor(PropertyDescriptor propertyDescriptor)
    {
        this.propertyDescriptor = propertyDescriptor;
    }

    @Override
    public String getId()
    {
        return this.propertyDescriptor.getId();
    }

    @Override
    public String getName()
    {
        return this.propertyDescriptor.getName();
    }

    @Override
    public String getDescription()
    {
        return this.propertyDescriptor.getDescription();
    }

    @Override
    public Type getPropertyType()
    {
        return this.propertyDescriptor.getPropertyType();
    }

    @Override
    public T getDefaultValue()
    {
        return (T) this.propertyDescriptor.getDefaultValue();
    }

    @Override
    public boolean isMandatory()
    {
        return this.propertyDescriptor.isMandatory();
    }
}
