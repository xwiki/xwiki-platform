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
 */
package com.xpn.xwiki.objects.classes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.web.XWikiMessageTool;


/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.classes.ListClass} class.
 * 
 * @version $Id$
 */
public aspect ListClassCompatibiityAspect
{

    @Deprecated
    public String ListClass.displaySearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        if (getDisplayType().equals("input")) {
            return super.displaySearch(name, prefix, criteria, context);
        } else if (getDisplayType().equals("radio") || getDisplayType().equals("checkbox")) {
            return displayRadioSearch(name, prefix, criteria, context);
        } else {
            return displaySelectSearch(name, prefix, criteria, context);
        }
    }

    @Deprecated
    public String ListClass.displayRadioSearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        List<String> list = getList(context);
        List<String> selectlist = new ArrayList<String>();

        /*
         * BaseProperty prop = (BaseProperty)object.safeget(name); if (prop==null) { selectlist = new ArrayList(); }
         * else if ((prop instanceof ListProperty)||(prop instanceof DBStringListProperty)) { selectlist = (List)
         * prop.getValue(); } else { selectlist = new ArrayList(); selectlist.add(prop.getValue()); }
         */

        // Add options from Set
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            String rawvalue = it.next();
            String value = getElementValue(rawvalue);
            String display = getDisplayValue(rawvalue, name, getMap(context), context);
            input radio =
                new input(getDisplayType().equals("radio") ? input.radio : input.checkbox, prefix + name, value);

            if (selectlist.contains(value)) {
                radio.setChecked(true);
            }
            radio.addElement(display);
            buffer.append(radio.toString());
            if (it.hasNext()) {
                buffer.append("<br/>");
            }
        }
        return buffer.toString();
    }

    @Deprecated
    public String ListClass.displaySelectSearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        select select = new select(prefix + name, 1);
        select.setMultiple(true);
        select.setSize(5);
        select.setName(prefix + name);
        select.setID(prefix + name);

        List<String> list = getList(context);
        String fieldFullName = getFieldFullName();
        String[] selectArray = ((String[]) criteria.getParameter(fieldFullName));
        List<String> selectlist = (selectArray != null) ? Arrays.asList(selectArray) : new ArrayList<String>();

        /*
         * BaseProperty prop = (BaseProperty)object.safeget(name); if (prop==null) { selectlist = new ArrayList(); }
         * else if ((prop instanceof ListProperty)||(prop instanceof DBStringListProperty)) { selectlist = (List)
         * prop.getValue(); } else { selectlist = new ArrayList(); selectlist.add(prop.getValue()); }
         */

        // Add options from Set
        for (String rawvalue : list) {
            String value = getElementValue(rawvalue);
            String display = getDisplayValue(rawvalue, name, getMap(context), context);
            option option = new option(display, value);
            option.addElement(display);
            if (selectlist.contains(value)) {
                option.setSelected(true);
            }
            select.addElement(option);
        }

        return select.toString();
    }

    @Deprecated
    public void ListClass.makeQuery(Map<String, Object> map, String prefix, XWikiCriteria query, List<String> criteriaList)
    {
        Object values = map.get(prefix);
        if ((values == null) || (values.equals(""))) {
            return;
        }

        // if multiselect
        // :category member of obj.category
        // otherwise 
        // :category = obj.category
        String separator = isMultiSelect() ? " member of " : " = ";


 
        if (values instanceof String) {
            // general comparison '=' - tests at least one value =
            criteriaList.add("'" + values.toString() + "'" + separator  + getFullQueryPropertyName());
        } else {
            String[] valuesarray = (String[]) values;
            String[] criteriaarray = new String[valuesarray.length];
            for (int i = 0; i < valuesarray.length; i++) {
                criteriaarray[i] = "'" + valuesarray[i] + "'" + separator + getFullQueryPropertyName();
            }
            criteriaList.add("(" + StringUtils.join(criteriaarray, " or ") + ")");
        }
        return;
    }

    @Deprecated
    public void ListClass.fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String[] data = map.get("");
        if (data != null) {
            query.setParam(getObject().getName() + "_" + getName(), data);
        }
    }
}
