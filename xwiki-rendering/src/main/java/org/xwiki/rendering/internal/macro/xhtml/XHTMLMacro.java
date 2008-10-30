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
package org.xwiki.rendering.internal.macro.xhtml;

import java.io.StringReader;
import java.util.List;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.EntityResolver;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.xhtml.XHTMLMacroParameters;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XHTMLMacro extends AbstractMacro<XHTMLMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Inserts XHTML code into the page.";

    /**
     * In order to speed up DTD loading/validation we use an entity resolver that can resolve DTDs locally. Injected by
     * the Component Manager.
     */
    protected EntityResolver entityResolver;

    /**
     * Injected by the Component Manager.
     */
    private Parser parser;

    /**
     * Create and initialize the description of the macro.
     */
    public XHTMLMacro()
    {
        this(DESCRIPTION);
    }

    /**
     * @param description the macro description.
     */
    protected XHTMLMacro(String description)
    {
        super(new DefaultMacroDescriptor(description, XHTMLMacroParameters.class));
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
    public List<Block> execute(XHTMLMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Parse the XHTML using an XML Parser and Wrap the XML elements in XMLBlock(s).
        // For each XML element's text, run it through the main Parser.

        XMLBlockConverterHandler handler = createContentHandler(parameters);

        try {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            xr.setEntityResolver(this.entityResolver);
            
            // Allow access to CDATA and XML Comments.
            xr.setProperty("http://xml.org/sax/properties/lexical-handler", handler);

            // Since XML can only have a single root node and since we want to allow users to put
            // content such as the following, we need to wrap the content in a root node:
            // <tag1>
            // ..
            // </tag1>
            // <tag2>
            // </tag2>
            String normalizedContent =
                "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\"\n"
                    + "\"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n" + "<root>" + content + "</root>";

            xr.parse(new InputSource(new StringReader(normalizedContent)));
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse content as XML [" + content + "]", e);
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
    protected XMLBlockConverterHandler createContentHandler(XHTMLMacroParameters xhtmlParameters)
        throws MacroExecutionException
    {
        // Check if the user has asked to escape wiki syntax or not
        boolean escapeWikiSyntax = xhtmlParameters.isEscapeWikiSyntax();

        return new XMLBlockConverterHandler(this.parser, escapeWikiSyntax);
    }
}
