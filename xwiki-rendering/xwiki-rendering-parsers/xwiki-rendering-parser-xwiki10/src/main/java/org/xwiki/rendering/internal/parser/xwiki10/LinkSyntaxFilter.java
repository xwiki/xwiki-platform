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
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * @version $Id$
 * @since 1.8M1
 */
@Component("link")
public class LinkSyntaxFilter extends AbstractFilter implements Initializable
{
    private static final Pattern LINKSYNTAX_PATTERN = Pattern.compile("\\[(.+?)\\]");

    /**
     * {@inheritDoc}
     * @see Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        setPriority(1000);
    }

    public String filter(String content, FilterContext filterContext)
    {
        StringBuffer result = new StringBuffer();

        Matcher matcher = LINKSYNTAX_PATTERN.matcher(content);
        int current = 0;
        for (; matcher.find(); current = matcher.end()) {
            String before = content.substring(current, matcher.start());

            // a standalone new line is not interpreted by XWiki 1.0 rendering
            result.append(CleanUtil.removeLastNewLines(before, 1, true));

            StringBuffer linkResult = new StringBuffer();
            linkResult.append("[[");

            String str = matcher.group(1);
            if (str != null) {
                // TODO: This line creates bug XWIKI-188. The encoder seems to be broken. Fix this!
                // The only unescaping done should be %xx => char,
                // since &#nnn; must be preserved (the active encoding cannot handle the character)
                // and + should be preserved (for "Doc.C++ examples").
                // Anyway, this unescaper only treats &#nnn;
                // trim the name and unescape it
                // str = Encoder.unescape(str.trim());
                str = str.trim();
                String text = null, href = null, target = null;

                // Is there an alias like [alias|link] ?
                int pipeIndex = str.indexOf('|');
                int pipeLength = 1;
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf('>');
                }
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf("&gt;");
                    pipeLength = 4;
                }
                if (-1 != pipeIndex) {
                    text = str.substring(0, pipeIndex).trim();
                    str = str.substring(pipeIndex + pipeLength);
                }

                // Is there a target like [alias|link|target] ?
                pipeIndex = str.indexOf('|');
                pipeLength = 1;
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf('>');
                }
                if (pipeIndex == -1) {
                    pipeIndex = str.indexOf("&gt;");
                    pipeLength = 4;
                }
                if (-1 != pipeIndex) {
                    target = str.substring(pipeIndex + pipeLength).trim();
                    str = str.substring(0, pipeIndex);
                }
                // Done splitting

                // Fill in missing components
                href = str.trim();

                // Done, now print the link
                if (text != null) {
                    linkResult.append(text);
                    linkResult.append(">>");
                }

                linkResult.append(href);

                if (target != null) {
                    linkResult.append("||target=");
                    linkResult.append(target);
                }
            }

            linkResult.append("]]");

            result.append(filterContext.addProtectedContent(linkResult.toString(), true));
        }

        if (current == 0) {
            return content;
        }

        result.append(content.substring(current));

        return result.toString();
    }
}
