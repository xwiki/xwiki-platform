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
 *
 */
package com.xpn.xwiki.internal.xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Stack;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;

/**
 * Extension to <code>{@link org.dom4j.io.XMLWriter}</code> to allow filling some element content with an input stream,
 * minimizing the memory footprint of the operation.
 * <p>
 * This extension is not intended to be used to format a DOM4J tree to a stream, but to immediately write out the tags
 * produced without building the document tree in memory. It is not compatible with the SAX part of the original
 * <code>{@link org.dom4j.io.XMLWriter}</code>.
 * </p>
 * <p>
 * An improvement to the writeOpen/writeClose functions ensure better handling of independent opening and closing of
 * tags by maintaining a state stack of opened tags. New writeDocumentStart/End function also ensure proper starting and
 * ending of the document it self.
 * </p>
 * 
 * @version $Id$
 */
public class XMLWriter extends org.dom4j.io.XMLWriter
{
    /**
     * <code>{@link Stack}</code> of currently opened <code>{@link Element}</code>, the first
     * <code>{@link Element}</code> is the document root element, and the top of the stack is the last opened
     * <code>{@link Element}</code>.
     */
    protected Stack<Element> parent = new Stack<Element>();

    /**
     * Current <code>{@link OutputStream}</code> of this writer.
     */
    private OutputStream out;

    /**
     * Default constructor used by <code>{@link DOMXMLWriter}</code>.
     * 
     * @see DOMXMLWriter
     */
    protected XMLWriter()
    {
    }

    /**
     * Create a new XMLWriter writing to a provided OutputStream in a given format. Note that other constructor of the
     * original DOM4J XMLWriter are unsupported since a OutputStream is the only way we can support the extensions
     * provided here.
     * <p>
     * Note that the writer is buffered and only a call to flush() or writeDocuemntEnd() will ensure the output has been
     * fully written to the <code>{@link OutputStream}</code>.
     * </p>
     * 
     * @param out an <code>{@link OutputStream}</code> where to output the XML produced.
     * @param format an <code>{@link OutputFormat}</code> defining the encoding that should be used and esthetics of the
     *            produced XML.
     * @throws UnsupportedEncodingException the requested encoding is unsupported.
     */
    public XMLWriter(OutputStream out, OutputFormat format) throws UnsupportedEncodingException
    {
        super(out, format);
        this.out = out;
    }

    /**
     * Write the <code>{@link Document}</code> declaration, and its <code>{@link DocumentType}</code> if available to
     * the output stream.
     * 
     * @param doc <code>{@link Document}</code> to be started, may specify a <code>{@link DocumentType}</code>.
     * @throws IOException a problem occurs during writing
     */
    public void writeDocumentStart(Document doc) throws IOException
    {
        writeDeclaration();

        if (doc.getDocType() != null) {
            indent();
            writeDocType(doc.getDocType());
        }
    }

    /**
     * Close all remaining opened <code>{@link Element}</code> including the root element to terminate the current
     * document. Also flush the writer to ensure the whole document has been written to the
     * <code>{@link OutputStream}</code>.
     * 
     * @param doc <code>{@link Document}</code> to be end, actually unused.
     * @throws IOException a problem occurs during writing.
     */
    public void writeDocumentEnd(Document doc) throws IOException
    {
        if (!this.parent.isEmpty()) {
            writeClose(this.parent.firstElement());
        }
        writePrintln();
        flush();
    }

    /**
     * Writes the <code>{@link Element}</code>, including its <code>{@link
     * Attribute}</code>s, using the <code>{@link Reader}</code>
     * for its content.
     * <p>
     * Note that proper decoding/encoding will occurs during this operation, converting the encoding of the Reader into
     * the encoding of the Writer.
     * </p>
     * 
     * @param element <code>{@link Element}</code> to output.
     * @param rd <code>{@link Reader}</code> that will be fully read and transfered into the element content.
     * @throws IOException a problem occurs during reading or writing.
     */
    public void write(Element element, Reader rd) throws IOException
    {
        writeOpen(element);
        IOUtils.copy(rd, this.writer);
        writeClose(element);
    }

    /**
     * Writes the <code>{@link Element}</code>, including its <code>{@link
     * Attribute}</code>s, using the
     * <code>{@link InputStream}</code> for its content.
     * <p>
     * Note that no decoding/encoding of the InputStream will be ensured during this operation. The byte content is
     * transfered untouched.
     * </p>
     * 
     * @param element <code>{@link Element}</code> to output.
     * @param is <code>{@link InputStream}</code> that will be fully read and transfered into the element content.
     * @throws IOException a problem occurs during reading or writing.
     */
    public void write(Element element, InputStream is) throws IOException
    {
        writeOpen(element);
        flush();
        IOUtils.copy(is, this.out);
        writeClose(element);
    }

    /**
     * Writes the <code>{@link Element}</code>, including its <code>{@link
     * Attribute}</code>s, using the
     * <code>{@link InputStream}</code> encoded in Base64 for its content.
     * 
     * @param element <code>{@link Element}</code> to output.
     * @param is <code>{@link InputStream}</code> that will be fully read and encoded in Base64 into the element
     *            content.
     * @throws IOException a problem occurs during reading or writing.
     */
    public void writeBase64(Element element, InputStream is) throws IOException
    {
        writeOpen(element);
        flush();
        Base64OutputStream base64 = new Base64OutputStream(new CloseShieldOutputStream(this.out));
        IOUtils.copy(is, base64);
        base64.close();
        writeClose(element);
    }

    /**
     * Writes the opening tag of an {@link Element}, including its {@link Attribute}s but without its content.
     * <p>
     * Compared to the DOM4J implementation, this function keeps track of opened elements.
     * </p>
     * 
     * @param element <code>{@link Element}</code> to output.
     * @throws IOException a problem occurs during writing.
     * @see org.dom4j.io.XMLWriter#writeOpen(org.dom4j.Element)
     */
    @Override
    public void writeOpen(Element element) throws IOException
    {
        super.writeOpen(element);
        this.parent.push(element);
    }

    /**
     * Writes the closing tag of an {@link Element}.
     * <p>
     * Compared to the DOM4J implementation, this function ensure closing of all opened element including the one that
     * is requested to be closed.
     * </p>
     * 
     * @param element <code>{@link Element}</code> to output.
     * @throws IOException a problem occurs during writing.
     * @see org.dom4j.io.XMLWriter#writeClose(org.dom4j.Element)
     */
    @Override
    public void writeClose(Element element) throws IOException
    {
        while (this.parent.peek() != element) {
            super.writeClose(this.parent.pop());
        }
        super.writeClose(this.parent.pop());
    }
}
