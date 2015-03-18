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

import javax.mail.Address;
import javax.mail.internet.InternetAddress;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.AddressConverter}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class AddressesConverterTest
{
    @Rule
    public MockitoComponentMockingRule<AddressesConverter> mocker =
        new MockitoComponentMockingRule<>(AddressesConverter.class);

    @Test
    public void convert() throws Exception
    {
        InternetAddress[] addresses = new InternetAddress[2];
        addresses[0] = new InternetAddress("John Doe(comment) <john1@doe.com>");
        addresses[1] = new InternetAddress("john2@doe.com");
        assertArrayEquals(addresses, (InternetAddress[]) this.mocker.getComponentUnderTest().convert(Address[].class,
            "John Doe(comment) <john1@doe.com>,john2@doe.com"));
    }

    @Test
    public void convertWhenNull() throws Exception
    {
        assertNull(this.mocker.getComponentUnderTest().convert(Address.class, null));
    }

    @Test
    public void convertWhenTypeIsAlreadyAnAddressArray() throws Exception
    {
        InternetAddress[] addresses = new InternetAddress[2];
        addresses[0] = new InternetAddress("John Doe(comment) <john1@doe.com>");
        addresses[1] = new InternetAddress("john2@doe.com");
        assertArrayEquals(addresses,
            (InternetAddress[]) this.mocker.getComponentUnderTest().convert(Address.class, addresses));
    }

    @Test
    public void convertWhenInvalid() throws Exception
    {
        try {
            this.mocker.getComponentUnderTest().convert(Address[].class, "invalid(");
            fail("Should have thrown an exception here");
        } catch (ConversionException expected) {
            assertEquals("Failed to convert [invalid(] to an array of [javax.mail.Address]", expected.getMessage());
        }
    }
}
