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
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiURLFactory;


public class Context extends Api {

    public Context(XWikiContext context) {
        super(context);
    }

    public XWikiRequest getRequest() {
       return context.getRequest();
    }

    public XWikiResponse getResponse() {
       return context.getResponse();
    }

    public int getMode() {
        return context.getMode();
    }

    public String getDatabase() {
        return context.getDatabase();
    }

    public String getOriginalDatabase() {
        return context.getOriginalDatabase();
    }

    public void setDatabase(String database) {
        if (checkProgrammingRights())
          context.setDatabase(database);
    }

    public XWikiURLFactory getURLFactory() {
        return context. getURLFactory();
    }

    public boolean isVirtual() {
        return context.isVirtual();
    }

    public String getAction() {
         return context.getAction();
    }

    public String getLanguage() {
         return context.getLanguage();
    }

    public com.xpn.xwiki.XWiki getXWiki() {
        if (checkProgrammingRights())
         return context.getWiki();
        else
         return null;
    }

    public XWikiDocument getDoc() {
        if (checkProgrammingRights())
         return context.getDoc();
        else
         return null;
    }

    public String getUser() {
         return context.getUser();
    }

    public void setDoc(XWikiDocument doc) {
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

    public void setFinished(boolean finished) {
        context.setFinished(finished);
    }
}
