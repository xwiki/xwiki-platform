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
package org.xwiki.xml;

import java.util.Stack;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <p>
 * Extracts a well-formed XML fragment by listening to SAX events. The result has the following semantic:<br/>
 * <code>xmlInput.dropAllTags().substring(start, length).unDropAssociatedTags()</code>
 * </p>
 * <p>
 * So basically we would create an instance like <code>new ExtractHandler(0, 400)</code> in order to obtain an XML
 * fragment with its inner text length of at most 400 characters, starting at position (character) 0 in the source
 * (input) XML's inner text. The ExtractHandler is used in feed plug-in to obtain a preview of an XML (HTML, to be more
 * specific). Another use case could be to paginate an XML source (keeping pages well-formed).
 * </p>
 * <p>
 * As an example, the result of applying an <code>ExtractHandler(3, 13)</code> to:<br/>
 * <code>&lt;p&gt;click &lt;a href="realyLongURL" title="Here"&gt;here&lt;/a&gt; to view the result&lt;/p&gt;</code>
 * <br/>is:<br/> <code>&lt;p&gt;ck &lt;a href="realyLongURL" title="Here"&gt;here&lt;/a&gt; to&lt;/p&gt;</code>
 * </p>
 * 
 * @version $Id$
 * @since 1.6M2
 */
public class ExtractHandler extends DefaultHandler
{
    /**
     * A simple utility bean for representing an XML tag.
     */
    private static class XMLTag
    {
        /**
         * Tag's qualified name.
         */
        private String qName;

        /**
         * Tag's attributes.
         */
        private Attributes atts;

        /**
         * Constructs a new XML tag with the given qualified name and attributes.
         * 
         * @param qName Tag's qualified name.
         * @param atts Tag's attributes.
         */
        public XMLTag(String qName, Attributes atts)
        {
            this.qName = qName;
            this.atts = atts;
        }

        /**
         * @return Tag's qualified name.
         */
        public String getQName()
        {
            return this.qName;
        }

        /**
         * @return Tag's attributes.
         */
        public Attributes getAtts()
        {
            return this.atts;
        }
    }

    /**
     * The number of characters, in text nodes, that have to be read before starting the extraction.
     */
    private int lowerBound;

    /**
     * The maximum number of characters that may be read during the parsing process.
     */
    private int upperBound;

    /**
     * The number of characters read so far.
     */
    private int counter;

    /**
     * The stack of open tags; when the lower bound is reached all the tags in the stack must be opened; when the upper
     * bound is reached all the tags in the stack must be closed.
     */
    private Stack<XMLTag> openedTags = new Stack<XMLTag>();

    /**
     * The fragment that is extracted during the parsing process.
     */
    private StringBuilder result = new StringBuilder();

    /**
     * <code>true</code> if the extraction was successful. The parsing process throws an exception when the upper bound
     * is reached; this flag is useful to distinguish between this exception and the others.
     */
    private boolean finished;

    /**
     * Creates a new instance.
     * 
     * @param start The character index from where to start the extraction.
     * @param length The number of plain text characters to extract.
     * @throws SAXException if start is less than zero or length is less than or equal to zero.
     */
    public ExtractHandler(int start, int length) throws SAXException
    {
        super();
        if (start < 0) {
            throw new SAXException("Start must be greater than or equal to 0");
        }
        if (length <= 0) {
            throw new SAXException("Length must be greater than 0");
        }
        this.lowerBound = start;
        this.upperBound = this.lowerBound + length;
    }

    /**
     * @return The extracted text.
     */
    public String getResult()
    {
        return this.result.toString();
    }

    /**
     * @return true if the extraction process has succeeded; false if an exception occurred during the process.
     */
    public boolean isFinished()
    {
        return this.finished;
    }

    /**
     * Append an open tag with the given specification to the result buffer.
     * 
     * @param qName Tag's qualified name.
     * @param atts Tag's attributes.
     */
    private void openTag(String qName, Attributes atts)
    {
        this.result.append('<').append(qName);
        for (int i = 0; i < atts.getLength(); i++) {
            this.result.append(' ').append(atts.getQName(i)).append("=\"").append(atts.getValue(i)).append('\"');
        }
        this.result.append('>');
    }

    /**
     * Open all pending tags.
     * 
     * @see #openTag(String, Attributes)
     */
    private void openTags()
    {
        for (XMLTag tag : this.openedTags) {
            openTag(tag.getQName(), tag.getAtts());
        }
    }

    /**
     * Close all pending tags.
     * 
     * @see #closeTag(String)
     */
    private void closeTags()
    {
        while (!this.openedTags.isEmpty()) {
            closeTag(this.openedTags.pop().getQName());
        }
    }

    /**
     * Append a closed tag with the given qualified name to the result buffer.
     * 
     * @param qName Tag's qualified name.
     */
    private void closeTag(String qName)
    {
        this.result.append("</").append(qName).append('>');
    }

    /**
     * @return true if the start point has been passed but the length limit hasn't been reached.
     */
    private boolean isExtracting()
    {
        return this.lowerBound <= this.counter && this.counter <= this.upperBound;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#startDocument()
     */
    @Override
    public void startDocument() throws SAXException
    {
        super.startDocument();
        this.counter = 0;
        this.openedTags.clear();
        this.result.setLength(0);
        this.finished = false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#startElement(String, String, String, Attributes)
     */
    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts) throws SAXException
    {
        this.openedTags.push(new XMLTag(qName, atts));
        if (isExtracting()) {
            openTag(qName, atts);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#characters(char[], int, int)
     */
    @Override
    public void characters(char[] ch, int start, int length) throws SAXException
    {
        int offset = this.lowerBound - this.counter;
        if (offset > 0) {
            if (offset > length) {
                this.counter += length;
                return;
            } else {
                this.counter = this.lowerBound;
                openTags();
                characters(ch, start + offset, length - offset);
                return;
            }
        }
        int remainingLength = this.upperBound - this.counter;
        if (remainingLength <= length) {
            String content = new String(ch, start, remainingLength);
            int spaceIndex = remainingLength;
            if (remainingLength == length || ch[remainingLength] != ' ') {
                spaceIndex = content.lastIndexOf(' ');
            }
            if (spaceIndex >= 0) {
                this.counter += spaceIndex;
                this.result.append(content.substring(0, spaceIndex));
            } else {
                this.counter = this.upperBound;
                this.result.append(content);
            }
            endDocument();
            throw new SAXException("Length limit reached");
        } else {
            this.counter += length;
            this.result.append(ch, start, length);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#endElement(String, String, String)
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException
    {
        // We assume the XML fragment is well defined, and thus we shouldn't have a closed tag
        // without its pair open tag. So we don't test for empty stack or tag match.
        this.openedTags.pop();
        if (isExtracting()) {
            closeTag(qName);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see DefaultHandler#endDocument()
     */
    @Override
    public void endDocument() throws SAXException
    {
        super.endDocument();
        // Close open tags
        if (isExtracting()) {
            closeTags();
        }
        // set finished flag to distinguish between "length limit reached" and other exceptions
        this.finished = true;
    }
}
