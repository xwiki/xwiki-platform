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
package org.xwiki.properties.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.xwiki.properties.PropertyDescriptor;

/**
 * Default implementation for {@link PropertyDescriptor}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class DefaultPropertyDescriptor implements PropertyDescriptor
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
     * @see #getPropertyClass()
     */
    private Class< ? > propertyClass;

    /**
     * @see #getDefaultValue()
     */
    private Object defaultValue;

    /**
     * @see #isMandatory()
     */
    private boolean mandatory;

    /**
     * @see #getFied()
     */
    private Field field;

    /**
     * @see #getReadMethod()
     */
    private Method readMethod;

    /**
     * @see #getWriteMethod()
     */
    private Method writeMethod;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#getName()
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * @param name the name of the property.
     * @see #getName()
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#getDescription()
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * @param description the description of the property.
     * @see #getDescription()
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#getPropertyClass()
     */
    public Class< ? > getPropertyClass()
    {
        return this.propertyClass;
    }

    /**
     * @param propertyClass the type of the property.
     * @see #getPropertyClass()
     */
    public void setPropertyClass(Class< ? > propertyClass)
    {
        this.propertyClass = propertyClass;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#getDefaultValue()
     */
    public Object getDefaultValue()
    {
        return this.defaultValue;
    }

    /**
     * @param defaultValue the default value of the property.
     * @see #getDefaultValue()
     */
    public void setDefaultValue(Object defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#isMandatory()
     */
    public boolean isMandatory()
    {
        return this.mandatory;
    }

    /**
     * @param mandatory indicate if the property is mandatory.
     * @see #isMandatory()
     */
    public void setMandatory(boolean mandatory)
    {
        this.mandatory = mandatory;
    }

    /**
     * @see org.xwiki.properties.PropertyDescriptor#getFied().
     * 
     * @param field the {@link Field}.
     */
    public void setField(Field field)
    {
        this.field = field;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#getFied()
     */
    public Field getFied()
    {
        return this.field;
    }

    /**
     * @see org.xwiki.properties.PropertyDescriptor#getReadMethod().
     * 
     * @param readMethod the read {@link Method}.
     */
    public void setReadMethod(Method readMethod)
    {
        this.readMethod = readMethod;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#getReadMethod()
     */
    public Method getReadMethod()
    {
        return this.readMethod;
    }

    /**
     * @see org.xwiki.properties.PropertyDescriptor#getWriteMethod().
     * 
     * @param writeMethod the write {@link Method}.
     */
    public void setWriteMethod(Method writeMethod)
    {
        this.writeMethod = writeMethod;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#getWriteMethod()
     */
    public Method getWriteMethod()
    {
        return this.writeMethod;
    }
}
