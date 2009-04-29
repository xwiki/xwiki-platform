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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.PrintRenderer;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.rendering.renderer.printer.XHTMLWikiPrinter;
import org.xwiki.rendering.util.ParserUtils;
import org.xwiki.xml.html.HTMLConstants;

/**
 * XML SAX handler that parses wiki syntax in XML element text and generate XHTML in output (as a String).
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XMLBlockConverterHandler extends DefaultHandler implements LexicalHandler
{
    /**
     * The parser to use to interpret the wiki syntax.
     */
    private Parser wikiParser;

    /**
     * The factory that will allow us to create a XHTML renderer to convert the wiki syntax into XHTML.
     */
    private PrintRendererFactory rendererFactory; 

    /**
     * Printer we use to serialize XML.
     */
    private XHTMLWikiPrinter xhtmlPrinter;
    
    /**
     * The printer containing the XHTML as a String.
     */
    private WikiPrinter printer;

    /**
     * If true then the user has asked to clean up the HTML he entered.
     */
    private boolean clean;
    
    /**
     * If true then XML element content contain wiki syntax.
     */
    private boolean wiki;
    
    /**
     * Used to parse inline content.
     */
    private ParserUtils inlineConverter = new ParserUtils();

    /**
     * @param wikiParser the parser to use to interpret the wiki syntax.
     * @param rendererFactory the factory that will allow us to create a XHTML renderer to use to convert the wiki 
     *        syntax into XHTML.
     * @param clean if true then the user has asked to clean up the HTML he entered
     * @param wiki if true then XML element contents contain wiki syntax
     */
    public XMLBlockConverterHandler(Parser wikiParser, PrintRendererFactory rendererFactory, boolean clean, 
        boolean wiki)
    {
        this.wikiParser = wikiParser;
        this.rendererFactory = rendererFactory;
        this.clean = clean;
        this.wiki = wiki;
        
        this.printer = new DefaultWikiPrinter();
        this.xhtmlPrinter = new XHTMLWikiPrinter(this.printer);
    }

    /**
     * @return the XHTML as a string
     */
    public String getOutput()
    {
        return this.printer.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        String content = new String(ch, start, length);

        // If we've been told by the user to not render wiki syntax we simply pass the text as is
        if (!this.wiki) {
            this.printer.print(content);
        } else {
            // Parse the content containing wiki syntax.
            XDOM dom;
            try {
                dom = this.wikiParser.parse(new StringReader(content));
            } catch (ParseException e) {
                throw new SAXException("Failed to parse [" + content + "]", e);
            }
    
            // Remove any paragraph that might have been added since we don't want paragraphs.
            // For example we want to generate <h1>hello</h1> and not <h1><p>hello</p></h1>.
            List<Block> children = dom.getChildren();
            this.inlineConverter.removeTopLevelParagraph(children);
    
            // Generate XHTML
            PrintRenderer xhtmlRenderer = 
                this.rendererFactory.createRenderer(new Syntax(SyntaxType.XHTML, "1.0"), this.xhtmlPrinter);
            dom.traverse(xhtmlRenderer);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String,
     *      org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException
    {
        // If we're in clean mode and we encounter a <html> tag don't print it (since the macro is inserting a 
        // XHTML fragment into an existing XHTML document. In non clean mode we honor whatever the user enters.
        if (!clean || !HTMLConstants.TAG_HTML.equalsIgnoreCase(name)) {
            
            Map<String, String> map = new LinkedHashMap<String, String>();
            for (int i = 0; i < attributes.getLength(); i++) {
                // The XHTML DTD specifies some default value for some attributes. For example for a TD element
                // it defines colspan=1 and rowspan=1. Thus we'll get a colspan and rowspan attribute passed to
                // the current method even though they are not defined in the source XHTML content.
                // However with SAX2 it's possible to check if an attribute is defined in the source or not using
                // the Attributes2 class.
                // See http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description
                if (attributes instanceof Attributes2) {
                    Attributes2 attributes2 = (Attributes2) attributes;
                    // If the attribute is present in the XHTML source file then add it, otherwise skip it.
                    if (attributes2.isSpecified(i)) {
                        map.put(attributes.getQName(i), attributes.getValue(i));
                    }
                } else {
                    map.put(attributes.getQName(i), attributes.getValue(i));
                }
            }
            this.xhtmlPrinter.printXMLStartElement(name, map);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String name) throws SAXException
    {
        // If we're in clean mode and we encounter a <html> tag don't print it (since the macro is inserting a 
        // XHTML fragment into an existing XHTML document. In non clean mode we honor whatever the user enters.
        if (!clean || !HTMLConstants.TAG_HTML.equalsIgnoreCase(name)) {

            this.xhtmlPrinter.printXMLEndElement(name);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ext.LexicalHandler#comment(char[], int, int)
     */
    public void comment(char[] value, int offset, int count) throws SAXException
    {
        this.xhtmlPrinter.printXMLComment(new String(value, offset, count));
    }

    /**
     * {@inheritDoc}
     * 
     * @see LexicalHandler#startCDATA()
     */
    public void startCDATA() throws SAXException
    {
        this.xhtmlPrinter.printXMLStartCData();
    }

    /**
     * {@inheritDoc}
     * 
     * @see LexicalHandler#endCDATA()
     */
    public void endCDATA() throws SAXException
    {
        this.xhtmlPrinter.printXMLEndCData();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ext.LexicalHandler#startDTD(java.lang.String, java.lang.String, java.lang.String)
     */
    public void startDTD(String arg0, String arg1, String arg2) throws SAXException
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ext.LexicalHandler#endDTD()
     */
    public void endDTD() throws SAXException
    {
        // Nothing to do
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ext.LexicalHandler#startEntity(java.lang.String)
     */
    public void startEntity(String arg0) throws SAXException
    {
        // Nothing to do since an entity definition shouldn't be present in a XHTML macro content
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.ext.LexicalHandler#endEntity(java.lang.String)
     */
    public void endEntity(String arg0) throws SAXException
    {
        // Nothing to do since an entity definition shouldn't be present in a XHTML macro content
    }
}
