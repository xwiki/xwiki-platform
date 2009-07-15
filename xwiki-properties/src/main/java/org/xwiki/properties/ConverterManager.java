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

import org.xwiki.component.annotation.ComponentRole;

/**
 * Simple API to do universal conversion between two types.
 * <p>
 * To add support for another target type, implements a new {@link org.xwiki.properties.converter.Converter} component
 * 
 * @version $Id$
 * @since 2.0M2
 */
@ComponentRole
public interface ConverterManager
{
    /**
     * Convert provided value into the provided target type.
     * 
     * @param <T> the type in which the provided value has o be converted
     * @param targetClass the type in which the provided value has o be converted
     * @param value the value to convert
     * @return the converted value
     */
    <T> T convert(Class<T> targetClass, Object value);
}
