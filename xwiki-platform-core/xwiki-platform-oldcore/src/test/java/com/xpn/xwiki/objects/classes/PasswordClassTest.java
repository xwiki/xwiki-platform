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

    @Test
    void arePasswordsMatchingWithLegacyHash()
    {
        // Legacy hash of PLAIN_PASSWORD, using SHA-512 and the salt "abcd", not re-encoded by
        // XWikiLegacyPasswordEncoder yet.
        String legacyHash = "hash:SHA-512:abcd:b352183394a5006c92614d401e22ef6ace71a3c46b3ef4109508fb59711282cd011d7735"
            + "da75dc6672ba8e413887d30a89e33aa30e2fdec1a562128425408981";

        assertTrue(this.passwordClass.arePasswordsMatching(PLAIN_PASSWORD, legacyHash));

        // The log must name the legacy algorithm the password relies on, not the one used for re-encoded passwords.
        assertEquals(1, this.logCapture.size());
        assertEquals("An outdated algorithm [SHA-512] (or an outdated version of it) is used in a PasswordClass "
            + "property not yet attached to an object", this.logCapture.getMessage(0));
    }
}
