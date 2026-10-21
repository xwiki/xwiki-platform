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
package org.xwiki.security.internal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link XWikiLegacyPasswordEncoder}.
 *
 * @version $Id$
 */
class XWikiLegacyPasswordEncoderTest
{
    // Both hash are for the word "test".
    private static final String TEST_SHA512_SALTED_LEGACY_ENCODED =
        "hash:SHA-512:706103959a3e9080e0c0832619d3ccda8e0d5b45008bc248773788d0c7a7662f:"
            + "d2321d4208fbff2f74f6476df9a79bc03b79a831f9d6741b8e928380bbbfa9912590c0436e174"
            + "94b67f788b0065710bb4d4b7d08e341d14c6e83e48088050832";
    private static final String TEST_SHA1_NO_SALT_LEGACY_ENCODED =
        "hash:SHA-1:a94a8fe5ccb19ba61c4c0873d391e987982fbbd3";

    XWikiLegacyPasswordEncoder legacyPasswordEncoder;

    @BeforeEach
    void beforeEach()
    {
        this.legacyPasswordEncoder = new XWikiLegacyPasswordEncoder();
    }

    @Test
    void reencodePassword()
    {
        String reencodedPassword = this.legacyPasswordEncoder.reencodePassword(TEST_SHA512_SALTED_LEGACY_ENCODED);
        // Example of expected result:
        // {XWikiLegacy}SHA-512:706103959a3e9080e0c0832619d3ccda8e0d5b45008bc248773788d0c7a7662f:
        // $argon2id$v=19$m=16384,t=2,p=1$QVsyPj7+Hge83FRyCMEn8Q$9PihtCQzGXcwOCc3BqK6exawnVB/nQA4fHupdFPxGx0
        String expectedPrefix = "{XWikiLegacy}SHA-512:706103959a3e9080e0c0832619d3ccda8e0d5b45008bc248773788d0c7a7662f:"
            + "$argon2id$v=19$m=16384,t=2,p=1$";
        assertTrue(reencodedPassword.startsWith(expectedPrefix), String.format("Obtained password [%s] doesn't start "
            + "with [%s]", reencodedPassword, expectedPrefix));
    }

    @Test
    void matchesLegacy()
    {
        assertTrue(this.legacyPasswordEncoder.matchesLegacy("test", TEST_SHA1_NO_SALT_LEGACY_ENCODED));
        assertTrue(this.legacyPasswordEncoder.matchesLegacy("test", TEST_SHA512_SALTED_LEGACY_ENCODED));
        assertFalse(this.legacyPasswordEncoder.matchesLegacy("test", TEST_SHA512_SALTED_LEGACY_ENCODED + "f23"));
    }

    @Test
    void matchesNonNull()
    {
        String reencodedNoSaltSHA1 = "SHA-1::$argon2id$v=19$m=16384,t=2,"
            + "p=1$b/afMMqK2xjq3kbd0qvjKg$voMNn8bxQipNP4tZT7/oGUMAt+XoBYgZykSVvsvEkpQ";
        String reencodedSaltSHA512 = "SHA-512:706103959a3e9080e0c0832619d3ccda8e0d5b45008bc248773788d0c7a7662f:"
            + "$argon2id$v=19$m=16384,t=2,p=1$qWImJ8NYvAbVRHpZAwum5g$O6T+9O0gHl1pBwC/i8S2x/fhdZBOkbE1G3LaM+YARhE";
        assertTrue(legacyPasswordEncoder.matches("test", reencodedNoSaltSHA1));
        assertTrue(legacyPasswordEncoder.matches("test", reencodedSaltSHA512));
        assertFalse(legacyPasswordEncoder.matches("test", "hash:SHA-1:a94a8fe5ccb19ba61c4c0873d391e987982fbbd3"));
    }
}