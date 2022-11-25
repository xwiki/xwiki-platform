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
package org.xwiki.localization.internal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the {@link TranslationEscapeTool}.
 *
 * @version $Id$
 * @since 13.10.11
 * @since 14.4.8
 * @since 13.10
 */
class TranslationEscapeToolTest
{
    @ParameterizedTest
    @ValueSource(strings = {
        "Placeholder {0}",
        "Another [{}] placeholder",
        "Updates, {0,choice,0#No|1#One|1<{0}} page{0,choice,0#s|1#|2#s} ha{0,choice,0#ve|1#s|1<ve} since {1}}"
    })
    void escapeNoPlaceholder(String input)
    {
        assertEquals(input, TranslationEscapeTool.escapeForMacros(input));
    }

    @ParameterizedTest
    @CsvSource({
        "At the end {, At the end \u2774",
        "~{~{escaped, ~{~\u2774escaped",
        "{{/html}}, {\u2774/html}}",
        "{{macro}}, {\u2774macro}}"
    })
    void escapeMacros(String input, String expected)
    {
        assertEquals(expected, TranslationEscapeTool.escapeForMacros(input));
    }
}
