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

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ComponentTest
public class SlugNameStrategyTest
{
    @InjectMockComponents
    private SlugNameStrategy slugNameValidator;

    @Test
    public void transformation()
    {
        assertEquals("test", slugNameValidator.transform("test"));
        assertEquals("test", slugNameValidator.transform("tést"));
        assertEquals("test-with-accents-and-special-characters",
            slugNameValidator.transform("test with âccents/and.special%characters"));
        assertEquals("test-many-forbidden",
            slugNameValidator.transform("+test | many forbidden:.?"));
        assertEquals("test-many-forbidden",
            slugNameValidator.transform("-test---many-forbidden---"));
        assertEquals("1-test-many-forbidden",
            slugNameValidator.transform("1-test---many-forbidden---"));
    }

    @Test
    public void isValid()
    {
        assertTrue(slugNameValidator.isValid("test"));
        assertFalse(slugNameValidator.isValid("tést"));
        assertFalse(slugNameValidator.isValid("test with âccents/and.special%characters"));
        assertTrue(slugNameValidator.isValid("test-with-accents-and-special-characters"));
        assertFalse(slugNameValidator.isValid("test---many-forbidden---"));
        assertFalse(slugNameValidator.isValid("-test-many-forbidden"));
        assertFalse(slugNameValidator.isValid("test-many-forbidden-"));
        assertTrue(slugNameValidator.isValid("test-many-forbidden"));
        assertTrue(slugNameValidator.isValid("1test-many-forbidden"));
    }
}
