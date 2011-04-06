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

import java.text.SimpleDateFormat;
import java.util.Date;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

public class DateProperty extends BaseProperty implements Cloneable
{
    private Date value;

    public DateProperty()
    {
    }

    @Override
    public Object getValue()
    {
        return this.value;
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
    public void setValue(Object value)
    {
        this.value = (Date) value;
    }

    @Override
    public String toText()
    {
        if (getValue() == null) {
            return "";
        }
        
        return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")).format(getValue());        
    }

    @Override
    public boolean equals(Object obj)
    {
        // Same Java object, they sure are equal
        if (this == obj) {
            return true;
        }

        if (!super.equals(obj)) {
            return false;
        }

        if ((getValue() == null) && (((DateProperty) obj).getValue() == null)) {
            return true;
        }

        return getValue().equals(((DateProperty) obj).getValue());
    }

    @Override
    public Object clone()
    {
        DateProperty property = (DateProperty) super.clone();
        property.setValue(getValue());

        return property;
    }

}
