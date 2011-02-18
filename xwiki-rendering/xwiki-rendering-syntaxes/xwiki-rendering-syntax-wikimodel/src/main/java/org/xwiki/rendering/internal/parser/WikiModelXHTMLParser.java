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
import org.wikimodel.wem.xhtml.impl.XhtmlHandler.TagStack;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser;
import org.xwiki.rendering.internal.parser.wikimodel.XWikiGeneratorListener;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XHTMLXWikiGeneratorListener;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiCommentHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiDivisionTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiHeaderTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiImageTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiReferenceTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiSpanTagHandler;
import org.xwiki.rendering.internal.parser.wikimodel.xhtml.XWikiTableDataTagHandler;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.parser.ResourceReferenceParser;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.StreamParser;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.util.IdGenerator;
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
     * The parser used for the link label parsing. For (x)html parsing, this will be an xwiki 2.0 parser, since it's
     * more convenient to pass link labels in xwiki syntax. See referred resource for more details.
     * 
     * @see XWikiCommentHandler#handleLinkCommentStop(String, TagStack)
     */
    @Requirement("xwiki/2.0")
    private StreamParser xwikiParser;

    /**
     * @see #getLinkReferenceParser()
     */
    @Requirement("link")
    private ResourceReferenceParser linkReferenceParser;

    /**
     * @see #getImageReferenceParser()
     */
    @Requirement("image")
    private ResourceReferenceParser imageReferenceParser;

    @Requirement("xwiki/2.1")
    private PrintRendererFactory xwikiSyntaxPrintRendererFactory;

    @Requirement
    private ComponentManager componentManager;

    @Requirement("xhtmlmarker")
    private ResourceReferenceParser xhtmlMarkerResourceReferenceParser;

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
        return Syntax.XHTML_1_0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractWikiModelParser#getLinkLabelParser()
     */
    @Override
    public StreamParser getLinkLabelParser()
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
        handlers.put("a", new XWikiReferenceTagHandler(this, this.xwikiSyntaxPrintRendererFactory));
        handlers.put("img", new XWikiImageTagHandler());
        handlers.put("span", new XWikiSpanTagHandler());
        handlers.put("div", new XWikiDivisionTagHandler());
        handlers.put("th", new XWikiTableDataTagHandler());

        XhtmlParser parser = new XhtmlParser();
        parser.setExtraHandlers(handlers);
        parser.setCommentHandler(new XWikiCommentHandler(this.componentManager, this, getImageReferenceParser(),
            this.xwikiSyntaxPrintRendererFactory, this.xhtmlMarkerResourceReferenceParser));

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
     * 
     * @see AbstractWikiModelParser#getLinkReferenceParser()
     * @since 2.5RC1
     */
    @Override
    public ResourceReferenceParser getLinkReferenceParser()
    {
        return this.linkReferenceParser;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.internal.parser.wikimodel.AbstractWikiModelParser#getImageReferenceParser()
     * @since 2.5RC1
     */
    @Override
    public ResourceReferenceParser getImageReferenceParser()
    {
        return this.imageReferenceParser;
    }

    /**
     * {@inheritDoc}
     *
     * @see AbstractWikiModelParser#createXWikiGeneratorListener(org.xwiki.rendering.listener.Listener , org.xwiki.rendering.util.IdGenerator)
     */
    @Override
    public XWikiGeneratorListener createXWikiGeneratorListener(Listener listener, IdGenerator idGenerator)
    {
        return new XHTMLXWikiGeneratorListener(getLinkLabelParser(), listener, getLinkReferenceParser(),
            getImageReferenceParser(), this.plainRendererFactory, idGenerator, getSyntax());
    }
}
