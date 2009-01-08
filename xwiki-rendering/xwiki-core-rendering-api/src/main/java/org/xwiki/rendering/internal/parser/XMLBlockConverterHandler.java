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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2;
import org.xml.sax.ext.LexicalHandler;
import org.xml.sax.helpers.DefaultHandler;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.block.XMLBlock;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.WordBlock;
import org.xwiki.rendering.block.AbstractBlock;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.util.ParserUtils;
import org.xwiki.rendering.listener.Listener;
import org.xwiki.rendering.listener.xml.XMLCData;
import org.xwiki.rendering.listener.xml.XMLComment;
import org.xwiki.rendering.listener.xml.XMLElement;

/**
 * XML SAX handler that converts XML events into Blocks.
 * 
 * @version $Id$
 * @since 1.5M2
 */
public class XMLBlockConverterHandler extends DefaultHandler implements LexicalHandler
{
    private Parser parser;

    private boolean interpretWikiSyntax;

    private Stack<Block> stack = new Stack<Block>();

    private ParserUtils inlineConverter = new ParserUtils();

    private final MarkerBlock marker = new MarkerBlock();

    private class MarkerBlock extends AbstractBlock
    {
        public void traverse(Listener listener)
        {
        }
    }

    public XMLBlockConverterHandler(Parser parser, boolean interpretWikiSyntax)
    {
        this.parser = parser;
        this.interpretWikiSyntax = interpretWikiSyntax;
    }

    public Block getRootBlock()
    {
        return this.stack.peek();
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
        
        // If we've been told by the user to not render wiki syntax we simply pass the text as a Word block as is
        if (!this.interpretWikiSyntax) {
            this.stack.push(new WordBlock(content));
        } else {

            // Parse the content containing wiki syntax.
            XDOM dom;
            try {
                dom = this.parser.parse(new StringReader(content));
            } catch (ParseException e) {
                throw new SAXException("Failed to parse [" + content + "]", e);
            }

            // Remove any paragraph that might have been added since we don't want paragraphs.
            // For example we want to generate <h1>hello</h1> and not <h1><p>hello</p></h1>.
            List<Block> children = dom.getChildren();
            this.inlineConverter.removeTopLevelParagraph(children);

            for (Block block : children) {
                this.stack.push(block);
            }
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
        this.stack.push(new XMLBlock(new XMLElement(name, map)));
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String uri, String localName, String name) throws SAXException
    {
        // Pop the stack until we reach a marker block
        List<Block> nestedBlocks = generateListFromStack();
        this.stack.peek().addChildren(nestedBlocks);
    }

    public void comment(char[] value, int offset, int count) throws SAXException
    {
        this.stack.push(new XMLBlock(new XMLComment(new String(value, offset, count))));
    }

    /**
     * {@inheritDoc}
     * @see LexicalHandler#startCDATA()
     */
    public void startCDATA() throws SAXException
    {
        this.stack.push(new XMLBlock(new XMLCData()));
        this.stack.push(this.marker);
    }

    /**
     * {@inheritDoc}
     * @see LexicalHandler#endCDATA()
     */
    public void endCDATA() throws SAXException
    {
        // Pop the stack until we reach a marker block
        List<Block> nestedBlocks = generateListFromStack();
        this.stack.peek().addChildren(nestedBlocks);
    }

    public void startDTD(String arg0, String arg1, String arg2) throws SAXException
    {
        // Nothing to do
    }

    public void endDTD() throws SAXException
    {
        // Nothing to do
    }

    public void startEntity(String arg0) throws SAXException
    {
        // Nothing to do since an entity definition shouldn't be present in a XHTML macro content
    }

    public void endEntity(String arg0) throws SAXException
    {
        // Nothing to do since an entity definition shouldn't be present in a XHTML macro content
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
