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

/**
 * The default implementation of {@link WikiStreamParameterDescriptor}.
 * 
 * @version $Id$
 */
public class DefaultWikiStreamParameterDescriptor implements WikiStreamParameterDescriptor
{

    /**
     * The description of the parameter.
     */
    private PropertyDescriptor propertyDescriptor;

    /**
     * @param propertyDescriptor The description of a parameter
     */
    public DefaultWikiStreamParameterDescriptor(PropertyDescriptor propertyDescriptor)
    {
        this.propertyDescriptor = propertyDescriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamParameterDescriptor#getId()
     */
    public String getId()
    {
        return this.propertyDescriptor.getId();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamParameterDescriptor#getName()
     */
    public String getName()
    {
        return this.propertyDescriptor.getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamParameterDescriptor#getDescription()
     */
    public String getDescription()
    {
        return this.propertyDescriptor.getDescription();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamParameterDescriptor#getParameterType()
     */
    public Type getParameterType()
    {
        return this.propertyDescriptor.getPropertyType();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamParameterDescriptor#getDefaultValue()
     */
    public Object getDefaultValue()
    {
        return this.propertyDescriptor.getDefaultValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.wikistream.descriptor.WikiStreamParameterDescriptor#isMandatory()
     */
    public boolean isMandatory()
    {
        return this.propertyDescriptor.isMandatory();
    }

}
