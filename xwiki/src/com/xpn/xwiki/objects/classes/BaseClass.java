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

import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseCollection;

import java.util.Map;
import java.util.Iterator;

import org.apache.ecs.xhtml.object;


public class BaseClass extends BaseCollection implements ClassInterface {
    public ElementInterface get(String name) {
        return safeget(name);
    }

    public void put(String name, ElementInterface property) {
        safeput(name, property);
    }

    public String toString() {
        return null;  //To change body of implemented methods use Options | File Templates.
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
        Iterator classit = getFields().values().iterator();
        while (classit.hasNext()) {
            PropertyClass property = (PropertyClass) classit.next();
            String name = property.getName();
            Object formvalues = map.get(name);
            if ((formvalues!=null)&&(formvalues instanceof String[])) {
             BaseProperty objprop = property.fromString(((String[])formvalues)[0]);
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

}
