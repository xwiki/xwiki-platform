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

    /**
     *
     * @return an object which contains the Request context
     */
    public XWikiRequest getRequest() {
       return context.getRequest();
    }

    /**
     *
     * @return an object which contains the response object
     */
    public XWikiResponse getResponse() {
       return context.getResponse();
    }

    public int getMode() {
        return context.getMode();
    }

    /**
     *
     * @return the current database name
     */
    public String getDatabase() {
        return context.getDatabase();
    }

    /**
     *
     * @return the original database
     */
    public String getOriginalDatabase() {
        return context.getOriginalDatabase();
    }

    /**
     * set the database if you have the programming right
     * @param database the data name
     */
    public void setDatabase(String database) {
        if (checkProgrammingRights())
          context.setDatabase(database);
    }

    /**
     *
     * @return the url Factory
     */
    public XWikiURLFactory getURLFactory() {
        return context.getURLFactory();
    }

    /**
     *
     * @return true if the server is in virtual mode (ie host more than one wiki)
     */
    public boolean isVirtual() {
        return context.isVirtual();
    }

    /**
     *
     * @return the requested action
     */
    public String getAction() {
         return context.getAction();
    }

    /**
     *
     * @return the language of the current user
     */
    public String getLanguage() {
         return context.getLanguage();
    }

    /**
     *
     * @return the interface language preference of the current user
     */
    public String getInterfaceLanguage() {
         return context.getInterfaceLanguage();
    }

    /**
     *
     * @return the XWiki object if you have the programming right
     */
    public com.xpn.xwiki.XWiki getXWiki() {
        if (checkProgrammingRights())
         return context.getWiki();
        else
         return null;
    }

    /**
     *
     * @return the current requested document
     */
    public XWikiDocument getDoc() {
        if (checkProgrammingRights())
         return context.getDoc();
        else
         return null;
    }

    /**
     *
     * @return the current user which made the request
     */
    public String getUser() {
         return context.getUser();
    }

    /**
     *
     * @return the local username of the current user which made the request
     */
    public String getLocalUser() {
        return context.getLocalUser();
    }

    /**
     * set the document if you have the programming right
     * @param doc
     */
    public void setDoc(XWikiDocument doc) {
        if (checkProgrammingRights())
          context.setDoc(doc);
    }

    /**
     *
     * @return the unwrapped version of the context if you have the programming right
     */
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

    public void put(String key, java.lang.Object value) {
        if (checkProgrammingRights())
            context.put(key, value);
    }

    public void setFinished(boolean finished) {
        context.setFinished(finished);
    }

    /**
     *
     * @return the cache duration
     */
    public int getCacheDuration() {
        return context.getCacheDuration();
    }

    /**
     *
     * @param duration in second
     */
    public void setCacheDuration(int duration) {
        context.setCacheDuration(duration);
    }

    public void setLinksAction(String action) {
        context.setLinksAction(action);
    }

    public void unsetLinksAction() {
        context.unsetLinksAction();
    }

    public String getLinksAction() {
        return context.getLinksAction();
    }

    public void setLinksQueryString(String value) {
        context.setLinksQueryString(value);
    }

    public void unsetLinksQueryString() {
        context.unsetLinksQueryString();
    }

    public String getLinksQueryString() {
        return context.getLinksQueryString();
    }
}
