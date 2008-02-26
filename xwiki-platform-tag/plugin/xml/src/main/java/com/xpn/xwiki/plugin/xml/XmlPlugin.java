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

package com.xpn.xwiki.plugin.xml;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.domapi.XPathEvaluatorImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSParser;
import org.w3c.dom.ls.LSSerializer;
import org.w3c.dom.xpath.XPathEvaluator;
import org.w3c.dom.xpath.XPathNSResolver;
import org.w3c.dom.xpath.XPathResult;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.plugin.XWikiDefaultPlugin;
import com.xpn.xwiki.plugin.XWikiPluginInterface;

/**
 * Plugin for XML processing. Uses Xerces as the XML processor.
 * 
 * @see XmlPluginApi
 */
public class XmlPlugin extends XWikiDefaultPlugin
{
    /** Log object to log messages in this class. */
    private static final Log LOG = LogFactory.getLog(XmlPlugin.class);

    private DOMImplementationLS lsImpl;

    /**
     * {@inheritDoc}
     * 
     * @see XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public XmlPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);
        try {
            lsImpl =
                (DOMImplementationLS) DOMImplementationRegistry.newInstance()
                    .getDOMImplementation("LS 3.0");
        } catch (Exception ex) {
            LOG.warn("Cannot initialize the Xml Plugin", ex);
        }
        init(context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getName()
     */
    public String getName()
    {
        return "xml";
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#getPluginApi
     */
    public Api getPluginApi(XWikiPluginInterface plugin, XWikiContext context)
    {
        return new XmlPluginApi((XmlPlugin) plugin, context);
    }

    protected Document parse(LSInput source)
    {
        try {
            LSParser p = lsImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null);
            return p.parse(source);
        } catch (Exception ex) {
            LOG.warn("Cannot parse: invalid XML document", ex);
            return null;
        }
    }

    public Document getDomDocument()
    {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            return null;
        }
    }

    public Document parse(byte[] content)
    {
        LSInput input = lsImpl.createLSInput();
        input.setByteStream(new ByteArrayInputStream(content));
        return parse(input);
    }

    public Document parse(String content)
    {
        LSInput input = lsImpl.createLSInput();
        input.setCharacterStream(new StringReader(content));
        return parse(input);
    }

    public Document parse(XWikiAttachment attachment, XWikiContext context)
    {
        try {
            return parse(attachment.getContent(context));
        } catch (XWikiException e) {
            return null;
        }
    }

    public Document parse(InputStream stream)
    {
        LSInput input = lsImpl.createLSInput();
        input.setByteStream(stream);
        return parse(input);
    }

    public String serializeToString(Node node)
    {
        return serializeToString(node, false);
    }

    public String serializeToString(Node node, boolean omitXmlDeclaration)
    {
        try {
            LSOutput output = lsImpl.createLSOutput();
            StringWriter result = new StringWriter();
            output.setCharacterStream(result);
            LSSerializer serializer = lsImpl.createLSSerializer();
            serializer.getDomConfig().setParameter("xml-declaration", !omitXmlDeclaration);
            if (!serializer.write(node, output)) {
                switch (node.getNodeType()) {
                    case Node.ATTRIBUTE_NODE:
                    case Node.CDATA_SECTION_NODE:
                    case Node.COMMENT_NODE:
                    case Node.PROCESSING_INSTRUCTION_NODE:
                    case Node.TEXT_NODE:
                        result.append(node.getNodeValue());
                        break;
                    default:
                        result.append(node.getNodeName());
                }
            }
            return result.toString();
        } catch (Exception ex) {
            LOG.warn("Failed to serialize node to XML String", ex);
            return "";
        }
    }

    public byte[] serializeToByteArray(Node node)
    {
        return serializeToByteArray(node, false);
    }

    public byte[] serializeToByteArray(Node node, boolean omitXmlDeclaration)
    {
        try {
            LSOutput output = lsImpl.createLSOutput();
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            output.setByteStream(result);
            LSSerializer serializer = lsImpl.createLSSerializer();
            serializer.getDomConfig().setParameter("xml-declaration", !omitXmlDeclaration);
            serializer.write(node, output);
            return result.toByteArray();
        } catch (Exception ex) {
            LOG.warn("Failed to serialize node to XML", ex);
            return new byte[] {};
        }
    }

    public byte[] transform(Source xml, Source xsl)
    {
        try {
            Transformer t =
                javax.xml.transform.TransformerFactory.newInstance().newTransformer(xsl);
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            Result output = new StreamResult(result);
            t.transform(xml, output);
            return result.toByteArray();
        } catch (Exception ex) {
            LOG.warn("Failed to apply XSLT transformation", ex);
        }
        return null;
    }

    public byte[] transform(Document data, Document xslt)
    {
        return transform(new DOMSource(data), new DOMSource(xslt));
    }

    public byte[] transform(byte[] data, byte[] xslt)
    {
        return transform(new StreamSource(new ByteArrayInputStream(data)),
            new StreamSource(new ByteArrayInputStream(xslt)));
    }

    public byte[] transform(String data, String xslt)
    {
        return transform(new StreamSource(new StringReader(data)),
            new StreamSource(new StringReader(xslt)));
    }

    public byte[] transform(Attachment xml, Attachment xsl)
    {
        try {
            return transform(xml.getContent(), xsl.getContent());
        } catch (Exception ex) {
            LOG.warn("Failed to apply XSLT transformation", ex);
        }
        return new byte[] {};
    }

    public XPathResult xpath(String xpath, Document doc)
    {
        XPathEvaluator evaluator = new XPathEvaluatorImpl(doc);
        XPathNSResolver resolver = evaluator.createNSResolver(doc);
        XPathResult result =
            (XPathResult) evaluator.evaluate(xpath, doc, resolver, XPathResult.ANY_TYPE, null);
        return result;
    }

    public String xpathToString(String xpath, Document doc)
    {
        XPathResult result = xpath(xpath, doc);
        StringBuilder str = new StringBuilder();
        switch (result.getResultType()) {
            case XPathResult.BOOLEAN_TYPE:
                str.append(result.getBooleanValue());
                break;
            case XPathResult.NUMBER_TYPE:
                str.append(result.getNumberValue());
                break;
            case XPathResult.STRING_TYPE:
                str.append(result.getStringValue());
                break;
            case XPathResult.FIRST_ORDERED_NODE_TYPE:
                str.append(serializeToString(result.getSingleNodeValue()));
                break;
            case XPathResult.ORDERED_NODE_ITERATOR_TYPE:
            case XPathResult.UNORDERED_NODE_ITERATOR_TYPE:
                Node n;
                while ((n = result.iterateNext()) != null) {
                    str.append(serializeToString(n, true));
                }
                break;
            case XPathResult.ORDERED_NODE_SNAPSHOT_TYPE:
            case XPathResult.UNORDERED_NODE_SNAPSHOT_TYPE:
                for (int i = 0; i < result.getSnapshotLength(); ++i) {
                    str.append(serializeToString(result.snapshotItem(i), true));
                }
                break;
        }
        return str.toString();
    }
}
