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
 * Add needed HTML open and close macro.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("htmlmacro")
public class HTMLFilter extends AbstractFilter implements Initializable
{
    public static final String HTML_SPATTERN = "(\\<!--)|(--\\>)|([\\<\\>])";

    public static final Pattern HTML_PATTERN = Pattern.compile(HTML_SPATTERN);

    public static final String HTMLOPEN_SUFFIX = "htmlopen";

    public static final String HTMLCLOSE_SUFFIX = "htmlclose";

    public static final String HTMLOPEN_SPATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENNI + HTMLOPEN_SUFFIX + "[\\d]+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String HTMLCLOSE_SPATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENNI + HTMLCLOSE_SUFFIX + "[\\d]+"
            + FilterContext.XWIKI1020TOKEN_CP + ")";

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(3000);
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

        Matcher matcher = HTML_PATTERN.matcher(content);

        boolean inHTMLMacro = false;
        boolean inHTMLComment = false;

        boolean velocityOpenBefore = false;
        boolean velocityCloseBefore = false;

        StringBuffer htmlContent = new StringBuffer();

        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (!inHTMLMacro) {
                Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(before);
                velocityOpenBefore = velocityOpenMatcher.find();
                Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(before);
                velocityCloseBefore = velocityCloseMatcher.find();

                result.append(before);
            } else {
                htmlContent.append(before);
            }

            inHTMLMacro = true;

            if (matcher.group(1) != null) {
                inHTMLComment = true;
                htmlContent.append(filterContext.addProtectedContent(matcher.group(0)));
            } else if (inHTMLComment && matcher.group(2) != null) {
                htmlContent.append(filterContext.addProtectedContent(matcher.group(0)));
                inHTMLComment = false;
            } else {
                htmlContent.append(matcher.group(0));
            }
        }

        if (currentIndex == 0) {
            return content;
        }

        // clean html content
        Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(htmlContent);
        boolean velocityOpen = velocityOpenMatcher.find();
        String cleanedHtmlContent = velocityOpenMatcher.replaceAll("");
        Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(cleanedHtmlContent);
        boolean velocityClose = velocityCloseMatcher.find();
        cleanedHtmlContent = velocityCloseMatcher.replaceAll("");

        // print the content

        boolean multilines = cleanedHtmlContent.indexOf("\n") != -1;

        if (velocityOpen) {
            VelocityFilter.appendVelocityOpen(result, filterContext, multilines);
        } else if (!velocityOpenBefore || velocityCloseBefore) {
            appendHTMLOpen(result, filterContext, multilines);
        }

        result.append(cleanedHtmlContent);

        if (velocityClose) {
            VelocityFilter.appendVelocityClose(result, filterContext, multilines);
        } else if (velocityCloseBefore || !velocityOpenBefore) {
            appendHTMLClose(result, filterContext, multilines);
        }

        if (currentIndex < content.length()) {
            result.append(content.substring(currentIndex));
        }

        return result.toString();
    }

    public static void appendHTMLOpen(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext
            .addProtectedContent("{{html wiki=true}}" + (nl ? "\n" : ""), HTMLOPEN_SUFFIX, false));
    }

    public static void appendHTMLClose(StringBuffer result, FilterContext filterContext, boolean nl)
    {
        result.append(filterContext.addProtectedContent((nl ? "\n" : "") + "{{/html}}", HTMLCLOSE_SUFFIX, false));
    }
}
