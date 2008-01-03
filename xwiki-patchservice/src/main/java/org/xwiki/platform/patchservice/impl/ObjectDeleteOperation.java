package org.xwiki.platform.patchservice.impl;

import java.util.Formatter;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class ObjectDeleteOperation extends AbstractOperationImpl implements RWOperation
{
    private String className;

    private int number = -1;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_OBJECT_DELETE,
            ObjectDeleteOperation.class);
    }

    public ObjectDeleteOperation()
    {
        this.setType(Operation.TYPE_OBJECT_DELETE);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        BaseObject obj = doc.getObject(className, number);
        if (obj == null) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                new Formatter().format("Invalid object type/number: %s[%d]",
                    new Object[] {this.className, new Integer(number)}).toString());
        }
        doc.removeObject(obj);
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteObject(String objectClass, int index)
    {
        this.className = objectClass;
        this.number = index;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.className = getObjectClassname(e);
        this.number = getObjectNumber(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        xmlNode.appendChild(createObjectNode(className, number, doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            ObjectDeleteOperation otherOperation = (ObjectDeleteOperation) other;
            return otherOperation.getType().equals(this.getType())
                && otherOperation.className.equals(this.className)
                && (otherOperation.number == this.number);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(19, 23).append(this.className).append(this.number).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.className + "[" + number + "]]";
    }
}
