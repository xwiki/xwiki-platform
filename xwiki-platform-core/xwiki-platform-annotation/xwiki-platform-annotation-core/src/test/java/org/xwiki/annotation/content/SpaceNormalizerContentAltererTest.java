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
package org.xwiki.annotation.content;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.xwiki.annotation.internal.content.SpaceNormalizerContentAlterer;
import org.xwiki.annotation.internal.content.filter.WhiteSpaceFilter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * Tests the {@link SpaceNormalizerContentAlterer}.
 *
 * @version $Id$
 * @since 2.3M1
 */
@ComponentTest
@ComponentList({ WhiteSpaceFilter.class })
class SpaceNormalizerContentAltererTest
{
    /**
     * The content alterer to test.
     */
    @InjectMockComponents
    private SpaceNormalizerContentAlterer alterer;

    public static Stream<Arguments> filteringSource()
    {
        return Stream.of(
            // unbreakable space
            of("not\u00A0to be", "not to be"),
            // tabs
            of("to be or not\tto be", "to be or not to be"),
            // commas, signs with regular spaces
            of("roses, see I in her cheeks;", "roses, see I in her cheeks;"),
            // new lines
            of("eyes nothing\nlike the sun", "eyes nothing like the sun"),
            // new line carriage return
            of("eyes\n\rnothing", "eyes nothing"),
            // multiple spaces one after the other
            of("roses, see I   in her cheeks;", "roses, see I in her cheeks;"),
            of("roses, see I\u00A0  in her cheeks;", "roses, see I in her cheeks;"),
            of("roses, see I\n  \n in her cheeks;", "roses, see I in her cheeks;"),
            // trim
            of(" roses, see I in her cheeks; ", "roses, see I in her cheeks;"),
            of("\n\n\nroses, see I in her cheeks;", "roses, see I in her cheeks;"),
            of("roses, see I in her cheeks;\n\n", "roses, see I in her cheeks;"),
            // starting or ending with a non-breakable space
            of("\u00A0roses, see I in her cheeks;", "roses, see I in her cheeks;"),
            of("roses, see I in her cheeks;\u00A0", "roses, see I in her cheeks;"),

            // empty string should stay empty string
            of("", ""),
            // spaces only string should become empty string
            of(" \t\n", "")
        );
    }

    /**
     * Tests that the content alterer filters correctly the characters out of the Strings.
     */
    @ParameterizedTest
    @MethodSource("filteringSource")
    public void filtering(String initial, String altered)
    {
        AlteredContent alteredContent = this.alterer.alter(initial);
        assertEquals(altered, alteredContent.getContent().toString());
    }

    // TODO: test indexes to be in the right place
}
