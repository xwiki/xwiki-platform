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

import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.internal.macro.xhtml.XHTMLMacro;
import org.xwiki.rendering.internal.parser.XMLBlockConverterHandler;
import org.xwiki.rendering.macro.xhtml.XHTMLMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.XMLUtils;
import org.jdom.input.DOMBuilder;
import org.jdom.output.SAXOutputter;
import org.jdom.JDOMException;
import org.w3c.dom.Document;

import java.io.StringReader;
import java.util.List;

/**
 * Allows inserting HTML in wiki pages. Allows the HTML to be non XHTML and transforms it into valid XHTML.
 * 
 * @version $Id$
 * @since 1.6M1
 */
public class HTMLMacro extends XHTMLMacro
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Inserts XHTML code into the page.";

    /**
     * Injected by the Component Manager.
     */
    private HTMLCleaner htmlCleaner;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public HTMLMacro()
    {
        super(DESCRIPTION);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(Object, String, MacroTransformationContext)
     */
    @Override
    public List<Block> execute(XHTMLMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        DOMBuilder builder = new DOMBuilder();

        // clean the HTML to transform it into valid XHTML
        Document document = this.htmlCleaner.clean(new StringReader(content));

        // Remove the HMTL envelope since this macro is only a fragment of a page which will already have an
        // HTML envelope when rendered.
        XMLUtils.stripHTMLEnvelope(document);

        org.jdom.Document jdomDoc = builder.build(document);

        XMLBlockConverterHandler handler = createContentHandler(parameters);

        SAXOutputter outputter = new SAXOutputter(handler, handler, null, this.entityResolver);
        try {
            outputter.output(jdomDoc);
        } catch (JDOMException e) {
            throw new MacroExecutionException("Failed to parse content as XML [" + content + "]", e);
        }

        return handler.getRootBlock().getChildren();
    }
}
