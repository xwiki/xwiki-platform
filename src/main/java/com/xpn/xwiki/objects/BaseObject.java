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
 * Time: 11:36:06
 */
package com.xpn.xwiki.objects;


import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;
import org.dom4j.dom.DOMElement;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;

public class BaseObject extends BaseCollection implements ObjectInterface, Serializable {

    public int hashCode() {
        String str = getName()+getClassName();
        int nb = getNumber();
        if (nb>0)
            str += "_" + nb;
        return str.hashCode();
    }

    public void setId(int id) {
    }

    public void displayHidden(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        ((PropertyClass)getxWikiClass(context).get(name)).displayHidden(buffer, name, prefix, this, context);
    }

    public void displaySearch(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        ((PropertyClass)getxWikiClass(context).get(name)).displaySearch(buffer, name, prefix, this, context);
    }

    public void displayView(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        ((PropertyClass)getxWikiClass(context).get(name)).displayView(buffer, name, prefix, this, context);
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, XWikiContext context) {
        ((PropertyClass)getxWikiClass(context).get(name)).displayEdit(buffer, name, prefix, this, context);
    }

    public String displayHidden(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass)getxWikiClass(context).get(name)).displayHidden(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displaySearch(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass)getxWikiClass(context).get(name)).displaySearch(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displayView(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass)getxWikiClass(context).get(name)).displayView(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displayEdit(String name, String prefix, XWikiContext context) {
        StringBuffer buffer = new StringBuffer();
        ((PropertyClass)getxWikiClass(context).get(name)).displayEdit(buffer, name, prefix, this, context);
        return buffer.toString();
    }

    public String displayHidden(String name, XWikiContext context) {
        return displayHidden(name, "", context);
    }

    public String displaySearch(String name, XWikiContext context) {
        return displaySearch(name, "", context);
    }

    public String displayView(String name, XWikiContext context) {
        return displayView(name, "", context);
    }

    public String displayEdit(String name, XWikiContext context) {
        return displayEdit(name, "", context);
    }

    public Object clone() {
        BaseObject object = (BaseObject) super.clone();
        return object;
    }

    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;

        if (getNumber()!=((BaseObject)obj).getNumber())
            return false;

        return true;
    }

    public Element toXML(BaseClass bclass) {
        Element oel = new DOMElement("object");

        // Add Class
        if (bclass!=null) {
        Collection fields = bclass.getFieldList();
        if (fields.size()>0) {
            oel.add(bclass.toXML());
          }
        }

        Element el = new DOMElement("name");
        el.addText(getName());
        oel.add(el);

        el = new DOMElement("number");
        el.addText(getNumber() + "");
        oel.add(el);

        el = new DOMElement("className");
        el.addText(getClassName());
        oel.add(el);

        Iterator it = getFieldList().iterator();
        while (it.hasNext()) {
            Element pel = new DOMElement("property");
            PropertyInterface bprop = (PropertyInterface)it.next();
            pel.add(bprop.toXML());
            oel.add(pel);
        }
        return oel;
    }

    public void fromXML(Element oel) throws XWikiException {
        Element cel = oel.element("class");
        BaseClass bclass = new BaseClass();
        if (cel!=null) {
            bclass.fromXML(cel);
            setClassName(bclass.getName());
        }

        setName(oel.element("name").getText());
        List list = oel.elements("property");
        for (int i=0;i<list.size();i++) {
            Element pcel = (Element)((Element) list.get(i)).elements().get(0);
            String name = pcel.getName();
            PropertyClass pclass = (PropertyClass) bclass.get(name);
            if (pclass!=null) {
                BaseProperty property = pclass.newPropertyfromXML(pcel);
                property.setName(name);
                property.setObject(this);
                safeput(name, property);
            }
        }
    }

}
