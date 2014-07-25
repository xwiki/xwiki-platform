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
import org.hibernate.annotations.common.reflection.XClass;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;


/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.objects.classes.PropertyClass} class.
 * 
 * @version $Id$
 */
public aspect PropertyClassCompatibiityAspect
{
    @Deprecated
    public void PropertyClass.displaySearch(StringBuffer buffer, String name, String prefix, XWikiCriteria criteria,
        XWikiContext context)
    {
        input input = new input();
        input.setType("text");
        input.setName(prefix + name);
        input.setID(prefix + name);
        input.setSize(20);
        String fieldFullName = getFieldFullName();
        Object value = criteria.getParameter(fieldFullName);
        if (value != null) {
            input.setValue(value.toString());
        }
        buffer.append(input.toString());
    }

    @Deprecated
    public String PropertyClass.displaySearch(String name, String prefix, XWikiCriteria criteria, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        displaySearch(buffer, name, prefix, criteria, context);
        return buffer.toString();
    }

    @Deprecated
    public String PropertyClass.displaySearch(String name, XWikiCriteria criteria, XWikiContext context)
    {
        return displaySearch(name, "", criteria, context);
    }

    @Deprecated
    public void PropertyClass.makeQuery(Map<String, Object> map, String prefix, XWikiCriteria query, List<String> criteriaList)
    {
    }

    @Deprecated
    public void PropertyClass.fromSearchMap(XWikiQuery query, Map<String, String[]> map)
    {
    }

    public BaseClass PropertyClass.getxWikiClass(XWikiContext context)
    {
        return getXClass(context);
    }
}
