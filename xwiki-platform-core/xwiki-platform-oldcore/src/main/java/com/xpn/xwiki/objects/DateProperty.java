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
 */
package com.xpn.xwiki.objects;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

/**
 * Represents a date property.
 * 
 * @version $Id$
 */
public class DateProperty extends BaseProperty implements Cloneable
{
    /**
     * The property value.
     */
    private Date value;

    @Override
    public Object getValue()
    {
        return this.value;
    }

    @Override
    public void setValue(Object value)
    {
        Date date = (Date) value;

        // Make sure to store a Date and not some extended Date or it's going to be a nightmare to compare between them
        if (date.getClass() != Date.class) {
            date = new Date(((Date) value).getTime());
        }

        setValueDirty(date);
        this.value = date;
    }

    @Override
    public Element toXML()
    {
        Element el = new DOMElement(getName());
        el.setText(toXMLString());
        return el;
    }

    @Override
    public String toXMLString()
    {
        return toText();
    }

    @Override
    public String toText()
    {
        // FIXME: The value of a date property should be serialized using the date timestamp or the date format
        // specified in the XClass the date property belongs to.
        return getValue() == null ? "" : new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(getValue());
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        // Same Java object, they sure are equal.
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if (getValue() == null && ((DateProperty) obj).getValue() == null) {
            return true;
        }

        return getValue().equals(((DateProperty) obj).getValue());
    }

    @Override
    public DateProperty clone()
    {
        return (DateProperty) super.clone();
    }

    @Override
    protected void cloneInternal(BaseProperty clone)
    {
        DateProperty property = (DateProperty) clone;
        property.setValue(getValue());
    }
}
