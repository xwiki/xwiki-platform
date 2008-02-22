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

import java.io.InputStream;

import javax.xml.transform.Source;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.xpath.XPathResult;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.plugin.PluginApi;

/**
 * Api for the XmlPlugin.
 * 
 * @see PluginApi
 * @see XmlPlugin
 */
public class XmlPluginApi extends PluginApi
{
    public XmlPluginApi(XmlPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
        setPlugin(plugin);
    }

    protected XmlPlugin getXmlPlugin()
    {
        return (XmlPlugin) getInternalPlugin();
    }
    
    public Document getDomDocument()
    {
        return getXmlPlugin().getDomDocument();
    }

    public Document parse(byte[] content)
    {
        return getXmlPlugin().parse(content);
    }

    public Document parse(String content)
    {
        return getXmlPlugin().parse(content);
    }

    public Document parse(InputStream stream)
    {
        return getXmlPlugin().parse(stream);
    }

    public Document parse(XWikiAttachment attachment)
    {
        return getXmlPlugin().parse(attachment, context);
    }

    public String serializeToString(Node node)
    {
        return getXmlPlugin().serializeToString(node);
    }

    public String serializeToString(Node node, boolean omitXmlDeclaration)
    {
        return getXmlPlugin().serializeToString(node, omitXmlDeclaration);
    }

    public byte[] serializeToByteArray(Node node)
    {
        return getXmlPlugin().serializeToByteArray(node);
    }

    public byte[] serializeToByteArray(Node node, boolean omitXmlDeclaration)
    {
        return getXmlPlugin().serializeToByteArray(node, omitXmlDeclaration);
    }

    public byte[] transform(Source xml, Source xsl)
    {
        return getXmlPlugin().transform(xml, xsl);
    }

    public byte[] transform(Document data, Document xslt)
    {
        return getXmlPlugin().transform(data, xslt);
    }

    public byte[] transform(byte[] data, byte[] xslt)
    {
        return getXmlPlugin().transform(data, xslt);
    }

    public byte[] transform(String data, String xslt)
    {
        return getXmlPlugin().transform(data, xslt);
    }

    public byte[] transform(Attachment xml, Attachment xsl)
    {
        return getXmlPlugin().transform(xml, xsl);
    }

    public XPathResult xpath(String xpath, Document doc)
    {
        return getXmlPlugin().xpath(xpath, doc);
    }

    public String xpathToString(String xpath, Document doc)
    {
        return getXmlPlugin().xpathToString(xpath, doc);
    }
}
