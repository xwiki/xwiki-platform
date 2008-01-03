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
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class ClassPropertyAddOperation extends AbstractOperationImpl implements RWOperation
{
    private String propertyName;

    private String propertyType;

    private Map propertyConfig;

    private String className;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_CLASS_PROPERTY_ADD,
            ClassPropertyAddOperation.class);
    }

    public ClassPropertyAddOperation()
    {
        this.setType(Operation.TYPE_CLASS_PROPERTY_ADD);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        BaseClass bclass = doc.getxWikiClass();
        MetaClass mclass = MetaClass.getMetaClass();
        PropertyMetaClass pmclass = (PropertyMetaClass) mclass.get(this.propertyType);
        if (pmclass != null) {
            PropertyClass pclass = (PropertyClass) pmclass.newObject(null);
            pclass.setObject(bclass);
            pclass.getxWikiClass(null).fromMap(propertyConfig, pclass);
            pclass.setName(propertyName);
            bclass.put(pclass.getName(), pclass);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS,
                XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid property type : " + this.propertyType);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean createType(String className, String propertyName, String propertyType,
        Map properties)
    {
        this.className = className;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
        this.propertyConfig = new HashMap();
        for (Iterator it = properties.keySet().iterator(); it.hasNext();) {
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
        this.className = getClassName(e);
        this.propertyName = getPropertyName(e);
        this.propertyType = getPropertyType(e);
        Element propertyNode = getPropertyNode(getClassNode(e));
        this.propertyConfig = new HashMap();
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
        Element typeNode = createClassPropertyNode(propertyName, propertyType, doc);
        for (Iterator it = propertyConfig.keySet().iterator(); it.hasNext();) {
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
            ClassPropertyAddOperation that = (ClassPropertyAddOperation) other;
            return (this.getType().equals(that.getType()))
                && this.propertyType.equals(that.propertyType)
                && this.propertyName.equals(that.propertyName)
                && (this.propertyConfig.values().containsAll(that.propertyConfig.values()))
                && (that.propertyConfig.values().containsAll(this.propertyConfig.values()))
                && (this.propertyConfig.keySet().containsAll(that.propertyConfig.keySet()))
                && (that.propertyConfig.keySet().containsAll(this.propertyConfig.keySet()));
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
            new HashCodeBuilder(11, 13).append(this.propertyName).append(this.propertyType)
                .append(this.propertyConfig).toHashCode();
        return i;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return this.getType() + ": [" + this.propertyName + ":" + this.propertyType + "] = "
            + this.propertyConfig;
    }
}
