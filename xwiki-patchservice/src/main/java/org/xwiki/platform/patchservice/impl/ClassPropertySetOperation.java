package org.xwiki.platform.patchservice.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class ClassPropertySetOperation extends AbstractOperationImpl implements RWOperation
{
    public static final String TYPE_NODE_NAME = "type";

    public static final String PROPERTY_NAME_ATTRIBUTE_NAME = "name";

    public static final String PROPERTY_VALUE_ATTRIBUTE_NAME = "value";

    private String propertyType;

    private Map propertyConfig;

    static {
        OperationFactoryImpl.registerTypeProvider(
            Operation.TYPE_CLASS_PROPERTY_ADD, ClassPropertySetOperation.class);
        OperationFactoryImpl.registerTypeProvider(
            Operation.TYPE_CLASS_PROPERTY_CHANGE, ClassPropertySetOperation.class);
    }

    public ClassPropertySetOperation()
    {
        this.setType(Operation.TYPE_CLASS_PROPERTY_ADD);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc) throws XWikiException
    {
        BaseClass bclass = doc.getxWikiClass();
        bclass.setName(doc.getFullName());
        MetaClass mclass = MetaClass.getMetaClass();
        PropertyMetaClass pmclass = (PropertyMetaClass) mclass.get(this.propertyType);
        if (pmclass != null) {
            PropertyClass pclass = (PropertyClass) pmclass.newObject(null);
            pclass.setObject(bclass);
            pclass.getxWikiClass(null).fromMap(propertyConfig, pclass);
            bclass.put(pclass.getName(), pclass);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid operation type : " + this.propertyType);
        }
    }

    public boolean createType(String propertyType, Map properties)
    {
        this.propertyType = propertyType;
        this.propertyConfig = new HashMap();
        for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
            String prop = (String) it.next();
            this.propertyConfig.put(prop, properties.get(prop).toString());
        }
        this.setType(Operation.TYPE_CLASS_PROPERTY_ADD);
        return true;
    }

    public boolean modifyType(String propertyType, Map properties)
    {
        this.propertyType = propertyType;
        this.propertyConfig = properties;
        this.setType(Operation.TYPE_CLASS_PROPERTY_CHANGE);
        return true;
    }

    public void fromXml(Element e) throws XWikiException
    {
        this.setType(e.getAttribute(TYPE_ATTRIBUTE_NAME));
        this.propertyType = e.getElementsByTagName(TYPE_NODE_NAME).item(0).getTextContent();
        this.propertyConfig = new HashMap();
        NodeList properties = e.getElementsByTagName(PROPERTY_NODE_NAME);
        for (int i = 0; i < properties.getLength(); ++i) {
            Element prop = (Element) properties.item(i);
            String name = prop.getAttribute(PROPERTY_NAME_ATTRIBUTE_NAME);
            String value = prop.getAttribute(PROPERTY_VALUE_ATTRIBUTE_NAME);
            propertyConfig.put(name, value);
        }
    }

    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = doc.createElement(AbstractOperationImpl.NODE_NAME);
        xmlNode.setAttribute(AbstractOperationImpl.TYPE_ATTRIBUTE_NAME, this.getType());
        Element typeNode = doc.createElement(TYPE_NODE_NAME);
        typeNode.appendChild(doc.createTextNode(this.propertyType));
        xmlNode.appendChild(typeNode);
        for (Iterator it = propertyConfig.keySet().iterator(); it.hasNext();) {
            Element propertyNode = doc.createElement(PROPERTY_NODE_NAME);
            String propName = (String) it.next();
            propertyNode.setAttribute(PROPERTY_NAME_ATTRIBUTE_NAME, propName);
            propertyNode.setAttribute(PROPERTY_VALUE_ATTRIBUTE_NAME, propertyConfig.get(propName)
                .toString());
            xmlNode.appendChild(propertyNode);
        }
        return xmlNode;
    }

    public boolean equals(Object other)
    {
        try {
            ClassPropertySetOperation otherOperation = (ClassPropertySetOperation) other;
            return (otherOperation.propertyType == this.propertyType)
                && (this.propertyConfig.values().containsAll(otherOperation.propertyConfig
                    .values()))
                && (otherOperation.propertyConfig.values().containsAll(this.propertyConfig
                    .values()))
                && (this.propertyConfig.keySet().containsAll(otherOperation.propertyConfig
                    .keySet()))
                && (otherOperation.propertyConfig.keySet().containsAll(this.propertyConfig
                    .keySet()));
        } catch (Exception e) {
            return false;
        }
    }

    public int hashCode()
    {
        // return new HashCodeBuilder(11, 13).append(this.propertyType).append(this.propertyConfig)
        // .toHashCode();
        int i =
            new HashCodeBuilder(11, 13).append(this.propertyType).append(this.propertyConfig)
                .toHashCode();
        return i;
    }

    public String toString()
    {
        return Operation.TYPE_CLASS_PROPERTY_ADD + ": [" + this.propertyType + "] = "
            + this.propertyConfig;
    }
}
