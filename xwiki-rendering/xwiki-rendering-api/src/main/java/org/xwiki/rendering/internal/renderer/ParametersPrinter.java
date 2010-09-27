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
package org.xwiki.rendering.internal.renderer;

import java.util.Map;

/**
 * Generates syntax for a parameters group like macros and links.
 * 
 * @version $Id$
 * @since 1.9RC2
 */
public class ParametersPrinter
{
    /**
     * Quote character.
     */
    private static final String QUOTE = "\"";

    /**
     * Print the parameters as a String.
     * 
     * @param parameters the parameters to print
     * @param escapeChar the character used in front of a special character when need to escape it
     * @return the printed parameters
     */
    public String print(Map<String, String> parameters, char escapeChar)
    {
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = entry.getValue();
            String key = entry.getKey();

            if (key != null && value != null) {
                if (buffer.length() > 0) {
                    buffer.append(' ');
                }
                buffer.append(print(key, value, escapeChar));
            }
        }

        return buffer.toString();
    }

    /**
     *  Print a parameter as a String.
     *
     * @param parameterName the name of the parameter to print
     * @param parameterValue the value of the parameter to print
     * @param escapeChar the character used in front of a special character when need to escape it
     * @return the printed parameter
     */
    public String print(String parameterName, String parameterValue, char escapeChar)
    {
        // escape the escaping character
        String value = parameterValue.replace(String.valueOf(escapeChar), String.valueOf(escapeChar) + escapeChar);
        // escape quote
        value = value.replace(QUOTE, String.valueOf(escapeChar) + QUOTE);

        return parameterName + "=" + QUOTE + value + QUOTE;
    }
}
