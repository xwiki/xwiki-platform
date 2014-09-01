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

import java.util.List;
import java.util.Map;

import org.apache.ecs.xhtml.input;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;


/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.classes.StringClass} class.
 * 
 * @version $Id$
 */
public aspect StringClassCompatibiityAspect
{
    @Deprecated
    public void StringClass.displaySearch(StringBuffer buffer, String name, String prefix,
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

    @Deprecated
    public void StringClass.makeQuery(Map<String, Object> map, String prefix, XWikiCriteria query, List<String> criteriaList)
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

    @Deprecated
    public void StringClass.fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String[] data = map.get("");
        if ((data != null) && (data.length == 1)) {
            query.setParam(getObject().getName() + "_" + getName(), data[0]);
        }
    }
}
