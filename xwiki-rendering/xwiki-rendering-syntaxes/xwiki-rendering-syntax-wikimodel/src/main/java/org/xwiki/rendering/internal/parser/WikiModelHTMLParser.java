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
package org.xwiki.rendering.internal.parser;

import java.io.Reader;
import java.io.StringReader;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLUtils;

/**
 * Parses HTML and generate a {@link XDOM} object.
 * 
 * @version $Id$
 * @since 1.5M2
 */
@Component("html/4.01")
public class WikiModelHTMLParser extends WikiModelXHTMLParser
{
    /**
     * Used to clean the HTML into valid XHTML. Injected by the Component Manager.
     */
    @Requirement
    private HTMLCleaner htmlCleaner;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.parser.Parser#getSyntax()
     */
    @Override
    public Syntax getSyntax()
    {
        return Syntax.HTML_4_01;
    }

    /**
     * {@inheritDoc}
     * 
     * @see WikiModelXHTMLParser#parse(Reader)
     */
    @Override
    public XDOM parse(Reader source) throws ParseException
    {
        return super.parse(new StringReader(HTMLUtils.toString(this.htmlCleaner.clean(source))));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser#parse(java.io.Reader,
     *      org.xwiki.rendering.listener.Listener)
     */
    @Override
    public void parse(Reader source, Listener listener) throws ParseException
    {
        super.parse(new StringReader(HTMLUtils.toString(this.htmlCleaner.clean(source))), listener);
    }
}
