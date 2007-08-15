/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
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
 */

package com.xpn.xwiki.objects.classes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.QueryPlugin;

public class DBListClass extends ListClass
{
    private List cachedDBList;

    public DBListClass(String name, String prettyname, PropertyMetaClass wclass)
    {
        super(name, prettyname, wclass);
    }

    public DBListClass(PropertyMetaClass wclass)
    {
        super("dblist", "DB List", wclass);
    }

    public DBListClass()
    {
        this(null);
    }

    public List makeList(List list)
    {
        List list2 = new ArrayList();
        for (int i = 0; i < list.size(); i++) {
            Object result = list.get(i);
            if (result instanceof String) {
                list2.add(new ListItem((String) result));
            } else {
                Object[] res = (Object[]) result;
                if (res.length == 1) {
                    list2.add(new ListItem(res[0].toString()));
                } else if (res.length == 2) {
                    list2.add(new ListItem(res[0].toString(), res[1].toString()));
                } else {
                    list2.add(new ListItem(res[0].toString(), res[1].toString(), res[2]
                        .toString()));
                }
            }
        }
        return list2;
    }

    public List getDBList(XWikiContext context)
    {
        List list = getCachedDBList();
        if (list==null) {

            XWiki xwiki = context.getWiki();
            String query = getQuery(context);

            if (query == null)
                list = new ArrayList();
            else {

                try {
                    if ((xwiki.getHibernateStore() != null) && (!query.startsWith("/"))) {
                        list = makeList(xwiki.search(query, context));
                    } else  {
                        list = makeList(((QueryPlugin) xwiki.getPlugin("query", context)).xpath(query).list());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    list = new ArrayList();
                }
            }
            setCachedDBList(list);
        }
        return list;
    }

    public List getList(XWikiContext context)
    {
        List dblist = getDBList(context);
        List list = new ArrayList();
        for (int i = 0; i < dblist.size(); i++) {
            list.add(((ListItem) dblist.get(i)).getId());
        }
        return list;
    }

    public Map getMap(XWikiContext context)
    {
        List list = getDBList(context);
        Map map = new HashMap();
        if ((list == null) || (list.size() == 0)) {
            return map;
        }
        for (int i = 0; i < list.size(); i++) {
            Object res = list.get(i);
            if (res instanceof String) {
                map.put(res, res);
            } else {
                ListItem item = (ListItem) res;
                map.put(item.getId(), item);
            }
        }
        return map;
    }

    public String getQuery(XWikiContext context)
    {
        String sql = getSql();
        try {
            sql = context.getDoc().getRenderedContent(sql, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ((sql == null) || (sql.trim().equals(""))) {
            String classname = getClassname();
            String idField = getIdField();
            String valueField = getValueField();
            if ((valueField == null) || (valueField.trim().equals(""))) {
                valueField = idField;
            }
            if (context.getWiki().getHibernateStore() != null) {
                String select = "select ";
                String tables = " from XWikiDocument as doc, BaseObject as obj";
                String where =
                    " where doc.fullName=obj.name and obj.className='" + classname + "'";
                if (idField.startsWith("doc.") || idField.startsWith("obj.")) {
                    select += idField + ",";
                } else {
                    select += "idprop.value,";
                    tables += ", StringProperty as idprop";
                    where += " and obj.id=idprop.id.id and idprop.id.name='" + idField + "'";
                }
                if (valueField.startsWith("doc.") || valueField.startsWith("obj.")) {
                    select += valueField + ",";
                }
                else {
                    if (idField.equals(valueField)) {
                        select += "idprop.value,";
                    } else {
                        select += "valueprop.value,";
                        tables += ", StringProperty as valueprop";
                        where +=
                            " and obj.id=valueprop.id.id and valueprop.id.name='" + valueField
                                + "'";
                    }
                }
                // Let's create the sql
                sql = select + tables + where;
            } else {
                // TODO: query plugin impl.
                // We need to generate the right query for the query plugin
            }

        }
        return context.getWiki().parseContent(sql, context);
    }

    public String getSql()
    {
        return getLargeStringValue("sql");
    }

    public void setSql(String sql)
    {
        setLargeStringValue("sql", sql);
    }

    public String getClassname()
    {
        return getStringValue("classname");
    }

    public void setClassname(String classname)
    {
        setStringValue("classname", classname);
    }

    public String getIdField()
    {
        return getStringValue("idField");
    }

    public void setIdField(String idField)
    {
        setStringValue("idField", idField);
    }

    public String getValueField()
    {
        return getStringValue("valueField");
    }

    public void setValueField(String valueField)
    {
        setStringValue("valueField", valueField);
    }

    public List getCachedDBList() {
        if (isCache())
            return cachedDBList;
        else
            return null;
    }

    public void setCachedDBList(List cachedDBList) {
        if (isCache())
            this.cachedDBList = cachedDBList;
    }

    public void flushCache() {
        this.cachedDBList = null;
    }
}
