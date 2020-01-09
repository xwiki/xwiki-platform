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
package org.xwiki.namestrategies.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ComponentTest
public class ReplaceCharacterNameStrategyTest
{
    @InjectMockComponents
    private ReplaceCharacterNameStrategy replaceCharacterValidator;

    @BeforeEach
    public void setup()
    {
        this.replaceCharacterValidator.setReplacementCharacters(Collections.emptyMap());
    }

    @Test
    public void transformation()
    {
        assertEquals("test", replaceCharacterValidator.transform("test"));
        assertEquals("tést", replaceCharacterValidator.transform("tést"));
        assertEquals("test with âccents/and.special%characters",
            replaceCharacterValidator.transform("test with âccents/and.special%characters"));

        this.replaceCharacterValidator.setReplacementCharacters(Collections.singletonMap("/", " "));
        assertEquals("test", replaceCharacterValidator.transform("test"));
        assertEquals("tést", replaceCharacterValidator.transform("tést"));
        assertEquals("test with âccents and.special%characters",
            replaceCharacterValidator.transform("test with âccents/and.special%characters"));

        this.replaceCharacterValidator.setReplacementCharacters(Collections.singletonMap("/", null));
        assertEquals("test", replaceCharacterValidator.transform("test"));
        assertEquals("tést", replaceCharacterValidator.transform("tést"));
        assertEquals("test with âccentsand.special%characters",
            replaceCharacterValidator.transform("test with âccents/and.special%characters"));

        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("/", null);
        replaceMap.put(" ", "-");
        replaceMap.put("%", "-");
        this.replaceCharacterValidator.setReplacementCharacters(replaceMap);
        assertEquals("test", replaceCharacterValidator.transform("test"));
        assertEquals("tést", replaceCharacterValidator.transform("tést"));
        assertEquals("test-with-âccentsand.special-characters",
            replaceCharacterValidator.transform("test with âccents/and.special%characters"));
    }

    @Test
    public void isValid()
    {
        assertTrue(replaceCharacterValidator.isValid("test"));
        assertTrue(replaceCharacterValidator.isValid("tést"));
        assertTrue(replaceCharacterValidator.isValid("test with âccents/and.special%characters"));
        assertTrue(replaceCharacterValidator.isValid("test with accents and special characters"));
        assertTrue(replaceCharacterValidator.isValid("test-with-accents-and-special-characters"));

        this.replaceCharacterValidator.setReplacementCharacters(Collections.singletonMap("/", " "));
        assertTrue(replaceCharacterValidator.isValid("test"));
        assertTrue(replaceCharacterValidator.isValid("tést"));
        assertFalse(replaceCharacterValidator.isValid("test with âccents/and.special%characters"));
        assertTrue(replaceCharacterValidator.isValid("test with accents and special characters"));
        assertTrue(replaceCharacterValidator.isValid("test-with-accents-and-special-characters"));

        this.replaceCharacterValidator.setReplacementCharacters(Collections.singletonMap("/", null));
        assertTrue(replaceCharacterValidator.isValid("test"));
        assertTrue(replaceCharacterValidator.isValid("tést"));
        assertFalse(replaceCharacterValidator.isValid("test with âccents/and.special%characters"));
        assertTrue(replaceCharacterValidator.isValid("test with accents and special characters"));
        assertTrue(replaceCharacterValidator.isValid("test-with-accents-and-special-characters"));

        Map<String, String> replaceMap = new HashMap<>();
        replaceMap.put("/", null);
        replaceMap.put(" ", "-");
        replaceMap.put("%", "-");
        this.replaceCharacterValidator.setReplacementCharacters(replaceMap);
        assertTrue(replaceCharacterValidator.isValid("test"));
        assertTrue(replaceCharacterValidator.isValid("tést"));
        assertFalse(replaceCharacterValidator.isValid("test with âccents/and.special%characters"));
        assertFalse(replaceCharacterValidator.isValid("test with accents and special characters"));
        assertTrue(replaceCharacterValidator.isValid("test-with-accents-and-special-characters"));
    }
}
