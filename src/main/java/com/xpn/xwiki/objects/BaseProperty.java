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

import com.xpn.xwiki.web.Utils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.dom.DOMDocument;
import org.dom4j.dom.DOMElement;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

// TODO: shouldn't this be abstract? toFormString and toText
// will never work unless getValue is overriden
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
        Object value = getValue();
        return (value==null) ? "" : value.toString();
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

    public Object getCustomMappingValue() {
        return getValue();
    }
}