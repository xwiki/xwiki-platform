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
 * Date: 28 déc. 2003
 * Time: 21:24:35
 */
package com.xpn.xwiki.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class ObjectAddForm extends XWikiForm {

    private String className;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void readRequest() {
        setClassName(getRequest().getParameter("classname"));
    }

    public Map getObject(String prefix) {
        Map map = getRequest().getParameterMap();
        HashMap map2 = new HashMap();
        Iterator it = map.keySet().iterator();
        while (it.hasNext()) {
            String name = (String) it.next();
            if (name.startsWith(prefix + "_")) {
                String newname = name.substring(prefix.length()+1);
                map2.put(newname, map.get(name));
            }
        }
        return map2;
    }
}
