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

import java.lang.reflect.Type;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Converter provided value in the provided target type.
 * <p>
 * The role of this component is supposed to be the supported target types. For example:
 * <p>
 * <blockquote>
 * 
 * <pre>
 * &#064;Component(&quot;java.lang.Integer&quot;)
 * public class IntegerConverter implements Converter
 * </pre>
 * 
 * </blockquote>
 * <p>
 * When something goes wrong a Converter can throw a {@link ConversionException}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@ComponentRole
public interface Converter
{
    /**
     * @param <T> the type in which the provided value has to be converted
     * @param targetType the type in which the provided value has to be converted
     * @param sourceValue the value to convert
     * @return the converted value
     * @since 3.0M1
     */
    <T> T convert(Type targetType, Object sourceValue);
}
