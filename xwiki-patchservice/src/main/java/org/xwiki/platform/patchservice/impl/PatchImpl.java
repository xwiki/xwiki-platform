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
package org.xwiki.platform.patchservice.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.xml.sax.SAXException;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Originator;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.PatchId;
import org.xwiki.platform.patchservice.api.RWPatch;
import org.xwiki.platform.patchservice.api.XmlSerializable;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation for {@link RWPatch}.
 * 
 * @see org.xwiki.platform.patchservice.api.RWPatch
 * @see org.xwiki.platform.patchservice.api.Patch
 * @version $Id$
 * @since XWikiPlatform 1.3
 */
public class PatchImpl implements Patch, RWPatch, XmlSerializable
{
    /**
     * The patch format version supported by this implementation.
     * 
     * @see Patch#getSpecVersion()
     */
    public static final String SPEC_VERSION = "1.0";

    /** The name of the XML element corresponding to patches. */
    public static final String NODE_NAME = "patch";

    /**
     * The name of the XML attribute holding the patch format version.
     * 
     * @see Patch#getSpecVersion()
     */
    public static final String SPEC_VERSION_ATTRIBUTE_NAME = "version";

    /**
     * The name of the XML attribute holding the patch description, if any.
     * 
     * @see Patch#getDescription()
     */
    public static final String DESCRIPTION_ATTRIBUTE_NAME = "description";

    /**
     * The patch format version of this patch.
     * 
     * @see Patch#getSpecVersion()
     */
    private String specVersion = SPEC_VERSION;

    /**
     * The patch description.
     * 
     * @see Patch#getDescription()
     */
    private String description = "";

    /**
     * The ID of this patch.
     * 
     * @see Patch#getId()
     */
    private PatchId id;

    /**
     * The origin of this patch.
     * 
     * @see Patch#getOriginator()
     */
    private Originator originator;

    /**
     * List of operations in this patch.
     * 
     * @see Patch#getOperations()
     */
    private List<Operation> operations = new ArrayList<Operation>();

    /**
     * {@inheritDoc}
     */
    public String getSpecVersion()
    {
        return this.specVersion;
    }

    /**
     * {@inheritDoc}
     */
    public void setSpecVersion(String specVersion)
    {
        this.specVersion = specVersion;
    }

    /**
     * {@inheritDoc}
     */
    public PatchId getId()
    {
        return this.id;
    }

    /**
     * {@inheritDoc}
     */
    public void setId(PatchId id)
    {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * {@inheritDoc}
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * {@inheritDoc}
     */
    public Originator getOriginator()
    {
        return this.originator;
    }

    /**
     * {@inheritDoc}
     */
    public void setOriginator(Originator originator)
    {
        this.originator = originator;
    }

    /**
     * {@inheritDoc}
     */
    public List<Operation> getOperations()
    {
        return this.operations;
    }

    /**
     * {@inheritDoc}
     */
    public void addOperation(Operation op)
    {
        this.operations.add(op);
    }

    /**
     * {@inheritDoc}
     */
    public void clearOperations()
    {
        this.operations.clear();
    }

    /**
     * {@inheritDoc}
     */
    public void setOperations(List<Operation> operations)
    {
        this.clearOperations();
        this.operations.addAll(operations);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        for (Operation op : this.operations) {
            op.apply(doc, context);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        try {
            Element xmlNode = doc.createElement(NODE_NAME);
            xmlNode.setAttribute(SPEC_VERSION_ATTRIBUTE_NAME, this.specVersion);
            xmlNode.setAttribute(DESCRIPTION_ATTRIBUTE_NAME, this.description);
            if (this.id != null) {
                xmlNode.appendChild(this.id.toXml(doc));
            }
            if (this.originator != null) {
                xmlNode.appendChild(this.originator.toXml(doc));
            }
            for (Operation op : this.operations) {
                xmlNode.appendChild(op.toXml(doc));
            }
            return xmlNode;
        } catch (RuntimeException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to export patch to XML", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        try {
            this.specVersion = e.getAttribute(SPEC_VERSION_ATTRIBUTE_NAME);
            this.description = e.getAttribute(DESCRIPTION_ATTRIBUTE_NAME);
            Element idElement = (Element) e.getElementsByTagName(PatchIdImpl.NODE_NAME).item(0);
            if (idElement != null) {
                this.id = new PatchIdImpl();
                this.id.fromXml(idElement);
            }
            NodeList operationNodes = e.getElementsByTagName(AbstractOperationImpl.NODE_NAME);
            clearOperations();
            for (int i = 0; i < operationNodes.getLength(); ++i) {
                Operation o = OperationFactoryImpl.getInstance().loadOperation((Element) operationNodes.item(i));
                this.operations.add(o);
            }
        } catch (RuntimeException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to load patch from XML", ex);
        }
    }

    public String getContent()
    {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            doc.appendChild(toXml(doc));
            DOMImplementationLS ls = (DOMImplementationLS) doc.getImplementation();

            LSOutput output = ls.createLSOutput();
            StringWriter content = new StringWriter();
            output.setCharacterStream(content);
            output.setEncoding("ISO-8859-1");
            ls.createLSSerializer().write(doc, output);
            return content.toString();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (DOMException e) {
            e.printStackTrace();
        } catch (XWikiException e) {
            e.printStackTrace();
        }
        return "";
    }

    public void setContent(String content)
    {
        try {
            Document doc =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new ByteArrayInputStream(content.getBytes()));
            this.fromXml(doc.getDocumentElement());
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (XWikiException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
