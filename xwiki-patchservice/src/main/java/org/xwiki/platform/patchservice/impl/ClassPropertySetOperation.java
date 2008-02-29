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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class ClassPropertySetOperation extends AbstractOperationImpl implements RWOperation
{
    private String propertyName;

    private Map<String, String> propertyConfig;

    private String className;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_CLASS_PROPERTY_CHANGE,
            ClassPropertySetOperation.class);
    }

    public ClassPropertySetOperation()
    {
        this.setType(Operation.TYPE_CLASS_PROPERTY_CHANGE);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        BaseClass bclass = doc.getxWikiClass();
        PropertyClass pclass = (PropertyClass) bclass.get(propertyName);
        if (pclass != null) {
            pclass.getxWikiClass(null).fromMap(propertyConfig, pclass);
            bclass.put(pclass.getName(), pclass);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid propert name : " + this.propertyName);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean modifyType(String className, String propertyName, Map<String, ?> properties)
    {
        this.className = className;
        this.propertyName = propertyName;
        this.propertyConfig = new HashMap<String, String>();
        for (Iterator<String> it = properties.keySet().iterator(); it.hasNext();) {
            String prop = (String) it.next();
            this.propertyConfig.put(prop, properties.get(prop).toString());
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.setType(getOperationType(e));
        this.className = getClassName(e);
        Element propertyNode = getPropertyNode(getClassNode(e));
        this.propertyName = propertyNode.getAttribute(PROPERTY_NAME_ATTRIBUTE_NAME);
        this.propertyConfig = new HashMap<String, String>();
        NodeList properties = propertyNode.getElementsByTagName(PROPERTY_NODE_NAME);
        for (int i = 0; i < properties.getLength(); ++i) {
            Element prop = (Element) properties.item(i);
            String name = prop.getAttribute(PROPERTY_NAME_ATTRIBUTE_NAME);
            String value = prop.getAttribute(PROPERTY_VALUE_ATTRIBUTE_NAME);
            propertyConfig.put(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        Element classNode = createClassNode(className, doc);
        Element typeNode = createPropertyNode(propertyName, doc);
        for (Iterator<String> it = propertyConfig.keySet().iterator(); it.hasNext();) {
            String propName = (String) it.next();
            typeNode.appendChild(createPropertyNode(propName, propertyConfig.get(propName)
                .toString(), doc));
        }
        classNode.appendChild(typeNode);
        xmlNode.appendChild(classNode);
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object other)
    {
        try {
            ClassPropertySetOperation otherOperation = (ClassPropertySetOperation) other;
            return otherOperation.propertyName.equals(this.propertyName)
                && (this.getType().equals(otherOperation.getType()))
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

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        // return new HashCodeBuilder(11, 13).append(this.propertyType).append(this.propertyConfig)
        // .toHashCode();
        int i =
            new HashCodeBuilder(11, 13).append(this.propertyName).append(this.propertyConfig)
                .toHashCode();
        return i;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.propertyName + "] = " + this.propertyConfig;
    }
}
