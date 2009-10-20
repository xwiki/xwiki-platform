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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroConverter;
import org.xwiki.rendering.parser.xwiki10.macro.RadeoxMacroParameters;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Convert 1.0 radeox macros to 2.0 macro. A conversion can be added by implementing RadeoxMacroConverter.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("radeoxmacros")
public class RadeoxMacrosFilter extends AbstractFilter implements Initializable
{
    /**
     * Regex pattern for matching macros that are written on single line.
     */
    public static final Pattern SINGLE_LINE_MACRO_PATTERN = Pattern.compile("\\{(\\w+)(:(.+))?\\}");

    /**
     * Regex pattern for matching macros that span several lines (i.e. macros that have a body block). Note that we're
     * using the {@link Pattern#DOTALL} flag to tell the compiler that "." should match any characters, including new
     * lines.
     */
    public static final Pattern MULTI_LINE_MACRO_PATTERN =
        Pattern.compile("\\{(\\w+)(:(.+?))?\\}(.+?)\\{\\1\\}", Pattern.DOTALL);

    @Requirement
    private ComponentManager componentManager;

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
        content = filterMacros(content, SINGLE_LINE_MACRO_PATTERN, false, filterContext);
        content = filterMacros(content, MULTI_LINE_MACRO_PATTERN, true, filterContext);

        return content;
    }

    private String filterMacros(String content, Pattern pattern, boolean supportContent, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();
        Matcher matcher = pattern.matcher(content);

        int currentIndex = 0;
        RadeoxMacroConverter currentMacro = null;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (currentMacro != null && !currentMacro.isInline()) {
                before = CleanUtil.setLeadingNewLines(before, 2);
            }

            String allcontent = matcher.group(0);

            String macroName = matcher.group(1);
            String params = matcher.group(3);
            String macroContent = matcher.groupCount() >= 4 ? matcher.group(4) : null;

            boolean velocityOpen = false;
            boolean velocityClose = false;

            if (params != null) {
                // Clean velocity open macro inside params
                Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(params);
                velocityOpen |= velocityOpenMatcher.find();
                params = velocityOpenMatcher.replaceAll("");
                Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(params);
                velocityClose |= velocityCloseMatcher.find();
                params = velocityCloseMatcher.replaceAll("");
            }

            if (macroContent != null) {
                // Clean velocity close macro inside content
                Matcher velocityOpenMatcher = VelocityFilter.VELOCITYOPEN_PATTERN.matcher(macroContent);
                velocityOpen |= velocityOpenMatcher.find();
                macroContent = velocityOpenMatcher.replaceAll("");
                Matcher velocityCloseMatcher = VelocityFilter.VELOCITYCLOSE_PATTERN.matcher(macroContent);
                velocityClose |= velocityCloseMatcher.find();
                macroContent = velocityCloseMatcher.replaceAll("");
            }

            try {
                currentMacro = this.componentManager.lookup(RadeoxMacroConverter.class, macroName);

                if (currentMacro.supportContent() == supportContent) {
                    // a standalone new line is not interpreted by XWiki 1.0 rendering
                    before = CleanUtil.removeTrailingNewLines(before, 1, true);

                    if (!currentMacro.isInline()) {
                        before = CleanUtil.setTrailingNewLines(before, 2);
                    }

                    allcontent =
                        currentMacro.convert(macroName, getMacroParameters(currentMacro, params), macroContent,
                            filterContext);
                    if (currentMacro.protectResult()) {
                        allcontent = filterContext.addProtectedContent(allcontent, currentMacro.isInline());
                    }
                } else {
                    currentMacro = null;
                }
            } catch (ComponentLookupException e) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Can't find macro converter [" + macroName + "]", e);
                }

                currentMacro = null;
            }

            result.append(before);

            boolean multilines = allcontent.indexOf("\n") != -1;

            if (velocityOpen) {
                VelocityFilter.appendVelocityOpen(result, filterContext, multilines);
            }

            result.append(allcontent);

            if (velocityClose) {
                VelocityFilter.appendVelocityClose(result, filterContext, multilines);
            }
        }

        if (currentIndex == 0) {
            return content;
        }

        if (currentMacro != null && !currentMacro.isInline()) {
            result.append(CleanUtil.setLeadingNewLines(content.substring(currentIndex), 2));
        } else {
            result.append(content.substring(currentIndex));
        }

        return result.toString();
    }

    public static RadeoxMacroParameters getMacroParameters(RadeoxMacroConverter macroConverter, String parameters)
    {
        RadeoxMacroParameters parameterMap = new RadeoxMacroParameters();

        if (parameters != null) {
            String[] parameterTable = parameters.split("\\|");

            for (int parameterIndex = 0; parameterIndex < parameterTable.length; ++parameterIndex) {
                String parameter = parameterTable[parameterIndex];
                int equalIndex = parameter.indexOf('=');

                int parameterType = macroConverter.getParameterType(parameterIndex);
                String parameterName;
                String parameterValue;
                if (equalIndex >= 0) {
                    parameterName = parameter.substring(0, equalIndex);
                    parameterValue = parameter.substring(equalIndex + 1);
                } else {
                    parameterName = macroConverter.getParameterName(parameterIndex);
                    parameterValue = parameter;
                }

                if (parameterType != RadeoxMacroConverter.PARAMETER_SIMPLE
                    && ((parameterType & RadeoxMacroConverter.PARAMETER_NOTEMPTY) != 0 && parameterValue.trim()
                        .length() == 0)
                    || (((parameterType & RadeoxMacroConverter.PARAMETER_NOTNONE) != 0 && "none".equals(parameterValue)))) {
                    parameterValue = null;
                }

                parameterMap.addParameter(parameterIndex, parameterName, parameterValue);
            }
        }

        return parameterMap;
    }
}
