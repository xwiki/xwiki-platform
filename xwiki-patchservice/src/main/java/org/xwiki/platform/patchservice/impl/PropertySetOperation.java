package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class PropertySetOperation extends AbstractOperationImpl implements RWOperation
{
    public static final String NAME_ATTRIBUTE_NAME = "name";

    public static final String VALUE_ATTRIBUTE_NAME = "value";

    private String propertyName;

    private String propertyValue;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_PROPERTY_SET,
            PropertySetOperation.class);
    }

    public PropertySetOperation()
    {
        this.setType(Operation.TYPE_PROPERTY_SET);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc) throws XWikiException
    {
        try {
            doc.getClass().getMethod("set" + StringUtils.capitalize(propertyName),
                new Class[] {String.class}).invoke(doc, new Object[] {this.propertyValue});
        } catch (Exception ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Patch cannot be applied",
                ex);
        }
    }

    public boolean setProperty(String property, String value)
    {
        this.propertyName = property;
        this.propertyValue = value;
        return true;
    }

    public void fromXml(Element e) throws XWikiException
    {
        Element textNode = (Element) e.getFirstChild();
        this.propertyName =
            StringEscapeUtils.unescapeXml(textNode.getAttribute(NAME_ATTRIBUTE_NAME));
        this.propertyValue =
            StringEscapeUtils.unescapeXml(textNode.getAttribute(VALUE_ATTRIBUTE_NAME));
    }

    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(AbstractOperationImpl.NODE_NAME);
        xmlNode.setAttribute(AbstractOperationImpl.TYPE_ATTRIBUTE_NAME,
            Operation.TYPE_PROPERTY_SET);
        Element propertyNode = doc.createElement(PROPERTY_NODE_NAME);
        propertyNode.setAttribute(NAME_ATTRIBUTE_NAME, StringEscapeUtils
            .escapeXml(this.propertyName));
        propertyNode.setAttribute(VALUE_ATTRIBUTE_NAME, StringEscapeUtils
            .escapeXml(this.propertyValue));
        xmlNode.appendChild(propertyNode);
        return xmlNode;
    }

    public boolean equals(Object other)
    {
        try {
            PropertySetOperation otherOperation = (PropertySetOperation) other;
            return otherOperation.propertyName.equals(this.propertyName)
                && otherOperation.propertyValue.equals(this.propertyValue);
        } catch (Exception e) {
            return false;
        }
    }

    public int hashCode()
    {
        return new HashCodeBuilder(7, 11).append(this.propertyName).append(this.propertyValue)
            .toHashCode();
    }

    public String toString()
    {
        return this.getType() + ": [" + this.propertyName + "] to [" + this.propertyValue + "]";
    }
}
