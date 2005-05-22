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
 * Time: 10:22:44
 */
package com.xpn.xwiki.web;

import javax.portlet.PortletContext;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class XWikiPortletContext implements XWikiEngineContext {
    private PortletContext pcontext;

    public XWikiPortletContext(PortletContext pcontext) {
        this.pcontext = pcontext;
    }

    public PortletContext getPortletContext() {
        return pcontext;
    }
    
    public Object getAttribute(String name) {
        return pcontext.getAttribute(name);
    }

    public void setAttribute(String name, Object value) {
        pcontext.setAttribute(name, value);
    }

    public String getRealPath(String path) {
        return pcontext.getRealPath(path);
    }

    public URL getResource(String name) throws MalformedURLException {
        return pcontext.getResource(name);
    }

    public InputStream getResourceAsStream(String name) {
        return pcontext.getResourceAsStream(name);
    }

    public String getMimeType(String filename) {
        return pcontext.getMimeType(filename);
    }


}
