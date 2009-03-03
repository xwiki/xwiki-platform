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

import java.util.Iterator;
import java.util.Map;

/**
 * Generates XWiki Syntax for a Macro Block.
 * 
 * @version $Id: $
 * @since 1.5RC1
 */
public class XWikiMacroPrinter
{
    public String print(String name, Map<String, String> parameters, String content)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("{{").append(name);
        if (!parameters.isEmpty()) {
            buffer.append(' ');
            buffer.append(printParameters(parameters));
        }
        if ((content == null) || (content.length() == 0)) {
            buffer.append("/}}");
        } else {
            buffer.append("}}");
            buffer.append(content);
            buffer.append("{{/").append(name).append("}}");
        }

        return buffer.toString();
    }

    public String printParameters(Map<String, String> parameters)
    {
        StringBuffer buffer = new StringBuffer();
        for (Iterator<Map.Entry<String, String>> entryIt = parameters.entrySet().iterator(); entryIt.hasNext();) {
            Map.Entry<String, String> entry = entryIt.next();
            buffer.append(entry.getKey()).append('=').append('\"').append(
                entry.getValue().replace("\\", "\\\\").replace("\"", "\\\"")).append('\"');
            if (entryIt.hasNext()) {
                buffer.append(' ');
            }
        }
        return buffer.toString();
    }
}
