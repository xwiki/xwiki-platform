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
 * Time: 11:51:16
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.objects.*;
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
        return new BaseObject();
    }

    public BaseCollection fromMap(Map map) {
        BaseCollection object = newObject();
        return fromMap(map, object);
    }

    public BaseCollection fromMap(Map map, BaseCollection object) {
        object.setxWikiClass(this);
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

    public Element toXML() {
        Element cel = new DOMElement("class");

        Element el = new DOMElement("name");
        el.addText(getName());
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
}
