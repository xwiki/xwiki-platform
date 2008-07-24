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
package com.xpn.xwiki.util;

import java.util.Iterator;
import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extracts a well-formed XML fragment by listening to SAX events.
 */
public class ExtractHandler extends DefaultHandler
{
    private static class XMLTag
    {
        private String qName;

        private Attributes atts;

        public XMLTag(String qName, Attributes atts)
        {
            this.qName = qName;
            this.atts = atts;
        }

        public String getQName()
        {
            return qName;
        }

        public Attributes getAtts()
        {
            return atts;
        }
    }

    /**
     * the number of characters, in text nodes, that have to be read before starting the extraction
     */
    private int lowerBound;

    /**
     * the maximum number of characters that may be read during the parsing process
     */
    private int upperBound;

    /**
     * the number of characters read so far
     */
    private int counter;

    /**
     * the stack of open tags; when the lower bound is reached all the tags in the stack must be opened; when the upper
     * bound is reached all the tags in the stack must be closed.
     */
    private Stack openTags = new Stack();

    /**
     * the fragment that is extracted during the parsing process
     */
    private StringBuffer result;

    /**
     * <code>true</code> if the extraction was successful. The parsing process throws an exception when the upper
     * bound is reached; this flag is useful to distinguish between this exception and the others.
     */
    private boolean finished = false;

    public ExtractHandler(int start, int length) throws SAXException
    {
        super();
        if (start < 0) {
            throw new SAXException("start must be greater than or equal to 0");
        }
        if (length <= 0) {
            throw new SAXException("length must be greater than 0");
        }
        lowerBound = start;
        upperBound = lowerBound + length;
    }

    public String getResult()
    {
        return result.toString();
    }

    public boolean isFinished()
    {
        return finished;
    }

    private void openTag(String qName, Attributes atts)
    {
        result.append("<" + qName);
        for (int i = 0; i < atts.getLength(); i++) {
            result.append(" " + atts.getQName(i) + "=\"" + atts.getValue(i) + "\"");
        }
        result.append(">");
    }

    private void openTags()
    {
        Iterator it = openTags.iterator();
        while (it.hasNext()) {
            XMLTag tag = (XMLTag) it.next();
            openTag(tag.getQName(), tag.getAtts());
        }
    }

    private void closeTags()
    {
        while (!openTags.isEmpty()) {
            XMLTag tag = (XMLTag) openTags.pop();
            closeTag(tag.getQName());
        }
    }

    private void closeTag(String qName)
    {
        result.append("</" + qName + ">");
    }

    private boolean isExtracting()
    {
        return lowerBound <= counter && counter <= upperBound;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#startDocument()
     */
    public void startDocument() throws SAXException
    {
        super.startDocument();
        counter = 0;
        openTags.clear();
        result = new StringBuffer();
        finished = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#startElement(String, String, String, Attributes)
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
    {
        openTags.push(new XMLTag(qName, atts));
        if (isExtracting()) {
            openTag(qName, atts);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#characters(char[], int, int)
     */
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        if (counter < lowerBound) {
            if (counter + length < lowerBound) {
                counter += length;
                return;
            } else {
                start += lowerBound - counter;
                length -= lowerBound - counter;
                counter = lowerBound;
                openTags();
            }
        }
        int remainingLength = upperBound - counter;
        if (remainingLength <= length) {
            String content = new String(ch, start, remainingLength);
            int spaceIndex = remainingLength;
            if (remainingLength == length || ch[remainingLength] != ' ') {
                spaceIndex = content.lastIndexOf(" ");
            }
            if (spaceIndex >= 0) {
                counter += spaceIndex;
                result.append(content.substring(0, spaceIndex));
            } else {
                counter = upperBound;
                result.append(content);
            }
            endDocument();
            throw new SAXException("length limit reached");
        } else {
            counter += length;
            result.append(ch, start, length);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#endElement(String, String, String)
     */
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException
    {
        // We assume the XML fragment is well defined, and thus we shouldn't have a closed tag
        // without its pair open tag. So we don't test for empty stack or tag match.
        openTags.pop();
        if (isExtracting()) {
            closeTag(qName);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#endDocument()
     */
    public void endDocument() throws SAXException
    {
        super.endDocument();
        // Close open tags
        if (isExtracting()) {
            closeTags();
        }
        // set finished flag to distinguish between "length limit reached" and other exceptions
        finished = true;
    }
}
