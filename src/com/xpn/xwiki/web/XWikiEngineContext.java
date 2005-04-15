/**
 * ===================================================================
 *
 * Copyright (c) 2003,2004 Ludovic Dubost, All rights reserved.
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

 * Created by
 * User: Ludovic Dubost
 * Date: 20 mai 2004
 * Time: 10:20:08
 */
package com.xpn.xwiki.web;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public interface XWikiEngineContext {
    public Object getAttribute(String name);
    public void setAttribute(String name, Object value);
    public String getRealPath(String path);
    public URL getResource(String name) throws MalformedURLException;
    public InputStream getResourceAsStream(String name);
    public String getMimeType(String filename);
}
