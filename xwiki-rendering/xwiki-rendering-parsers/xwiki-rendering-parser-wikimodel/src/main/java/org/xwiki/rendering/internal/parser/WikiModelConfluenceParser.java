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

import org.wikimodel.wem.IWikiParser;
import org.wikimodel.wem.confluence.ConfluenceWikiParser;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;

/**
 * Parser for Confluence syntax.
 * 
 * @version $Id: $
 * @since 1.8.2
 */
@Component("confluence/1.0")
public class WikiModelConfluenceParser extends AbstractWikiModelParser
{
    /**
     * Confluence 1.0 Syntax.
     */
    private static final Syntax SYNTAX = new Syntax(SyntaxType.CONFLUENCE, "1.0");

    /**
     * @see #getLinkParser()
     */
    @Requirement
    private LinkParser linkParser;

    /**
     * @see #getImageParser()
     */
    @Requirement
    private ImageParser imageParser;

    /**
     * {@inheritDoc}
     * 
     * @see Parser#getSyntax()
     */
    public Syntax getSyntax()
    {
        return SYNTAX;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser#createWikiModelParser()
     */
    @Override
    public IWikiParser createWikiModelParser()
    {
        return new ConfluenceWikiParser();
    }
    
    /**
     * {@inheritDoc}
     * @see AbstractWikiModelParser#getImageParser()
     */
    @Override
    public ImageParser getImageParser()
    {
        return this.imageParser;
    }

    /**
     * {@inheritDoc}
     * @see AbstractWikiModelParser#getLinkParser()
     */
    @Override
    public LinkParser getLinkParser()
    {
        return this.linkParser;
    }
}
