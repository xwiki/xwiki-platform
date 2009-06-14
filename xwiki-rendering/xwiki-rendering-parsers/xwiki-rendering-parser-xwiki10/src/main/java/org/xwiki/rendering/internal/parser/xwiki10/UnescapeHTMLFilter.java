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

import org.apache.commons.lang.StringEscapeUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

/**
 * Unescape HTML escaped content outside of matched HTML macros.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("unescapehtml")
public class UnescapeHTMLFilter extends AbstractFilter implements Initializable
{
    public static final Pattern HTMLVELOCITY_SPATTERN =
        Pattern.compile("(" + VelocityFilter.VELOCITYOPEN_SPATTERN + ")|(" + VelocityFilter.VELOCITYCLOSE_PATTERN
            + ")|(" + HTMLFilter.HTMLOPEN_SPATTERN + ")|(" + HTMLFilter.HTMLCLOSE_SPATTERN + ")");

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(3001);
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

        Matcher matcher = HTMLVELOCITY_SPATTERN.matcher(content);

        boolean inHTML = false;

        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (!inHTML) {
                result.append(unescapeNonHtmlContent(before));
            } else {
                result.append(before);
            }

            if (matcher.group(1) != null || matcher.group(3) != null) {
                inHTML = true;
            } else {
                inHTML = false;
            }

            result.append(matcher.group());
        }

        if (currentIndex == 0) {
            return unescapeNonHtmlContent(content);
        }

        // Print remaining content
        result.append(unescapeNonHtmlContent(content.substring(currentIndex)));

        return result.toString();
    }

    private String unescapeNonHtmlContent(String content)
    {
        return StringEscapeUtils.unescapeHtml(content);
    }
}
