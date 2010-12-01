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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxType;
import org.xwiki.rendering.parser.xwiki10.Filter;
import org.xwiki.rendering.parser.xwiki10.FilterContext;
import org.xwiki.rendering.parser.xwiki10.util.CleanUtil;

/**
 * Convert XWiki 1.0 content into 2.0 content and call XWiki 2.0 parser to generate the XDOM.
 * 
 * @version $Id$
 * @since 1.8M1
 */
@Component("xwiki/1.0")
public class XWikiParser extends AbstractLogEnabled implements Parser, Initializable
{
    /**
     * The syntax identifier of the parser.
     */
    private static final Syntax SYNTAX = new Syntax(SyntaxType.XWIKI, "1.0");

    /**
     * Use to create the XDOM from converted content.
     */
    @Requirement("xwiki/2.0")
    private Parser xwiki20Parser;

    /**
     * The filters use to convert 1.0 content to 2.0.
     */
    @Requirement
    private List<Filter> filters;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        // order filters
        Collections.sort(this.filters, new Comparator<Filter>()
        {
            public int compare(Filter filter1, Filter filter2)
            {
                return filter1.getPriority() - filter2.getPriority();
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.Parser#getSyntax()
     */
    public Syntax getSyntax()
    {
        return SYNTAX;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.Parser#parse(java.io.Reader)
     */
    public XDOM parse(Reader source) throws ParseException
    {
        // Convert from 1.0 syntax to 2.0 syntax
        String content20 = xwiki10To20(source);

        // Generate the XDOM using 2.0 syntax parser
        return this.xwiki20Parser.parse(new StringReader(content20));
    }

    /**
     * Convert XWiki 1.0 content to 2.0.
     * 
     * @param source the 1.0 source.
     * @return the 2.0 converted content.
     * @throws ParseException error when converting content.
     */
    public String xwiki10To20(Reader source) throws ParseException
    {
        String content;
        try {
            content = IOUtils.toString(source);
        } catch (IOException e) {
            throw new ParseException("Failed to read source", e);
        }

        FilterContext filterContext = new FilterContext();

        for (Filter filter : this.filters) {
            content = filter.filter(content, filterContext);
        }

        content = filterContext.unProtect(content);

        content = CleanUtil.removeLeadingNewLines(content);
        content = CleanUtil.removeTrailingNewLines(content);

        return content;
    }
}
