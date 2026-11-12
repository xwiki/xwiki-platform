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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Splits the value of a notification parameter holding a list, like the wikis, spaces, pages, tags or users to display
 * the notifications of. The values are separated with commas, and a value can be put in double or single quotes, a
 * backslash escaping the next character inside them. This is how the macro editor writes a value holding a comma, a
 * whitespace, a quote or a brace. Unlike the conversion of a collection macro parameter, the values aren't split on
 * whitespaces, so that a value written without quotes is kept whole.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
final class ListParameterSplitter
{
    private static final char SEPARATOR = ',';

    private static final char ESCAPE = '\\';

    private ListParameterSplitter()
    {
    }

    /**
     * @param text the value of the parameter
     * @return the values it holds, without their quotes and escaping, empty values excluded
     */
    static List<String> split(String text)
    {
        List<String> values = new ArrayList<>();
        if (StringUtils.isNotBlank(text)) {
            int start = 0;
            while (start < text.length()) {
                start = readValue(text, start, values);
            }
        }
        return values;
    }

    /**
     * Reads the value that starts at the given index. A value without quotes ends at the next comma. A value put in
     * quotes ends at its closing quote, and the text that follows it before the next comma is read as another value.
     *
     * @param text the text to read from
     * @param start the index where the value starts, whitespaces included
     * @param values where to add the value, unless it is empty
     * @return the index where the next value starts, or the length of the text when it is the last value
     */
    private static int readValue(String text, int start, List<String> values)
    {
        int index = skipWhitespaces(text, start);
        String value;
        int next;
        if (index < text.length() && isQuote(text.charAt(index))) {
            StringBuilder quotedValue = new StringBuilder();
            int closingQuote = readQuotedValue(text, index, quotedValue);
            value = quotedValue.toString();
            next = skipWhitespaces(text, closingQuote + 1);
            if (next < text.length() && text.charAt(next) == SEPARATOR) {
                next++;
            }
        } else {
            int end = indexOfSeparator(text, index);
            value = text.substring(index, end).trim();
            next = end + 1;
        }
        if (!value.isEmpty()) {
            values.add(value);
        }
        return Math.min(next, text.length());
    }

    private static int skipWhitespaces(String text, int start)
    {
        int index = start;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return index;
    }

    /**
     * Reads the value put in quotes that starts at the given index.
     *
     * @param text the text to read from
     * @param start the index of the opening quote
     * @param value where to put the value, without its quotes and escaping
     * @return the index of the closing quote, or the length of the text when it is missing
     */
    private static int readQuotedValue(String text, int start, StringBuilder value)
    {
        char quote = text.charAt(start);
        int index = start + 1;
        while (index < text.length() && text.charAt(index) != quote) {
            if (text.charAt(index) == ESCAPE && index + 1 < text.length()) {
                index++;
            }
            value.append(text.charAt(index));
            index++;
        }
        return index;
    }

    private static int indexOfSeparator(String text, int start)
    {
        int index = start < text.length() ? text.indexOf(SEPARATOR, start) : -1;
        return index < 0 ? text.length() : index;
    }

    private static boolean isQuote(char character)
    {
        return character == '"' || character == '\'';
    }
}
