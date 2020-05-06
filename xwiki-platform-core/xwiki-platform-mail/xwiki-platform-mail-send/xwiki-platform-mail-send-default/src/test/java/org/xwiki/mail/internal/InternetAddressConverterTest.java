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

import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.Test;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link InternetAddressConverter}.
 *
 * @version $Id$
 * @since 12.4RC1
 */
@ComponentTest
public class InternetAddressConverterTest
{
    @InjectMockComponents
    private InternetAddressConverter converter;

    @Test
    public void convert() throws Exception
    {
        InternetAddress address = new InternetAddress("John Doe(comment) <john1@doe.com>");
        assertEquals(address, this.converter.convert(InternetAddress.class, "John Doe(comment) <john1@doe.com>"));
    }

    @Test
    void convertWhenNull()
    {
        assertNull(this.converter.convert(InternetAddress.class, null));
    }

    @Test
    void convertWhenTypeIsAlreadyAnAddress() throws Exception
    {
        InternetAddress address = new InternetAddress("John Doe(comment) <john1@doe.com>");
        assertEquals(address, this.converter.convert(InternetAddress.class, address));
    }

    @Test
    void convertWhenInvalid()
    {
        Throwable exception = assertThrows(ConversionException.class,
            () -> this.converter.convert(InternetAddress.class, "invalid("));
        assertEquals("Failed to convert [invalid(] to [javax.mail.internet.InternetAddress]", exception.getMessage());
    }
}
