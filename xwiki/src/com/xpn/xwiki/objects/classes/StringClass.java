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
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.objects.*;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import org.apache.ecs.html.Input;

public class StringClass extends PropertyClass {

    public StringClass(PropertyMetaClass wclass) {
        setxWikiClass(wclass);
        setName("string");
        setPrettyName("String");
        setSize(30);
    }

    public StringClass() {
        setName("string");
        setPrettyName("String");
        setSize(30);
    }

    public int getSize() {
        return getIntValue("size");
    }

    public void setSize(int size) {
        setIntValue("size", size);
    }

    public BaseProperty fromString(String value) {
        StringProperty property = new StringProperty();
        property.setName(getName());
        property.setValue(value);
        // property.setPropertyClass(this);
        return property;
    }

    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        super.displayHidden(buffer, name, prefix, object, context);
    }

    public void displaySearch(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        super.displaySearch(buffer, name, prefix, object, context);
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        super.displayView(buffer, name, prefix, object, context);
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        Input input = new Input();
        ElementInterface prop = object.safeget(name);
        if (prop!=null) input.setValue(formEncode(prop.toString()));

        input.setType("text");
        input.setName(prefix + name);
        input.setSize(getSize());
        buffer.append(input.toString());
    }
}
