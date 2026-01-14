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

import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SlugEntityNameValidation}.
 *
 * @version $Id$
 */
@ComponentTest
class SlugEntityNameValidationTest
{
    private static final Set<String> STOP_WORDS = new TreeSet<>(Arrays.asList(
        "with", "and", "above", "after"
    ));

    @InjectMockComponents
    private SlugEntityNameValidation slugNameValidator;

    @MockComponent
    private SlugEntityNameValidationConfiguration configuration;

    @Test
    void transformation()
    {
        assertEquals("TeSt", this.slugNameValidator.transform("TeSt"));
        validateCommonCases();

        assertEquals("test-with-accents-and-special-characters",
            this.slugNameValidator.transform("test with âccents/and.special%characters"));
    }

    @Test
    void transformationWhenLowercaseAndDotsAllowedBetweenDigitsAndForbiddenWords()
    {
        when(this.configuration.isDotAllowedBetweenDigits()).thenReturn(true);
        when(this.configuration.convertToLowercase()).thenReturn(true);
        when(this.configuration.getForbiddenWords()).thenReturn(STOP_WORDS);

        assertEquals("test", this.slugNameValidator.transform("TeSt"));
        validateCommonCases();

        // Verify that dots are not transformed when between digits only.
        assertEquals("te-st", this.slugNameValidator.transform("te.st"));
        assertEquals("12.4.7", this.slugNameValidator.transform("12.4.7"));
        assertEquals("4-test", this.slugNameValidator.transform("4.test"));
        assertEquals("test-4", this.slugNameValidator.transform("test.4"));
        assertEquals("4-test", this.slugNameValidator.transform("4.test"));
        assertEquals("4", this.slugNameValidator.transform(".4"));
        assertEquals("4", this.slugNameValidator.transform("4."));

        // Verify that forbidden words are removed.
        assertEquals("test-accents-special-characters",
            this.slugNameValidator.transform("test with âccents/and.special%characters"));
        assertEquals("clouds",
            this.slugNameValidator.transform("ABOVE-clouds"));
        assertEquals("clouds",
            this.slugNameValidator.transform("clouds-after"));
    }

    @Test
    void isValid()
    {
        when(this.configuration.getForbiddenWords()).thenReturn(STOP_WORDS);

        assertTrue(this.slugNameValidator.isValid("TeSt"));
        isValidCommonCases();

        assertFalse(this.slugNameValidator.isValid("abc-3.14"));
        assertFalse(this.slugNameValidator.isValid("ver-1.2-rc3"));
        assertFalse(this.slugNameValidator.isValid("1.2.3"));
    }

    @Test
    void isValidWhenLowercaseAndDotsAllowedBetweenDigitsAndForbiddenWords()
    {
        when(this.configuration.isDotAllowedBetweenDigits()).thenReturn(true);
        when(this.configuration.convertToLowercase()).thenReturn(true);
        when(this.configuration.getForbiddenWords()).thenReturn(STOP_WORDS);

        assertFalse(this.slugNameValidator.isValid("TeSt"));
        isValidCommonCases();

        // Verify that dots are not transformed when between digits only.
        assertTrue(this.slugNameValidator.isValid("abc-3.14"));
        assertTrue(this.slugNameValidator.isValid("ver-1.2-rc3"));
        assertTrue(this.slugNameValidator.isValid("1.2.3"));
        assertFalse(this.slugNameValidator.isValid("abc..def"));
        assertFalse(this.slugNameValidator.isValid("abc-.def"));
        assertFalse(this.slugNameValidator.isValid("file.name"));
        assertFalse(this.slugNameValidator.isValid("1."));
        assertFalse(this.slugNameValidator.isValid(".1"));

        // Verify that forbidden words are removed.
        assertFalse(this.slugNameValidator.isValid("test-with-accents"));
        assertFalse(this.slugNameValidator.isValid("above-clouds"));
        assertFalse(this.slugNameValidator.isValid("clouds-after"));
    }

    private void isValidCommonCases()
    {
        assertFalse(this.slugNameValidator.isValid("tést"));
        assertFalse(this.slugNameValidator.isValid("test âccents/many.special%characters"));
        assertTrue(this.slugNameValidator.isValid("test-accents-many-special-characters"));
        assertFalse(this.slugNameValidator.isValid("test---many-forbidden---"));
        assertFalse(this.slugNameValidator.isValid("-test-many-forbidden"));
        assertFalse(this.slugNameValidator.isValid("test-many-forbidden-"));
        assertTrue(this.slugNameValidator.isValid("test-many-forbidden"));
        assertTrue(this.slugNameValidator.isValid("1test-many-forbidden"));
        assertTrue(this.slugNameValidator.isValid("1-2-test"));
        assertTrue(this.slugNameValidator.isValid("t"));
    }

    private void validateCommonCases()
    {
        assertEquals("test", this.slugNameValidator.transform("tést"));
        assertEquals("test-accents-many-special-characters",
            this.slugNameValidator.transform("test âccents/many.special%characters"));
        assertEquals("test-many-forbidden",
            this.slugNameValidator.transform("+test | many forbidden:.?"));
        assertEquals("test-many-forbidden",
            this.slugNameValidator.transform("-test---many-forbidden---"));
        assertEquals("1-test-many-forbidden",
            this.slugNameValidator.transform("1-test---many-forbidden---"));
        assertEquals("x", this.slugNameValidator.transform("-- x --"));
        assertEquals("_-_", this.slugNameValidator.transform("¯\\_(ツ)_/¯"));
    }
}
