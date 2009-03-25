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
package org.xwiki.rendering.internal.macro.html;

import java.io.StringReader;
import java.util.List;

import org.w3c.dom.Document;
import org.wikimodel.wem.xhtml.filter.AccumulationXMLFilter;
import org.wikimodel.wem.xhtml.filter.DTDXMLFilter;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.internal.parser.XMLBlockConverterHandler;
import org.xwiki.rendering.internal.parser.XWikiXHTMLWhitespaceXMLFilter;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.html.HTMLMacroParameters;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.xml.XMLReaderFactory;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;

/**
 * Allows inserting HTML and XHTML in wiki pages. This macro also accepts wiki syntax alongside (X)HTML elements (it's
 * also possible to disable this feature using a macro parameter). When wiki syntax is used inside XML elements, the
 * leading and trailing spaces and newlines are stripped.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class HTMLMacro extends AbstractMacro<HTMLMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Inserts HTML or XHTML code into the page.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the html content to insert in the page";

    /**
     * In order to speed up DTD loading/validation we use an entity resolver that can resolve DTDs locally. Injected by
     * the Component Manager.
     */
    protected EntityResolver entityResolver;

    /**
     * Injected by the Component Manager.
     */
    private HTMLCleaner htmlCleaner;

    /**
     * Used to create an optimized SAX XML Reader. In general SAX parsers don't cache DTD grammars and as a consequence
     * parsing a document with a grammar such as the XHTML DTD takes a lot more time than required. Injected by the
     * Component Manager.
     */
    private XMLReaderFactory xmlReaderFactory;

    /**
     * Injected by the Component Manager.
     */
    private Parser parser;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public HTMLMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            HTMLMacroParameters.class));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    public List<Block> execute(HTMLMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Parse the XHTML using an XML Parser and Wrap the XML elements in XMLBlock(s).
        // For each XML element's text, run it through the main Parser.

        XMLBlockConverterHandler handler = createContentHandler(parameters);

        try {
            // Use a performant XML Reader (which does DTD caching for Xerces)
            XMLReader xr = this.xmlReaderFactory.createXMLReader();

            // Ignore SAX callbacks when the parser parses the DTD
            DTDXMLFilter dtdFilter = new DTDXMLFilter(xr);

            // Add a XML Filter to accumulate onCharacters() calls since SAX
            // parser may call it several times.
            AccumulationXMLFilter accumulationFilter = new AccumulationXMLFilter(dtdFilter);

            // Add a XML Filter to remove non-semantic white spaces. We need to do that since all WikiModel
            // events contain only semantic information.
            XWikiXHTMLWhitespaceXMLFilter whitespaceFilter =
                new XWikiXHTMLWhitespaceXMLFilter(accumulationFilter, parameters.getWiki());

            whitespaceFilter.setContentHandler(handler);
            whitespaceFilter.setErrorHandler(handler);
            whitespaceFilter.setEntityResolver(this.entityResolver);

            // Allow access to CDATA and XML Comments.
            whitespaceFilter.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

            // Since XML can only have a single root node and since we want to allow users to put
            // content such as the following, we need to wrap the content in a root node:
            // <tag1>
            // ..
            // </tag1>
            // <tag2>
            // </tag2>
            // In addition we also need to ensure the XHTML DTD is defined so that valid XHTML entities can be
            // specified.

            // TODO: Find a good way to use either DOM4J or JDOM's SAXOutputter/SAXWriter for better performances.
            // Right now there are the following problems:
            // - with SAXOutputter I don't see how to pass our SAX filter chain
            // - with SAXWriter the output is a bit strange

            // Clean the HTML to transform it into valid XHTML
            // Note that we trim the content since we want to be lenient with the user in case he has entered
            // some spaces/newlines before a XML declaration (prolog). Otherwise the XML parser would fail to parse.
            Document document = this.htmlCleaner.clean(new StringReader(content.trim()));

            // Remove the HMTL envelope since this macro is only a fragment of a page which will already have an
            // HTML envelope when rendered.
            XMLUtils.stripHTMLEnvelope(document);
            String cleanedText = XMLUtils.toString(document);
            whitespaceFilter.parse(new InputSource(new StringReader(cleanedText)));

        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse HTML content [" + content + "]", e);
        }

        return handler.getRootBlock().getChildren();
    }

    /**
     * Create a SAX {@link org.xml.sax.ContentHandler} to parse the passed XML.
     * 
     * @param xhtmlParameters the macro parameters since the behavior of the content handler depend on them. For example
     *            the rendering of XML text as wiki syntax depends on such a parameter.
     * @return the content handler to use
     * @throws MacroExecutionException if the passed parameter is invalid for some reason
     */
    protected XMLBlockConverterHandler createContentHandler(HTMLMacroParameters xhtmlParameters)
        throws MacroExecutionException
    {
        // Check if the user has asked to interpret wiki syntax or not
        boolean interpretWikiSyntax = xhtmlParameters.getWiki();

        return new XMLBlockConverterHandler(this.parser, interpretWikiSyntax);
    }
}
