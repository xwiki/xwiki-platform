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
import org.wikimodel.wem.xhtml.filter.AccumulationXMLFilter;
import org.wikimodel.wem.xhtml.filter.DTDXMLFilter;
import org.wikimodel.wem.xhtml.filter.XHTMLWhitespaceXMLFilter;
import org.wikimodel.wem.xhtml.handler.TagHandler;
import org.xml.sax.XMLReader;
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
public class WikiModelXHTMLParser extends AbstractWikiModelParser
{
    /**
     * The XHTML syntax supported by this parser.
     */
    private static final Syntax SYNTAX = new Syntax(SyntaxType.XHTML, "1.0");

    private PrintRendererFactory printRendererFactory;

    /**
     * Used to create an optimized SAX XML Reader. In general SAX parsers don't cache DTD grammars and
     * as a consequence parsing a document with a grammar such as the XHTML DTD takes a lot more time
     * than required. Injected by the Component Manager. 
     */
    private XMLReaderFactory xmlReaderFactory;
    
    /**
     * The parser used for the link label parsing. For (x)html parsing, this will be an xwiki 2.0 parser, since it's 
     * more convenient to pass link labels in xwiki syntax. See referred resource for more details.
     * 
     * @see XWikiCommentHandler#handleLinkCommentStop(String, TagStack)
     */
    private Parser xwikiParser;

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
    	parser.setXmlReader(createXMLReader());
    	
    	return parser;
    }
    
    /**
     * Create a special XML filter chain so that we can use our extended implementation of
     * {@link XHTMLWhitespaceXMLFilter} that knows how to handle XWiki's special comment placeholders.
     * 
     * @return the top level XML Filter chain
     * @throws ParseException in case of an error while initializing the XML filters
     */
    private XMLReader createXMLReader() throws ParseException
    {
        XMLReader xmlReader;

        try {

            // Use a performant XML Reader (which does DTD caching for Xerces)
            xmlReader = this.xmlReaderFactory.createXMLReader();
            
            // Ignore SAX callbacks when the parser parses the DTD
            DTDXMLFilter dtdFilter = new DTDXMLFilter(xmlReader);
            
            // Add a XML Filter to accumulate onCharacters() calls since SAX
            // parser may call it several times.
            AccumulationXMLFilter accumulationFilter = new AccumulationXMLFilter(dtdFilter);
    
            // Add a XML Filter to remove non-semantic white spaces.
            XWikiXHTMLWhitespaceXMLFilter whitespaceFilter = 
                new XWikiXHTMLWhitespaceXMLFilter(accumulationFilter, false);
            
            xmlReader = whitespaceFilter;
            
        } catch (Exception e) {
            throw new ParseException("Failed to create XML reader", e);
        }
        
        return xmlReader;
    }
}
