/**
 * ===================================================================
 *
 * Copyright (c) 2003 Ludovic Dubost, All rights reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details, published at
 * http://www.gnu.org/copyleft/gpl.html or in gpl.txt in the
 * root folder of this distribution.
 *
 * User: ludovic
 * Date: 15 mars 2004
 * Time: 00:41:24
 */

package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.web.XWikiAction;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class Context extends Api {

    public Context(XWikiContext context) {
        super(context);
    }

    public HttpServletRequest getRequest() {
       return context.getRequest();
    }

    public HttpServletResponse getResponse() {
       return context.getResponse();
    }

    public String getDatabase() {
        return context.getDatabase();
    }

    public void setDatabase(String database) {
        if (checkProgrammingRights())
          context.setDatabase(database);
    }

    public String getBaseUrl() {
        return context.getBaseUrl();
    }

    public boolean isVirtual() {
        return context.isVirtual();
    }

    public HttpServlet getServlet() {
        if (checkProgrammingRights())
         return context.getServlet();
        else
         return null;
    }

    public String getAction() {
         return context.getAction();
    }

    public com.xpn.xwiki.XWiki getXWiki() {
        if (checkProgrammingRights())
         return context.getWiki();
        else
         return null;
    }

    public XWikiDocInterface getDoc() {
        if (checkProgrammingRights())
         return context.getDoc();
        else
         return null;
    }

    public String getUser() {
         return context.getUser();
    }

    public void setDoc(XWikiDocInterface doc) {
        if (checkProgrammingRights())
          context.setDoc(doc);
    }

    public XWikiContext getContext() {
        if (checkProgrammingRights())
         return context;
        else
         return null;
    }

    public java.lang.Object get(String key) {
        if (checkProgrammingRights())
            return context.get(key);
        else
            return null;
    }

    public void put(String key, Object value) {
        if (checkProgrammingRights())
            context.put(key, value);
    }
}
