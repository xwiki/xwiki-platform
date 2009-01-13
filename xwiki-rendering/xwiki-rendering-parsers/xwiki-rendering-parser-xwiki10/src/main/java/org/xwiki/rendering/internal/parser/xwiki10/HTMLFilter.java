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
import org.apache.commons.lang.StringUtils;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;

/**
 * Register all Velocity comments in order to protect them from following filters.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class HTMLFilter extends AbstractFilter
{
    public static final String VELOCITYOPEN_PATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKEN + VelocityFilter.VELOCITYOPEN_SUFFIX
            + "[\\d]+" + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String VELOCITYCLOSE_PATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKEN + VelocityFilter.VELOCITYCLOSE_SUFFIX
            + "[\\d]+" + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String HTML_PATTERN = "([\\<\\>])";

    public static final Pattern HTMLVELOCITY_PATTERN =
        Pattern.compile(VELOCITYOPEN_PATTERN + "|" + VELOCITYCLOSE_PATTERN + "|" + HTML_PATTERN);

    public String filter(String content, FilterContext filterContext)
    {
        content = filterMacros(content, filterContext);

        return content;
    }

    private String filterMacros(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();
        Matcher matcher = HTMLVELOCITY_PATTERN.matcher(content);

        int currentIndex = 0;
        boolean inHTMLMacro = false;
        boolean htmlMacroInVelocityMacro = false;

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
                if (inHTMLMacro) {
                    nonHtmlContent.append(before);
                    nonHtmlContentWithVelocity.append(before);
                    nonHtmlContentWithVelocity.append(matchedContent);

                    ++nbVOpen;
                } else {
                    result.append(StringEscapeUtils.unescapeHtml(before));
                    result.append(matchedContent);

                    htmlMacroInVelocityMacro = true;
                }
            } else if (matcher.group(2) != null) {
                if (inHTMLMacro) {
                    nonHtmlContent.append(before);
                    nonHtmlContentWithVelocity.append(before);
                    nonHtmlContentWithVelocity.append(matchedContent);

                    ++nbVClose;
                } else {
                    result.append(StringEscapeUtils.unescapeHtml(before));
                    result.append(matchedContent);

                    htmlMacroInVelocityMacro = false;
                }
            } else {
                if (StringUtils.countMatches(nonHtmlContent.toString() + before, "\n") > 10) {
                    if (!htmlMacroInVelocityMacro && nbVOpen > 0) {
                        result.append(filterContext.addProtectedContent("{{velocity}}",
                            VelocityFilter.VELOCITYOPEN_SUFFIX));
                    }
                    result.append(filterContext.addProtectedContent("{{html wiki=true}}"));
                    result.append(htmlContent);
                    result.append(filterContext.addProtectedContent("{{/html}}"));
                    result.append(StringEscapeUtils.unescapeHtml(nonHtmlContentWithVelocity.toString()));
                    if (nbVCloseInHTML > nbVOpenInHTML) {
                        result.append(filterContext.addProtectedContent("{{/velocity}}",
                            VelocityFilter.VELOCITYCLOSE_SUFFIX));
                    }

                    result.append(StringEscapeUtils.unescapeHtml(before));

                    htmlContent = new StringBuffer();
                    htmlMacroInVelocityMacro =
                        (htmlMacroInVelocityMacro && nbVOpen + nbVOpenInHTML == nbVClose + nbVCloseInHTML)
                            || (!htmlMacroInVelocityMacro && nbVOpen + nbVOpenInHTML > nbVClose + nbVCloseInHTML);
                    nbVOpenInHTML = 0;
                    nbVCloseInHTML = 0;
                } else {
                    htmlContent.append(nonHtmlContent);

                    if (!inHTMLMacro) {
                        result.append(StringEscapeUtils.unescapeHtml(before));
                    } else {
                        htmlContent.append(before);

                        nbVOpenInHTML += nbVOpen;
                        nbVCloseInHTML += nbVClose;
                    }
                }

                nbVOpen = 0;
                nbVClose = 0;

                inHTMLMacro = true;

                nonHtmlContent = new StringBuffer();
                nonHtmlContentWithVelocity = new StringBuffer();

                htmlContent.append(matchedContent);
            }
        }

        if (currentIndex == 0) {
            return StringEscapeUtils.unescapeHtml(content);
        }

        // Close html macro
        if (inHTMLMacro) {
            if (!htmlMacroInVelocityMacro && nbVOpen > 0) {
                result.append(filterContext.addProtectedContent("{{velocity}}", VelocityFilter.VELOCITYOPEN_SUFFIX));
            }
            result.append(filterContext.addProtectedContent("{{html wiki=true}}"));
            result.append(htmlContent);
            result.append(filterContext.addProtectedContent("{{/html}}"));
            result.append(StringEscapeUtils.unescapeHtml(nonHtmlContentWithVelocity.toString()));
            if (nbVCloseInHTML > nbVOpenInHTML) {
                result.append(filterContext.addProtectedContent("{{/velocity}}", VelocityFilter.VELOCITYCLOSE_SUFFIX));
            }
        }

        result.append(StringEscapeUtils.unescapeHtml(content.substring(currentIndex)));

        return result.toString();
    }
}
