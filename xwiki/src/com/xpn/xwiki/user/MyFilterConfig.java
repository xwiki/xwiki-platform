/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details, published at
 * http://www.gnu.org/copyleft/lesser.html or in lesser.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 24 mars 2004
 * Time: 17:57:57
 */

package com.xpn.xwiki.user;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Hashtable;

public class MyFilterConfig implements FilterConfig {
    protected Hashtable params = new Hashtable();

    public String getFilterName() {
        return null;
    }

    public ServletContext getServletContext() {
        return null;
    }

    public String getInitParameter(String s) {
        return (String) params.get(s);
    }

    public Enumeration getInitParameterNames() {
        return params.keys();
    }

    public void setInitParameter(String key, String s) {
        params.put(key, s);
    }

}
