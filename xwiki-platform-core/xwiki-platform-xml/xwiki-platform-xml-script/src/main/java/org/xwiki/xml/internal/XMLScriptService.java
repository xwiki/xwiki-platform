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
package org.xwiki.xml.internal;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import org.xwiki.component.annotation.Component;
import org.xwiki.script.service.ScriptService;
import org.xwiki.xml.XMLUtils;

/**
 * Provides Scripting APIs for handling XML.
 * 
 * @version $Id$
 * @since 2.7M1
 */
@Component("xml")
public class XMLScriptService implements ScriptService
{
    /** Xerces configuration parameter for disabling fetching and checking XMLs against their DTD. */
    private static final String DISABLE_DTD_PARAM = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /** Helper object for manipulating DOM Level 3 Load and Save APIs. */
    private DOMImplementationLS lsImpl;

    /**
     * Default component constructor.
     */
    public XMLScriptService()
    {
        try {
            this.lsImpl = (DOMImplementationLS) DOMImplementationRegistry.newInstance().getDOMImplementation("LS 3.0");
        } catch (Exception ex) {
            this.logger.warn("Cannot initialize the XML Script Service", ex);
        }
    }

    /**
     * Escapes all the XML special characters in a <code>String</code> using numerical XML entities. Specifically,
     * escapes &lt;, &gt;, ", ' and &amp;.
     *
     * @param content the text to escape, may be {@code null}
     * @return a new escaped {@code String}, {@code null} if {@code null} input
     */
    public static String escape(Object content)
    {
        return XMLUtils.escape(content);
    }

    /**
     * Escapes all the XML special characters in a <code>String</code> using numerical XML entities, so that the
     * resulting string can safely be used as an XML attribute value. Specifically, escapes &lt;, &gt;, ", ' and &amp;.
     *
     * @param content the text to escape, may be {@code null}
     * @return a new escaped {@code String}, {@code null} if {@code null} input
     */
    public static String escapeForAttributeValue(Object content)
    {
        return XMLUtils.escapeAttributeValue(content);
    }

    /**
     * Escapes the XML special characters in a <code>String</code> using numerical XML entities, so that the resulting
     * string can safely be used as an XML text node. Specifically, escapes &lt;, &gt;, and &amp;.
     *
     * @param content the text to escape, may be {@code null}
     * @return a new escaped {@code String}, {@code null} if {@code null} input
     */
    public static String escapeForElementContent(Object content)
    {
        return XMLUtils.escapeElementContent(content);
    }

    /**
     * Unescape encoded special XML characters. Only &gt;, &lt; &amp;, " and ' are unescaped, since they are the only
     * ones that affect the resulting markup.
     *
     * @param content the text to decode, may be {@code null}
     * @return unescaped content, {@code null} if {@code null} input
     */
    public static String unescape(Object content)
    {
        return XMLUtils.unescape(content);
    }

