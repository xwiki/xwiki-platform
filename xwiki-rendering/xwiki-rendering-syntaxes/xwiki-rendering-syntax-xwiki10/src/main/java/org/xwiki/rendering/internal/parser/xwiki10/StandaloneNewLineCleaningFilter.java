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
 * XWiki 1.0 does not interpret standalone new line in paragraph. This is because it render in as is in XHTML (which
 * does not interpret them).
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("standalonenewlinecleanning")
public class StandaloneNewLineCleaningFilter extends AbstractFilter implements Initializable
{
    private static final Pattern SANDALONENEWLINE_PATTERN = Pattern.compile("([^\\n])\\n([^\\n])", Pattern.MULTILINE);

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(4000);
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

        Matcher matcher = FilterContext.XWIKI1020TOKENS_PATTERN.matcher(content);
        int current = 0;
        for (; matcher.find(); current = matcher.end()) {
            String before = content.substring(current, matcher.start());

            result.append(SANDALONENEWLINE_PATTERN.matcher(before).replaceAll("$1 $2"));
            result.append(matcher.group(0));
        }

        result.append(SANDALONENEWLINE_PATTERN.matcher(content.substring(current)).replaceAll("$1 $2"));

        return result.toString();
    }
}
