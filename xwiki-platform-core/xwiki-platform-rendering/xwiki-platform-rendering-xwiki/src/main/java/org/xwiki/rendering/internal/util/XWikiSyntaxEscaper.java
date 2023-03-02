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
package org.xwiki.rendering.internal.util;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.syntax.Syntax;

/**
 * Escaping helpers.
 *
 * @version $Id$
 * @since 14.10.6
 * @since 15.2RC1
 */
@Component(roles = XWikiSyntaxEscaper.class)
@Singleton
public class XWikiSyntaxEscaper
{
    /**
     * Escapes a give text using the escaping method specific to the given syntax.
     * <p>
     * One example of escaping method is using escape characters like {@code ~} for the {@link Syntax#XWIKI_2_1} syntax
     * on all or just some characters of the given text.
     * <p>
     * The current implementation only escapes XWiki 1.0, 2.0 and 2.1 syntaxes.
     *
     * @param content the text to escape
     * @param syntax the syntax to escape the content in (e.g. {@link Syntax#XWIKI_1_0}, {@link Syntax#XWIKI_2_0},
     *     {@link Syntax#XWIKI_2_1}, etc.). This is the syntax where the output will be used and not necessarily the
     *     same syntax of the input content
     * @return the escaped text or {@code null} if the given content or the given syntax are {@code null}, or if the
     *     syntax is not supported
     */
    public String escape(String content, Syntax syntax)
    {
        if (content == null || syntax == null) {
            return null;
        }

        // Determine the escape character for the syntax.
        char escapeChar;
        try {
            escapeChar = getEscapeCharacter(syntax);
        } catch (Exception e) {
            // We don`t know how to proceed, so we just return null.
            return null;
        }

        // Since we prefix all characters, the result size will be double the input's, so we can just use char[].
        char[] result = new char[content.length() * 2];

        // Escape the content.
        for (int i = 0; i < content.length(); i++) {
            result[2 * i] = escapeChar;
            result[2 * i + 1] = content.charAt(i);
        }

        return String.valueOf(result);
    }

    private char getEscapeCharacter(Syntax syntax) throws IllegalArgumentException
    {
        if (Syntax.XWIKI_1_0.equals(syntax)) {
            return '\\';
        } else if (Syntax.XWIKI_2_0.equals(syntax) || Syntax.XWIKI_2_1.equals(syntax)) {
            return '~';
        }

        throw new IllegalArgumentException(String.format("Escaping is not supported for Syntax [%s]", syntax));
    }
}
