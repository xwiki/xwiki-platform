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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.VelocityMacroConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Register all Velocity comments in order to protect them from following filters.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class VelocityFilter extends AbstractFilter implements Composable
{
    public static final String VELOCITYOPEN_SUFFIX = "velocityopen";

    public static final String VELOCITYCLOSE_SUFFIX = "velocityclose";

    public static final String VELOCITY_COMMENT_SPATTERN = "((?m)\\n?\\#\\#.*$)|((?s)\\#\\*(.*?)\\*\\#)";

    public static final String VELOCITY_MACRO_SPATTERN = "\\#(\\w+)\\(([^)]*)\\)";

    public static final String VELOCITY_VARIABLEMETHOD_SPATTERN = "\\.\\p{Alpha}\\w*(\\(.*\\))?";
    
    public static final String VELOCITY_VARIABLE_SPATTERN = "\\$\\{?\\p{Alpha}\\w*("+VELOCITY_VARIABLEMETHOD_SPATTERN+")*\\}?";

    public static final Pattern VELOCITY_PATTERN =
        Pattern.compile(VELOCITY_COMMENT_SPATTERN + "|" + VELOCITY_MACRO_SPATTERN + "|" + VELOCITY_VARIABLE_SPATTERN);

    public static final String VELOCITYOPEN_SPATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENIL + VelocityFilter.VELOCITYOPEN_SUFFIX
            + "[\\d]+" + FilterContext.XWIKI1020TOKEN_CP + ")";

    public static final String VELOCITYCLOSE_SPATTERN =
        "(" + FilterContext.XWIKI1020TOKEN_OP + FilterContext.XWIKI1020TOKENIL + VelocityFilter.VELOCITYCLOSE_SUFFIX
            + "[\\d]+" + FilterContext.XWIKI1020TOKEN_CP + ")";

    private ComponentManager componentManager;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Composable#compose(org.xwiki.component.manager.ComponentManager)
     */
    public void compose(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    public String filter(String content, FilterContext filterContext)
    {
        content = filterMacros(content, filterContext);

        return content;
    }

    private String filterMacros(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();
        Matcher matcher = VELOCITY_PATTERN.matcher(content);

        int currentIndex = 0;

        boolean inVelocityMacro = false;
        VelocityMacroConverter currentMacro = null;
        String nonVelocityContent = null;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());
            nonVelocityContent = nonVelocityContent != null ? nonVelocityContent + before : before;

            String matchedContent = matcher.group(0);

            String macroName = matcher.group(4);

            if (macroName != null) {
                // If it's a velocity macro it can be converted in 2.0 syntax
                String params = matcher.group(5);

                try {
                    currentMacro =
                        (VelocityMacroConverter) this.componentManager.lookup(VelocityMacroConverter.ROLE, macroName);

                    if (!currentMacro.isInline()) {
                        nonVelocityContent = CleanUtil.setLastNewLines(nonVelocityContent, 2);
                    } else {
                        // A standalone new line is not interpreted by XWiki 1.0 rendering
                        nonVelocityContent = CleanUtil.removeLastNewLines(nonVelocityContent, 1, true);
                    }

                    matchedContent = currentMacro.convert(macroName, getMacroParameters(params));
                    if (currentMacro.protectResult()) {
                        matchedContent = filterContext.addProtectedContent(matchedContent);
                    }

                    nonVelocityContent += matchedContent;
                    continue;
                } catch (ComponentLookupException e) {
                    if (getLogger().isDebugEnabled()) {
                        getLogger().debug("Can't find macro converter [" + macroName + "]", e);
                    }
                }
            }

            if (StringUtils.countMatches(nonVelocityContent, "\n") > 10) {
                appendVelocityClose(result, filterContext);
                inVelocityMacro = false;
            }

            result.append(nonVelocityContent);

            if (!inVelocityMacro) {
                appendVelocityOpen(result, filterContext);
                matchedContent = CleanUtil.removeFirstNewLines(matchedContent, 1, false);
                inVelocityMacro = true;
            }

            result.append(filterContext.addProtectedContent(matchedContent));

            nonVelocityContent = null;
            currentMacro = null;
        }

        if (currentIndex == 0) {
            return content;
        }

        // Close velocity macro
        if (inVelocityMacro) {
            appendVelocityClose(result, filterContext);
        }

        if (nonVelocityContent != null) {
            result.append(nonVelocityContent);
        }

        // Make sure the last non inline macro is followed by 2 new lines
        if (currentMacro != null && !currentMacro.isInline()) {
            result.append(CleanUtil.setFirstNewLines(content.substring(currentIndex), 2));
        } else {
            result.append(content.substring(currentIndex));
        }

        return result.toString();
    }

    private List<String> getMacroParameters(String parameters)
    {
        List<String> parameterList = new ArrayList<String>();

        if (parameters != null) {
            String[] parameterTable = parameters.split(" ");

            for (String parameterValue : parameterTable) {
                if (parameterValue.length() > 0) {
                    parameterList.add(parameterValue);
                }
            }
        }

        return parameterList;
    }

    public static void appendVelocityOpen(StringBuffer result, FilterContext filterContext)
    {
        result.append(filterContext.addProtectedContent("{{velocity}}", VELOCITYOPEN_SUFFIX, true));
    }

    public static void appendVelocityClose(StringBuffer result, FilterContext filterContext)
    {
        result.append(filterContext.addProtectedContent("{{/velocity}}", VELOCITYCLOSE_SUFFIX, true));
    }
}
