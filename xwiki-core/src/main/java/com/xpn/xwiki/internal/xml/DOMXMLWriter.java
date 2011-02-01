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
package com.xpn.xwiki.internal.xml;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.EmptyStackException;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.apache.commons.io.IOUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * This a minimal implementation to transform an XMLWriter into a <code>{@link org.dom4j.dom.DOMDocument}</code>
 * builder.
 * <p>
 * This implementation allow the use of a same function accepting an XMLWriter, to either produce output into an
 * <code>{@link java.io.OutputStream}</code> or to create a <code>{@link org.dom4j.dom.DOMDocument}</code>. Here a
 * sample of the way to do so:
 * </p>
 * <code><pre>
 *     public void toXML(XMLWriter wr) throws IOException
 *     {
 *          Element docel = new DOMElement("html");
 *          wr.writeOpen(docel);
 *          Element hbel = new DOMElement("head");
 *          wr.writeOpen(hbel);
 *          Element el = new DOMElement("title");
 *          el.addText("My Title");
 *          wr.write(el);
 *          wr.writeClose(hbel);
 *          hbel = new DOMElement("body");
 *          wr.writeOpen(hbel);
 *          el = new DOMElement("p");
 *          el.addText("My Body");
 *          wr.write(el);
 *     }
 * 
 *     public void toXML(OutputStream out) throws IOException
 *     {
 *          XMLWriter wr = new XMLWriter(out, new OutputFormat("  ", true, "UTF-8"));
 * 
 *          Document doc = new DOMDocument();
 *          wr.writeDocumentStart(doc);
 *          toXML(wr);
 *          wr.writeDocumentEnd(doc);
 *     }
 * 
 *     public Document toXMLDocument()
 *     {
 *          Document doc = new DOMDocument();
 *          DOMXMLWriter wr = new DOMXMLWriter(doc, new OutputFormat("  ", true, "UTF-8"));
 * 
 *          try {
 *              toXML(wr);
 *              return doc;
 *          } catch (IOException e) {
 *              throw new RuntimeException(e);
 *          }
 *     }
 * </pre></code>
 * <p>
 * <b>WARNING</b> - This implementation in INCOMPLETE and a minimal support. It should be improve as needed over time
 * </p>
 * 
 * @version $Id$
 */
public class DOMXMLWriter extends XMLWriter
{
    /**
     * The <code>{@link Document}</code> currently built by this writer.
     */
    private Document doc;

    /**
     * The <code>{@link OutputFormat}</code> providing the encoding requested.
     */
    private OutputFormat format;

    /**
     * Create a new <code>{@link DOMXMLWriter}</code> that will build into the provided document using the default
     * encoding.
     * 
     * @param doc <code>{@link Document}</code> that will be build by this writer
     */
    public DOMXMLWriter(Document doc)
    {
        this.format = DEFAULT_FORMAT;
        this.doc = doc;
    }

