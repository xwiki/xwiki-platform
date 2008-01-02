package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class ClassPropertyDeleteOperation extends AbstractOperationImpl implements RWOperation
{
    public static final String PROPERTY_NAME_ATTRIBUTE_NAME = "name";

    private String propertyName;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_CLASS_PROPERTY_DELETE,
            ClassPropertyDeleteOperation.class);
    }

    public ClassPropertyDeleteOperation()
    {
        this.setType(Operation.TYPE_CLASS_PROPERTY_DELETE);
    }

    /**
     * {@inheritDoc}
     */
    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc) throws XWikiException
    {
        BaseClass bclass = doc.getxWikiClass();
        PropertyClass prop = (PropertyClass) bclass.get(propertyName);
        if (prop != null) {
            bclass.removeField(propertyName);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid property name: " + this.propertyName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean deleteType(String propertyName)
    {
        this.propertyName = propertyName;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.propertyName = e.getAttribute(PROPERTY_NAME_ATTRIBUTE_NAME);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(AbstractOperationImpl.NODE_NAME);
        xmlNode.setAttribute(AbstractOperationImpl.TYPE_ATTRIBUTE_NAME, this.getType());
        xmlNode.setAttribute(PROPERTY_NAME_ATTRIBUTE_NAME, propertyName);
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            ClassPropertyDeleteOperation otherOperation = (ClassPropertyDeleteOperation) other;
            return otherOperation.propertyName.equals(this.propertyName);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return new HashCodeBuilder(13, 17).append(this.propertyName).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.propertyName + "]";
    }
}
