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
 * User: ludovic
 * Date: 18 mars 2004
 * Time: 13:40:57
 */

package com.xpn.xwiki.api;

import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseElement;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.XWikiContext;

import java.util.Iterator;

public class Class extends Collection {

    public Class(BaseClass obj, XWikiContext context) {
        super(obj, context);
    }

    protected BaseClass getBaseClass() {
        return (BaseClass) getCollection();
    }

    public Element[] getProperties() {
        java.util.Collection coll = getCollection().getFieldList();
        if (coll==null)
         return null;
        PropertyClass[] properties = new PropertyClass[coll.size()];
        int i=0;
        for (Iterator it = coll.iterator(); it.hasNext();i++) {
            properties[i] = new PropertyClass((com.xpn.xwiki.objects.classes.PropertyClass) it.next(), context);
        }
        return properties;
    }

    public Element get(String name) {
       return new PropertyClass((com.xpn.xwiki.objects.classes.PropertyClass) getCollection().safeget(name), context);
    }

}