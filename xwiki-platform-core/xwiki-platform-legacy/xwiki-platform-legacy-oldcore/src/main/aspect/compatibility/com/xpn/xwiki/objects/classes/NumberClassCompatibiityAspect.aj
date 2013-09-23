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
import com.xpn.xwiki.web.XWikiMessageTool;


/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.classes.NumberClass} class.
 * 
 * @version $Id$
 */
public aspect NumberClassCompatibiityAspect
{
    @Deprecated
    public void NumberClass.displaySearch(StringBuffer buffer, String name, String prefix, XWikiCriteria criteria,
        XWikiContext context)
    {
        input input1 = new input();
        input1.setType("text");
        input1.setName(prefix + name + "_morethan");
        input1.setID(prefix + name);
        input1.setSize(getSize());
        String fieldFullName = getFieldFullName();
        Number value = (Number) criteria.getParameter(fieldFullName + "_morethan");
        if (value != null) {
            input1.setValue(value.toString());
        }

        input input2 = new input();

        input2.setType("text");
        input2.setName(prefix + name + "_lessthan");
        input2.setID(prefix + name);
        input2.setSize(getSize());
        value = (Number) criteria.getParameter(fieldFullName + "_lessthan");
        if (value != null) {
            input2.setValue(value.toString());
        }

        XWikiMessageTool msg = context.getMessageTool();
        buffer.append((msg == null) ? "from" : msg.get("from"));
        buffer.append(input1.toString());
        buffer.append((msg == null) ? "from" : msg.get("to"));
        buffer.append(input2.toString());
    }

    @Deprecated
    public void NumberClass.makeQuery(Map<String, Object> map, String prefix, XWikiCriteria query, List<String> criteriaList)
    {
        Number value = (Number) map.get(prefix);
        if ((value != null) && (!value.equals(""))) {
            criteriaList.add(getFullQueryPropertyName() + "=" + value.toString());
            return;
        }

        value = (Number) map.get(prefix + "lessthan");
        if ((value != null) && (!value.equals(""))) {
            criteriaList.add(getFullQueryPropertyName() + "<=" + value.toString());
            return;
        }

        value = (Number) map.get(prefix + "morethan");
        if ((value != null) && (!value.equals(""))) {
            criteriaList.add(getFullQueryPropertyName() + ">=" + value.toString());
            return;
        }
    }

    @Deprecated
    public void NumberClass.fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String data[] = map.get("");
        if ((data != null) && (data.length == 1)) {
            query.setParam(getObject().getName() + "_" + getName(), fromString(data[0]).getValue());
        } else {
            data = map.get("lessthan");
            if ((data != null) && (data.length == 1)) {
                query.setParam(getObject().getName() + "_" + getName() + "_lessthan", fromString(data[0]).getValue());
            }
            data = map.get("morethan");
            if ((data != null) && (data.length == 1)) {
                query.setParam(getObject().getName() + "_" + getName() + "_morethan", fromString(data[0]).getValue());
            }
        }
    }
}
