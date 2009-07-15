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
package org.xwiki.properties.internal.converter;

import org.apache.commons.beanutils.ConvertUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.Converter;

/**
 * {@link ConvertUtils} based converter.
 * <p>
 * It's the default {@link Converter}, the one used when no other Converter could be found by
 * {@link org.xwiki.properties.ConverterManager}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Component
public class ConvertUtilsConverter implements Converter
{
    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.properties.converter.Converter#convert(java.lang.Class, java.lang.Object)
     */
    @SuppressWarnings("unchecked")
    public <T> T convert(Class<T> targetType, Object sourceValue)
    {
        // We can't use Class#cast(Object) because ConvertUtils#convert always return Object form of the targetType even
        // if targetType is a primitive. When using casting syntax Object form is implicitly converter to proper
        // primitive type.
        return (T) ConvertUtils.convert(sourceValue, targetType);
    }
}
