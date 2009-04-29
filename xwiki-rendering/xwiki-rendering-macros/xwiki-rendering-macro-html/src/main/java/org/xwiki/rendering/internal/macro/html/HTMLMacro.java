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
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.rendering.internal.parser.XWikiXHTMLWhitespaceXMLFilter;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.html.HTMLMacroParameters;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.parser.Syntax;
import org.xwiki.rendering.parser.SyntaxType;
import org.xwiki.rendering.renderer.PrintRendererFactory;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.xml.XMLReaderFactory;
import org.xwiki.xml.XMLUtils;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.html.HTMLConstants;
import org.xwiki.xml.html.HTMLUtils;

/**
 * Allows inserting HTML and XHTML in wiki pages. This macro also accepts wiki syntax alongside (X)HTML elements (it's
 * also possible to disable this feature using a macro parameter). When wiki syntax is used inside XML elements, the
 * leading and trailing spaces and newlines are stripped.
 * 
 * @version $Id$
 * @since 1.6M1
 */
@Component("html")
public class HTMLMacro extends AbstractMacro<HTMLMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Inserts HTML or XHTML code into the page.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "The HTML content to insert in the page.";

    /**
     * The syntax representing the output of this macro (used for the RawBlock).
     */
    private static final Syntax XHTML_SYNTAX = new Syntax(SyntaxType.XHTML, "1.0");
    
    /**
     * To clean the passed HTML so that it's valid XHTML (this is required since we use an XML parser to parse it).
     */
    @Requirement
    private HTMLCleaner htmlCleaner;

    /**
     * The factory that will allow us to create a XHTML renderer to convert the wiki syntax into XHTML.
     */
    @Requirement
    private PrintRendererFactory rendererFactory;
    
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
     * The parser to use when the user specifies that HTML content should be parsed in wiki syntax.
     * 
     * @todo make this generic by loading the parser dynamically from the Macro execution context syntax
     */
    @Requirement("xwiki/2.0")
    private Parser wikiParser;

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
        List<Block> blocks;
        String normalizedContent = content;
        
        // Clean the HTML into valid XHTML if the user has asked (it's the default).
        if (parameters.getClean()) {
            // Note that we trim the content since we want to be lenient with the user in case he has entered
            // some spaces/newlines before a XML declaration (prolog). Otherwise the XML parser would fail to parse.
            Document document = this.htmlCleaner.clean(new StringReader(content.trim()));

            // Since XML can only have a single root node and since we want to allow users to put
            // content such as the following, we need to wrap the content in a root node:
            // <tag1>
            // ..
            // </tag1>
            // <tag2>
            // </tag2>
            // In addition we also need to ensure the XHTML DTD is defined so that valid XHTML entities can be
            // specified.

            // Remove the HMTL envelope since this macro is only a fragment of a page which will already have an
            // HTML envelope when rendered. We remove it so that the HTML <head> tag isn't output.
            HTMLUtils.stripHTMLEnvelope(document);
            
            // If in inline mode remove the top level paragraph if there's one.
            if (context.isInline()) {
                HTMLUtils.stripFirstElementInside(document, HTMLConstants.TAG_HTML, HTMLConstants.TAG_P);
            }
            
            normalizedContent = XMLUtils.toString(document);
        }
        
        // If the user has mentioned that there's wiki syntax in the macro then we need to parse the content using
        // an XML parser. We also use a XML parser if the user has asked to clean since it's the easiest way to
        // ignore XML declaration, doctype, html element and the first paragraph if in inline mode.
        if (parameters.getClean() || parameters.getWiki()) {
            normalizedContent = parseXHTML(normalizedContent, parameters.getClean(), parameters.getWiki());
        }

        blocks = Arrays.asList((Block) new RawBlock(normalizedContent, XHTML_SYNTAX));
        
        return blocks;
    }
    
    /**
     * Parse the XHTML using a XML parser since XML elements can contain wiki syntax which we parse with a XWiki syntax
     * Parser and convert to XHTML.
     * 
     * @param xhtml the XHTML to parse
     * @return the output XHTML as a string containing the XWiki Syntax resolved as XHTML
     * @param clean if true then the user has asked to clean up the HTML he entered
     * @param wiki if true then XML element contents contain wiki syntax
     * @throws MacroExecutionException in case there's a parsing problem
     */
    private String parseXHTML(String xhtml, boolean clean, boolean wiki) throws MacroExecutionException
    {
        XMLBlockConverterHandler handler = new XMLBlockConverterHandler(this.wikiParser, this.rendererFactory,
            clean, wiki);

        try {
            XMLReader xr = this.xmlReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            
            // Control whitespace stripping depending on whether XHTML elements contain wiki syntax or not.
            xr.setProperty(XWikiXHTMLWhitespaceXMLFilter.SAX_CONTAINS_WIKI_SYNTAX_PROPERTY, wiki);

            // Allow access to CDATA and XML Comments.
            xr.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

            xr.parse(new InputSource(new StringReader(xhtml)));

        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse HTML content [" + xhtml + "]", e);
        }

        return handler.getOutput();
    }
}
