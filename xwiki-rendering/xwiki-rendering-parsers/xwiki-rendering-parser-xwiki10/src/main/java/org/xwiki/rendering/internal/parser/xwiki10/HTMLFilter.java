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
 * Register all Velocity comments in order to protect them from following filters.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("htmlmacro")
public class HTMLFilter extends AbstractFilter implements Initializable
{
    public static final String HTML_PATTERN = "(\\<!--)|(--\\>)|([\\<\\>])";

    public static final Pattern VELOCITYOPEN_PATTERN = Pattern.compile(VelocityFilter.VELOCITYOPEN_SPATTERN);

    public static final Pattern VELOCITYCLOSE_PATTERN = Pattern.compile(VelocityFilter.VELOCITYCLOSE_SPATTERN);

    public static final Pattern HTMLVELOCITY_PATTERN = Pattern.compile(HTML_PATTERN);

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
        Matcher matcher = HTMLVELOCITY_PATTERN.matcher(content);

        int currentIndex = 0;
        boolean inHTMLMacro = false;
        boolean inHTMLComment = false;

        boolean velocityOpenBefore = false;
        boolean velocityCloseBefore = false;

        StringBuffer htmlContent = new StringBuffer();
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (!inHTMLMacro) {
                // make velocity support html
                Matcher velocityOpenMatcher = VELOCITYOPEN_PATTERN.matcher(before);
                velocityOpenBefore = velocityOpenMatcher.find();
                before = velocityOpenMatcher.replaceAll(getVelocityOpen(filterContext));
                Matcher velocityCloseMatcher = VELOCITYCLOSE_PATTERN.matcher(before);
                velocityCloseBefore = velocityCloseMatcher.find();
                before = velocityCloseMatcher.replaceAll(getVelocityClose(filterContext));

                result.append(StringEscapeUtils.unescapeHtml(before));
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
            String cleanedContent = StringEscapeUtils.unescapeHtml(content);

            // make velocity support html
            cleanedContent = VELOCITYOPEN_PATTERN.matcher(cleanedContent).replaceAll(getVelocityOpen(filterContext));
            cleanedContent = VELOCITYCLOSE_PATTERN.matcher(cleanedContent).replaceAll(getVelocityClose(filterContext));

            return cleanedContent;
        }

        // clean html content
        Matcher velocityOpenMatcher = VELOCITYOPEN_PATTERN.matcher(htmlContent);
        boolean velocityOpen = velocityOpenMatcher.find();
        String cleanedHtmlContent = velocityOpenMatcher.replaceAll("");
        Matcher velocityCloseMatcher = VELOCITYCLOSE_PATTERN.matcher(cleanedHtmlContent);
        boolean velocityClose = velocityCloseMatcher.find();
        cleanedHtmlContent = velocityCloseMatcher.replaceAll("");

        // print the content

        if (velocityOpen) {
            appendVelocityOpen(result, filterContext);
        } else if (!velocityOpenBefore || velocityCloseBefore) {
            appendHTMLOpen(result, filterContext);
        }

        result.append(cleanedHtmlContent);

        if (velocityClose) {
            appendVelocityClose(result, filterContext);
        } else if (velocityCloseBefore || !velocityOpenBefore) {
            appendHTMLClose(result, filterContext);
        }

        if (currentIndex < content.length()) {
            String after = StringEscapeUtils.unescapeHtml(content.substring(currentIndex));

            // make velocity support html
            after = VELOCITYOPEN_PATTERN.matcher(after).replaceAll(getVelocityOpen(filterContext));
            after = VELOCITYCLOSE_PATTERN.matcher(after).replaceAll(getVelocityClose(filterContext));

            result.append(StringEscapeUtils.unescapeHtml(after));
        }

        return result.toString();
    }

    public static void appendHTMLOpen(StringBuffer result, FilterContext filterContext)
    {
        result.append(filterContext.addProtectedContent("{{html wiki=true}}", false));
    }

    public static void appendHTMLClose(StringBuffer result, FilterContext filterContext)
    {
        result.append(filterContext.addProtectedContent("{{/html}}", false));
    }

    public static void appendVelocityOpen(StringBuffer result, FilterContext filterContext)
    {
        VelocityFilter.appendVelocityOpen(result, filterContext);
        appendHTMLOpen(result, filterContext);
    }

    public static void appendVelocityClose(StringBuffer result, FilterContext filterContext)
    {
        appendHTMLClose(result, filterContext);
        VelocityFilter.appendVelocityClose(result, filterContext);
    }

    public static String getVelocityOpen(FilterContext filterContext)
    {
        StringBuffer str = new StringBuffer();

        appendVelocityOpen(str, filterContext);

        return str.toString();
    }

    public static String getVelocityClose(FilterContext filterContext)
    {
        StringBuffer str = new StringBuffer();

        appendVelocityClose(str, filterContext);

        return str.toString();
    }
}
