/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.DBStringListProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;

/**
 * @version $Id: $
 */
public class DBTreeListClass extends DBListClass
{
    private static final Log LOG = LogFactory.getLog(DBTreeListClass.class);

    public DBTreeListClass(PropertyMetaClass wclass)
    {
        super("dbtreelist", "DB Tree List", wclass);
    }

    public DBTreeListClass()
    {
        this(null);
    }

    public String getParentField()
    {
        return getStringValue("parentField");
    }

    public void setParentField(String parentField)
    {
        setStringValue("parentField", parentField);
    }

    public Map getTreeMap(XWikiContext context)
    {
        List list = getDBList(context);
        Map map = new HashMap();
        if ((list == null) || (list.size() == 0)) {
            return map;
        }
        for (int i = 0; i < list.size(); i++) {
            Object result = list.get(i);
            if (result instanceof String) {
                ListItem item = new ListItem((String) result);
                map.put(result, item);
            } else {
                ListItem item = (ListItem) result;
                addToList(map, item.getParent(), item);
            }
        }
        return map;
    }

    /**
     * Gets an ordered list of items in the tree. This is necessary to make sure childs are coming
     * after their parents
     * 
     * @param treemap
     * @return list of ListItems
     */
    protected List getTreeList(Map treemap)
    {
        List list = new ArrayList();
        addToTreeList(list, treemap, "");
        return list;
    }

