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

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * @version $Id$
 * @since 1.8M1
 */
@Component("section")
public class SectionSyntaxFilter extends AbstractFilter implements Initializable
{
    private static final Pattern SECTIONSYNTAX_PATTERN =
        Pattern.compile("^(" + VelocityFilter.SPACEGROUP_OC_SPATTERN + ")?(1(\\.1){0,5}+)[ \\t]++(.++)$",
            Pattern.MULTILINE);

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(1000);
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

        Matcher matcher = SECTIONSYNTAX_PATTERN.matcher(content);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            before += matcher.group(1);

            if (currentIndex > 0) {
                // 1.0 syntax section consume all following new lines
                before = CleanUtil.removeLeadingNewLines(before);
                before = "\n\n" + before;
            }

            result.append(before);

            CleanUtil.setTrailingNewLines(result, 2);

            String headerSyntax =
                filterContext.addProtectedContent(StringUtils.repeat("=", (matcher.group(2).length() + 1) / 2), false);

            String headerContent = headerSyntax + ' ' + matcher.group(4) + ' ' + headerSyntax;

            result.append(CleanUtil.extractVelocity(headerContent, filterContext));
        }

        if (currentIndex == 0) {
            return content;
        }

        String end = content.substring(currentIndex);

        if (currentIndex > 0) {
            // 1.0 syntax section consume all following new lines
            end = CleanUtil.removeLeadingNewLines(end);
            end = "\n\n" + end;
        }

        result.append(end);

        return result.toString();
    }
}
