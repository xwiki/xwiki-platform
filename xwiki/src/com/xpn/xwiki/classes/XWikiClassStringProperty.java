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
 * Time: 13:47:20
 */
package com.xpn.xwiki.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

public class XWikiClassStringProperty extends XWikiClassProperty {

    public XWikiClassStringProperty(XWikiClass wclass) {
        setxWikiClass(wclass);
        setType("stringclass");
        setSize(30);
    }

    public XWikiClassStringProperty() {
        setType("stringclass");
        setSize(30);
    }

    public XWikiClass getxWikiClass() {
        XWikiMetaClass wclass = (XWikiMetaClass)super.getxWikiClass();
        if (wclass==null) {
          wclass = new XWikiMetaClass();

        XWikiClassNumberProperty size_class = new XWikiClassNumberProperty(wclass);
        size_class.setSize(5);
        size_class.setNumberType("integer");
        wclass.put("size", size_class);
        setxWikiClass(wclass);
        }
        return wclass;
    }


    public int getSize() {
        try {
            return ((XWikiObjectNumberProperty)get("size")).getValue().intValue();
        } catch (Exception e) {
            return 30;
        }
    }

    public void setSize(int size) {
        XWikiObjectNumberProperty property = new XWikiObjectNumberProperty();
        property.setValue(new Integer(size));
        try {
            put("size", property);
        } catch (XWikiException e) {
            // This should not happen
        }
    }

    public XWikiObjectProperty fromString(String value) {
        XWikiObjectStringProperty property = new XWikiObjectStringProperty();
        property.setValue(value);
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
