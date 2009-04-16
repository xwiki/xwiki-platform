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
 * Generates XWiki Syntax for a parameters like macros and links.
 * 
 * @version $Id$
 * @since 1.9M2
 */
public class XWikiParametersPrinter
{
    /**
     * Print parameters in a String.
     * 
     * @param parameters the parameters to print.
     * @return the printed parameters.
     */
    public String print(Map<String, String> parameters)
    {
        StringBuffer buffer = new StringBuffer();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            String value = entry.getValue();
            String key = entry.getKey();

            if (key != null && value != null) {
                if (buffer.length() > 0) {
                    buffer.append(' ');
                }

                buffer.append(entry.getKey()).append('=').append('\"').append(
                    entry.getValue().replace("\\", "\\\\").replace("\"", "\\\"")).append('\"');
            }
        }

        return buffer.toString();
    }
}
