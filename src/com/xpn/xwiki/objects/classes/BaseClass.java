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
 * Time: 11:51:16
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.PropertyInterface;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class BaseClass extends BaseCollection implements ClassInterface {

    // This insures natural ordering between properties
    public void addField(String name, PropertyInterface element) {
        Set properties = getPropertyList();
        if (!properties.contains(name)) {
            if (((BaseCollection)element).getNumber()==0)
                ((BaseCollection)element).setNumber(properties.size()+1);
        }
        super.addField(name, element);
    }

    public PropertyInterface get(String name) {
        return safeget(name);
    }

    public void put(String name, PropertyInterface property) {
        safeput(name, property);
    }

    public BaseProperty fromString(String value) {
        return null;  //To change body of implemented methods use Options | File Templates.
    }

    public BaseCollection newObject() {
        BaseObject bobj = new BaseObject();
        bobj.setClassName(getName());
        return bobj;
    }

    public BaseCollection fromMap(Map map) {
        BaseCollection object = newObject();
        return fromMap(map, object);
    }

    public BaseCollection fromMap(Map map, BaseCollection object) {
        object.setClassName(getName());
        Iterator classit = getFieldList().iterator();
        while (classit.hasNext()) {
            PropertyClass property = (PropertyClass) classit.next();
            String name = property.getName();
            Object formvalues = map.get(name);
            if (formvalues!=null) {
                BaseProperty objprop;
                if (formvalues instanceof String[]) {
                    objprop = property.fromStringArray(((String[])formvalues));
                } else {
                    objprop = property.fromString(formvalues.toString());
                }
                if (objprop!=null) {
                    objprop.setObject(object);
                    object.safeput(name, objprop);
                }
            }
        }
        return object;
    }

    public Object clone() {
        BaseClass bclass = (BaseClass) super.clone();
        return bclass;
    }

    public void merge(BaseClass bclass) {
    }

    public Element toXML(BaseClass bclass) {
        return toXML();
    }

    public Element toXML() {
        Element cel = new DOMElement("class");

        Element el = new DOMElement("name");
        el.addText((getName()==null) ? "" : getName());
        cel.add(el);

        Iterator it = getFieldList().iterator();
        while (it.hasNext()) {
            PropertyClass bprop = (PropertyClass)it.next();
            cel.add(bprop.toXML());
        }
        return cel;
    }

    public void fromXML(Element cel) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        setName(cel.element("name").getText());
        List list = cel.elements();
        for (int i=1;i<list.size();i++) {
            Element pcel = (Element) list.get(i);
            String name = pcel.getName();
            String classType = pcel.element("classType").getText();
            PropertyClass property = (PropertyClass) Class.forName(classType).newInstance();
            property.setName(name);
            property.setObject(this);
            property.fromXML(pcel);
            safeput(name, property);
        }
    }

    public boolean addTextField(String fieldName, String fieldPrettyName, int size) {
        if (get(fieldName)==null) {
            StringClass text_class = new StringClass();
            text_class.setName(fieldName);
            text_class.setPrettyName(fieldPrettyName);
            text_class.setSize(size);
            text_class.setObject(this);
            put(fieldName, text_class);
            return true;
        }
        return false;
    }

    public boolean addPasswordField(String fieldName, String fieldPrettyName, int size) {
        if (get(fieldName)==null) {
            PasswordClass text_class = new PasswordClass();
            text_class.setName(fieldName);
            text_class.setPrettyName(fieldPrettyName);
            text_class.setSize(size);
            text_class.setObject(this);
            put(fieldName, text_class);
            return true;
        }
        return false;
    }

    public boolean addBooleanField(String fieldName, String fieldPrettyName, String displayType) {
        if (get(fieldName)==null) {
            BooleanClass boolean_class = new BooleanClass();
            boolean_class.setName(fieldName);
            boolean_class.setPrettyName(fieldPrettyName);
            boolean_class.setDisplayType(displayType);
            boolean_class.setObject(this);
            put(fieldName, boolean_class);
            return true;
        }
        return false;
    }

    public boolean addTemplateField(String fieldName, String fieldPrettyName) {
        return addTextAreaField(fieldName, fieldPrettyName, 80, 15);
    }
    public boolean addTextAreaField(String fieldName, String fieldPrettyName, int cols, int rows) {
        if (get(fieldName)==null) {
            TextAreaClass template_class = new TextAreaClass();
            template_class.setName(fieldName);
            template_class.setPrettyName(fieldPrettyName);
            template_class.setSize(cols);
            template_class.setRows(rows);
            template_class.setObject(this);
            put(fieldName, template_class);
            return true;
        }
        return false;
    }

    public boolean addStaticListField(String fieldName, String fieldPrettyName, String values) {
        return addStaticListField(fieldName, fieldPrettyName, 1, false, values);
    }

    public boolean addStaticListField(String fieldName, String fieldPrettyName, int size, boolean multiSelect, String values) {
        if (get(fieldName)==null) {
            StaticListClass list_class = new StaticListClass();
            list_class.setName(fieldName);
            list_class.setPrettyName(fieldPrettyName);
            list_class.setSize(size);
            list_class.setMultiSelect(multiSelect);
            list_class.setValues(values);
            list_class.setObject(this);
            put(fieldName, list_class);
            return true;
        }
        return false;
    }

    public boolean addNumberField(String fieldName, String fieldPrettyName, int size, String type) {
        if (get(fieldName)==null) {
            NumberClass number_class = new NumberClass();
            number_class.setName(fieldName);
            number_class.setPrettyName(fieldPrettyName);
            number_class.setSize(size);
            number_class.setNumberType(type);
            number_class.setObject(this);
            put(fieldName, number_class);
            return true;
        }
        return false;
    }

    public boolean addDateField(String fieldName, String fieldPrettyName) {
        return addDateField(fieldName, fieldPrettyName, null);
    }

    public boolean addDateField(String fieldName, String fieldPrettyName, String dformat) {
        if (get(fieldName)==null) {
            DateClass date_class = new DateClass();
            date_class.setName(fieldName);
            date_class.setPrettyName(fieldPrettyName);
            if (dformat!=null)
                date_class.setDateFormat(dformat);
            date_class.setObject(this);
            put(fieldName, date_class);
            return true;
        }
        return false;
    }

}
