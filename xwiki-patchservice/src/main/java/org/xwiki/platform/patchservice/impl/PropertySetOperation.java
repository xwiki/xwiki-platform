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

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xwiki.platform.patchservice.api.Operation;
import org.xwiki.platform.patchservice.api.Patch;
import org.xwiki.platform.patchservice.api.RWOperation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

public class PropertySetOperation extends AbstractOperationImpl implements RWOperation
{
    private String propertyName;

    private String propertyValue;

    static {
        OperationFactoryImpl.registerTypeProvider(Operation.TYPE_PROPERTY_SET, PropertySetOperation.class);
    }

    public PropertySetOperation()
    {
        this.setType(Operation.TYPE_PROPERTY_SET);
    }

    /**
     * {@inheritDoc}
     */
    public void apply(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        try {
            doc.getClass().getMethod("set" + StringUtils.capitalize(this.propertyName), new Class[] {String.class})
                .invoke(doc, new Object[] {this.propertyValue});
        } catch (NoSuchMethodException ex) {
            try {
                doc.getClass().getMethod("set" + StringUtils.capitalize(this.propertyName), new Class[] {int.class})
                    .invoke(doc, new Object[] {new Integer(Integer.parseInt(this.propertyValue))});
                return;
            } catch (NoSuchMethodException ex2) {
            } catch (NumberFormatException ex2) {
            } catch (Exception ex2) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Patch cannot be applied", ex2);
            }
            try {
                doc.getClass().getMethod("set" + StringUtils.capitalize(this.propertyName), new Class[] {Date.class})
                    .invoke(doc, new Object[] {Patch.DATE_FORMAT.parse(this.propertyValue)});
            } catch (Exception ex3) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Patch cannot be applied", ex3);
            }
        } catch (Exception ex) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_PLUGINS, XWikiException.ERROR_XWIKI_UNKNOWN,
                "Patch cannot be applied", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setProperty(String property, String value)
    {
        this.propertyName = property;
        this.propertyValue = value;
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public void fromXml(Element e) throws XWikiException
    {
        this.propertyName = getPropertyName(e);
        this.propertyValue = getTextValue(e);
    }

    /**
     * {@inheritDoc}
     */
    public Element toXml(Document doc) throws XWikiException
    {
        Element xmlNode = createOperationNode(doc);
        xmlNode.appendChild(createPropertyNode(this.propertyName, doc));
        xmlNode.appendChild(createTextNode(this.propertyValue, doc));
        return xmlNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(7, 11).append(this.propertyName).append(this.propertyValue).toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return this.getType() + ": [" + this.propertyName + "] to [" + this.propertyValue + "]";
    }
}
