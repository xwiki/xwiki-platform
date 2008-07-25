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
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.XMLBlock;
import org.xwiki.rendering.block.ParagraphBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.EscapeBlock;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;

/**
 * @version $Id$
 * @since 1.5M2
 */
public class XMLBlockConverterHandler extends DefaultHandler
{
    private Parser parser;

    private boolean escapeWikiSyntax;
    
    private Stack<XMLBlock> stack = new Stack<XMLBlock>();
    
    /**
     * SAX parsers are allowed to call the characters() method several times in a row.
     * Some parsers have a buffer of 8K (Crimson), others of 16K (Xerces) and others can
     * even call characters() for every single character! Thus we need to accumulate
     * the characters in a buffer before we process them.
     */
    private StringBuffer accumulationBuffer;

    public XMLBlockConverterHandler(Parser parser, boolean escapeWikiSyntax)
    {
        this.parser = parser;
        this.escapeWikiSyntax = escapeWikiSyntax;
    }
    
    public XMLBlock getRootBlock()
    {
        return this.stack.peek();
    }
    
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        if (accumulationBuffer != null) {
            accumulationBuffer.append(ch, start, length);
        }
    }

    private void processCharacters(char[] ch, int start, int length) throws SAXException
    {
        String content = new String(ch, start, length);

        // If we've been told by the user to not render wiki syntax we escape it.
        if (this.escapeWikiSyntax) {
            this.stack.peek().addChild(new EscapeBlock(content));
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

            this.stack.peek().addChildren(dom.getChildren());
        }
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes)
        throws SAXException
    {
        if (accumulationBuffer != null && accumulationBuffer.length() > 0) {
            processCharacters(accumulationBuffer.toString().toCharArray(), 0, accumulationBuffer.length());
        }
        accumulationBuffer = new StringBuffer();

        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < attributes.getLength(); i++) {
            map.put(attributes.getQName(i), attributes.getValue(i));
        }
        this.stack.push(new XMLBlock(name, map));
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException
    {
        if (accumulationBuffer != null && accumulationBuffer.length() > 0) {
            processCharacters(accumulationBuffer.toString().toCharArray(), 0, accumulationBuffer.length());
            accumulationBuffer.setLength(0);
        }

        // Pop the stack until we reach a matching Block element
        List<XMLBlock> blocks = new ArrayList<XMLBlock>();
        while (!this.stack.peek().getName().equals(name)) {
            blocks.add(this.stack.pop());
        }
        Collections.reverse(blocks);
        this.stack.peek().addChildren(blocks);
    }
}
