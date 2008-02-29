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

public class ObjectPropertySetOperation extends AbstractOperationImpl implements RWOperation
{
    private String className;

    private int number = -1;

    private String propertyName;

    private String value;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_OBJECT_PROPERTY_SET,
            ObjectPropertySetOperation.class);
    }

    public ObjectPropertySetOperation()
    {
        this.setType(Operation.TYPE_OBJECT_PROPERTY_SET);
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
        try {
            obj.set(propertyName, value, context);
        } catch (RuntimeException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                new Formatter().format("Invalid value: [%s] for object property [%s]",
                    new Object[] {value, propertyName}).toString(), e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean setObjectProperty(String objectClass, int number, String propertyName,
        String value)
    {
        this.className = objectClass;
        this.number = number;
        this.propertyName = propertyName;
        this.value = value;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.className = getObjectClassname(e);
        this.number = getObjectNumber(e);
        this.propertyName = getPropertyName(e);
        this.value = getTextValue(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        Element objectNode = createObjectNode(className, number, doc);
        objectNode.appendChild(createPropertyNode(propertyName, doc));
        xmlNode.appendChild(objectNode);
        xmlNode.appendChild(createTextNode(value, doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            ObjectPropertySetOperation otherOperation = (ObjectPropertySetOperation) other;
            return otherOperation.getType().equals(this.getType())
                && otherOperation.className.equals(this.className)
                && (otherOperation.number == this.number)
                && otherOperation.propertyName.equals(this.propertyName)
                && otherOperation.value.equals(this.value);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(23, 29).append(className).append(number).append(propertyName)
            .append(value).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return getType() + ": [" + className + "[" + number + "]#" + propertyName + "] = ["
            + value + "]";
    }
}
