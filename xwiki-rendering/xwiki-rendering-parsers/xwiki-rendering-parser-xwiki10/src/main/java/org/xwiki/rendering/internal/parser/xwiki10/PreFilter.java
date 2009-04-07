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
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * @version $Id$
 * @since 1.8M1
 */
@Component("pre")
public class PreFilter extends AbstractFilter implements Initializable
{
    private static final Pattern PRE_PATTERN =
        Pattern.compile("\\{pre\\}(.*?)\\{/pre\\}", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    public static final Pattern VELOCITYOPEN_PATTERN = Pattern.compile(VelocityFilter.VELOCITYOPEN_SPATTERN);

    public static final Pattern VELOCITYCLOSE_PATTERN = Pattern.compile(VelocityFilter.VELOCITYCLOSE_SPATTERN);

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(100);
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

        Matcher matcher = PRE_PATTERN.matcher(content);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            // a standalone new line is not interpreted by XWiki 1.0 rendering
            result.append(CleanUtil.removeTrailingNewLines(before, 1, true));

            String preContent = matcher.group(1);

            Matcher velocityOpenMatcher = VELOCITYOPEN_PATTERN.matcher(preContent);
            boolean velocityOpen = velocityOpenMatcher.find();
            preContent = velocityOpenMatcher.replaceAll("");
            Matcher velocityCloseMatcher = VELOCITYCLOSE_PATTERN.matcher(preContent);
            boolean velocityClose = velocityCloseMatcher.find();
            preContent = velocityCloseMatcher.replaceAll("");

            if (velocityOpen) {
                VelocityFilter.appendVelocityOpen(result, filterContext);
            }

            result.append("{{{");
            result.append(filterContext.addProtectedContent(CleanUtil.cleanSpacesAndNewLines(preContent).trim()));
            result.append("}}}");

            if (velocityClose) {
                VelocityFilter.appendVelocityClose(result, filterContext);
            }
        }

        if (currentIndex == 0) {
            return content;
        }

        result.append(content.substring(currentIndex));

        return result.toString();
    }
}
