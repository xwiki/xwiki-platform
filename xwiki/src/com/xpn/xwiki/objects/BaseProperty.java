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
 * Time: 11:36:16
 */
package com.xpn.xwiki.objects;

import com.xpn.xwiki.test.Utils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

public class BaseProperty extends BaseElement implements PropertyInterface, Serializable {
    private BaseCollection object;
    private int id;

    public BaseCollection getObject() {
        return object;
    }

    public void setObject(BaseCollection object) {
        this.object = object;
    }

    public boolean equals(Object el) {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        if ((object==null)||((BaseProperty)el).getObject()==null) {
            return (hashCode()==el.hashCode());
        }

        if (!super.equals(el))
            return false;

        return (getId()==((BaseProperty)el).getId());
    }

    public int getId() {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        if (object==null)
          return id;
        else
         return getObject().getId();
    }

    public void setId(int id) {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
        this.id = id;
    }

    public int hashCode() {
        // I hate this.. needed for hibernate to find the object
        // when loading the collections..
       return ("" + getId() + getName()).hashCode();
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
        Object value = getValue();
        el.setText( (value==null) ? "" : value.toString());
        return el;
    }

    public String toFormString() {
        return Utils.formEncode(toText());
    }

    public String toText() {
        return getValue().toString();
    }

    public String toXMLString() {
        Document doc = new DOMDocument();
        doc.setRootElement(toXML());
        OutputFormat outputFormat = new OutputFormat("", true);
        StringWriter out = new StringWriter();
        XMLWriter writer = new XMLWriter( out, outputFormat );
        try {
            writer.write(doc);
            return out.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public String toString() {
        return toXMLString();
    }

}