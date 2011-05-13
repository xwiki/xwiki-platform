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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.xhtml.input;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.ListProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.QueryPlugin;

public class DBListClass extends ListClass
{
    protected static final String DEFAULT_QUERY = "select doc.name from XWikiDocument doc where 1 = 0";

    private static final Log LOG = LogFactory.getLog(DBListClass.class);

    private List<ListItem> cachedDBList;

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

    public List<ListItem> makeList(List<Object> list)
    {
        List<ListItem> result = new ArrayList<ListItem>();
        for (Object item : list) {
            // Oracle databases treat NULL and empty strings similarly. Thus the list passed
            // as parameter can have some elements being NULL (for XWiki string properties which
            // were empty strings). This means we need to check for NULL and ignore NULL entries
            // from the list.
            if (item != null) {
                if (item instanceof String) {
                    result.add(new ListItem((String) item));
                } else {
                    Object[] res = (Object[]) item;
                    if (res.length == 1) {
                        result.add(new ListItem(res[0].toString()));
                    } else if (res.length == 2) {
                        result.add(new ListItem(res[0].toString(), res[1].toString()));
                    } else {
                        result.add(new ListItem(res[0].toString(), res[1].toString(), res[2].toString()));
                    }
                }
            }
        }
        return result;
    }

    public List<ListItem> getDBList(XWikiContext context)
    {
        List<ListItem> list = getCachedDBList(context);
        if (list == null) {
            XWiki xwiki = context.getWiki();
            String query = getQuery(context);

            if (query == null) {
                list = new ArrayList<ListItem>();
            } else {
                try {
                    if ((xwiki.getHibernateStore() != null) && (!query.startsWith("/"))) {
                        list = makeList(xwiki.search(query, context));
                    } else {
                        list = makeList(((QueryPlugin) xwiki.getPlugin("query", context)).xpath(query).list());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    list = new ArrayList<ListItem>();
                }
            }
            setCachedDBList(list, context);
        }
        return list;
    }

    @Override
    public List<String> getList(XWikiContext context)
    {
        List<ListItem> dblist = getDBList(context);

        String sort = getSort();

        if ("id".equals(sort)) {
            Collections.sort(dblist, ListItem.ID_COMPARATOR);
        } else if ("value".equals(sort)) {
            Collections.sort(dblist, ListItem.VALUE_COMPARATOR);
        }

        List<String> result = new ArrayList<String>(dblist.size());
        for (ListItem value : dblist) {
            result.add(value.getId());
        }
        return result;
    }

    @Override
    public Map<String, ListItem> getMap(XWikiContext context)
    {
        List<ListItem> list = getDBList(context);
        Map<String, ListItem> result = new HashMap<String, ListItem>();
        if ((list == null) || (list.size() == 0)) {
            return result;
        }
        for (int i = 0; i < list.size(); i++) {
            Object res = list.get(i);
            if (res instanceof String) {
                result.put((String) res, new ListItem((String) res));
            } else {
                ListItem item = (ListItem) res;
                result.put(item.getId(), item);
            }
        }
        return result;
    }

    /**
     * <p>
     * Computes the query corresponding to the current XProperty. The query is either manually specified by the XClass
     * creator in the <tt>sql</tt> field, or, if the query field is blank, constructed using the <tt>classname</tt>,
     * <tt>idField</tt> and <tt>valueField</tt> properties. The query is constructed according to the following rules:
     * </p>
     * <ul>
     * <li>If no classname, id or value fields are selected, return a query that return no rows.</li>
     * <li>If only the classname is provided, select all document names which have an object of that type.</li>
     * <li>If only one of id and value is provided, select just one column.</li>
     * <li>If id = value, select just one column.</li>
     * <li>If no classname is provided, assume the fields are document properties.</li>
     * <li>If the document is not used at all, don't put it in the query.</li>
     * <li>If the object is not used at all, don't put it in the query.</li>
     * </ul>
     * <p>
     * If there are two columns selected, use the first one as the stored value and the second one as the displayed
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
        // If the query field is blank, construct a query using the classname, idField and
        // valueField properties.
        if (StringUtils.isBlank(sql)) {
            if (context.getWiki().getHibernateStore() != null) {
                // Extract the 3 properties in non-null variables.
                String classname = StringUtils.defaultString(getClassname());
                String idField = StringUtils.defaultString(getIdField());
                String valueField = StringUtils.defaultString(getValueField());

                // Check if the properties are specified or not.
                boolean hasClassname = !StringUtils.isBlank(classname);
                boolean hasIdField = !StringUtils.isBlank(idField);
                boolean hasValueField = !StringUtils.isBlank(valueField);

                if (!(hasIdField || hasValueField)) {
                    // If only the classname is specified, return a query that selects all the
                    // document names which have an object of that type.
                    if (hasClassname) {
                        sql =
                            "select distinct doc.fullName from XWikiDocument as doc, BaseObject as obj"
                            + " where doc.fullName=obj.name and obj.className='" + classname + "'";
                    } else {
                        // If none of the 3 properties is specified, return a query that always
                        // returns no rows.
                        sql = DEFAULT_QUERY;
                    }
                    return sql;
                }

                // If the value field is specified, but the id isn't, swap them.
                if (!hasIdField && hasValueField) {
                    idField = valueField;
                    valueField = "";
                    hasValueField = false;
                } else if (idField.equals(valueField)) {
                    // If the value field is the same as the id field, ignore it.
                    hasValueField = false;
                }

                // Check if the document and object are needed or not.
                // The object is needed if there is a classname, or if at least one of the selected
                // columns is an object property.
                boolean usesObj = hasClassname || idField.startsWith("obj.") || valueField.startsWith("obj.");
                // The document is needed if one of the selected columns is a document property, or
                // if there is no classname specified and at least one of the selected columns is
                // not an object property.
                boolean usesDoc = idField.startsWith("doc.") || valueField.startsWith("doc.");
                if ((!idField.startsWith("obj.") || (hasValueField && !valueField.startsWith("obj."))) && !hasClassname) {
                    usesDoc = true;
                }

                // Build the query in this variable.
                StringBuffer select = new StringBuffer("select distinct ");
                // These will hold the components of the from and where parts of the query.
                List<String> fromStatements = new ArrayList<String>();
                List<String> whereStatements = new ArrayList<String>();

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
                    whereStatements.add("obj.id=idprop.id.id and idprop.id.name='" + idField + "'");
                }

                // If specified, add the second column to the query.
                if (hasValueField) {
                    if (valueField.startsWith("doc.") || valueField.startsWith("obj.")) {
                        select.append(", ").append(valueField);
                    } else if (!hasClassname) {
                        select.append(", doc." + valueField);
                    } else {
                        select.append(", valueprop.value");
                        fromStatements.add("StringProperty as valueprop");
                        whereStatements.add("obj.id=valueprop.id.id and valueprop.id.name='" + valueField + "'");
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
            LOG.error("Failed to parse SQL script [" + sql + "]. Continuing with non-rendered script.", e);
        }
        return sql;
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

    public List<ListItem> getCachedDBList(XWikiContext context)
    {
        if (isCache()) {
            return this.cachedDBList;
        } else {
            return (List<ListItem>) context.get(context.getDatabase() + ":" + getFieldFullName());
        }
    }

    public void setCachedDBList(List<ListItem> cachedDBList, XWikiContext context)
    {
        if (isCache()) {
            this.cachedDBList = cachedDBList;
        } else {
            context.put(context.getDatabase() + ":" + getFieldFullName(), cachedDBList);
        }
    }

    @Override
    public void flushCache()
    {
        this.cachedDBList = null;
    }

    // return first or second column from user query
    public String returnCol(String hibquery, boolean first)
    {
        String firstCol = "-", secondCol = "-";

        int fromIndx = hibquery.indexOf("from");

        if (fromIndx > 0) {
            String firstPart = hibquery.substring(0, fromIndx);
            firstPart.replaceAll("\\s+", " ");
            int comIndx = hibquery.indexOf(",");

            // there are more than one columns to select- take the second one (the value)
            if (comIndx > 0 && comIndx < fromIndx) {
                StringTokenizer st = new StringTokenizer(firstPart, " ,()", true);
                ArrayList<String> words = new ArrayList<String>();

                while (st.hasMoreTokens()) {
                    words.add(st.nextToken().toLowerCase());
                }

                int comma = words.indexOf(",") - 1;
                while (words.get(comma).toString().compareTo(" ") == 0) {
                    comma--;
                }
                firstCol = words.get(comma).toString().trim();

                comma = words.indexOf(",") + 1;
                while (words.get(comma).toString().compareTo(" ") == 0) {
                    comma++;
                }

                if (words.get(comma).toString().compareTo("(") == 0) {
                    int i = comma + 1;
                    while (words.get(i).toString().compareTo(")") != 0) {
                        secondCol += words.get(i).toString();
                        i++;
                    }
                    secondCol += ")";
                } else {
                    secondCol = words.get(comma).toString().trim();
                }
            }
            // has only one column
            else {
                int i = fromIndx - 1;
                while (firstPart.charAt(i) == ' ') {
                    --i;
                }
                String col = " ";
                while (firstPart.charAt(i) != ' ') {
                    col += firstPart.charAt(i);
                    --i;
                }
                String reverse = " ";
                for (i = (col.length() - 1); i >= 0; --i) {
                    reverse += col.charAt(i);
                }
                firstCol = reverse.trim();
            }
        }
        if (first == true) {
            return firstCol;
        } else {
            return secondCol;
        }
    }

    // the result of the second query, to retrieve the value
    public String getValue(String val, String sql, XWikiContext context)
    {
        // Make sure the query does not contain ORDER BY, as it will fail in certain databases.
        int orderByPos = sql.toLowerCase().lastIndexOf("order by");
        if (orderByPos >= 0) {
            sql = sql.substring(0, orderByPos);
        }
        String firstCol = returnCol(sql, true);
        String secondCol = returnCol(sql, false);

        String newsql = sql.substring(0, sql.indexOf(firstCol));
        newsql += secondCol + " ";
        newsql += sql.substring(sql.indexOf("from"));
        newsql += "and " + firstCol + "='" + val + "'";

        Object[] list = null;
        XWiki xwiki = context.getWiki();
        String res = "";
        try {
            list = xwiki.search(newsql, context).toArray();
            if (list.length > 0) {
                res = list[0].toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    // override the method from parent ListClass
    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        // input display
        if (getDisplayType().equals("input")) {
            input input = new input();
            input.setType("text");
            input.setSize(getSize());
            boolean changeInputName = false;
            boolean setInpVal = true;

            BaseProperty prop = (BaseProperty) object.safeget(name);
            String val = "";
            if (prop != null) {
                val = prop.toFormString();
            }

            if (isPicker()) {
                input.setClass("suggested");
                String path = "";
                XWiki xwiki = context.getWiki();
                path = xwiki.getURL("Main.WebHome", "view", context);
                String classname = this.getObject().getName();
                String fieldname = this.getName();
                String hibquery = this.getSql();
                String secondCol = "-", firstCol = "-";

                if (hibquery != null && !hibquery.equals("")) {
                    firstCol = returnCol(hibquery, true);
                    secondCol = returnCol(hibquery, false);

                    if (secondCol.compareTo("-") != 0) {
                        changeInputName = true;
                        input hidden = new input();
                        hidden.setID(prefix + name);
                        hidden.setName(prefix + name);
                        hidden.setType("hidden");
                        hidden.setDisabled(isDisabled());
                        if (val != null && !val.equals("")) {
                            hidden.setValue(val);
                        }
                        buffer.append(hidden.toString());

                        input.setValue(getValue(val, hibquery, context));
                        setInpVal = false;
                    }
                }

                String script =
                    "\"" + path + "?xpage=suggest&amp;classname=" + classname + "&amp;fieldname=" + fieldname
                    + "&amp;firCol=" + firstCol + "&amp;secCol=" + secondCol + "&amp;\"";
                String varname = "\"input\"";
                String seps = "\"" + this.getSeparators() + "\"";
                if (isMultiSelect()) {
                    input.setOnFocus("new ajaxSuggest(this, {script:" + script + ", varname:" + varname + ", seps:"
                        + seps + "} )");
                } else {
                    input.setOnFocus("new ajaxSuggest(this, {script:" + script + ", varname:" + varname + "} )");
                }
            }

            if (changeInputName == true) {
                input.setName(prefix + name + "_suggest");
                input.setID(prefix + name + "_suggest");
            } else {
                input.setName(prefix + name);
                input.setID(prefix + name);
            }
            if (setInpVal == true) {
                input.setValue(val);
            }

            input.setDisabled(isDisabled());
            buffer.append(input.toString());
        } else if (getDisplayType().equals("radio") || getDisplayType().equals("checkbox")) {
            displayRadioEdit(buffer, name, prefix, object, context);
        } else {
            displaySelectEdit(buffer, name, prefix, object, context);
        }

        if (!getDisplayType().equals("input")) {
            org.apache.ecs.xhtml.input hidden = new input(input.hidden, prefix + name, "");
            buffer.append(hidden);
        }
    }

    @Override
    public void displayView(StringBuffer buffer, String name, String prefix, BaseCollection object, XWikiContext context)
    {
        List<String> selectlist;
        String separator = getSeparator();
        BaseProperty prop = (BaseProperty) object.safeget(name);
        Map<String, ListItem> map = getMap(context);
        if (prop instanceof ListProperty) {
            selectlist = ((ListProperty) prop).getList();
            List<String> newlist = new ArrayList<String>();
            for (String entry : selectlist) {
                newlist.add(getDisplayValue(entry, name, map, context));
            }
            buffer.append(StringUtils.join(newlist, separator));
        } else {
            buffer.append(getDisplayValue(prop.getValue(), name, map, context));
        }
    }
}
