package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class ObjectAddOperation extends AbstractOperationImpl implements RWOperation
{
    private String className;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_OBJECT_ADD,
            ObjectAddOperation.class);
    }

    public ObjectAddOperation()
    {
        this.setType(Operation.TYPE_OBJECT_ADD);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        BaseObject obj = doc.newObject(this.className, context);
        if (obj == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid class name: " + this.className);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean addObject(String objectClass)
    {
        this.className = objectClass;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.className = getObjectClassname(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        xmlNode.appendChild(createObjectNode(className, doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            ObjectAddOperation otherOperation = (ObjectAddOperation) other;
            return otherOperation.getType().equals(this.getType())
                && otherOperation.className.equals(this.className);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(17, 19).append(this.className).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.className + "]";
    }
}
