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

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.phase.Composable;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.macro.VelocityMacroConverter;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Convert 1.0 Velocity macros to 2.0 macro. A conversion can be added by implementing VelocityMacroConverter.
 * 
 * @version $Id$
 * @since 1.8M1
 */
public class VelocityMacrosFilter extends AbstractFilter implements Composable
{
    /**
     * Regex pattern for matching macros that are written on single line.
     */
    public static final Pattern VELOCITY_MACRO_PATTERN = Pattern.compile("\\#(\\w+)\\(([^)]*)\\)");

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
        Matcher matcher = VELOCITY_MACRO_PATTERN.matcher(content);

        int currentIndex = 0;
        VelocityMacroConverter currentMacro = null;
        for (; matcher.find(); currentIndex = matcher.end()) {
            String before = content.substring(currentIndex, matcher.start());

            if (currentMacro != null && !currentMacro.isInline()) {
                before = CleanUtil.setFirstNL(before, 2);
            }

            String allcontent = matcher.group(0);

            String macroName = matcher.group(1);
            String params = matcher.group(2);

            try {
                currentMacro =
                    (VelocityMacroConverter) this.componentManager.lookup(VelocityMacroConverter.ROLE, macroName);

                // a standalone new line is not interpreted by XWiki 1.0 rendering
                before = CleanUtil.removeLastStandaloneNewLine(before);

                if (!currentMacro.isInline()) {
                    before = CleanUtil.setLastNL(before, 2);
                }

                allcontent = currentMacro.convert(macroName, getMacroParameters(params));
                if (currentMacro.protectResult()) {
                    allcontent = filterContext.addProtectedContent(allcontent);
                }
            } catch (ComponentLookupException e) {
                if (getLogger().isDebugEnabled()) {
                    getLogger().debug("Can't find macro converter [" + macroName + "]", e);
                }

                currentMacro = null;
            }

            result.append(before);
            result.append(allcontent);
        }

        if (currentIndex == 0) {
            return content;
        }

        if (currentMacro != null && !currentMacro.isInline()) {
            result.append(CleanUtil.setFirstNL(content.substring(currentIndex), 2));
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
}
