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
package org.xwiki.rendering.internal.macro.velocity.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;

/**
 * Replace each white space/new lines group by a space and inject $nl and $sp bindings in {@link VelocityContext} which
 * are used to respectively force a new line or a space before executing the velocity script. The bindings are removed
 * after script execution.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component("html")
public class HTMLVelocityMacroFilter implements VelocityMacroFilter
{
    /**
     * The name of the new line binding.
     */
    private static final String BINDING_NEWLINE = "nl";

    /**
     * The name of the space binding.
     */
    private static final String BINDING_SPACE = "sp";

    /**
     * Match velocity key works containing parameters and consuming trailing new line.
     */
    private static final String PATTERNS_KEYWORDSWITHPARAMS =
        "(?:(?:#if|#elseif|#set|#foreach|#macro)\\s*\\(.*\\)(?:\n|\r\n|\r))";

    /**
     * Match velocity key works without parameters and consuming trailing new line.
     */
    private static final String PATTERNS_KEYWORDSSIMPLE = "(?:(?:#else|#end|(?:##[^\n\r]*))(?:\n|\r\n|\r))";

    /**
     * Match expression to clean.
     */
    private static final Pattern PATTERN_VELOCITYKEYWORDS =
        Pattern.compile("((\\s*)(" + PATTERNS_KEYWORDSSIMPLE + "|" + PATTERNS_KEYWORDSWITHPARAMS
            + ")\\s*)|(\\s*\\$\\{?nl\\}?$\\s*)|(\\s+)", Pattern.MULTILINE);

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter#after(java.lang.String,
     *      org.apache.velocity.VelocityContext)
     */
    public String before(String content, VelocityContext velocityContect)
    {
        String cleanedContent = content;

        Matcher matcher = PATTERN_VELOCITYKEYWORDS.matcher(content);

        StringBuffer result = new StringBuffer();

        int index = 0;
        for (; matcher.find(); index = matcher.end()) {
            result.append(content.substring(index, matcher.start()));

            if (matcher.group(1) != null) {
                if (matcher.group(2) != null && matcher.group(2).replaceFirst(" *$", "").length() > 0) {
                    result.append(" ");
                }
                result.append(matcher.group(3));
            } else if (matcher.group(4) != null) {
                result.append("${nl}");
            } else {
                result.append(" ");
            }
        }

        if (index > 0) {
            result.append(content.substring(index));

            cleanedContent = result.toString();
        }

        velocityContect.put(BINDING_NEWLINE, "\n");
        velocityContect.put(BINDING_SPACE, " ");

        return cleanedContent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter#before(java.lang.String,
     *      org.apache.velocity.VelocityContext)
     */
    public String after(String content, VelocityContext velocityContect)
    {
        velocityContect.remove(BINDING_NEWLINE);
        velocityContect.remove(BINDING_SPACE);

        return content;
    }
}
