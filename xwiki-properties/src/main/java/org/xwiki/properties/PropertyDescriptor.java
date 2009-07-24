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
package org.xwiki.properties;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Describe a property in a bean.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public interface PropertyDescriptor
{
    /**
     * @return the name of the property.
     */
    String getName();

    /**
     * @return the description of the property.
     */
    String getDescription();

    /**
     * @return the type of the property.
     */
    Class< ? > getPropertyClass();

    /**
     * @return the default value of the property.
     */
    Object getDefaultValue();

    /**
     * @return indicate if the property is mandatory.
     */
    boolean isMandatory();

    /**
     * @return the read method. If null it generally mean that the property is a public field.
     */
    Method getReadMethod();

    /**
     * @return the write method. If null it generally mean that the property is a public field.
     */
    Method getWriteMethod();

    /**
     * @return the field. If null if generally mean that the property is based on getter/setter.
     */
    Field getFied();
}
