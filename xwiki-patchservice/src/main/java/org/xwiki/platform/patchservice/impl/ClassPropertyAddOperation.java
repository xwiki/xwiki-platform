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
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

public class ClassPropertyAddOperation extends AbstractOperationImpl implements RWOperation
{
    private String typeName;

    private String typeType;

    private Map<String, String> typeConfig;

    private String className;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_CLASS_PROPERTY_ADD, ClassPropertyAddOperation.class);
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
        PropertyMetaClass pmclass = (PropertyMetaClass) mclass.get(this.typeType);
        if (pmclass != null) {
            PropertyClass pclass = (PropertyClass) pmclass.newObject(null);
            pclass.setObject(bclass);
            pclass.getxWikiClass(null).fromMap(this.typeConfig, pclass);
            pclass.setName(this.typeName);
            bclass.put(pclass.getName(), pclass);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid property type : " + this.typeType);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean createType(String className, String typeName, String typeType, Map<String, ? > typeConfig)
    {
        this.className = className;
        this.typeName = typeName;
        this.typeType = typeType;
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
        this.className = getClassName(e);
        this.typeName = getPropertyName(e);
        this.typeType = getPropertyType(e);
        Element propertyNode = getPropertyNode(getClassNode(e));
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
        Element typeNode = createClassPropertyNode(this.typeName, this.typeType, doc);
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
            ClassPropertyAddOperation that = (ClassPropertyAddOperation) other;
            return (this.getType().equals(that.getType())) && this.typeType.equals(that.typeType)
                && this.typeName.equals(that.typeName)
                && (this.typeConfig.values().containsAll(that.typeConfig.values()))
                && (that.typeConfig.values().containsAll(this.typeConfig.values()))
                && (this.typeConfig.keySet().containsAll(that.typeConfig.keySet()))
                && (that.typeConfig.keySet().containsAll(this.typeConfig.keySet()));
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
        int i =
            new HashCodeBuilder(11, 13).append(this.typeName).append(this.typeType).append(this.typeConfig)
                .toHashCode();
        return i;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.getType() + ": [" + this.typeName + ":" + this.typeType + "] = " + this.typeConfig;
    }
}
