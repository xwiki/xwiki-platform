/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
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
    private String typeName;

    private Map<String, String> typeConfig;

    private String className;

    static {
        OperationFactoryImpl
            .registerTypeProvider(Operation.TYPE_CLASS_PROPERTY_CHANGE, ClassPropertySetOperation.class);
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
        PropertyClass pclass = (PropertyClass) bclass.get(this.typeName);
        if (pclass != null) {
            pclass.getxWikiClass(null).fromMap(this.typeConfig, pclass);
            bclass.put(pclass.getName(), pclass);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid property name : " + this.typeName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean modifyType(String className, String typeName, Map<String, ? > typeConfig)
    {
        this.className = className;
        this.typeName = typeName;
        this.typeConfig = new HashMap<String, String>();
        for (Iterator<String> it = typeConfig.keySet().iterator(); it.hasNext();) {
            String prop = it.next();
            this.typeConfig.put(prop, typeConfig.get(prop).toString());
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
        this.typeName = propertyNode.getAttribute(PROPERTY_NAME_ATTRIBUTE_NAME);
        this.typeConfig = new HashMap<String, String>();
        NodeList properties = propertyNode.getElementsByTagName(PROPERTY_NODE_NAME);
        for (int i = 0; i < properties.getLength(); ++i) {
            Element prop = (Element) properties.item(i);
            String name = prop.getAttribute(PROPERTY_NAME_ATTRIBUTE_NAME);
            String value = prop.getAttribute(PROPERTY_VALUE_ATTRIBUTE_NAME);
            this.typeConfig.put(name, value);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        Element classNode = createClassNode(this.className, doc);
        Element typeNode = createPropertyNode(this.typeName, doc);
        for (Iterator<String> it = this.typeConfig.keySet().iterator(); it.hasNext();) {
            String propName = it.next();
            typeNode.appendChild(createPropertyNode(propName, this.typeConfig.get(propName).toString(), doc));
        }
        classNode.appendChild(typeNode);
        xmlNode.appendChild(classNode);
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object other)
    {
        try {
            ClassPropertySetOperation otherOperation = (ClassPropertySetOperation) other;
            return otherOperation.typeName.equals(this.typeName) && (this.getType().equals(otherOperation.getType()))
                && (this.typeConfig.values().containsAll(otherOperation.typeConfig.values()))
                && (otherOperation.typeConfig.values().containsAll(this.typeConfig.values()))
                && (this.typeConfig.keySet().containsAll(otherOperation.typeConfig.keySet()))
                && (otherOperation.typeConfig.keySet().containsAll(this.typeConfig.keySet()));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        // return new HashCodeBuilder(11, 13).append(this.propertyType).append(this.propertyConfig)
        // .toHashCode();
        int i = new HashCodeBuilder(11, 13).append(this.typeName).append(this.typeConfig).toHashCode();
        return i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.getType() + ": [" + this.typeName + "] = " + this.typeConfig;
    }
}
