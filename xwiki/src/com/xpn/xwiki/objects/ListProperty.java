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

 * Created by
 * User: Ludovic Dubost
 * Date: 1 févr. 2004
 * Time: 21:40:32
 */
package com.xpn.xwiki.objects;

import net.sf.hibernate.collection.PersistentCollection;
import org.apache.commons.lang.StringUtils;
import org.apache.ecs.filter.CharacterFilter;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ListProperty extends BaseProperty {
    protected List list = new ArrayList();

    public Object getValue() {
        return getList();
    }

    public void setValue(Object value) {
        this.setList((List)value);
    }

    public String getTextValue() {
        return toFormString();
    }

    public String toText() {
         if ((getList() instanceof PersistentCollection)
            &&(!((PersistentCollection)getList()).wasInitialized()))
           return "";
        else
           return StringUtils.join(getList().toArray(), " ");
    }

    public String toSingleFormString() {
        return super.toFormString();
    }

    public String toFormString() {
        CharacterFilter filter = new CharacterFilter();
        filter.addAttribute("|", "\\|");

        List list = getList();
        Iterator it = list.iterator();
        if (!it.hasNext()) {
             return "";
        }
        StringBuffer result = new StringBuffer();
        result.append(it.next());
        while (it.hasNext()) {
            result.append("|");
            result.append(filter.process((String)it.next()));
        }
        return result.toString();
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj))
         return false;

        List list1 = (List) getValue();
        List list2 = (List) ((BaseProperty)obj).getValue();

        // If the collection was not yet initialized by Hibernate
        // Let's use the super result..
        if ((list1 instanceof PersistentCollection)
            &&(!((PersistentCollection)list1).wasInitialized()))
            return true;

        if ((list2 instanceof PersistentCollection)
                    &&(!((PersistentCollection)list2).wasInitialized()))
            return true;

        if (list1.size()!=list2.size())
         return false;

        for (int i=0;i<list1.size();i++) {
            Object obj1 = list1.get(i);
            Object obj2 = list2.get(i);

            if (!obj1.equals(obj2))
                return false;
        }
        return true;
    }


    public Object clone() {
        ListProperty property = (ListProperty) super.clone();
        List list = new ArrayList();
        for (Iterator it=getList().iterator();it.hasNext();) {
                    list.add(it.next());
                }
        property.setValue(list);
        return property;
    }

    public List getList() {
        return list;
    }

    public void setList(List list) {
        if (list==null)
         this.list = new ArrayList();
        else
         this.list = list;
    }

    public Element toXML() {
        Element el = new DOMElement(getName());
        List list = (List)getValue();
        for (int i=0;i<list.size();i++) {
            String value = list.get(i).toString();
            Element vel = new DOMElement("value");
            vel.setText((value==null) ? "" : value.toString());
            el.add(vel);
        }
        return el;
    }

    public String toString() {
        if ((getList() instanceof PersistentCollection)
               &&(!((PersistentCollection)getList()).wasInitialized()))
              return "";
        return toXMLString();
    }
}
