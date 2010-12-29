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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

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
     * @see #getPropertyType()
     */
    private Type propertyType;

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
     * @see org.xwiki.properties.PropertyDescriptor#getId()
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * @param id the identifier of the property.
     * @see #getId()
     * @since 2.1M1
     */
    public void setId(String id)
    {
        this.id = id;
    }

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
     * @param name the display name of the property.
     * @see #getName()
     * @since 2.1M1
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
    @Deprecated
    public Class< ? > getPropertyClass()
    {
        Class< ? > clazz;
        if (this.propertyType instanceof Class) {
            clazz = (Class) this.propertyType;
        } else if (this.propertyType instanceof ParameterizedType) {
            clazz = (Class) ((ParameterizedType) this.propertyType).getRawType();
        } else {
            clazz = null;
        }

        return clazz;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.PropertyDescriptor#getPropertyType()
     */
    public Type getPropertyType()
    {
        return this.propertyType;
    }

    /**
     * @param propertyType the class of the property.
     * @see #getPropertyClass()
     */
    public void setPropertyType(Type propertyType)
    {
        this.propertyType = propertyType;
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
