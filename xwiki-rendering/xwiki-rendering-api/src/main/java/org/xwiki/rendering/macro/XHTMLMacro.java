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

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.DOM;
import org.xwiki.rendering.parser.Parser;

public class XHTMLMacro extends AbstractMacro
{
    /**
     * Injected by the Component Manager.
     */
    private Parser parser;
    
    /**
     * {@inheritDoc}
     * @see Macro#execute(Map, String, org.xwiki.rendering.block.DOM)
     */
    public List<Block> execute(Map<String, String> parameters, String content, DOM dom)
        throws MacroExecutionException
    {
        // Parse the XHTML using an XML Parser and Wrap the XML elements in XMLBlock(s).
        // For each XML element's text, run it through the main Parser.
        
        XMLBlockConverterHandler handler;
        try {
            XMLReader xr = XMLReaderFactory.createXMLReader();
            handler = new XMLBlockConverterHandler(this.parser);
            xr.setContentHandler(handler);
            xr.setErrorHandler(handler);
            
            // Since XML can only have a single root node and since we want to allow users to put
            // content such as the following, we need to wrap the content in a root node:
            //   <tag1>
            //     ..
            //   </tag1>
            //   <tag2>
            //   </tag2>
            String normalizedContent = "<root>" + content + "</root>";
            
            xr.parse(new InputSource(new StringReader(normalizedContent)));
        } catch (Exception e) {
            throw new MacroExecutionException("Failed to parse content as XML ["
                + content + "]", e);
        }

        return handler.getRootBlock().getChildren();
    }

}
