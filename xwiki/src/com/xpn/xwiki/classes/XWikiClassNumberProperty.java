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
package com.xpn.xwiki.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class XWikiClassNumberProperty  extends XWikiClassProperty {

    /*
    public static final int TYPE_INTEGER = 0;
    public static final int TYPE_LONG = 1;
    public static final int TYPE_FLOAT = 2;
    public static final int TYPE_DOUBLE = 3;
    */

    public XWikiClassNumberProperty(XWikiClass wclass) {
        setxWikiClass(wclass);
        setType("numberclass");
        setSize(30);
        setNumberType("long");
    }

    public XWikiClassNumberProperty() {
        setType("numberclass");
        setSize(30);
        setNumberType("long");
    }

    public XWikiClass getxWikiClass() {
        XWikiMetaClass wclass = (XWikiMetaClass)super.getxWikiClass();
        if (wclass==null) {
          wclass = new XWikiMetaClass();

        XWikiClassStringProperty type_class = new XWikiClassStringProperty(wclass);
        type_class.setSize(20);
        XWikiClassNumberProperty size_class = new XWikiClassNumberProperty(wclass);
        size_class.setSize(5);
        size_class.setNumberType("integer");
        wclass.put("number_type", type_class);
        wclass.put("size", size_class);
        setxWikiClass(wclass);
        }
        return wclass;
    }



    public int getSize() {
        try {
            return ((XWikiObjectNumberProperty)get("size")).getValue().intValue();
        } catch (Exception e) {
            // This should not happen
            return 30;
        }
    }

    public void setSize(int size) {
        XWikiObjectNumberProperty property = new XWikiObjectNumberProperty();
        property.setValue(new Integer(size));
        try {
            put("size", property);
        } catch (XWikiException e) {
            // This should never happen because size has been declared in the meta-class
        };
    }

    public String getNumberType() {
        try {
            return ((XWikiObjectStringProperty)get("number_type")).getValue();
        } catch (Exception e) {
            return "long";
        }
    }

    public void setNumberType(String ntype) {
        XWikiObjectStringProperty property = new XWikiObjectStringProperty();
        property.setValue(ntype);
        try {
            put("number_type", property);
        } catch (XWikiException e) {
            // This should never happen because number_type has been declared in the meta-class
        }
    }


    public XWikiObjectProperty fromString(String value) {
        XWikiObjectNumberProperty property = new XWikiObjectNumberProperty();
        String ntype = getNumberType();
        Number nvalue;
        if (ntype.equals("integer")) {
          nvalue = new Integer(value);
        } else if (ntype.equals("float")) {
          nvalue = new Float(value);
        } else if (ntype.equals("double")) {
          nvalue = new Double(value);
        } else {
          nvalue = new Long(value);
        }
        property.setValue(nvalue);
        property.setPropertyClass(this);
        return property;
    }

    public void displayHidden(StringBuffer buffer, String name, XWikiObject object, XWikiContext context) {
        super.displayHidden(buffer, name, object, context);    //To change body of overriden methods use Options | File Templates.
    }

    public void displaySearch(StringBuffer buffer, String name, XWikiObject object, XWikiContext context) {
        super.displaySearch(buffer, name, object, context);    //To change body of overriden methods use Options | File Templates.
    }

    public void displayView(StringBuffer buffer, String name, XWikiObject object, XWikiContext context) {
        super.displayView(buffer, name, object, context);    //To change body of overriden methods use Options | File Templates.
    }

    public void displayEdit(StringBuffer buffer, String name, XWikiObject object, XWikiContext context) {
        super.displayEdit(buffer, name, object, context);    //To change body of overriden methods use Options | File Templates.
    }

}
