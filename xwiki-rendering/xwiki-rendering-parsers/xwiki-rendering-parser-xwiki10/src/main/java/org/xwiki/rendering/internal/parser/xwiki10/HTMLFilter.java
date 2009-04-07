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

    public static final Pattern HTMLVELOCITY_PATTERN =
        Pattern.compile(VelocityFilter.VELOCITYOPEN_SPATTERN + "|" + VelocityFilter.VELOCITYCLOSE_SPATTERN + "|"
            + HTML_PATTERN);

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
        boolean htmlMacroInVelocityMacro = false;
        boolean inHTMLComment = false;

        int nbVOpen = 0;
        int nbVClose = 0;
        int nbVOpenInHTML = 0;
        int nbVCloseInHTML = 0;

        StringBuffer htmlContent = new StringBuffer();
        StringBuffer nonHtmlContent = new StringBuffer();
        StringBuffer nonHtmlContentWithVelocity = new StringBuffer();
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            String matchedContent = matcher.group(0);

            if (matcher.group(1) != null) {
                // Velocity open
                if (inHTMLMacro) {
                    nonHtmlContent.append(before);
                    nonHtmlContentWithVelocity.append(before);
                    appendVelocityOpen(nonHtmlContentWithVelocity, filterContext);

                    ++nbVOpen;
                } else {
                    result.append(StringEscapeUtils.unescapeHtml(before));
                    appendVelocityOpen(result, filterContext);

                    htmlMacroInVelocityMacro = true;
                }
            } else if (matcher.group(2) != null) {
                // Velocity close
                if (inHTMLMacro) {
                    nonHtmlContent.append(before);
                    nonHtmlContentWithVelocity.append(before);
                    appendVelocityClose(nonHtmlContentWithVelocity, filterContext);

                    ++nbVClose;
                } else {
                    result.append(StringEscapeUtils.unescapeHtml(before));
                    appendVelocityClose(result, filterContext);

                    htmlMacroInVelocityMacro = false;
                }
            } else {
                // html
                htmlContent.append(nonHtmlContent);

                if (!inHTMLMacro) {
                    result.append(StringEscapeUtils.unescapeHtml(before));
                } else {
                    htmlContent.append(before);

                    nbVOpenInHTML += nbVOpen;
                    nbVCloseInHTML += nbVClose;
                }

                nbVOpen = 0;
                nbVClose = 0;

                inHTMLMacro = true;

                nonHtmlContent.setLength(0);
                nonHtmlContentWithVelocity.setLength(0);

                if (matcher.group(3) != null) {
                    inHTMLComment = true;
                    htmlContent.append(filterContext.addProtectedContent(matchedContent));
                } else if (inHTMLComment && matcher.group(4) != null) {
                    htmlContent.append(filterContext.addProtectedContent(matchedContent));
                    inHTMLComment = false;
                } else {
                    htmlContent.append(matchedContent);
                }
            }
        }

        if (currentIndex == 0) {
            return StringEscapeUtils.unescapeHtml(content);
        }

        // Close html macro
        if (inHTMLMacro) {
            if (!htmlMacroInVelocityMacro && nbVOpenInHTML > 0) {
                appendVelocityOpen(result, filterContext);
            } else if (nbVCloseInHTML == 0 && nbVOpenInHTML == 0) {
                appendHTMLOpen(result, filterContext);
            }

            if (htmlContent.length() > 0) {
                result.append(htmlContent);
            }

            if (nbVCloseInHTML == 0 && nbVOpenInHTML == 0) {
                appendHTMLClose(result, filterContext);
            } else if (htmlMacroInVelocityMacro || nbVCloseInHTML > 0) {
                appendVelocityClose(result, filterContext);
            }

            result.append(StringEscapeUtils.unescapeHtml(nonHtmlContentWithVelocity.toString()));
        }

        result.append(StringEscapeUtils.unescapeHtml(content.substring(currentIndex)));

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
}
