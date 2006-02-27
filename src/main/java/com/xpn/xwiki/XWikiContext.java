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
 * @author wr0ngway
 * @author sdumitriu
 */

package com.xpn.xwiki;

import java.net.URL;
import java.util.Hashtable;

import org.apache.xmlrpc.XmlRpcServer;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiForm;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiURLFactory;

public class XWikiContext extends Hashtable {

   public static final int MODE_SERVLET = 0;
   public static final int MODE_PORTLET = 1;
   public static final int MODE_XMLRPC = 2;
   public static final int MODE_ATOM = 3;
   public static final int MODE_PDF = 4;

   private boolean finished = false;
   private XWiki wiki;
   private XWikiEngineContext engine_context;
   private XWikiRequest request;
   private XWikiResponse response;
   private XWikiForm form;
   private String action;
   private String orig_database;
   private String database;
   private boolean virtual;
   private XWikiUser user;
   private String language;
   private int mode;
   private URL url;
   private XWikiURLFactory URLFactory;
   private XmlRpcServer xmlRpcServer;
   private String wikiOwner;
   private XWikiDocument wikiServer;
   private int cacheDuration = 0;

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

    public XWikiEngineContext getEngineContext() {
        return engine_context;
    }

    public void setEngineContext(XWikiEngineContext engine_context) {
        this.engine_context = engine_context;
    }

    public XWikiRequest getRequest() {
        return request;
    }

    public void setRequest(XWikiRequest request) {
        this.request = request;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public XWikiResponse getResponse() {
        return response;
    }

    public void setResponse(XWikiResponse response) {
        this.response = response;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
        if (orig_database==null)
            orig_database = database;
    }

    public String getOriginalDatabase() {
        return orig_database;
    }

    public void setOriginalDatabase(String database) {
        this.orig_database = database;
    }

    public boolean isVirtual() {
        return virtual;
    }

    public void setVirtual(boolean virtual) {
        this.virtual = virtual;
    }

    public XWikiDocument getDoc() {
        return (XWikiDocument) get("doc");
    }

    public void setDoc(XWikiDocument doc) {
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

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public URL getURL() {
        return url;
    }

    public void setURL(URL url) {
        this.url = url;
    }

    public XWikiURLFactory getURLFactory() {
        return URLFactory;
    }

    public void setURLFactory(XWikiURLFactory URLFactory) {
        this.URLFactory = URLFactory;
    }

    public XWikiForm getForm() {
        return form;
    }

    public void setForm(XWikiForm form) {
        this.form = form;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    public XmlRpcServer getXMLRPCServer() {
        return xmlRpcServer;
    }

    public void setXMLRPCServer(XmlRpcServer xmlRpcServer) {
        this.xmlRpcServer = xmlRpcServer;
    }

    public void setWikiOwner(String wikiOwner) {
        this.wikiOwner = wikiOwner;
    }

    public String getWikiOwner() {
        return wikiOwner;
    }

    public void setWikiServer(XWikiDocument doc) {
        wikiServer = doc;
    }

    public XWikiDocument getWikiServer() {
        return wikiServer;
    }

    public int getCacheDuration() {
        return cacheDuration;
    }

    public void setCacheDuration(int cacheDuration) {
        this.cacheDuration = cacheDuration;
    }
}