    /**
     * Create a new <code>{@link DOMXMLWriter}</code> that will build into the provided document using the encoding
     * provided in the <code>{@link OutputFormat}</code>.
     * 
     * @param doc <code>{@link Document}</code> that will be build by this writer
     * @param format <code>{@link OutputFormat}</code> used to retrieve the proper encoding
     */
    public DOMXMLWriter(Document doc, OutputFormat format)
    {
        this.format = format;
        this.doc = doc;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Add the element into the <code>{@link Document}</code> as a children of the element at the top of the stack of
     * opened elements, putting the whole stream content as text in the content of the <code>{@link Element}</code>. The
     * stream is converted to a String encoded in the current encoding.
     * 
     * @see com.xpn.xwiki.internal.xml.XMLWriter#write(org.dom4j.Element, java.io.InputStream)
     */
    @Override
    public void write(Element element, InputStream is) throws IOException
    {
        element.addText(IOUtils.toString(is, format.getEncoding()));
        write(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Add the element into the <code>{@link Document}</code> as a children of the element at the top of the stack of of
     * the <code>{@link Element}</code>.
     * 
     * @see com.xpn.xwiki.internal.xml.XMLWriter#write(org.dom4j.Element, java.io.Reader)
     */
    @Override
    public void write(Element element, Reader rd) throws IOException
    {
        element.addText(IOUtils.toString(rd));
        write(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Add the element into the <code>{@link Document}</code> as a children of the element at the top of the stack of
     * opened elements, putting the whole stream content as Base64 text in the content of the
     * <code>{@link Element}</code>.
     * 
     * @see com.xpn.xwiki.internal.xml.XMLWriter#writeBase64(org.dom4j.Element, java.io.InputStream)
     */
    @Override
    public void writeBase64(Element element, InputStream is) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Base64OutputStream out = new Base64OutputStream(baos, true, 0, null);
        IOUtils.copy(is, out);
        out.close();
        element.addText(baos.toString(format.getEncoding()));
        write(element);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cleanup the stack of opened elements.
     * 
     * @see com.xpn.xwiki.internal.xml.XMLWriter#writeDocumentEnd(org.dom4j.Document)
     */
    @Override
    public void writeDocumentEnd(Document doc) throws IOException
    {
        if (!parent.isEmpty()) {
            writeClose(parent.firstElement());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Does nothing, avoid default action.
     * 
     * @see com.xpn.xwiki.internal.xml.XMLWriter#writeDocumentStart(org.dom4j.Document)
     */
    @Override
    public void writeDocumentStart(Document doc) throws IOException
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * Add the element into the <code>{@link Document}</code> as a children of the element at the top of the stack of
     * opened elements.
     * 
     * @see org.dom4j.io.XMLWriter#write(org.dom4j.Element)
     */
    @Override
    public void write(Element element) throws IOException
    {
        if (parent.isEmpty()) {
            doc.setRootElement(element);
        } else {
            this.parent.peek().add(element);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cleanup the stack of opened elements up to the given element.
     * 
     * @see com.xpn.xwiki.internal.xml.XMLWriter#writeClose(org.dom4j.Element)
     */
    @Override
    public void writeClose(Element element) throws IOException
    {
        try {
            while (parent.peek() != element) {
                parent.pop();
            }
            parent.pop();
        } catch (EmptyStackException e) {
            throw new IOException("FATAL: Closing a element that have never been opened");
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Add the element into the <code>{@link Document}</code> as a children of the element at the top of the stack of
     * opened elements. Add this element at the top of the stack of opened elements.
     * 
     * @see com.xpn.xwiki.internal.xml.XMLWriter#writeOpen(org.dom4j.Element)
     */
    @Override
    public void writeOpen(Element element) throws IOException
    {
        if (parent.isEmpty()) {
            doc.setRootElement(element);
        } else {
            this.parent.add(element);
        }

        parent.push(element);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.dom4j.io.XMLWriter#startCDATA()
     */
    @Override
    public void startCDATA() throws SAXException
    {
        throw new SAXUnsupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.dom4j.io.XMLWriter#startElement(java.lang.String, java.lang.String, java.lang.String,
     *      org.xml.sax.Attributes)
     */
    @Override
    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes)
        throws SAXException
    {
        throw new SAXUnsupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.dom4j.io.XMLWriter#startEntity(java.lang.String)
     */
    @Override
    public void startEntity(String name) throws SAXException
    {
        throw new SAXUnsupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.dom4j.io.XMLWriter#endCDATA()
     */
    @Override
    public void endCDATA() throws SAXException
    {
        throw new SAXUnsupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.dom4j.io.XMLWriter#endElement(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException
    {
        throw new SAXUnsupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.dom4j.io.XMLWriter#endEntity(java.lang.String)
     */
    @Override
    public void endEntity(String name) throws SAXException
    {
        throw new SAXUnsupportedException();
    }

    /**
     * Thrown for SAX api methods since we don't support them.
     */
    public class SAXUnsupportedException extends RuntimeException
    {
        /**
         * Constructs a <code>SAXUnsupportedException</code>.
         */
        public SAXUnsupportedException()
        {
            super("SAX api is not supported");
        }
    }
}
