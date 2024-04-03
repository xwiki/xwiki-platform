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
package org.xwiki.model.validation.internal;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ComponentTest
class SlugEntityNameValidationTest
{
    @InjectMockComponents
    private SlugEntityNameValidation slugNameValidator;

    @Test
    void transformation()
    {
        assertEquals("test", this.slugNameValidator.transform("test"));
        assertEquals("test", this.slugNameValidator.transform("tést"));
        assertEquals("test-with-accents-and-special-characters",
            this.slugNameValidator.transform("test with âccents/and.special%characters"));
        assertEquals("test-many-forbidden",
            this.slugNameValidator.transform("+test | many forbidden:.?"));
        assertEquals("test-many-forbidden",
            this.slugNameValidator.transform("-test---many-forbidden---"));
        assertEquals("1-test-many-forbidden",
            this.slugNameValidator.transform("1-test---many-forbidden---"));
        assertEquals("x", this.slugNameValidator.transform("-- x --"));
        assertEquals("_-_", this.slugNameValidator.transform("¯\\_(ツ)_/¯"));
    }

    @Test
    void isValid()
    {
        assertTrue(this.slugNameValidator.isValid("test"));
        assertFalse(this.slugNameValidator.isValid("tést"));
        assertFalse(this.slugNameValidator.isValid("test with âccents/and.special%characters"));
        assertTrue(this.slugNameValidator.isValid("test-with-accents-and-special-characters"));
        assertFalse(this.slugNameValidator.isValid("test---many-forbidden---"));
        assertFalse(this.slugNameValidator.isValid("-test-many-forbidden"));
        assertFalse(this.slugNameValidator.isValid("test-many-forbidden-"));
        assertTrue(this.slugNameValidator.isValid("test-many-forbidden"));
        assertTrue(this.slugNameValidator.isValid("1test-many-forbidden"));
        assertTrue(this.slugNameValidator.isValid("1-2-Test"));
        assertTrue(this.slugNameValidator.isValid("t"));
    }
}
