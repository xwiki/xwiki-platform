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
import org.xwiki.annotation.internal.content.WhiteSpaceContentAlterer;
import org.xwiki.annotation.internal.content.filter.WhiteSpaceFilter;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.of;

/**
 * @version $Id$
 * @since 2.3M1
 */
@ComponentTest
@ComponentList({ WhiteSpaceFilter.class })
public class WhiteSpaceContentAltererTest
{
    /**
     * The content alterer to test.
     */
    @InjectMockComponents
    private WhiteSpaceContentAlterer alterer;

    /**
     * @return list of corpus files to instantiate tests for
     */
    public static Stream<Arguments> filteringSource()
    {
        return Stream.of(
            // unbreakable space
            of("not\u00A0to be", "nottobe"),
            // tabs
            of("to be or not\tto be", "tobeornottobe"),
            // commas, signs with regular spaces
            of("roses, see I in her cheeks;", "roses,seeIinhercheeks;"),
            // new lines
            of("eyes nothing\nlike the sun", "eyesnothinglikethesun"),
            // new line carriage return
            of("eyes\n\rnothing", "eyesnothing")
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
}
