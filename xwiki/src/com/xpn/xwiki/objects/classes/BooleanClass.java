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
 * Time: 21:54:09
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.XWikiContext;
import org.apache.ecs.xhtml.select;
import org.apache.ecs.xhtml.option;

public class BooleanClass extends PropertyClass {

    public BooleanClass(PropertyMetaClass wclass) {
        super("boolean", "Boolean", wclass);
    }

    public BooleanClass() {
        this(null);
    }

    public String getDisplayType() {
        return getStringValue("displayType");
    }

    public void setDisplayType(String type) {
        setStringValue("displayType", type);
    }

    public BaseProperty fromString(String value) {
        NumberProperty property;
        Number nvalue = null;
        property = new IntegerProperty();
        if ((value!=null)&&(!value.equals("")))
                nvalue = new Integer(value);
        property.setName(getName());
        property.setValue(nvalue);
        return property;
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        IntegerProperty prop = (IntegerProperty) object.safeget(name);
        if (prop==null)
            return;

        int value = ((Integer)prop.getValue()).intValue();
        if (value==1)
            buffer.append("True");
        else
            buffer.append("False");
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        select select = new select(prefix + name, 1);
        option[] options = { new option("1" , "True" ), new option("0" , "False")};
        options[0].addElement("True");
        options[1].addElement("False");

        IntegerProperty prop = (IntegerProperty) object.safeget(name);
        if (prop!=null) {
            int value = ((Integer)prop.getValue()).intValue();
            if (value==1)
                options[0].setSelected(true);
            else
                options[1].setSelected(true);
        }
        select.addElement(options);
        buffer.append(select.toString());
    }

}
