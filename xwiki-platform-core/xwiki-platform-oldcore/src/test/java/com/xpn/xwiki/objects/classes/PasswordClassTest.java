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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.meta.PasswordMetaClass;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link PasswordClass}.
 *
 * @version $Id$
 */
class PasswordClassTest
{
    private static final String PLAIN_PASSWORD = "secret";

    private final PasswordClass passwordClass = new PasswordClass();

    @RegisterExtension
    private final LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @Test
    void fromStringWithFormPlaceholder() throws Exception
    {
        // The edit form displays a placeholder instead of the real password: submitting it back must not replace the
        // stored password.
        assertNull(this.passwordClass.fromString(PasswordClass.FORM_PASSWORD_PLACEHODLER));
    }

    @Test
    void fromStringWithFormPlaceholderAndClearStorage() throws Exception
    {
        this.passwordClass.setStorageType(PasswordMetaClass.CLEAR);

        assertNull(this.passwordClass.fromString(PasswordClass.FORM_PASSWORD_PLACEHODLER));
    }

    @Test
    void fromStringWithEmptyValue() throws Exception
    {
        BaseProperty property = this.passwordClass.fromString("");

        assertEquals("", property.getValue());
    }

    @Test
    void fromStringWithAlreadyHashedValue() throws Exception
    {
        String hash = this.passwordClass.getPasswordHash(PLAIN_PASSWORD, PasswordClass.BCRYPT_ALGORITHM);

        BaseProperty property = this.passwordClass.fromString(hash);

        assertEquals(hash, property.getValue());
    }

    @Test
    void fromStringWithPlainValueAndHashStorage() throws Exception
    {
        BaseProperty property = this.passwordClass.fromString(PLAIN_PASSWORD);

        String storedValue = (String) property.getValue();
        assertNotEquals(PLAIN_PASSWORD, storedValue);
        assertTrue(storedValue.startsWith("{" + PasswordClass.ARGON2_ALGORITHM + "}"));
        assertTrue(this.passwordClass.arePasswordsMatching(PLAIN_PASSWORD, storedValue));
    }

    @Test
    void fromStringWithPlainValueAndClearStorage() throws Exception
    {
        this.passwordClass.setStorageType(PasswordMetaClass.CLEAR);

        BaseProperty property = this.passwordClass.fromString(PLAIN_PASSWORD);

        assertEquals(PLAIN_PASSWORD, property.getValue());
    }

    @Test
    void getPasswordHashWithDeprecatedAlgorithm()
    {
        String hash = this.passwordClass.getPasswordHash(PLAIN_PASSWORD, PasswordClass.SHA_256_ALGORITHM);

        assertTrue(this.passwordClass.arePasswordsMatching(PLAIN_PASSWORD, hash));

        // The usage of the deprecated algorithm is logged once for computing the hash, and once for checking it.
        assertEquals(2, this.logCapture.size());
        assertEquals("An outdated algorithm [SHA-256] (or an outdated version of it) is used in a PasswordClass "
            + "property not yet attached to an object", this.logCapture.getMessage(0));
        assertEquals(this.logCapture.getMessage(0), this.logCapture.getMessage(1));
    }
}
