/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 * @author ludovic
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

    protected XWikiContext getProtectedContext() {
         return context;
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

    public int getCacheDuration() {
        return context.getCacheDuration();
    }

    public void setCacheDuration(int duration) {
        context.setCacheDuration(duration);
    }
}
