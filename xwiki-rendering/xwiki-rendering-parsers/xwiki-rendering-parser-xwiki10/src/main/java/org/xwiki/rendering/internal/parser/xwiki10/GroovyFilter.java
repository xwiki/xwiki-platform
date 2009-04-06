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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.parser.xwiki10.AbstractFilter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * @version $Id$
 * @since 1.8RC3
 */
@Component("groovy")
public class GroovyFilter extends AbstractFilter implements Initializable
{
    public static final String GROOVY_BEGIN = "<%";

    public static final String GROOVY_END = "%>";

    /**
     * {@inheritDoc}
     * 
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(30);
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

        String currentContent = content;
        for (int index = currentContent.indexOf(GROOVY_BEGIN); index != -1; index =
                currentContent.indexOf(GROOVY_BEGIN)) {
            result.append(currentContent.substring(0, index));

            currentContent = currentContent.substring(index + 2);

            int endIndex = currentContent.indexOf(GROOVY_END);

            String groovyContent;
            if (endIndex != -1) {
                groovyContent = currentContent.substring(0, endIndex);
                currentContent = currentContent.substring(endIndex + GROOVY_END.length());
            } else {
                groovyContent = currentContent;
                currentContent = "";
            }

            if (groovyContent.trim().length() > 0) {
                boolean newline = currentContent.startsWith("\n");

                result
                    .append(filterContext.addProtectedContent("{{groovy}}" + groovyContent + "{{/groovy}}", !newline));

                if (newline) {
                    currentContent = CleanUtil.setLeadingNewLines(currentContent, 2);
                }
            }
        }

        result.append(currentContent);

        return result.toString();
    }
}
