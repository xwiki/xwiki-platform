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

import java.util.List;
import java.util.Map;

import org.apache.ecs.xhtml.input;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseCollection;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.StringProperty;
import com.xpn.xwiki.objects.meta.PropertyMetaClass;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;

public class StringClass extends PropertyClass
{

    public StringClass(String name, String prettyname, PropertyMetaClass wclass)
    {
        super(name, prettyname, wclass);
        setSize(30);
    }

    public StringClass(PropertyMetaClass wclass)
    {
        this("string", "String", wclass);
    }

    public StringClass()
    {
        this(null);
    }

    public int getSize()
    {
        return getIntValue("size");
    }

    public void setSize(int size)
    {
        setIntValue("size", size);
    }

    public boolean isPicker()
    {
        return (getIntValue("picker") == 1);
    }

    public void setPicker(boolean picker)
    {
        setIntValue("picker", picker ? 1 : 0);
    }

    @Override
    public BaseProperty fromString(String value)
    {
        BaseProperty property = newProperty();
        property.setValue(value);
        return property;
    }

    @Override
    public BaseProperty newProperty()
    {
        BaseProperty property = new StringProperty();
        property.setName(getName());
        return property;
    }

    @Override
    public void displayEdit(StringBuffer buffer, String name, String prefix,
        BaseCollection object, XWikiContext context)
    {
        input input = new input();
        BaseProperty prop = (BaseProperty) object.safeget(name);
        if (prop != null) {
            input.setValue(prop.toFormString());
        }

        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        input.setDisabled(isDisabled());

        if (isPicker()) {
            input.setClass("suggested");
            String path = "";
            XWiki xwiki = context.getWiki();
            path = xwiki.getURL("Main.WebHome", "view", context);

            String classname = this.getObject().getName();
            String fieldname = this.getName();
            String secondCol = "-", firstCol = "-";

            String script =
                "\"" + path + "?xpage=suggest&amp;classname=" + classname + "&amp;fieldname=" + fieldname
                    + "&amp;firCol=" + firstCol + "&amp;secCol=" + secondCol + "&amp;\"";
            String varname = "\"input\"";
            input.setOnFocus("new ajaxSuggest(this, {script:" + script + ", varname:" + varname + "} )");
        }

        buffer.append(input.toString());
    }

    @Override
    public void displaySearch(StringBuffer buffer, String name, String prefix,
        XWikiCriteria criteria, XWikiContext context)
    {
        input input = new input();
        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(getSize());
        String fieldFullName = getFieldFullName();
        Object value = criteria.getParameter(fieldFullName);
        if (value != null) {
            input.setValue(value.toString());
        }
        buffer.append(input.toString());
    }

    @Override
    public void makeQuery(Map<String, Object> map, String prefix, XWikiCriteria query, List<String> criteriaList)
    {
        String value = (String) map.get(prefix);
        if ((value != null) && (!value.equals(""))) {
            String startsWith = (String) map.get(prefix + "startswith");
            String endsWith = (String) map.get(prefix + "endswith");
            if ("1".equals(startsWith)) {
                criteriaList.add("lower(" + getFullQueryPropertyName() + ") like '" + value.toLowerCase() + "%'");
            } else if ("1".equals(endsWith)) {
                criteriaList.add("lower(" + getFullQueryPropertyName() + ") like '%" + value.toLowerCase() + "'");
            } else {
                criteriaList.add("lower(" + getFullQueryPropertyName() + ") like '%" + value.toLowerCase() + "%'");
            }
            return;
        }

        value = (String) map.get(prefix + "exact");
        if ((value != null) && (!value.equals(""))) {
            criteriaList.add(getFullQueryPropertyName() + "='" + value + "'");
            return;
        }

        value = (String) map.get(prefix + "not");
        if ((value != null) && (!value.equals(""))) {
            criteriaList.add(getFullQueryPropertyName() + "!='" + value + "'");
            return;
        }
    }

    @Override
    public void fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String[] data = map.get("");
        if ((data != null) && (data.length == 1)) {
            query.setParam(getObject().getName() + "_" + getName(), data[0]);
        }
    }

}
