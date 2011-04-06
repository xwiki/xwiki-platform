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

package com.xpn.xwiki.user.impl.xwiki;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;

public class MyFilterConfig implements FilterConfig
{
    protected Hashtable params = new Hashtable();

    public String getFilterName()
    {
        return null;
    }

    public ServletContext getServletContext()
    {
        return null;
    }

    public String getInitParameter(String s)
    {
        return (String) params.get(s);
    }

    public Enumeration getInitParameterNames()
    {
        return params.keys();
    }

    public void setInitParameter(String key, String s)
    {
        params.put(key, s);
    }
}
