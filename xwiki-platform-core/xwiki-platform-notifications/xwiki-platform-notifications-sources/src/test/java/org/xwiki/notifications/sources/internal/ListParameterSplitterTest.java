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
package org.xwiki.notifications.sources.internal;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link ListParameterSplitter}.
 *
 * @version $Id$
 */
class ListParameterSplitterTest
{
    @Test
    void splitBlankValue()
    {
        assertEquals(List.of(), ListParameterSplitter.split(null));
        assertEquals(List.of(), ListParameterSplitter.split(" "));
    }

    @Test
    void splitUnquotedValues()
    {
        assertEquals(List.of("a", "b c", "It's"), ListParameterSplitter.split("a, b c ,It's,,"));
    }

    @Test
    void splitQuotedValues()
    {
        assertEquals(List.of("My, Space", "It's", "Say \"Hi\"", "a\\b", "{Draft}"),
            ListParameterSplitter.split("\"My, Space\", 'It\\'s',\"Say \\\"Hi\\\"\",\"a\\\\b\",\"{Draft}\""));
    }

    @Test
    void splitTextAfterClosingQuote()
    {
        assertEquals(List.of("a", "b", "c", "x", "y"), ListParameterSplitter.split("\"a\"b,c,\"x\" \"y\""));
    }

    @Test
    void splitQuotedValueWithoutClosingQuote()
    {
        assertEquals(List.of("a", "b, c"), ListParameterSplitter.split("a,\"b, c"));
    }
}
