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
package org.xwiki.store.serialization.xml.internal;

import java.io.FilterWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.Stack;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;

/**
 * Extension to <code>{@link org.dom4j.io.XMLWriter}</code> to allow filling some element content
 * with an input stream, minimizing the memory footprint of the operation.
 * <p>
 * This extension is not intended to be used to format a DOM4J tree to a stream, but to immediately
 * write out the tags produced without building the document tree in memory. It is not compatible
 * with the SAX part of the original
 * <code>{@link org.dom4j.io.XMLWriter}</code>.
 * </p>
 * <p>
 * An improvement to the writeOpen/writeClose functions ensure better handling of independent
 * opening and closing of tags by maintaining a state stack of opened tags.
 * New writeDocumentStart/End function also ensure proper starting and
 * ending of the document it self.
 * </p>
 *
 * @version $Id$
 * @since 3.0M2
 */
public class XMLWriter extends org.dom4j.io.XMLWriter
{
    /**
     * Number of characters wide base64 content will be.
     */
    private static final int BASE64_WIDTH = 80;

    /**
     * Platform dependent line seperator.
     */
    private static final byte[] NEWLINE;

    /**
     * If the last character written is this then it is safe to indent the next tag.
     */
    private static final char CLOSE_ANGLE_BRACKET = '>';

    /**
     * <code>{@link Stack}</code> of currently opened <code>{@link Element}</code>, the first
     * <code>{@link Element}</code> is the document root element,
     * and the top of the stack is the last opened
     * <code>{@link Element}</code>.
     */
    protected Stack<Element> parent = new Stack<Element>();

    /**
     * Current <code>{@link OutputStream}</code> of this writer.
     */
    private OutputStream out;

    /**
     * The underlying writer which is not cast to Writer.
     */
    private LastCharWriter lcWriter;

    /** True if the last thing written was content from an InputStream and 
     private boolean indentUnsafe;

     /** Need to catch this exception so this has to be done in an initializer block. */
    static {
        try {
            NEWLINE = System.getProperty("line.separator").getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("No UTF-8, this Java VM is not standards compliant!", e);
        }
    }

    /**
     * Default constructor used by <code>{@link DOMXMLWriter}</code>.
     *
     * @see DOMXMLWriter
     */
    protected XMLWriter()
    {
    }

    /**
     * Create a new XMLWriter writing to a provided OutputStream in a given format.
     * Note that other constructor of the original DOM4J XMLWriter are unsupported since an
     * OutputStream is the only way we can support the extensions provided here.
     * <p>
     * Note that the writer is buffered and only a call to flush() or writeDocuemntEnd()
     * will ensure the output has been fully written to the <code>{@link OutputStream}</code>.
     * </p>
     *
     * @param out an <code>{@link OutputStream}</code> where to output the XML produced.
     * @param format an <code>{@link OutputFormat}</code> defining the encoding that
     * should be used and esthetics of the produced XML.
     * @throws UnsupportedEncodingException the requested encoding is unsupported.
     */
    public XMLWriter(final OutputStream out, final OutputFormat format)
        throws UnsupportedEncodingException
    {
        super(out, format);
        this.lcWriter = new LastCharWriter(super.writer);
        super.writer = this.lcWriter;
        this.out = out;
    }

    /**
     * Write the <code>{@link Document}</code> declaration, and its <code>{@link DocumentType}</code>
     * if available to the output stream.
     *
     * @param doc <code>{@link Document}</code> to be started, may specify a
     * <code>{@link DocumentType}</code>.
     * @throws IOException a problem occurs during writing
     */
    public void writeDocumentStart(final Document doc) throws IOException
    {
        writeDeclaration();

        if (doc.getDocType() != null) {
            super.indent();
            super.writeDocType(doc.getDocType());
        }
    }

    /**
     * Write the end of the document.
     * Close all remaining opened <code>{@link Element}</code> including the root element to
     * terminate the current document.
     * Also flush the writer to ensure the whole document has been written to the
     * <code>{@link OutputStream}</code>.
     *
     * @param doc <code>{@link Document}</code> to be end, actually unused.
     * @throws IOException a problem occurs during writing.
     */
    public void writeDocumentEnd(final Document doc) throws IOException
    {
        if (!this.parent.isEmpty()) {
            this.writeClose(this.parent.firstElement());
        }
        super.writePrintln();
        super.flush();
    }

    /**
     * Writes the <code>{@link Element}</code>, including its <code>{@link
     * Attribute}</code>s, using the <code>{@link Reader}</code>
     * for its content.
     * <p>
     * Note that proper decoding/encoding will occurs during this operation,
     * converting the encoding of the Reader into the encoding of the Writer.
     * </p>
     *
     * @param element <code>{@link Element}</code> to output.
     * @param rd <code>{@link Reader}</code> that will be fully read and transfered
     * into the element content.
     * @throws IOException a problem occurs during reading or writing.
     */
    public void write(final Element element, final Reader rd) throws IOException
    {
        this.writeOpen(element);
        IOUtils.copy(rd, this.lcWriter);
        this.writeClose(element);
    }

