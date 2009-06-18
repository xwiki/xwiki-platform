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
package com.xpn.xwiki.plugin.query;

import com.xpn.xwiki.util.Util;

import java.util.*;

public class XWikiCriteria
{
    protected Map<String, Object> params = new HashMap<String, Object>();

    public Object getParameter(String field)
    {
        return params.get(field);
    }

    public Map<String, Object> getParameters(String field)
    {
        return Util.getSubMap(params, field);
    }

    public void setParam(String field, Object value)
    {
        params.put(field, value);
    }

    public Set<String> getClasses()
    {
        Set<String> set = new HashSet<String>();
        for (Iterator<String> it = params.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            String objname = key.substring(0, key.indexOf('_'));
            if ((!objname.equals("") && (!objname.equals("doc"))))
                set.add(objname);
        }
        return set;
    }
}
