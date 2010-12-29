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
package org.xwiki.properties.converter;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Helper base class for a {@link Converter} component.
 * <p>
 * Commonly a new component is supposed to implements {@link AbstractConverter#convertToString(Object)} and
 * {@link AbstractConverter#convertToType(Class, Object)}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public abstract class AbstractConverter implements Converter
{
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.converter.Converter#convert(java.lang.reflect.Type, java.lang.Object)
     */
    public <T> T convert(Type targetType, Object sourceValue)
    {
        Class< ? > sourceType = sourceValue == null ? null : sourceValue.getClass();

        T result;
        if (targetType.equals(String.class)) {
            // Convert --> String
            result = (T) ((Class) targetType).cast(convertToString(sourceValue));
        } else if (targetType.equals(sourceType)) {
            // No conversion necessary
            result = (T) sourceValue;
        } else {
            // Convert --> Type
            result = (T) convertToType(targetType, sourceValue);
        }
        
        return result;
    }

    /**
     * Convert the input object into a String.
     * <p>
     * <b>N.B.</b>This implementation simply uses the value's <code>toString()</code> method and should be overriden if
     * a more sophisticated mechanism for <i>conversion to a String</i> is required.
     * 
     * @param value The input value to be converted.
     * @return the converted String value.
     */
    protected String convertToString(Object value)
    {
        return value.toString();
    }

    /**
     * Convert the input object into an output object of the specified type.
     * <p>
     * Typical implementations will provide a minimum of <code>String --> type</code> conversion.
     * 
     * @param <T> the type in which the provided value has o be converted
     * @param targetType Data type to which this value should be converted.
     * @param value The input value to be converted.
     * @return The converted value.
     * @since 3.0M1
     */
    protected <T> T convertToType(Type targetType, Object value)
    {
        Class<T> clazz;
        if (targetType instanceof Class) {
            clazz = (Class) targetType;
        } else if (targetType instanceof ParameterizedType) {
            clazz = (Class) ((ParameterizedType) targetType).getRawType();
        } else {
            throw new ConversionException("Unknown type [" + targetType + "]");
        }

        // Call #convertToType(Class<T> type, Object value) for retro-compatibility
        return convertToType(clazz, value);
    }

    /**
     * Convert the input object into an output object of the specified type.
     * <p>
     * Typical implementations will provide a minimum of <code>String --> type</code> conversion.
     * 
     * @param <T> the type in which the provided value has o be converted
     * @param type Data type to which this value should be converted.
     * @param value The input value to be converted.
     * @return The converted value.
     * @deprecated since 3.0M1 overwrite {@link #convertToType(Type, Object)} instead
     */
    @Deprecated
    protected <T> T convertToType(Class<T> type, Object value)
    {
        throw new ConversionException("Not implemented.");
    }
}
