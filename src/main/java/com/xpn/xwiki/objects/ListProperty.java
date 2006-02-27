/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author erwan
 * @author sdumitriu
 */

package com.xpn.xwiki.objects;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ecs.filter.CharacterFilter;
import org.dom4j.Element;
import org.dom4j.dom.DOMElement;
import org.hibernate.collection.PersistentCollection;

public class ListProperty extends BaseProperty {
    protected List list = new ArrayList();
    private String formStringSeparator = "|";

    public String getFormStringSeparator() {
        return formStringSeparator;
    }

    public void setFormStringSeparator(String formStringSeparator) {
        this.formStringSeparator = formStringSeparator;
    }

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
        filter.addAttribute(formStringSeparator, "\\" + formStringSeparator);

        List list = getList();
        Iterator it = list.iterator();
        if (!it.hasNext()) {
             return "";
        }
        StringBuffer result = new StringBuffer();
        result.append(it.next());
        while (it.hasNext()) {
            result.append(formStringSeparator);
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

    // This is important.. Otherwise we can get a stackoverflow calling toXML()
    public String toString() {
        if ((getList() instanceof PersistentCollection)
               &&(!((PersistentCollection)getList()).wasInitialized()))
              return "";
        return toXMLString();
    }

}
