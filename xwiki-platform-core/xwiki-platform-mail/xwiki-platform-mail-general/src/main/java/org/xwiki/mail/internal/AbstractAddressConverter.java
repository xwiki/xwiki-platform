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
package org.xwiki.mail.internal;

import java.lang.reflect.Array;
import java.lang.reflect.Type;

import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

/**
 * Converts a String to a {@link InternetAddress} instance when the target type asked is of type {@code T}.
 *
 * @param <T> the type of the target Address
 * @version $Id$
 * @since 12.4RC1
 */
public abstract class AbstractAddressConverter<T extends Address> extends AbstractConverter<T>
{
    @Override
    protected Address convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        Address address;

        try {
            address = InternetAddress.parse(value.toString())[0];
        } catch (AddressException e) {
            throw new ConversionException(
                String.format("Failed to convert [%s] to [%s]", value, targetType.getTypeName()), e);
        }

        return address;
    }

    @Override
    protected String convertToString(T value)
    {
        T[] array = (T[]) Array.newInstance(value.getClass(), 1);
        array[0] = value;
        return InternetAddress.toString(array);
    }
}
