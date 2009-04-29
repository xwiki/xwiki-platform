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
import java.util.HashMap;
import java.util.Map;

import org.wikimodel.wem.IWikiParser;
import org.wikimodel.wem.xhtml.XhtmlParser;
import org.wikimodel.wem.xhtml.handler.TagHandler;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.parser.ImageParser;
import org.xwiki.rendering.parser.LinkParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiCommentHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiDivisionTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiHeaderTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiImageTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiReferenceTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiSpanTagHandler;
import org.xwiki.xml.XMLReaderFactory;

/**
 * Parses XHTML and generate a {@link XDOM} object.
 * 
 * @version $Id$
 * @since 1.5M2
 */
@Component("xhtml/1.0")
public class WikiModelXHTMLParser extends AbstractWikiModelParser
{
    /**
     * The XHTML syntax supported by this parser.
     */
    private static final Syntax SYNTAX = new Syntax(SyntaxType.XHTML, "1.0");

    /**
     * The parser used for the link label parsing. For (x)html parsing, this will be an xwiki 2.0 parser, since it's 
     * more convenient to pass link labels in xwiki syntax. See referred resource for more details.
     * 
     * @see XWikiCommentHandler#handleLinkCommentStop(String, TagStack)
     */
    @Requirement("xwiki/2.0")
    private Parser xwikiParser;

    /**
     * @see #getLinkParser()
     */
    @Requirement("xwiki/2.0")
    private LinkParser linkParser;

    /**
     * @see #getImageParser()
     */
    @Requirement("xwiki/2.0")
    private ImageParser imageParser;

    @Requirement
    private PrintRendererFactory printRendererFactory;

    /**
     * A special factory that create foolproof XML reader that have the following characteristics:
     * <ul>
     * <li>Use DTD caching when the underlying XML parser is Xerces</li>
     * <li>Ignore SAX callbacks when the parser parses the DTD</li>
     * <li>Accumulate onCharacters() calls since SAX parser may normally call this event several times.</li>
     * <li>Remove non-semantic white spaces where needed</li>
     * <li>Resolve DTDs locally to speed DTD loading/validation</li> 
     * </ul>
     */
    @Requirement("xwiki")
    private XMLReaderFactory xmlReaderFactory;
    
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
     * @see WikiModelXHTMLParser#parse(Reader)
     */
    @Override
    public XDOM parse(Reader source) throws ParseException
    {
        return super.parse(source);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiModelParser#getLinkLabelParser()
     */
    @Override
    public Parser getLinkLabelParser()
    {
        return this.xwikiParser;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiModelParser#createWikiModelParser()
     */
    @Override
    public IWikiParser createWikiModelParser() throws ParseException
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
    	handlers.put("div", new XWikiDivisionTagHandler());
    	
    	XhtmlParser parser = new XhtmlParser();
    	parser.setExtraHandlers(handlers);
    	parser.setCommentHandler(new XWikiCommentHandler(this, this.linkParser, this.imageParser, this.printRendererFactory));
    	
    	// Construct our own XML filter chain since we want to use our own Comment filter.
    	try {
    	    parser.setXmlReader(this.xmlReaderFactory.createXMLReader());
    	} catch (Exception e) {
    	    throw new ParseException("Failed to create XML reader", e);
    	}
    	
    	return parser;
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
