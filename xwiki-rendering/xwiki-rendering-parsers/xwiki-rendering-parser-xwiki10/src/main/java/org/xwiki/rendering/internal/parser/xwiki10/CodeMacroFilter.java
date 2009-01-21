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
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * @version $Id$
 * @since 1.8M1
 */
public class CodeMacroFilter extends AbstractFilter
{
    private static final Pattern CODEMACRO_PATTERN =
        Pattern.compile("\\{(code)(?::([^\\}]*))?\\}(.*?)\\{code\\}", Pattern.DOTALL);

    private RadeoxMacroConverter codeMacroConverter;

    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = CODEMACRO_PATTERN.matcher(content);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (currentIndex > 0) {
                before = CleanUtil.setFirstNewLines(before, 2);
            }

            String macroResult =
                this.codeMacroConverter.convert("code", RadeoxMacrosFilter.getMacroParameters(this.codeMacroConverter,
                    matcher.group(2)), matcher.group(3), filterContext);

            result.append(before);
            result.append(filterContext.addProtectedContent(macroResult));
        }

        if (currentIndex == 0) {
            return content;
        }

        result.append(CleanUtil.setFirstNewLines(content.substring(currentIndex), 2));

        return result.toString();
    }
}
