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

import java.util.HashMap;
import java.util.Map;

import org.wikimodel.wem.IWikiParser;
import org.wikimodel.wem.xhtml.XhtmlParser;
import org.wikimodel.wem.xhtml.handler.TagHandler;
import org.wikimodel.wem.xwiki.XWikiXhtmlEscapeHandler;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiCommentHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiHeaderTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiImageTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiReferenceTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiSpanTagHandler;

/**
 * Parses XHTML and generate a {@link XDOM} object.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class WikiModelXHTMLParser extends AbstractWikiModelParser
{
    private static final Syntax SYNTAX = new Syntax(SyntaxType.XHTML, "1.0");

    private PrintRendererFactory printRendererFactory;
    
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
     * @see AbstractWikiModelParser#createWikiModelParser()
     */
    @Override
    public IWikiParser createWikiModelParser()
    {
    	// Override some of the WikiModel XHTML parser tag handlers to introduce our own logic.
    	Map<String, TagHandler> handlers = new HashMap<String, TagHandler>();
    	TagHandler handler = new XWikiHeaderTagHandler();
    	handlers.put("h1", handler);
    	handlers.put("h2", handler);
    	handlers.put("h3", handler);
    	handlers.put("h4", handler);
    	handlers.put("h5", handler);
    	handlers.put("h6", handler);
    	handlers.put("a", new XWikiReferenceTagHandler());
        handlers.put("img", new XWikiImageTagHandler());
    	handlers.put("span", new XWikiSpanTagHandler());
    	
    	XhtmlParser parser = new XhtmlParser();
    	parser.setExtraHandlers(handlers);
    	parser.setEscapeHandler(new XWikiXhtmlEscapeHandler());
    	parser.setCommentHandler(
    	    new XWikiCommentHandler(this, this.linkParser, this.urlFactory, this.printRendererFactory));
    	return parser;
    }
}
