/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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
 * Date: 5 févr. 2004
 * Time: 15:58:43
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWiki;

import java.util.List;
import java.util.ArrayList;

public class DBListClass extends ListClass {
    public DBListClass(PropertyMetaClass wclass) {
        super("dblist", "DB List", wclass);
    }

    public DBListClass() {
        this(null);
    }

    public List getList(XWikiContext context) {
        XWiki xwiki = context.getWiki();
        try {
            return xwiki.search(getSql());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList();
        }
    }

    public String getSql() {
        return getLargeStringValue("sql");
    }

    public void setSql(String sql) {
        setLargeStringValue("sql", sql);
    }
}
