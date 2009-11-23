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
 * @version $Id$
 * @since 1.8M1
 */
@Component("escape20")
public class Escape20SyntaxFilter extends AbstractFilter implements Initializable
{
    private static final String LISTSYNTAX_SPATTERN =
        Pattern.quote("*") + "|" + Pattern.quote(":") + "|" + Pattern.quote(";") + "|" + Pattern.quote("1.");

    private static final String CELLSYNTAX_SPATTERN =
        Pattern.quote("|=") + "|" + Pattern.quote("!=") + "|" + Pattern.quote("!!") + "|" + Pattern.quote("|");

    private static final String FORMATSYNTAX_SPATTERN =
        Pattern.quote("//") + "|" + Pattern.quote("**") + "|" + Pattern.quote("__") + "|" + Pattern.quote("--") + "|"
            + Pattern.quote("^^") + "|" + Pattern.quote(",,") + "|" + Pattern.quote("##");

    private static final String NEWLINESYNTAX_CONTENT_SPATTERN = LISTSYNTAX_SPATTERN + "|" + Pattern.quote("=");

    private static final String NEWLINESYNTAX_SPATTERN = "^( *)((?:" + NEWLINESYNTAX_CONTENT_SPATTERN + "))";

    private static final String INLINESYNTAX_PATTERN =
        FORMATSYNTAX_SPATTERN + "|" + CELLSYNTAX_SPATTERN + "|" + Pattern.quote("~") + "|" + Pattern.quote("{{") + "|"
            + Pattern.quote("}}") + "|" + Pattern.quote("(((") + "|" + Pattern.quote(")))");

    private static final Pattern SYNTAX_PATTERN =
        Pattern.compile("(" + NEWLINESYNTAX_SPATTERN + ")|(" + INLINESYNTAX_PATTERN + ")", Pattern.MULTILINE);

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(10000);
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

        Matcher matcher = SYNTAX_PATTERN.matcher(content);
        int currentIndex = 0;
        for (; matcher.find(); currentIndex = matcher.end()) {
            result.append(content.substring(currentIndex, matcher.start()));

            if (matcher.group(1) != null) {
                result.append(matcher.group(2) + "~" + matcher.group(3));
            } else {
                result.append("~" + matcher.group(4));
            }
        }

        if (currentIndex == 0) {
            return content;
        }

        result.append(content.substring(currentIndex));

        return result.toString();
    }
}
