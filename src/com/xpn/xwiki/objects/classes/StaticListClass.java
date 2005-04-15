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

 * Created by
 * User: Ludovic Dubost
 * Date: 1 févr. 2004
 * Time: 21:51:06
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

import java.util.List;

public class StaticListClass extends ListClass {

    public StaticListClass(PropertyMetaClass wclass) {
        super("staticlist", "Static List", wclass);
    }

    public StaticListClass() {
        this(null);
    }

    public String getValues() {
        return getStringValue("values");
    }

    public void setValues(String values) {
        setStringValue("values", values);
    }

    public List getList(XWikiContext context) {
        String values = (String) getValues();
        return getListFromString(values);
    }
}
