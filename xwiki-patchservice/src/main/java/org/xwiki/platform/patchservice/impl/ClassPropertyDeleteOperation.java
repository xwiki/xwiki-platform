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
    private String typeName;

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
        PropertyClass prop = (PropertyClass) bclass.get(this.typeName);
        if (prop != null) {
            bclass.removeField(this.typeName);
        } else {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Invalid property name: " + this.typeName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteType(String className, String typeName)
    {
        this.className = className;
        this.typeName = typeName;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.className = getClassName(e);
        this.typeName = getPropertyName(getClassNode(e));
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        Element classNode = createClassNode(this.className, doc);
        classNode.appendChild(createPropertyNode(this.typeName, doc));
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
            ClassPropertyDeleteOperation that = (ClassPropertyDeleteOperation) other;
            return this.className.equals(that.className) && this.typeName.equals(that.typeName);
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
        return new HashCodeBuilder(13, 17).append(this.typeName).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.getType() + ": [" + this.typeName + "]";
    }
}
