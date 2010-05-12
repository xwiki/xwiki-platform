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
package org.xwiki.gwt.user.client;

/**
 * Provides utility methods for escaping strings.
 * 
 * @version $Id$
 */
public class EscapeUtils
{
    /**
     * Constructor.
     */
    protected EscapeUtils()
    {
    }

    /**
     * Unescapes characters escaped with the specified escape character.
     * 
     * @param text the text to be unescaped
     * @param escapeChar the character that was used for escaping
     * @return the unescaped text
     */
    public static String unescape(String text, char escapeChar)
    {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        StringBuffer result = new StringBuffer();
        boolean escaped = false;
        for (char c : text.toCharArray()) {
            if (!escaped && c == escapeChar) {
                escaped = true;
                continue;
            }

            result.append(c);
            escaped = false;
        }
        return result.toString();
    }

    /**
     * Unescapes characters escaped with backslash in the given text.
     * 
     * @param text the text to be unescaped
     * @return the unescaped text
     */
    public static String unescapeBackslash(String text)
    {
        return unescape(text, '\\');
    }

    /**
     * Escapes the {@code --} sequence before setting the text of a comment DOM node.
     * 
     * @param text the text that needs to be put in a comment node
     * @return the escaped text, which will be put in a comment node
     */
    public static String escapeComment(String text)
    {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        StringBuffer result = new StringBuffer();
        char lastChar = 0;
        for (char c : text.toCharArray()) {
            if (c == '\\') {
                // Escape the backslash (the escaping character).
                result.append('\\');
            } else if (c == '-' && lastChar == '-') {
                // Escape the second short dash.
                result.append('\\');
            }

            result.append(c);
            lastChar = c;
        }
        if (lastChar == '-') {
            // If the comment data ends with a short dash, add an escaping character.
            result.append('\\');
        }
        return result.toString();
    }
}