    protected void addToTreeList(List treelist, Map treemap, String parent)
    {
        List list = (List) treemap.get(parent);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ListItem item = (ListItem) list.get(i);
                treelist.add(item);
                addToTreeList(treelist, treemap, item.getId());
            }
        }
    }

    protected void addToList(Map map, String key, ListItem item)
    {
        List list = (List) map.get(key);
        if (list == null) {
            list = new ArrayList();
            map.put(key, list);
        }
        list.add(item);
    }

    public void displayView(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
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
        String result = displayFlatView(selectlist, context);
        if (result.equals("")) {
            super.displayView(buffer, name, prefix, object, context);
        } else {
            buffer.append(result);
        }
    }

    public void displayEdit(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
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

    private String displayFlatView(List selectlist, XWikiContext context)
    {
        Map map = getTreeMap(context);
        List fullTreeList = getTreeList(map);
        List resList = new ArrayList(selectlist.size());

        Iterator it = selectlist.iterator();
        while (it.hasNext()) {
            String item = (String) it.next();
            List itemPath = getItemPath(item, fullTreeList, new ArrayList());
            mergeItems(itemPath, resList);
        }

        return renderItemsList(resList);
    }

    protected String renderItemsList(List resList)
    {
        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < resList.size(); i++) {
            List items = (List) resList.get(i);
            for (int j = 0; j < items.size(); j++) {
                ListItem item = (ListItem) items.get(j);
                buff.append(item.getValue());
                if (j < items.size() - 1) {
                    buff.append(" &gt; ");
                }
            }
            if (i < resList.size() - 1) {
                buff.append("<br />");
            }
        }
        return buff.toString();
    }

    private void mergeItems(List itemPath, List resList)
    {
        if (itemPath.size() == 0) {
            return;
        }

        for (int i = 0; i < resList.size(); i++) {
            List items = (List) resList.get(i);
            if (items.size() < itemPath.size()) {
                ListItem item1 = (ListItem) items.get(items.size() - 1);
                ListItem item2 = (ListItem) itemPath.get(items.size() - 1);
                if (item1.equals(item2)) {
                    resList.set(i, itemPath);
                    return;
                }
            } else {
                ListItem item1 = (ListItem) items.get(itemPath.size() - 1);
                ListItem item2 = (ListItem) itemPath.get(itemPath.size() - 1);
                if (item1.equals(item2)) {
                    return;
                }
            }
        }
        resList.add(itemPath);
    }

    private List getItemPath(String item, List treeList, ArrayList resList)
    {
        Iterator it = treeList.iterator();
        while (it.hasNext()) {
            ListItem tmpItem = (ListItem) it.next();
            if (item.equals(tmpItem.getId())) {
                if (tmpItem.getParent().length() > 0) {
                    getItemPath(tmpItem.getParent(), treeList, resList);
                }
                resList.add(tmpItem);
                return resList;
            }
        }
        return null;
    }

    private String displayTree(String name, String prefix, List selectlist, String mode,
        XWikiContext context)
    {
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        Map map = getTreeMap(context);
        vcontext.put("selectlist", selectlist);
        vcontext.put("fieldname", prefix + name);
        vcontext.put("tree", map);
        vcontext.put("treelist", getTreeList(map));
        vcontext.put("mode", mode);
        return context.getWiki().parseTemplate("treeview.vm", context);
    }

    protected void addToSelect(select select, List selectlist, Map map, Map treemap,
        String parent, String level, XWikiContext context)
    {
        List list = (List) treemap.get(parent);
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                ListItem item = (ListItem) list.get(i);
                String display = level + getDisplayValue(item.getId(), "", map, context);
                option option = new option(display, item.getId());
                option.addElement(display);
                if (selectlist.contains(item.getId())) {
                    option.setSelected(true);
                }
                select.addElement(option);
                addToSelect(select, selectlist, map, treemap, item.getId(), level + "&nbsp;",
                    context);
            }
        }
    }

    protected void displayTreeSelectEdit(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
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

    /**
     * <p>
     * Computes the query corresponding to the current XProperty. The query is either manually
     * specified by the XClass creator in the <tt>sql</tt> field, or, if the query field is blank,
     * constructed using the <tt>classname</tt>, <tt>idField</tt>, <tt>valueField</tt> and
     * <tt>parentField</tt> properties. The query is constructed according to the following rules:
     * </p>
     * <ul>
     * <li>If no classname, id and value fields are selected, return a query that return no rows,
     * as the parent is not enough to make a query.</li>
     * <li>If no parent field is provided, use the document "parent" medatada.</li>
     * <li>If only the classname is provided, select all document names which have an object of
     * that type, preserving the hierarchy defined by the parent field.</li>
     * <li>If only one of id and value is provided, use it for both columns.</li>
     * <li>If no classname is provided, assume the fields are document properties.</li>
     * <li>If the document is not used at all, don't put it in the query.</li>
     * <li>If the object is not used at all, don't put it in the query.</li>
     * </ul>
     * <p>
     * The generated query always selects 3 columns, the first one is used as the stored value, the
     * second one as the displayed value, and the third one defines the "parent" of the current
     * value.
     * </p>
     * 
     * @param context The current {@link XWikiContext context}.
     * @return The HQL query corresponding to this property.
     */
    public String getQuery(XWikiContext context)
    {
        // First, get the hql query entered by the user.
        String sql = getSql();
        // If the query field is blank, construct a query using the classname, idField,
        // valueField and parentField properties.
        if (StringUtils.isBlank(sql)) {
            if (context.getWiki().getHibernateStore() != null) {
                // Extract the 3 properties in non-null variables.
                String classname = StringUtils.defaultString(getClassname());
                String idField = StringUtils.defaultString(getIdField());
                String valueField = StringUtils.defaultString(getValueField());
                String parentField = StringUtils.defaultString(getParentField());

                // Check if the properties are specified or not.
                boolean hasClassname = !StringUtils.isBlank(classname);
                boolean hasIdField = !StringUtils.isBlank(idField);
                boolean hasValueField = !StringUtils.isBlank(valueField);
                boolean hasParentField = !StringUtils.isBlank(parentField);

                if (!(hasIdField || hasValueField)) {
                    // If only the classname is specified, return a query that selects all the
                    // document names which have an object of that type, and the hierarchy is
                    // defined by the document "parent" property (unless a parent property is
                    // specified).
                    if (hasClassname) {
                        sql =
                            "select distinct doc.fullName, doc.fullName, "
                                + (hasParentField ? parentField : "doc.parent")
                                + " from XWikiDocument as doc, BaseObject as obj"
                                + " where doc.fullName=obj.name and obj.className='" + classname
                                + "'";
                    } else {
                        // If none of the first 3 properties is specified, return a query that
                        // always returns no rows (only with the parent field no query can be made)
                        sql = DEFAULT_QUERY;
                    }
                    return sql;
                }

                // If only one of the id and value fields is specified, use it for both columns.
                if (!hasIdField && hasValueField) {
                    idField = valueField;
                } else if (hasIdField && !hasValueField) {
                    valueField = idField;
                }

                // If no parent field was specified, use the document "parent" metadata
                if (!hasParentField) {
                    parentField = "doc.parent";
                }

                // Check if the document and object are needed or not.
                // The object is needed if there is a classname, or if at least one of the selected
                // columns is an object property.
                boolean usesObj =
                    hasClassname || idField.startsWith("obj.") || valueField.startsWith("obj.")
                        || parentField.startsWith("obj.");
                // The document is needed if one of the selected columns is a document property, or
                // if there is no classname specified and at least one of the selected columns is
                // not an object property.
                boolean usesDoc =
                    idField.startsWith("doc.") || valueField.startsWith("doc.")
                        || parentField.startsWith("doc.");
                if ((!idField.startsWith("obj.") || !valueField.startsWith("obj.") || !parentField
                    .startsWith("obj."))
                    && !hasClassname) {
                    usesDoc = true;
                }

                // Build the query in this variable.
                StringBuffer select = new StringBuffer("select distinct ");
                // These will hold the components of the from and where parts of the query.
                ArrayList fromStatements = new ArrayList();
                ArrayList whereStatements = new ArrayList();

                // Add the document to the query only if it is needed.
                if (usesDoc) {
                    fromStatements.add("XWikiDocument as doc");
                    if (usesObj) {
                        whereStatements.add("doc.fullName=obj.name");
                    }
                }
                // Add the object to the query only if it is needed.
                if (usesObj) {
                    fromStatements.add("BaseObject as obj");
                    if (hasClassname) {
                        whereStatements.add("obj.className='" + classname + "'");
                    }
                }

                // Add the first column to the query.
                if (idField.startsWith("doc.") || idField.startsWith("obj.")) {
                    select.append(idField);
                } else if (!hasClassname) {
                    select.append("doc." + idField);
                } else {
                    select.append("idprop.value");
                    fromStatements.add("StringProperty as idprop");
                    whereStatements.add("obj.id=idprop.id.id and idprop.id.name='" + idField
                        + "'");
                }

                // Add the second column to the query.
                if (valueField.startsWith("doc.") || valueField.startsWith("obj.")) {
                    select.append(", ").append(valueField);
                } else if (!hasClassname) {
                    select.append(", doc." + valueField);
                } else {
                    if (valueField.equals(idField)) {
                        select.append(", idprop.value");
                    } else {
                        select.append(", valueprop.value");
                        fromStatements.add("StringProperty as valueprop");
                        whereStatements.add("obj.id=valueprop.id.id and valueprop.id.name='"
                            + valueField + "'");
                    }
                }

                // Add the third column to the query.
                if (parentField.startsWith("doc.") || parentField.startsWith("obj.")) {
                    select.append(", ").append(parentField);
                } else if (!hasClassname) {
                    select.append(", doc." + parentField);
                } else {
                    if (parentField.equals(idField)) {
                        select.append(", idprop.value");
                    } else if (parentField.equals(valueField)) {
                        select.append(", valueprop.value");
                    } else {
                        select.append(", parentprop.value");
                        fromStatements.add("StringProperty as parentprop");
                        whereStatements.add("obj.id=parentprop.id.id and parentprop.id.name='"
                            + parentField + "'");
                    }
                }
                // Let's create the complete query
                select.append(" from ");
                select.append(StringUtils.join(fromStatements.iterator(), ", "));
                if (whereStatements.size() > 0) {
                    select.append(" where ");
                    select.append(StringUtils.join(whereStatements.iterator(), " and "));
                }
                sql = select.toString();
            } else {
                // TODO: query plugin impl.
                // We need to generate the right query for the query plugin
            }
        }
        // Parse the query, so that it can contain velocity scripts, for example to use the
        // current document name, or the current username.
        try {
            sql = context.getWiki().parseContent(sql, context);
        } catch (Exception e) {
            LOG.warn("Failed to parse SQL script [" + sql + "]. Internal error ["
                + e.getMessage() + "]. Continuing with non-rendered script.");
        }
        return sql;
    }
}
