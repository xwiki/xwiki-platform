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

package org.xwiki.velocity.tools;

/**
 * Tool for working with escaping in Velocity templates. It provides methods to escape outputs for Velocity, Java,
 * JavaScript, HTML, XML and SQL.
 * 
 * @version $Id$
 * @since 2.7RC1
 */
public class EscapeTool extends org.apache.velocity.tools.generic.EscapeTool
{
    /**
     * Escapes the XML special characters in a <code>String</code> using numerical XML entities.
     * 
     * @param value the text to escape, may be {@code null}
     * @return a new escaped {@code String}, {@code null} if {@code null} input
     */
    @Override
    public String xml(Object source)
    {
        if (source == null) {
            return null;
        }
        String str = String.valueOf(source);
        StringBuilder result = new StringBuilder((int) (str.length() * 1.1));
        int length = str.length();
        char c;
        for (int i = 0; i < length; ++i) {
            c = str.charAt(i);
            switch (c) {
                case '&':
                    result.append("&#38;");
                    break;
                case '<':
                    result.append("&#60;");
                    break;
                case '>':
                    result.append("&#62;");
                    break;
                case '"':
                    result.append("&#34;");
                    break;
                case '\'':
                    result.append("&#39;");
                    break;
                default:
                    result.append(c);
            }
        }
        return result.toString();
    }
}
