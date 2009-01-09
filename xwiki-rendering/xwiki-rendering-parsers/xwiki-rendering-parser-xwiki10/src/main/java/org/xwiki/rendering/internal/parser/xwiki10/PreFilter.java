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
package org.xwiki.rendering.internal.parser.xwiki10;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class PreFilter extends AbstractFilter
{
    private static final Pattern PRE_PATTERN =
        Pattern.compile("\\{pre\\}(.*?)\\{/pre\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = PRE_PATTERN.matcher(content);
        int current = 0;
        while (matcher.find()) {
            String before = content.substring(current, matcher.start());
            current = matcher.end();

            // a standalone new line is not interpreted by XWiki 1.0 rendering
            result.append(CleanUtil.removeLastStandaloneNewLine(before));

            result.append("{{{");
            result.append(filterContext.addProtectedContent(CleanUtil.cleanSpacesAndNewLines(matcher.group(1)).trim()));
            result.append("}}}");
        }

        if (current == 0) {
            return content;
        }

        result.append(content.substring(current));

        return result.toString();
    }
}
