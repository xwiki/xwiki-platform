package org.xwiki.platform.patchservice.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
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
 * @version $Id: $
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
    private List operations = new ArrayList();

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
    public List getOperations()
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
    public void setOperations(List operations)
    {
        this.clearOperations();
        this.operations.addAll(operations);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        for (Iterator it = operations.iterator(); it.hasNext();) {
            ((Operation) it.next()).apply(doc, context);
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
            xmlNode.appendChild(id.toXml(doc));
            xmlNode.appendChild(originator.toXml(doc));
            for (Iterator it = operations.iterator(); it.hasNext();) {
                xmlNode.appendChild(((Operation) it.next()).toXml(doc));
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
            id = new PatchIdImpl();
            id.fromXml((Element) e.getElementsByTagName(PatchIdImpl.NODE_NAME).item(0));
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
}
