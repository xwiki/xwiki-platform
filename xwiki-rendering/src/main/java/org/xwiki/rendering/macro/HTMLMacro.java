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
package org.xwiki.rendering.macro;

import org.xwiki.rendering.macro.parameter.descriptor.MacroParameterDescriptor;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.block.Block;
import org.xwiki.xml.html.HTMLCleaner;
import org.xwiki.xml.XMLUtils;
import org.jdom.input.DOMBuilder;
import org.jdom.output.SAXOutputter;
import org.jdom.JDOMException;
import org.w3c.dom.Document;

import java.util.Map;
import java.util.Collections;
import java.util.List;

/**
 * Allows inserting HTML in wiki pages. Allows the HTML to be non XHTML and transforms it into valid XHTML.
 *
 * @version $Id: $
 * @since 1.6M1
 */
public class HTMLMacro extends XHTMLMacro
{
    private static final String DESCRIPTION = "Inserts HTML code into the page.";

    /**
     * Injected by the Component Manager.
     */
    private HTMLCleaner htmlCleaner;
    
    /**
     * {@inheritDoc}
     *
     * @see Macro#getDescription()
     */
    public String getDescription()
    {
        // TODO: Use an I8N service to translate the description in several languages
        return DESCRIPTION;
    }

    /**
     * {@inheritDoc}
     *
     * @see Macro#getAllowedParameters()
     */
    public Map<String, MacroParameterDescriptor< ? >> getAllowedParameters()
    {
        // We send a copy of the map and not our map since we don't want it to be modified.
        return Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     *
     * @see org.xwiki.rendering.macro.Macro#execute(java.util.Map, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(Map<String, String> parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        DOMBuilder builder = new DOMBuilder();

        // clean the HTML to transform it into valid XHTML
        Document document = this.htmlCleaner.clean(content);

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
