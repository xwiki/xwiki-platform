/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * @author sdumitriu
 */

package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.QueryPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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
        	if (xwiki.getHibernateStore()!=null)
        		return xwiki.search(getSql(), context);
        	else
        		return ((QueryPlugin)xwiki.getPlugin("query", context)).xpath(getSql()).list();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList();
        }
    }

    public Map getMap(XWikiContext context) {
        List list = getList(context);
        Map map = new HashMap();
        if ((list==null)||(list.size()==0))
         return map;
        for(int i=0;i<list.size();i++) {
            Object res = list.get(i);
            if (res instanceof String)
             map.put(res, res);
            else {
                String[] res2 = (String[]) res;
                if (res2.length==1)
                    map.put(res2[0], res2[0]);
                else
                    map.put(res2[0], res2[1]);
            }
        }
        return map;
    }

    public String getSql() {
        return getLargeStringValue("sql");
    }

    public void setSql(String sql) {
        setLargeStringValue("sql", sql);
    }
}
