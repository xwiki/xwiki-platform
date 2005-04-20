/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 9 déc. 2003
 * Time: 14:04:34
 */
package com.xpn.xwiki.objects;

import com.xpn.xwiki.objects.classes.DateClass;

import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;


public class DateProperty extends BaseProperty {
    private Date value;

    public DateProperty() {
    }

    public Object getValue() {
        return value;
    }

    public Element toXML() {
        Element el = new DOMElement(getName());
        el.setText(toXMLString());
        return el;
    }

    public String toXMLString() {
        try {
         return (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")).format(getValue());
        } catch (Exception e) {
            return "";
        }
    }

    public void setValue(Object value) {
        this.value = (Date)value;
    }

    public String toText() {
        Date d = (Date)getValue();
        return (d==null) ? "" : d.toString();
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj))
         return false;

       if ((getValue()==null)
            && (((DateProperty)obj).getValue()==null))
         return true;

       return getValue().equals(((DateProperty)obj).getValue());
    }

    public Object clone() {
        DateProperty property = (DateProperty) super.clone();
        property.setValue(getValue());
        return property;
    }

}
