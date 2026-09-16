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

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link NumberClass} class.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class NumberClassTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    /** Test the fromString method. */
    @Test
    void fromString() throws XWikiException
    {
        // Create a default Number property
        NumberClass nc = new NumberClass();
        BaseClass bc = new BaseClass();
        bc.setName("Some.Class");
        nc.setObject(bc);

        when(this.contextualLocalizationManager.getTranslationPlain(
            "core.model.xclass.classProperty.error.invalidNumberFormat", "asd", "long"))
                .thenReturn("The value \"asd\" is not a valid number of type \"long\".");
        when(this.contextualLocalizationManager.getTranslationPlain(
            "core.model.xclass.classProperty.error.invalidNumberFormat", "1111111111111111111111111111111111",
            "long")).thenReturn("The value \"1111111111111111111111111111111111\" is not a valid number of type "
                + "\"long\".");

        // A String value containing non-numeric characters can not be represented as a numeric value, so this
        // should throw an exception
        XWikiException xWikiException = assertThrows(XWikiException.class, () -> nc.fromString("asd"));
        assertEquals(XWikiException.MODULE_XWIKI_CLASSES, xWikiException.getModule());
        assertEquals(XWikiException.ERROR_XWIKI_CLASSES_FIELD_INVALID, xWikiException.getCode());
        verify(this.contextualLocalizationManager)
            .getTranslationPlain("core.model.xclass.classProperty.error.invalidNumberFormat", "asd", "long");

        // A much too long number cannot be represented as a long value, so this should throw an exception
        xWikiException = assertThrows(XWikiException.class, () -> nc.fromString("1111111111111111111111111111111111"));
        assertEquals(XWikiException.MODULE_XWIKI_CLASSES, xWikiException.getModule());
        assertEquals(XWikiException.ERROR_XWIKI_CLASSES_FIELD_INVALID, xWikiException.getCode());
        verify(this.contextualLocalizationManager).getTranslationPlain(
            "core.model.xclass.classProperty.error.invalidNumberFormat", "1111111111111111111111111111111111",
            "long");

        BaseProperty p;

        // A null value should lead to creating an object with an empty value
        p = nc.fromString(null);
        assertNotNull(p);
        assertNull(p.getValue());

        // An empty String should lead to creating an object with an empty value
        p = nc.fromString("");
        assertNotNull(p);
        assertNull(p.getValue());

        // An integer value should lead to creating an object containing that integer as value
        p = nc.fromString("4");
        assertNotNull(p);
        assertEquals(4, Integer.parseInt(p.getValue().toString()));
    }

    /**
     * Arguments for {@link #displayEdit(String, String, String, String)}: number type, expected {@code step}, and
     * expected {@code min}/{@code max} ({@code null} for the types that don't get a range).
     */
    private static Stream<Arguments> numberTypes()
    {
        return Stream.of(
            Arguments.of(NumberClass.TYPE_INTEGER, "1", String.valueOf(Integer.MIN_VALUE),
                String.valueOf(Integer.MAX_VALUE)),
            Arguments.of(NumberClass.TYPE_LONG, "1", null, null),
            Arguments.of(NumberClass.TYPE_FLOAT, "any", null, null),
            Arguments.of(NumberClass.TYPE_DOUBLE, "any", null, null));
    }

    /** Test that displayEdit() sets the HTML5 validation attributes matching the number type. */
    @ParameterizedTest
    @MethodSource("numberTypes")
    void displayEdit(String numberType, String expectedStep, String expectedMin, String expectedMax)
    {
        NumberClass nc = new NumberClass();
        BaseClass bc = new BaseClass();
        bc.setName("Some.Class");
        nc.setObject(bc);
        nc.setName("number1");
        nc.setNumberType(numberType);

        when(this.contextualLocalizationManager.getTranslationPlain("core.validation.number.message.invalidformat"))
            .thenReturn("Please enter a valid number.");
        when(this.contextualLocalizationManager
            .getTranslationPlain("core.validation.number.message.wholenumberrequired"))
            .thenReturn("Please enter a whole number.");
        if (expectedMin != null) {
            when(this.contextualLocalizationManager.getTranslationPlain("core.validation.number.message.outofrange",
                expectedMin, expectedMax)).thenReturn("Out of range.");
        }

        StringBuffer buffer = new StringBuffer();
        nc.displayEdit(buffer, "number1", "prefix_", bc, this.oldcore.getXWikiContext());
        String html = buffer.toString();

        assertTrue(html.contains("type='number'"), html);
        assertTrue(html.contains("step='" + expectedStep + "'"), html);
        assertTrue(html.contains("data-validation-bad-input='Please enter a valid number.'"), html);
        if ("1".equals(expectedStep)) {
            assertTrue(html.contains("data-validation-step-mismatch='Please enter a whole number.'"), html);
        } else {
            assertFalse(html.contains("data-validation-step-mismatch"), html);
        }
        if (expectedMin != null) {
            assertTrue(html.contains("min='" + expectedMin + "'"), html);
            assertTrue(html.contains("max='" + expectedMax + "'"), html);
            assertTrue(html.contains("data-validation-range-overflow='Out of range.'"), html);
            assertTrue(html.contains("data-validation-range-underflow='Out of range.'"), html);
        } else {
            assertFalse(html.contains("min="), html);
            assertFalse(html.contains("max="), html);
            assertFalse(html.contains("data-validation-range-overflow"), html);
            assertFalse(html.contains("data-validation-range-underflow"), html);
        }
    }
}
