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
 * Created by
 * User: Ludovic Dubost
 * Date: 26 nov. 2003
 * Time: 15:33:03
 */
package com.xpn.xwiki.web;

import org.apache.struts.action.Action;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWikiContext;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;


public class XWikiAction extends Action {

    public XWiki getXWiki(XWikiContext context, boolean test) throws XWikiException {
        String xwikicfg = "WEB-INF/xwiki.cfg";
        String xwikiname = "xwiki";


        if (test) {
            xwikicfg = "WEB-INF/xwiki-test.cfg";
            xwikiname = "xwikitest";
        }

        XWiki xwiki = (XWiki) servlet.getServletContext().getAttribute(xwikiname);

        if (xwiki == null) {
            String path = servlet.getServletContext().getRealPath(xwikicfg);
            xwiki = new XWiki(path, context);
            servlet.getServletContext().setAttribute(xwikiname, xwiki);
        }
        context.setWiki(xwiki);
        return xwiki;
    }

    public String getRealPath(String path) {
        return servlet.getServletContext().getRealPath(path);
    }
    
}