    /**
     * Writes the <code>{@link Element}</code>, including its <code>{@link
     * Attribute}</code>s, using the
     * <code>{@link InputStream}</code> for its content.
     * <p>
     * Note that no decoding/encoding of the InputStream will be ensured during this operation.
     * The byte content is transfered untouched.
     * </p>
     *
     * @param element <code>{@link Element}</code> to output.
     * @param is <code>{@link InputStream}</code> that will be fully read and transfered into
     * the element content.
     * @throws IOException a problem occurs during reading or writing.
     */
    public void write(final Element element, final InputStream is) throws IOException
    {
        this.writeOpen(element);
        super.flush();
        IOUtils.copy(is, this.out);

        // We must prevent indentation even though the
        // last character written through the writer is a >
        super.writeClose(element);
    }

    /**
     * Writes the <code>{@link Element}</code>, including its <code>{@link
     * Attribute}</code>s, using the
     * <code>{@link InputStream}</code> encoded in Base64 for its content.
     *
     * @param element <code>{@link Element}</code> to output.
     * @param is <code>{@link InputStream}</code> that will be fully read and encoded
     * in Base64 into the element content.
     * @throws IOException a problem occurs during reading or writing.
     */
    public void writeBase64(final Element element, final InputStream is) throws IOException
    {
        this.writeOpen(element);
        super.writePrintln();

        super.flush();
        final Base64OutputStream base64 =
            new Base64OutputStream(new CloseShieldOutputStream(this.out), true, BASE64_WIDTH, NEWLINE);
        IOUtils.copy(is, base64);
        base64.close();

        // The last char written was a newline, not a > so it will not indent unless it is done manually.
        super.setIndentLevel(this.parent.size() - 1);
        super.indent();

        this.writeClose(element);
    }

    /**
     * Writes the opening tag of an {@link Element}.
     * Includes its {@link Attribute}s but without its content.
     * <p>
     * Compared to the DOM4J implementation, this function keeps track of opened elements.
     * </p>
     *
     * @param element <code>{@link Element}</code> to output.
     * @throws IOException a problem occurs during writing.
     * @see org.dom4j.io.XMLWriter#writeOpen(org.dom4j.Element)
     */
    @Override
    public void writeOpen(final Element element) throws IOException
    {
        if (this.lcWriter.getLastChar() == CLOSE_ANGLE_BRACKET) {
            super.writePrintln();
            super.indent();
        }
        super.writeOpen(element);
        this.parent.push(element);
        super.setIndentLevel(this.parent.size());
    }

    /**
     * Writes the closing tag of an {@link Element}.
     * <p>
     * Compared to the DOM4J implementation, this function ensure closing of all opened
     * element including the one that is requested to be closed. Also writes a newline and
     * indents the closing element if required and if the last thing written was not a string.
     * </p>
     *
     * @param element <code>{@link Element}</code> to output.
     * @throws IOException a problem occurs during writing.
     * @see org.dom4j.io.XMLWriter#writeClose(org.dom4j.Element)
     */
    @Override
    public void writeClose(final Element element) throws IOException
    {
        while (!this.parent.peek().getQualifiedName().equals(element.getQualifiedName())) {
            this.writeClose(this.parent.peek());
        }

        super.setIndentLevel(this.parent.size() - 1);
        if (this.lcWriter.getLastChar() == CLOSE_ANGLE_BRACKET) {
            super.writePrintln();
            super.indent();
        }

        super.writeClose(this.parent.pop());
    }

    /**
     * An OutputStream which allows you to get the last byte which was written to it.
     */
    private static class LastCharWriter extends FilterWriter
    {
        /**
         * The last byte written to the stream.
         */
        private char lastChar;

        /**
         * The Constructor.
         *
         * @param toWrap the Writer to send all calls to.
         */
        public LastCharWriter(final Writer toWrap)
        {
            super(toWrap);
        }

        /**
         * {@inheritDoc}
         *
         * @see java.io.FilterWriter#write(char[], int, int)
         */
        @Override
        public void write(final char[] buffer, final int offset, final int count)
            throws IOException
        {
            super.write(buffer, offset, count);
            this.lastChar = buffer[offset + count - 1];
        }

        /**
         * {@inheritDoc}
         *
         * @see java.io.FilterWriter#write(String, int, int)
         */
        @Override
        public void write(final String str, final int offset, final int count)
            throws IOException
        {
            super.write(str, offset, count);
            this.lastChar = str.charAt(offset + count - 1);
        }

        /**
         * {@inheritDoc}
         *
         * @see java.io.FilterWriter#write(int)
         */
        @Override
        public void write(final int oneChar) throws IOException
        {
            super.write(oneChar);
            this.lastChar = (char) oneChar;
        }

        /**
         * @return the last character written.
         */
        public char getLastChar()
        {
            return lastChar;
        }
    }
}
