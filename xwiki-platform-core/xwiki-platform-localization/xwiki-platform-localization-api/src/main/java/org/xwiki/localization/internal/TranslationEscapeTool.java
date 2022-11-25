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

/**
 * Tool for escaping translations.
 *
 * @version $Id$
 * @since 13.10.11
 * @since 14.4.8
 * @since 14.10
 */
public final class TranslationEscapeTool
{
    private TranslationEscapeTool()
    {
    }

    /**
     * Escapes the provided translation result such that it cannot contain XWiki macro syntax.
     * <p>
     * Normally, this should be handled by specifying the correct target syntax but in many cases it is omitted and in
     * these cases this escaping makes sure that the translation is safe.
     *
     * @param input the input to escape
     * @return the escaped result
     */
    public static String escapeForMacros(String input)
    {
        String result = null;

        if (input != null) {
            StringBuilder builder = new StringBuilder(input.length());

            // Escape all { following a '{' plus any amount of '~' as this could be a macro opening or closing pattern
            // when ~ are removed (which happens in link parsing, in particular with nesting).
            boolean shallEscape = false;
            for (int i = 0; i < input.length(); ++i) {
                char currentChar = input.charAt(i);
                // Escape { if we're at the end or shallEscape is true
                if ((!shallEscape && i < input.length() - 1) || currentChar != '{') {
                    builder.append(currentChar);
                } else {
                    builder.append('\u2774');
                }

                // Set shallEscape when a first { is found (which itself is not escaped).
                if (currentChar == '{') {
                    shallEscape = true;
                } else if (currentChar != '~') {
                    // Stop escaping if we find a character that is neither { nor ~.
                    shallEscape = false;
                }
            }

            result = builder.toString();
        }

        return result;
    }
}
