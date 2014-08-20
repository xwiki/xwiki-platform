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

import java.lang.reflect.Type;

import javax.inject.Singleton;
import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.xwiki.component.annotation.Component;
import org.xwiki.properties.converter.AbstractConverter;
import org.xwiki.properties.converter.ConversionException;

/**
 * Converts a String to a {@link javax.mail.Address} instance. Useful when using the Mail Sender
 * scripting API to add a recipient.
 *
 * @version $Id$
 * @since 6.2M1
 */
@Component
@Singleton
public class AddressConverter extends AbstractConverter<Address>
{
    @Override
    protected Address convertToType(Type targetType, Object value)
    {
        if (value == null) {
            return null;
        }

        if (value instanceof Address) {
            return (Address) value;
        }

        Address address;

        try {
            address = InternetAddress.parse(value.toString())[0];
        } catch (AddressException e) {
            throw new ConversionException(
                String.format("Failed to convert [%s] to [%s]", value, Address.class.getName()), e);
        }

        return address;
    }

    @Override
    protected String convertToString(Address value)
    {
        return InternetAddress.toString(new Address[] {value});
    }
}
