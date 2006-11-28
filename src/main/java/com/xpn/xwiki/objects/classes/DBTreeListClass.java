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
 * @author ludovic
 */
package com.xpn.xwiki.objects.classes;

import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.XWikiContext;

import java.util.*;

import org.apache.velocity.VelocityContext;
import org.apache.ecs.xhtml.select;
import org.apache.ecs.xhtml.option;
import org.apache.commons.lang.StringUtils;

public class DBTreeListClass extends DBListClass {

    public DBTreeListClass(PropertyMetaClass wclass) {
        super("dbtreelist", "DB Tree List", wclass);
    }

    public DBTreeListClass() {
        this(null);
    }

    public String getParentField() {
        return getStringValue("parentField");
    }

    public void setParentField(String parentField) {
        setStringValue("parentField", parentField);
    }

    public Map getTreeMap(XWikiContext context) {
        List list = getDBList(context);
        Map map = new HashMap();
        if ((list==null)||(list.size()==0))
         return map;
        for(int i=0;i<list.size();i++) {
            Object result = list.get(i);
            if (result instanceof String) {
                ListItem item = new ListItem((String)result);
                map.put(result, item);
            }
            else {
                ListItem item = (ListItem) result;
                addToList(map, item.getParent(), item);
            }
        }
        return map;
    }

    /**
     * Gets an ordered list of items in the tree
     * This is necessary to make sure childs are coming after their parents
     * @param treemap
     * @return  list of ListItems
     */
    protected List getTreeList(Map treemap) {
        List list = new ArrayList();
        addToTreeList(list, treemap, "");
        return list;
    }

    protected void addToTreeList(List treelist, Map treemap, String parent) {
        List list = (List)treemap.get(parent);
        if (list!=null) {
            for (int i=0;i<list.size();i++) {
                ListItem item = (ListItem) list.get(i);
                treelist.add(item);
                addToTreeList(treelist, treemap, item.getId());
            }
        }
    }

    protected void addToList(Map map, String key, ListItem item) {
        List list = (List)map.get(key);
        if (list==null) {
            list = new ArrayList();
            map.put(key, list);
        }
        list.add(item);
    }

    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        List selectlist;
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop == null) {
            selectlist = new ArrayList();
        } else if ((prop instanceof ListProperty) || (prop instanceof DBStringListProperty)) {
            selectlist = (List) prop.getValue();
        } else {
            selectlist = new ArrayList();
            selectlist.add(prop.getValue());
        }
        String result = displayTree(name, prefix, selectlist, "view", context);
        if (result.equals(""))
            super.displayView(buffer, name, prefix, object, context);
        else
            buffer.append(result);
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
        List selectlist;
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop == null) {
            selectlist = new ArrayList();
        } else if ((prop instanceof ListProperty) || (prop instanceof DBStringListProperty)) {
            selectlist = (List) prop.getValue();
        } else {
            selectlist = new ArrayList();
            selectlist.add(prop.getValue());
        }

        if (isPicker()) {
            String result = displayTree(name, prefix, selectlist, "edit", context);
            if (result.equals(""))
                displayTreeSelectEdit(buffer, name, prefix, object, context);
            else {
                displayHidden(buffer, name, prefix, object, context);
                buffer.append(result);
            }
        } else {
            displayTreeSelectEdit(buffer, name, prefix, object, context);
        }
    }

    private String displayTree(String name, String prefix, List selectlist, String mode, XWikiContext context){
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            Map map = getTreeMap(context);
            vcontext.put("selectlist", selectlist);
            vcontext.put("fieldname", prefix + name);
            vcontext.put("tree", map);
            vcontext.put("treelist", getTreeList(map));
            vcontext.put("mode", mode);
            return context.getWiki().parseTemplate("treeview.vm", context);
    }

    protected void addToSelect(select select, List selectlist, Map map, Map treemap, String parent, String level, XWikiContext context) {
        List list = (List)treemap.get(parent);
        if (list!=null) {
            for (int i=0;i<list.size();i++) {
                ListItem item = (ListItem) list.get(i);
                String display = level + getDisplayValue(item.getId(), map, context);
                option option = new option(display, item.getId());
                option.addElement(display);
                if (selectlist.contains(item.getId()))
                    option.setSelected(true);
                select.addElement(option);
                addToSelect(select, selectlist, map, treemap, item.getId(), level + "&nbsp;", context);
            }
        }
    }

    protected void displayTreeSelectEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context) {
		select select = new select(prefix + name, 1);
		select.setMultiple(isMultiSelect());
		select.setSize(getSize());
		select.setName(prefix + name);
		select.setID(prefix + name);

        Map map = getMap(context);
        Map treemap = getTreeMap(context);
		List selectlist;

		BaseProperty prop = (BaseProperty) object.safeget(name);
		if (prop == null) {
			selectlist = new ArrayList();
		} else if ((prop instanceof ListProperty) || (prop instanceof DBStringListProperty)) {
			selectlist = (List) prop.getValue();
		} else {
			selectlist = new ArrayList();
			selectlist.add(prop.getValue());
		}

		// Add options from Set
        addToSelect(select, selectlist, map, treemap, "", "", context);
		buffer.append(select.toString());
	}

    public String getQuery(XWikiContext context) {
        String sql = getSql();
        if ((sql==null)||(sql.trim().equals(""))) {
            String classname = getClassname();
            String idField = getIdField();
            String valueField = getValueField();
            String parentField = getParentField();
            if ((valueField==null)||(valueField.trim().equals("")))
             valueField =  idField;
            if (context.getWiki().getHibernateStore()!=null) {
             String select = "select ";
             String tables = " from XWikiDocument as doc, BaseObject as obj";
             String where = " where doc.fullName=obj.name and obj.className='" + classname + "'";
                if (idField.startsWith("doc.")||idField.startsWith("obj."))
                    select += idField + ",";
                else {
                    select += "idprop.value,";
                    tables += ", StringProperty as idprop";
                    where += " and obj.id=idprop.id.id and idprop.id.name='" + idField + "'";
                }
                if (valueField.startsWith("doc.")||valueField.startsWith("obj."))
                    select += valueField + ",";
                else {
                    if (idField.equals(valueField)) {
                        select += "idprop.value,";
                    } else {
                        select += "valueprop.value,";
                        tables += ", StringProperty as valueprop";
                        where += " and obj.id=valueprop.id.id and valueprop.id.name='" + valueField + "'";
                    }
                }

                if (parentField.startsWith("doc.")||parentField.startsWith("obj."))
                    select += parentField + ",";
                else {
                    if (idField.equals(parentField)) {
                        select += "idprop.value,";
                    } else if (valueField.equals(parentField)) {
                        select += "valueprop.value,";
                    } else {
                        select += "parentprop.value,";
                        tables += ", StringProperty as parentprop";
                        where += " and obj.id=parentprop.id.id and parentprop.id.name='" + parentField + "'";
                    }
                }
                // Let's create the sql
                sql = select +  tables + where;
            } else {
                // TODO: query plugin impl.
                // We need to generate the right query for the query plugin
            }

        }
        return sql;
    }
}
