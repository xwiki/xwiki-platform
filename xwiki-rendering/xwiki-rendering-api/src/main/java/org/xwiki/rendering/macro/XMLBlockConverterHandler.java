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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.XMLBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.listener.Listener;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XMLBlockConverterHandler extends DefaultHandler
{
    private Parser parser;

    private boolean escapeWikiSyntax;
    
    private Stack<Block> stack = new Stack<Block>();
    
    /**
     * SAX parsers are allowed to call the characters() method several times in a row.
     * Some parsers have a buffer of 8K (Crimson), others of 16K (Xerces) and others can
     * even call characters() for every single character! Thus we need to accumulate
     * the characters in a buffer before we process them.
     */
    private StringBuffer accumulationBuffer = new StringBuffer();

    private final MarkerBlock marker = new MarkerBlock();

    private class MarkerBlock extends AbstractBlock
    {
        public void traverse(Listener listener)
        {
        }
    }

    public XMLBlockConverterHandler(Parser parser, boolean escapeWikiSyntax)
    {
        this.parser = parser;
        this.escapeWikiSyntax = escapeWikiSyntax;
    }
    
    public Block getRootBlock()
    {
        return this.stack.peek();
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        this.accumulationBuffer.append(ch, start, length);
    }

    private void processCharacters(char[] ch, int start, int length) throws SAXException
    {
        // Ignore white space/new lines characters between XHTML elements and remove whitespaces at beginning
        // and end of elements. We do this since this is the HTML behavior.
        String content = new String(ch, start, length).trim();

        if (content.length() > 0) {
            // If we've been told by the user to not render wiki syntax we simply pass the text as a Word block as is
            if (this.escapeWikiSyntax) {
                this.stack.push(new WordBlock(content));
            } else {

                XDOM dom;
                try {
                    dom = this.parser.parse(new StringReader(content));
                } catch (ParseException e) {
                    throw new SAXException("Failed to parse [" + content + "]", e);
                }

                // Remove any paragraph that might have been added since we don't want paragraphs.
                // For example we want to generate <h1>hello</h1> and not <h1><p>hello</p></h1>.
                List<Block> children = dom.getChildren();
                if (children.size() > 0) {
                    if (ParagraphBlock.class.isAssignableFrom(children.get(0).getClass())) {
                        dom = new XDOM(children.get(0).getChildren());
                    }
                }

                for (Block block: dom.getChildren()) {
                    this.stack.push(block);
                }
            }
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes)
        throws SAXException
    {
        if (this.accumulationBuffer.length() > 0) {
            processCharacters(this.accumulationBuffer.toString().toCharArray(), 0, this.accumulationBuffer.length());
            this.accumulationBuffer.delete(0, this.accumulationBuffer.length());
        }

        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < attributes.getLength(); i++) {
            // The XHTML DTD specifies some default value for some attributes. For example for a TD element
            // it defines colspan=1 and rowspan=1. Thus we'll get a colspan and rowspan attribute passed to
            // the current method even though they are not defined in the source XHTML content.
            // However with SAX2 it's possible to check if an attribute is defined in the source or not using
            // the Attributes2 class.
            // See http://www.saxproject.org/apidoc/org/xml/sax/package-summary.html#package_description
            if (attributes instanceof Attributes2) {
                Attributes2 attribute2 = (Attributes2) attributes;
                // present in XHTML source file
                if (attribute2.isSpecified(i)) {
                    map.put(attributes.getQName(i), attributes.getValue(i));
                }
            } else {
                map.put(attributes.getQName(i), attributes.getValue(i));
            }
        }
        this.stack.push(new XMLBlock(name, map));
        this.stack.push(this.marker);
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException
    {
        if (accumulationBuffer.length() > 0) {
            processCharacters(this.accumulationBuffer.toString().toCharArray(), 0, this.accumulationBuffer.length());
            this.accumulationBuffer.delete(0, this.accumulationBuffer.length());
        }

        // Pop the stack until we reach a marker block
        List<Block> nestedBlocks = generateListFromStack();
        this.stack.peek().addChildren(nestedBlocks);
    }

    private List<Block> generateListFromStack()
    {
        List<Block> blocks = new ArrayList<Block>();
        while (!this.stack.empty()) {
            if (this.stack.peek() != this.marker) {
                blocks.add(this.stack.pop());
            } else {
                this.stack.pop();
                break;
            }
        }
        Collections.reverse(blocks);
        return blocks;
    }

}
