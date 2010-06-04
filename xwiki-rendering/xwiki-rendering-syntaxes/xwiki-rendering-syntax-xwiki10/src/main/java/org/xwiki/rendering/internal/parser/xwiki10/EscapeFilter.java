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

/**
 * Match 1.0 escape syntax.
 * 
 * @version $Id$
 * @since 1.9M2
 */
@Component("escape")
public class EscapeFilter extends AbstractFilter implements Initializable
{
    /**
     * Regex pattern for matching 1.0 syntax escaping.
     */
    public static final Pattern ESCAPE_PATTERN = Pattern.compile("\\\\(\\\\\\\\)(?!\\\\)|\\\\(.)");

    public static final String ESCAPE_SUFFIX = "escape";

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(25);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.xwiki10.Filter#filter(java.lang.String,
     *      org.xwiki.rendering.parser.xwiki10.FilterContext)
     */
    public String filter(String content, FilterContext filterContext)
    {
        return filter(content, filterContext, true);
    }

    public String filter(String content, FilterContext filterContext, boolean clean)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = ESCAPE_PATTERN.matcher(content);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            result.append(content.substring(currentIndex, matcher.start()));

            if (clean) {
                if (matcher.group(2) != null) {
                    String escaped = matcher.group(2);
                    if ("\\".equals(escaped)) {
                        result.append("\\\\");
                    } else {
                        result.append(filterContext.addProtectedContent(matcher.group(2), ESCAPE_SUFFIX, true));
                    }
                } else {
                    result.append(filterContext.addProtectedContent("\\", ESCAPE_SUFFIX, true));
                }
            } else {
                result.append(filterContext.addProtectedContent(matcher.group(0), ESCAPE_SUFFIX, true));
            }
        }

        if (currentIndex == 0) {
            return content;
        }

        result.append(content.substring(currentIndex));

        return result.toString();
    }
}
