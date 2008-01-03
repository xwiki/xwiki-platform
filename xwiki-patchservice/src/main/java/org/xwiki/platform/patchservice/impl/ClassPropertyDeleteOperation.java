package org.xwiki.platform.patchservice.impl;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class ClassPropertyDeleteOperation extends AbstractOperationImpl implements RWOperation
{
    private String propertyName;

    private String className;

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
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
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
    public boolean deleteType(String className, String propertyName)
    {
        this.className = className;
        this.propertyName = propertyName;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.className = getClassName(e);
        this.propertyName = getPropertyName(getClassNode(e));
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        Element classNode = createClassNode(className, doc);
        classNode.appendChild(createPropertyNode(propertyName, doc));
        xmlNode.appendChild(classNode);
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            ClassPropertyDeleteOperation that = (ClassPropertyDeleteOperation) other;
            return this.className.equals(that.className)
                && this.propertyName.equals(that.propertyName);
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
