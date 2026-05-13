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
package com.xpn.xwiki.objects.classes;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.mail.EmailAddressObfuscator;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.web.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EmailClass}.
 *
 * @version $Id$
 */
@ComponentTest
class EmailClassTest
{
    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private EmailAddressObfuscator emailAddressObfuscator;

    private final EmailClass emailClass = new EmailClass();

    @BeforeEach
    void setUp() throws Exception
    {
        Utils.setComponentManager(this.componentManager);
        this.emailAddressObfuscator = this.componentManager.registerMockComponent(EmailAddressObfuscator.class);
    }

    @Test
    void getObfuscatedValueWithValidEmail() throws AddressException
    {
        when(this.emailAddressObfuscator.obfuscate(InternetAddress.parse("john@doe.com")[0]))
            .thenReturn("j...@doe.com");
        assertEquals("j...@doe.com", this.emailClass.getObfuscatedValue("john@doe.com"));
    }

    @Test
    void getObfuscatedValueWithEmptyString()
    {
        assertEquals("", this.emailClass.getObfuscatedValue(""));
    }

    @Test
    void getObfuscatedValueWithNullValue()
    {
        assertNull(this.emailClass.getObfuscatedValue(null));
    }

    @Test
    void getObfuscatedValueWithMalformedAddress()
    {
        assertNull(this.emailClass.getObfuscatedValue("not a valid email"));
    }
}
