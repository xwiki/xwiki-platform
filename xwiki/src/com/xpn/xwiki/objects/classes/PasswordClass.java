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
 * Time: 10:27:20
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ElementInterface;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import org.apache.ecs.xhtml.input;

public class PasswordClass extends StringClass {
    public PasswordClass(PropertyMetaClass wclass) {
        super("password", "Password", wclass);
        setxWikiClass(wclass);
    }

    public PasswordClass() {
        this(null);
    }

    public BaseProperty fromString(String value) {
        if (value.equals("********"))
         return null;
        else
         return super.fromString(value);
    }

    public void displayHidden(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        // Passwords cannot go through the preview interface of we don't do something here..
    }

    public void displaySearch(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        super.displaySearch(buffer, name, prefix, object, context);
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        ElementInterface prop = object.safeget(name);
        if (prop!=null)
         buffer.append("********");
    }

   public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        input input = new input();
        ElementInterface prop = object.safeget(name);
        if (prop!=null) input.setValue("********");

        input.setType("text");
        input.setName(prefix + name);
        input.setSize(getSize());
        buffer.append(input.toString());
    }
}