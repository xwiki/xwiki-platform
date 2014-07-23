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

import java.util.Date;
import java.util.Map;

import org.apache.ecs.xhtml.input;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.DateProperty;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.web.XWikiMessageTool;


/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.classes.DateClass} class.
 * 
 * @version $Id$
 */
public aspect DateClassCompatibiityAspect
{
    @Deprecated
    public void DateClass.displaySearch(StringBuffer buffer, String name, String prefix, XWikiCriteria criteria,
        XWikiContext context)
    {
        input input1 = new input();
        input1.setType("text");
        input1.setName(prefix + name + "_morethan");
        input1.setID(prefix + name);
        input1.setSize(getSize());
        String fieldFullName = getFieldFullName();

        Date value = (Date) criteria.getParameter(fieldFullName + "_morethan");
        if (value != null) {
            DateProperty dprop = new DateProperty();
            dprop.setValue(value);
            input1.setValue(toFormString(dprop));
        }

        input input2 = new input();

        input2.setType("text");
        input2.setName(prefix + name + "_lessthan");
        input2.setID(prefix + name);
        input2.setSize(getSize());
        value = (Date) criteria.getParameter(fieldFullName + "_lessthan");
        if (value != null) {
            DateProperty dprop = new DateProperty();
            dprop.setValue(value);
            input2.setValue(toFormString(dprop));
        }

        XWikiMessageTool msg = context.getMessageTool();
        buffer.append((msg == null) ? "from" : msg.get("from"));
        buffer.append(input1.toString());
        buffer.append((msg == null) ? "from" : msg.get("to"));
        buffer.append(input2.toString());
    }

    @Deprecated
    public void DateClass.fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
        String[] data = map.get("");
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
