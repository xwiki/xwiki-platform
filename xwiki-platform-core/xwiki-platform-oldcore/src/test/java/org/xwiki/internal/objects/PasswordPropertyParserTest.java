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
package org.xwiki.internal.objects;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PasswordProperty;
import com.xpn.xwiki.objects.classes.PasswordClass;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PasswordPropertyParser}.
 *
 * @version $Id$
 */
@ComponentTest
class PasswordPropertyParserTest
{
    @InjectMockComponents
    private PasswordPropertyParser parser;

    @Test
    void fromString() throws XWikiException
    {
        String value = "test";

        BaseProperty<?> property = parser.fromString(value);

        // We cannot check that password property are equals because we use random salt.
        assertInstanceOf(PasswordProperty.class, property);
        PasswordProperty passwordProperty = (PasswordProperty) property;

        // here the password is in clear
        assertEquals("test", passwordProperty.getValue());

        // hashed test value
        value = "hash:SHA-512:706103959a3e9080e0c0832619d3ccda8e0d5b45008bc248773788d0c7a7662f:"
            + "d2321d4208fbff2f74f6476df9a79bc03b79a831f9d6741b8e928380bbbfa9912590c0436e17494b67f788b0065710bb4d4b7d08"
            + "e341d14c6e83e48088050832";

        property = parser.fromValue(value);
        PasswordClass passwordClass = new PasswordClass();
        assertInstanceOf(PasswordProperty.class, property);
        passwordProperty = (PasswordProperty) property;
        assertEquals(passwordProperty.getValue(), passwordClass.getEquivalentPassword(passwordProperty.getValue(),
            "test"));
    }
}