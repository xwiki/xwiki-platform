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
 * Date: 24 janv. 2004
 * Time: 10:29:38
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import org.apache.ecs.html.TextArea;

public class TextAreaClass extends StringClass {
    public TextAreaClass(PropertyMetaClass wclass) {
        this();
        setxWikiClass(wclass);
    }

    public TextAreaClass() {
        setName("textarea");
        setPrettyName("Text Area");
        setSize(40);
        setRows(5);
    }

    public int getRows() {
        return getIntValue("rows");
    }

    public void setRows(int rows) {
        setIntValue("rows", rows);
    }

    public BaseProperty fromString(String value) {
        return super.fromString(value);
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        TextArea textarea = new TextArea();
        ElementInterface prop = object.safeget(name);
        if (prop!=null) textarea.addElement(formEncode(prop.toString()));

        textarea.setName(prefix + name);
        textarea.setCols(getSize());
        textarea.setRows(getRows());
        buffer.append(textarea.toString());
    }
}
