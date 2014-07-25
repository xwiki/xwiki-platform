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
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.input;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;


/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.classes.BooleanClass} class.
 * 
 * @version $Id$
 */
public privileged aspect BooleanClassCompatibiityAspect
{
    @Deprecated
    public String BooleanClass.displaySearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        if (getDisplayType().equals("input")) {
            return super.displaySearch(name, prefix, criteria, context);
        } else if (getDisplayType().equals("radio")) {
            return displayCheckboxSearch(name, prefix, criteria, context);
        } else {
            return displaySelectSearch(name, prefix, criteria, context);
        }
    }

    @Deprecated
    public String BooleanClass.displaySelectSearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        select select = new select(prefix + name, 1);
        select.setMultiple(true);
        select.setSize(3);
        String String0 = getDisplayValue(context, 0);
        String String1 = getDisplayValue(context, 1);
        String fieldFullName = getFieldFullName();
        Number[] selectArray = ((Number[]) criteria.getParameter(fieldFullName));
        List<Number> selectlist = (selectArray != null) ? Arrays.asList(selectArray) : new ArrayList<Number>();

        option[] options = {new option(String1, "1"), new option(String0, "0")};
        options[0].addElement(String1);
        options[1].addElement(String0);
        if (selectlist.contains(new Integer(1)))
         options[0].setSelected(true); 
        if (selectlist.contains(new Integer(0)))
         options[1].setSelected(true); 

        /*
         * try { IntegerProperty prop = (IntegerProperty) object.safeget(name); if (prop!=null) { Integer ivalue =
         * (Integer)prop.getValue(); if (ivalue!=null) { int value = ivalue.intValue(); if (value==1)
         * options[1].setSelected(true); else if (value==0) options[2].setSelected(true); } else { int value =
         * getDefaultValue(); if (value==1) options[1].setSelected(true); else if (value==0)
         * options[2].setSelected(true); } } } catch (Exception e) { // This should not happen e.printStackTrace(); }
         */
        select.addElement(options);
        return select.toString();
    }

    @Deprecated
    public String BooleanClass.displayCheckboxSearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        org.apache.ecs.xhtml.input check = new input(input.checkbox, prefix + name, 1);
        org.apache.ecs.xhtml.input checkNo = new input(input.hidden, prefix + name, 0);

        /*
         * try { IntegerProperty prop = (IntegerProperty) object.safeget(name); if (prop!=null) { Integer ivalue =
         * (Integer)prop.getValue(); if (ivalue!=null) { int value = ivalue.intValue(); if (value==1)
         * check.setChecked(true); else if (value==0) check.setChecked(false); } else { int value = getDefaultValue();
         * if (value==1) check.setChecked(true); else check.setChecked(false); } }} catch (Exception e) { // This should
         * not happen e.printStackTrace(); }
         */
        buffer.append(check.toString());
        buffer.append(checkNo.toString());
        return buffer.toString();
    }

    @Deprecated
    public void BooleanClass.makeQuery(Map<String, Object> map, String prefix, XWikiCriteria query, List<String> criteriaList)
    {
        Object values = map.get(prefix);
        if ((values == null) || (values.equals(""))) {
            return;
        }

        // :value = doc.object(XWiki.ArticleClass).category

        Number[] valuesarray = (Number[]) values;
        String[] criteriaarray = new String[valuesarray.length];
        for (int i = 0; i < valuesarray.length; i++) {
            criteriaarray[i] =  "" + valuesarray[i] + " = " + getFullQueryPropertyName();
        }
        criteriaList.add("(" + StringUtils.join(criteriaarray, " or ") + ")");
        return;
    }


    @Deprecated
    public void BooleanClass.fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String[] data = map.get("");
        if (data != null) {
            Number[] data2 = new Number[data.length];
            for (int i = 0; i < data.length; i++) {
                data2[i] = (Number) fromString(data[i]).getValue();
            }
            query.setParam(getObject().getName() + "_" + getName(), data2);
        }
    }
}
