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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.ecs.xhtml.option;
import org.apache.ecs.xhtml.select;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.query.OrderClause;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;


/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.classes.BaseClass} class.
 * 
 * @version $Id$
 */
public aspect BaseClassCompatibiityAspect
{
    @Deprecated
    public String BaseClass.makeQuery(XWikiCriteria query)
    {
        List<String> criteriaList = new ArrayList<String>();
        for (PropertyClass property : (Collection<PropertyClass>) getFieldList()) {
            String name = property.getName();
            Map<String, Object> map = query.getParameters(getName() + "_" + name);
            if (map.size() > 0) {
                property.makeQuery(map, "", query, criteriaList);
            }
        }

        return StringUtils.join(criteriaList.toArray(), " and ");
    }

    @Deprecated
    public String BaseClass.displaySearchColumns(String prefix, XWikiQuery query, XWikiContext context)
    {
        select select = new select(prefix + "searchcolumns", 5);
        select.setMultiple(true);
        select.setName(prefix + "searchcolumns");
        select.setID(prefix + "searchcolumns");

        List<String> list = Arrays.asList(getPropertyNames());
        Map<String, String> prettynamesmap = new HashMap<String, String>();
        for (int i = 0; i < list.size(); i++) {
            String propname = list.get(i);
            list.set(i, prefix + propname);
            prettynamesmap.put(prefix + propname, ((PropertyClass) get(propname)).getPrettyName());
        }

        List<String> selectlist = query.getDisplayProperties();

        // Add options from Set
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            String value = it.next().toString();
            String displayValue = prettynamesmap.get(value);
            option option = new option(displayValue, displayValue);
            option.addElement(displayValue);
            option.setValue(value);
            if (selectlist.contains(value)) {
                option.setSelected(true);
            }
            select.addElement(option);
        }

        return select.toString();
    }

    @Deprecated
    public String BaseClass.displaySearchOrder(String prefix, XWikiQuery query, XWikiContext context)
    {
        select select = new select(prefix + "searchorder", 5);
        select.setMultiple(true);
        select.setName(prefix + "searchorder");
        select.setID(prefix + "searchorder");

        List<String> list = Arrays.asList(getPropertyNames());
        Map<String, String> prettynamesmap = new HashMap<String, String>();
        for (int i = 0; i < list.size(); i++) {
            String propname = list.get(i);
            list.set(i, prefix + propname);
            prettynamesmap.put(prefix + propname, ((PropertyClass) get(propname)).getPrettyName());
        }

        OrderClause order = null;
        if ((query != null) && (query.getOrderProperties() != null) && (query.getOrderProperties().size() > 0)) {
            order = query.getOrderProperties().get(0);
        }

        // Add options from Set
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            String value = it.next().toString();
            String displayValue = prettynamesmap.get(value);
            option option = new option(displayValue, displayValue);
            option.addElement(displayValue);
            option.setValue(value);
            if ((order != null) && (value.equals(order.getProperty()))) {
                option.setSelected(true);
            }
            select.addElement(option);
        }

        return select.toString();
    }
}
