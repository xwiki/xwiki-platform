/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * Created by
 * User: Ludovic Dubost
 * Date: 9 déc. 2003
 * Time: 11:36:16
 */
package com.xpn.xwiki.objects;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PropertyClass;

import java.io.Serializable;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

public class BaseProperty extends BaseElement implements PropertyInterface, Serializable {
    private BaseCollection object;

    public BaseCollection getObject() {
        return object;
    }

    public void setObject(BaseCollection object) {
        this.object = object;
    }

    public int getId() {
        return getObject().getId();
    }

    public void setId(int id) {
    }

    public String getClassType() {
        return getClass().getName();
    }

    public void setClassType(String type) {
    }

    public Object clone() {
        BaseProperty property = (BaseProperty) super.clone();
        property.setObject(getObject());
        return property;
    }

    public Object getValue() {
        return null;
    }

    public void setValue(Object value) {
    }

    public Element toXML() {
        Element el = new DOMElement(getName());
        el.setText(getValue().toString());
        return el;
    }

}