    /**
     * Construct a new (empty) DOM Document and return it.
     * 
     * @return an empty DOM Document
     */
    public Document createDOMDocument()
    {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            this.logger.error("Cannot create DOM Documents", ex);
            return null;
        }
    }

    /**
     * Parse a DOM Document from a source.
     * 
     * @param source the source input to parse
     * @return the equivalent DOM Document, or {@code null} if the parsing failed.
     */
    public Document parse(LSInput source)
    {
        try {
            LSParser p = this.lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
            // Disable validation, since this takes a lot of time and causes unneeded network traffic
            p.getDomConfig().setParameter("validate", false);
            if (p.getDomConfig().canSetParameter(DISABLE_DTD_PARAM, false)) {
                p.getDomConfig().setParameter(DISABLE_DTD_PARAM, false);
            }
            return p.parse(source);
        } catch (Exception ex) {
            this.logger.warn("Cannot parse XML document: " + ex.getMessage());
            return null;
        }
    }

    /**
     * Parse a {@code byte[]} into a DOM Document.
     * 
     * @param content the content to parse
     * @return a DOM Document corresponding to the input, {@code null} if the content can't be parsed successfully
     */
    public Document parse(byte[] content)
    {
        if (content == null) {
            return null;
        }
        LSInput input = this.lsImpl.createLSInput();
        input.setByteStream(new ByteArrayInputStream(content));
        return parse(input);
    }

    /**
     * Parse a {@code String} into a DOM Document.
     * 
     * @param content the content to parse
     * @return a DOM Document corresponding to the input, {@code null} if the content can't be parsed successfully
     */
    public Document parse(String content)
    {
        if (content == null) {
            return null;
        }
        LSInput input = this.lsImpl.createLSInput();
        input.setCharacterStream(new StringReader(content));
        return parse(input);
    }

    /**
     * Parse an {@code InputStream} into a DOM Document.
     * 
     * @param stream the content input to parse
     * @return a DOM Document corresponding to the input, {@code null} if the content can't be parsed successfully
     */
    public Document parse(InputStream stream)
    {
        if (stream == null) {
            return null;
        }
        LSInput input = this.lsImpl.createLSInput();
        input.setByteStream(stream);
        return parse(input);
    }

    /**
     * Serialize a DOM Node into a string, including the XML declaration at the start.
     * 
     * @param node the node to export
     * @return the serialized node, or an empty string if the serialization fails
     */
    public String serialize(Node node)
    {
        return serialize(node, true);
    }

    /**
     * Serialize a DOM Node into a string, with an optional XML declaration at the start.
     * 
     * @param node the node to export
     * @param withXmlDeclaration whether to output the XML declaration or not
     * @return the serialized node, or an empty string if the serialization fails or the node is {@code null}
     */
    public String serialize(Node node, boolean withXmlDeclaration)
    {
        if (node == null) {
            return "";
        }
        try {
            LSOutput output = this.lsImpl.createLSOutput();
            StringWriter result = new StringWriter();
            output.setCharacterStream(result);
            LSSerializer serializer = this.lsImpl.createLSSerializer();
            serializer.getDomConfig().setParameter("xml-declaration", withXmlDeclaration);
            serializer.setNewLine("\n");
            String encoding = "UTF-8";
            if (node instanceof Document) {
                encoding = ((Document) node).getXmlEncoding();
            } else if (node.getOwnerDocument() != null) {
                encoding = node.getOwnerDocument().getXmlEncoding();
            }
            output.setEncoding(encoding);
            serializer.write(node, output);
            return result.toString();
        } catch (Exception ex) {
            this.logger.warn("Failed to serialize node to XML String", ex);
            return "";
        }
    }

    /**
     * Apply an XSLT transformation to a Document.
     * 
     * @param xml the document to transform
     * @param xslt the stylesheet to apply
     * @return the transformation result, or {@code null} if an error occurs or {@code null} xml or xslt input
     */
    public String transform(Source xml, Source xslt)
    {
        if (xml != null && xslt != null) {
            try {
                StringWriter output = new StringWriter();
                Result result = new StreamResult(output);
                javax.xml.transform.TransformerFactory.newInstance().newTransformer(xslt).transform(xml, result);
                return output.toString();
            } catch (Exception ex) {
                this.logger.warn("Failed to apply XSLT transformation: " + ex.getMessage());
            }
        }
        return null;
    }

    /**
     * Apply an XSLT transformation to a Document, both given as DOM Documents.
     * 
     * @param xml the document to transform
     * @param xslt the stylesheet to apply
     * @return the transformation result, or {@code null} if an error occurs or {@code null} xml or xslt input
     */
    public String transform(Document xml, Document xslt)
    {
        if (xml == null || xslt == null) {
            return null;
        }
        return transform(new DOMSource(xml), new DOMSource(xslt));
    }

    /**
     * Apply an XSLT transformation to a Document, both given as byte arrays.
     * 
     * @param xml the document to transform
     * @param xslt the stylesheet to apply
     * @return the transformation result, or {@code null} if an error occurs or {@code null} xml or xslt input
     */
    public String transform(byte[] xml, byte[] xslt)
    {
        if (xml == null || xslt == null) {
            return null;
        }
        return transform(new StreamSource(new ByteArrayInputStream(xml)),
            new StreamSource(new ByteArrayInputStream(xslt)));
    }

    /**
     * Apply an XSLT transformation to a Document, both given as strings.
     * 
     * @param xml the document to transform
     * @param xslt the stylesheet to apply
     * @return the transformation result, or {@code null} if an error occurs or {@code null} xml or xslt input
     */
    public String transform(String xml, String xslt)
    {
        if (xml == null || xslt == null) {
            return null;
        }
        return transform(new StreamSource(new StringReader(xml)), new StreamSource(new StringReader(xslt)));
    }
}
