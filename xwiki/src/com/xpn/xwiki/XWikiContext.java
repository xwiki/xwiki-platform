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
 * Created by
 * User: Ludovic Dubost
 * Date: 4 déc. 2003
 * Time: 22:47:17
 */
package com.xpn.xwiki;

import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.user.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiAction;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Hashtable;

public class XWikiContext extends Hashtable {

   private XWiki wiki;
   private HttpServlet servlet;
   private HttpServletRequest request;
   private HttpServletResponse response;
   private String action;
   private String database;
   private String baseUrl;
   private boolean virtual;
   private XWikiUser user;
   private String language;

   public XWikiContext() {
   }

   public XWiki getWiki() {
       return wiki;
   }

   public Util getUtil() {
       Util util = (Util) this.get("util");
       if (util==null) {
           util = new Util();
           this.put("util", util);
       }
       return util;
    }

    public void setWiki(XWiki wiki) {
        this.wiki = wiki;
    }

    public HttpServlet getServlet() {
        return servlet;
    }

    public void setServlet(HttpServlet servlet) {
        this.servlet = servlet;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public HttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(HttpServletResponse response) {
        this.response = response;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public XWikiDocInterface getDoc() {
        return (XWikiDocInterface) get("doc");
    }

    public void setDoc(XWikiDocInterface doc) {
        put("doc", doc);
    }

    public void setUser(String user, boolean main) {
        this.user = new XWikiUser(user, main);
    }

    public void setUser(String user) {
        this.user = new XWikiUser(user);
    }

    public String getUser() {
        if (user!=null)
         return user.getUser();
        else
         return "XWiki.XWikiGuest";
    }

    public XWikiUser getXWikiUser() {
        return user;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

}
