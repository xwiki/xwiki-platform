package org.xwiki.platform.patchservice.impl;

import java.util.Formatter;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Position;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class ObjectPropertyDeleteAtOperation extends AbstractOperationImpl implements RWOperation
{
    private String className;

    private int number = -1;

    private String propertyName;

    private Position position;

    private String deletedContent;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_OBJECT_PROPERTY_DELETE_AT,
            ObjectPropertyDeleteAtOperation.class);
    }

    public ObjectPropertyDeleteAtOperation()
    {
        this.setType(Operation.TYPE_OBJECT_PROPERTY_DELETE_AT);
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
            String value = obj.getLargeStringValue(propertyName);
            if (!position.checkPosition(value)
                || !position.getTextAfterPosition(value).startsWith(this.deletedContent)) {
                throw new Exception();
            }
            value =
                position.getTextBeforePosition(value)
                    + StringUtils.substring(position.getTextAfterPosition(value), deletedContent
                        .length());
            obj.set(propertyName, value, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                new Formatter().format("Invalid deleted text: [%s] for object property [%s]",
                    new Object[] {deletedContent, propertyName}).toString());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteFromProperty(String objectClass, int index, String propertyName,
        String value, Position position)
    {
        this.className = objectClass;
        this.number = index;
        this.propertyName = propertyName;
        this.deletedContent = value;
        this.position = position;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.className = getObjectClassname(e);
        this.number = getObjectNumber(e);
        this.propertyName = getPropertyName(getObjectNode(e));
        this.deletedContent = getTextValue(e);
        this.position = loadPositionNode(e);
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
        xmlNode.appendChild(createTextNode(deletedContent, doc));
        xmlNode.appendChild(position.toXml(doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            ObjectPropertyDeleteAtOperation that = (ObjectPropertyDeleteAtOperation) other;
            return that.getType().equals(this.getType()) && that.className.equals(this.className)
                && (that.number == this.number) && that.propertyName.equals(this.propertyName)
                && that.deletedContent.equals(this.deletedContent)
                && that.position.equals(this.position);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(31, 37).append(className).append(number).append(propertyName)
            .append(deletedContent).append(position).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return getType() + ": [" + className + "[" + number + "]#" + propertyName + "]@"
            + position + " = [" + deletedContent + "]";
    }
}
