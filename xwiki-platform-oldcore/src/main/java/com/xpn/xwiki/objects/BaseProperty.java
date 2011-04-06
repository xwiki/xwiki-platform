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

package com.xpn.xwiki.objects;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import com.xpn.xwiki.web.Utils;

/**
 * @version $Id$
 */
// TODO: shouldn't this be abstract? toFormString and toText
// will never work unless getValue is overriden
public class BaseProperty extends BaseElement implements PropertyInterface, Serializable, Cloneable
{
    private BaseCollection object;

    private int id;

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.PropertyInterface#getObject()
     */
    public BaseCollection getObject()
    {
        return this.object;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.PropertyInterface#setObject(com.xpn.xwiki.objects.BaseCollection)
     */
    public void setObject(BaseCollection object)
    {
        this.object = object;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseElement#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object el)
    {
        // Same Java object, they sure are equal
        if (this == el) {
            return true;
        }

        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        if ((this.object == null) || ((BaseProperty) el).getObject() == null) {
            return (hashCode() == el.hashCode());
        }

        if (!super.equals(el)) {
            return false;
        }

        return (getId() == ((BaseProperty) el).getId());
    }

    public int getId()
    {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        if (this.object == null) {
            return this.id;
        } else {
            return getObject().getId();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.PropertyInterface#setId(int)
     */
    public void setId(int id)
    {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        this.id = id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        return ("" + getId() + getName()).hashCode();
    }

    public String getClassType()
    {
        return getClass().getName();
    }

    public void setClassType(String type)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.BaseElement#clone()
     */
    @Override
    public Object clone()
    {
        BaseProperty property = (BaseProperty) super.clone();
        property.setObject(getObject());
        return property;
    }

    public Object getValue()
    {
        return null;
    }

    public void setValue(Object value)
    {
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.PropertyInterface#toXML()
     */
    public Element toXML()
    {
        Element el = new DOMElement(getName());
        Object value = getValue();
        el.setText((value == null) ? "" : value.toString());

        return el;
    }

    /**
     * {@inheritDoc}
     * 
     * @see com.xpn.xwiki.objects.PropertyInterface#toFormString()
     */
    public String toFormString()
    {
        return Utils.formEncode(toText());
    }

    public String toText()
    {
        Object value = getValue();

        return (value == null) ? "" : value.toString();
    }

    public String toXMLString()
    {
        Document doc = new DOMDocument();
        doc.setRootElement(toXML());
        OutputFormat outputFormat = new OutputFormat("", true);
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter(out, outputFormat);
        try {
            writer.write(doc);

            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();

            return "";
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return toXMLString();
    }

    public Object getCustomMappingValue()
    {
        return getValue();
    }
}
