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
package org.xwiki.store.hibernate.internal.bridge;

import java.lang.reflect.Array;

/**
 * Runtime helpers called by the bridge methods generated in the {@code jakarta.persistence} interfaces to convert the
 * values between a {@code javax.persistence} type and its {@code jakarta.persistence} twin.
 * <p>
 * The interfaces don't need any conversion since each {@code jakarta.persistence} interface also implements its
 * {@code javax.persistence} twin: only the enums and the arrays need it.
 *
 * @version $Id$
 */
public final class PackageBridgeUtils
{
    private PackageBridgeUtils()
    {
        // Utility class
    }

    /**
     * @param value the enum constant to convert
     * @param target the enum type to convert to
     * @return the constant of the target enum with the same name
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object toEnum(Object value, Class<?> target)
    {
        if (value == null) {
            return null;
        }

        return Enum.valueOf((Class) target, ((Enum<?>) value).name());
    }

    /**
     * @param value the array to convert
     * @param componentType the component type of the array to return
     * @return a new array containing the converted elements
     */
    public static Object[] toArray(Object[] value, Class<?> componentType)
    {
        if (value == null) {
            return null;
        }

        Object[] result = (Object[]) Array.newInstance(componentType, value.length);
        for (int i = 0; i < value.length; ++i) {
            result[i] = componentType.isEnum() ? toEnum(value[i], componentType) : componentType.cast(value[i]);
        }

        return result;
    }
}
