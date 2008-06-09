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
        return specVersion;
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
        return id;
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
        return description;
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
        return originator;
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
        return operations;
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
        for (Operation op : operations) {
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
            xmlNode.setAttribute(SPEC_VERSION_ATTRIBUTE_NAME, specVersion);
            xmlNode.setAttribute(DESCRIPTION_ATTRIBUTE_NAME, description);
            if (id != null) {
                xmlNode.appendChild(id.toXml(doc));
            }
            if (originator != null) {
                xmlNode.appendChild(originator.toXml(doc));
            }
            for (Operation op : operations) {
                xmlNode.appendChild(op.toXml(doc));
            }
            return xmlNode;
        } catch (RuntimeException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to export patch to XML",
                ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        try {
            specVersion = e.getAttribute(SPEC_VERSION_ATTRIBUTE_NAME);
            description = e.getAttribute(DESCRIPTION_ATTRIBUTE_NAME);
            Element idElement = (Element) e.getElementsByTagName(PatchIdImpl.NODE_NAME).item(0);
            if (idElement != null) {
                id = new PatchIdImpl();
                id.fromXml(idElement);
            }
            NodeList operationNodes = e.getElementsByTagName(AbstractOperationImpl.NODE_NAME);
            clearOperations();
            for (int i = 0; i < operationNodes.getLength(); ++i) {
                Operation o =
                    OperationFactoryImpl.getInstance().loadOperation(
                        (Element) operationNodes.item(i));
                operations.add(o);
            }
        } catch (RuntimeException ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Failed to load patch from XML",
                ex);
        }
    }

    public String getContent()
    {
        try {
            Document doc =
                DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
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
            this.fromXml((Element) doc.getDocumentElement());
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
