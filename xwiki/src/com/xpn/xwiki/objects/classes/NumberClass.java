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
 * Time: 13:58:38

 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import org.apache.ecs.xhtml.input;

public class NumberClass  extends PropertyClass {

    public NumberClass(PropertyMetaClass wclass) {
        super("number", "Number", wclass);
        setSize(30);
        setNumberType("long");
    }

    public NumberClass() {
        this(null);
    }

    public int getSize() {
        return getIntValue("size");
    }

    public void setSize(int size) {
        setIntValue("size", size);
    }

    public String getNumberType() {
        return getStringValue("numberType");
    }

    public void setNumberType(String ntype) {
        setStringValue("numberType", ntype);
    }


    public BaseProperty fromString(String value) {
        NumberProperty property;
        String ntype = getNumberType();
        Number nvalue = null;
        if (ntype.equals("integer")) {
            property = new IntegerProperty();
            if ((value!=null)&&(!value.equals("")))
                nvalue = new Integer(value);
        } else if (ntype.equals("float")) {
            property = new FloatProperty();
            if ((value!=null)&&(!value.equals("")))
                nvalue = new Float(value);
        } else if (ntype.equals("double")) {
            property = new DoubleProperty();
            if ((value!=null)&&(!value.equals("")))
                nvalue = new Double(value);
        } else {
            property = new LongProperty();
            if ((value!=null)&&(!value.equals("")))
                nvalue = new Long(value);
        }
        property.setName(getName());
        property.setValue(nvalue);
        // property.setPropertyClass(this);
        return property;
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        input input = new input();

        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop!=null) input.setValue(prop.toFormString());

        input.setType("text");
        input.setName(prefix + name);
        input.setSize(getSize());
        buffer.append(input.toString());
    }

}
