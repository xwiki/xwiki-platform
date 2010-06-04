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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.Filter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Converts Code Macro.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("code")
public class CodeMacroFilter extends AbstractFilter implements Initializable
{
    private static final Pattern CODEMACRO_PATTERN =
        Pattern.compile("\\{(code)(?::([^\\}]*))?\\}(.*?)\\{code\\}", Pattern.DOTALL);

    @Requirement("code")
    private RadeoxMacroConverter codeMacroConverter;

    @Requirement("unescape")
    private Filter unescapeFilter;

    private EscapeFilter escapeFilter = new EscapeFilter();

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(10);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.xwiki10.Filter#filter(java.lang.String,
     *      org.xwiki.rendering.parser.xwiki10.FilterContext)
     */
    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        String escapedContent = this.escapeFilter.filter(content, filterContext, false);

        Matcher matcher = CODEMACRO_PATTERN.matcher(escapedContent);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = escapedContent.substring(currentIndex, matcher.start());

            if (result.length() > 0) {
                before = CleanUtil.setLeadingNewLines(before, 2);
            }

            before = CleanUtil.setTrailingNewLines(before, 2);

            String macroResult =
                this.codeMacroConverter.convert("code", RadeoxMacrosFilter.getMacroParameters(this.codeMacroConverter,
                    matcher.group(2)), matcher.group(3), filterContext);

            result.append(before);
            result.append(filterContext.addProtectedContent(macroResult, false));
        }

        if (currentIndex == 0) {
            return content;
        }

        result.append(CleanUtil.setLeadingNewLines(escapedContent.substring(currentIndex), 2));

        return this.unescapeFilter.filter(result.toString(), filterContext);
    }
}
