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
 * Time: 13:52:39
 */

package com.xpn.xwiki;

import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiCache;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.objects.classes.*;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationInterface;
import com.xpn.xwiki.notify.PropertyChangedRule;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.user.XWikiUserProvider;
import com.xpn.xwiki.user.XWikiGroupProvider;
import com.xpn.xwiki.util.Util;
import com.opensymphony.user.UserManager;
import com.opensymphony.user.ImmutableException;
import com.opensymphony.user.DuplicateEntityException;
import com.opensymphony.user.User;
import com.opensymphony.user.adapter.catalina.OSUserRealm;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.IOException;
import java.security.Principal;

import org.apache.ecs.html.TextArea;
import org.apache.ecs.filter.CharacterFilter;
import org.apache.ecs.Filter;
import org.securityfilter.authenticator.BasicAuthenticator;
import org.securityfilter.authenticator.Authenticator;
import org.securityfilter.filter.SavedRequest;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.realm.SecurityRealmInterface;
import org.securityfilter.realm.catalina.CatalinaRealmAdapter;
import org.securityfilter.config.SecurityConfig;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class XWiki implements XWikiNotificationInterface {

    private XWikiConfig config;
    private XWikiStoreInterface store;
    private XWikiRenderingEngine renderingEngine;
    private XWikiPluginManager pluginManager;
    private XWikiNotificationManager notificationManager;

    private UserManager usermanager;
    private Authenticator authenticator;
    private SecurityRealmInterface realm;

    private MetaClass metaclass = MetaClass.getMetaClass();
    private boolean test = false;
    private String version = null;
    private HttpServlet servlet;

    public XWiki(String path, XWikiContext context) throws XWikiException {
        this(path,context,null);
    }

    public XWiki(String path, XWikiContext context, HttpServlet servlet) throws XWikiException {
        // Important to have these in the context
        setServlet(servlet);
        context.setWiki(this);

        // Create the notification manager
        setNotificationManager(new XWikiNotificationManager());

        // Prepare the store
        XWikiStoreInterface basestore;
        setConfig(new XWikiConfig(path));
        String storeclass = Param("xwiki.store.class","com.xpn.xwiki.store.XWikiRCSFileStore");
        try {
            Class[] classes = new Class[2];
            classes[0] = this.getClass();
            classes[1] = context.getClass();
            Object[] args = new Object[2] ;
            args[0] = this;
            args[1] = context;
            basestore = (XWikiStoreInterface)Class.forName(storeclass).getConstructor(classes).newInstance(args);
        }
        catch (InvocationTargetException e)
        {
            Object[] args = { storeclass };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR,
                    "Cannot load store class {0}",e.getTargetException(), args);
        } catch (Exception e) {
            Object[] args = { storeclass };
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR,
                    "Cannot load store class {0}",e, args);
        }

        // Check if we need to use the cache store..
        boolean nocache = "0".equals(Param("xwiki.store.cache", "1"));
        if (!nocache)
            setStore(new XWikiCache(basestore));
        else
            setStore(basestore);

        // Prepare the Rendering Engine
        setRenderingEngine(new XWikiRenderingEngine(this));

        // Prepare the Plugin Engine
        setPluginManager(new XWikiPluginManager(getXWikiPreference("plugins", context), context));
        // Add a notification rule if the preference property plugin is modified
        getNotificationManager().addNamedRule("XWiki.XWikiPreferences",
                new PropertyChangedRule(this, "XWiki.XWikiPreferences", "plugin"));

        // Make sure these classes exists
        getUserClass();
        getGroupClass();

        // Initialize User Manager
        usermanager = UserManager.getInstance();
        ((XWikiUserProvider) usermanager.getProfileProviders().toArray()[0]).setxWiki(this);
        ((XWikiGroupProvider) usermanager.getAccessProviders().toArray()[0]).setxWiki(this);
    }

    public String getVersion() {
        if (version==null) {
            version = Param("xwiki.version",  "");
            String bnb = null;
            try {
                XWikiConfig vprop = new XWikiConfig(getRealPath("WEB-INF/version.properties"));
                bnb = vprop.getProperty("build.number");
            } catch (Exception e) {}
            if (bnb!=null)
                version = version + "." + bnb;            
        }
        return version;
    }

    public XWikiConfig getConfig() {
        return config;
    }

    public String getRealPath(String path) {
        return getServlet().getServletContext().getRealPath(path);
    }

    public String Param(String key) {
        return getConfig().getProperty(key);
    }

    public String ParamAsRealPath(String key) {
        String param = Param(key);
        try {

            return getRealPath(param);
        } catch (Exception e) {
            return param;
        }
    }

    public String ParamAsRealPath(String key, XWikiContext context) {
        return ParamAsRealPath(key);
    }

    public String ParamAsRealPathVerified(String param) {
        String path;
        File fpath;

        path = Param(param);
        if (path==null)
            return null;

        fpath = new File(path);
        if (fpath.exists()) {
            return path;
        }

        path = getRealPath(path);
        if (path==null)
            return null;

        fpath = new File(path);
        if (fpath.exists()) {
            return path;
        } else {
        }
        return null;
    }


    public String Param(String key, String default_value) {
        return getConfig().getProperty(key, default_value);
    }

    public XWikiStoreInterface getStore() {
        return store;
    }

    public void saveDocument(XWikiDocInterface doc) throws XWikiException {
        getStore().saveXWikiDoc(doc);
    }

    public void saveDocument(XWikiDocInterface doc, XWikiDocInterface olddoc, XWikiContext context) throws XWikiException {
        getStore().saveXWikiDoc(doc);
        getNotificationManager().verify(doc, olddoc, 0, context);
    }

    public XWikiDocInterface getDocument(XWikiDocInterface doc) throws XWikiException {
        try {
            doc = getStore().loadXWikiDoc(doc);
        }  catch (XWikiException e) {
            throw e;
        }
        return doc;
    }

    public XWikiDocInterface getDocument(XWikiDocInterface doc, String revision) throws XWikiException {
        try {
            doc = getStore().loadXWikiDoc(doc, revision);
        }  catch (XWikiException e) {
            // TODO: log error for document version that does not exist.
        }
        return doc;
    }

    public XWikiDocInterface getDocument(String web, String name) throws XWikiException {
        XWikiSimpleDoc doc = new XWikiSimpleDoc(web, name);
        return getDocument(doc);
    }

    public XWikiDocInterface getDocument(String fullname) throws XWikiException {
        int i1 = fullname.lastIndexOf(".");
        String web = fullname.substring(0,i1);
        String name = fullname.substring(i1+1);
        if (name.equals(""))
            name = "WebHome";
        return getDocument(web,name);
    }

    public XWikiDocInterface getDocumentFromPath(String path) throws XWikiException {
        String web, name;
        int i1 = path.indexOf("/",1);
        int i2 = path.indexOf("/", i1+1);
        int i3 = path.indexOf("/", i2+1);
        web = path.substring(i1+1,i2);
        if (i3==-1)
         name = path.substring(i2+1);
        else
         name = path.substring(i2+1,i3);
        if (name.equals(""))
            name = "WebHome";
        return getDocument(web,name);
    }

    public String getBase() {
        return Param("xwiki.base","../../");
    }

    public XWikiRenderingEngine getRenderingEngine() {
        return renderingEngine;
    }

    public void setRenderingEngine(XWikiRenderingEngine renderingEngine) {
        this.renderingEngine = renderingEngine;
    }

    public MetaClass getMetaclass() {
        return metaclass;
    }

    public void setMetaclass(MetaClass metaclass) {
        this.metaclass = metaclass;
    }

    public String getFormEncoded(String content) {
        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String scontent = filter.process(content);
        return scontent;
    }

    public String getTextArea(String content) {
        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String scontent = filter.process(content);

        TextArea textarea = new TextArea();
        textarea.setFilter(filter);
        textarea.setRows(20);
        textarea.setCols(80);
        textarea.setName("content");
        textarea.addElement(scontent);
        return textarea.toString();
    }

    public List getClassList() throws XWikiException {
        return getStore().getClassList();
    }
    /*
    public String[] getClassList() throws XWikiException {
    List list = store.getClassList();
    String[] array = new String[list.size()];
    for (int i=0;i<list.size();i++)
    array[i] = (String)list.get(i);
    return array;
    }
    */

    public List searchDocuments(String wheresql) throws XWikiException {
        return getStore().searchDocuments(wheresql);
    }

    public List searchDocuments(String wheresql, int nb, int start) throws XWikiException {
        return getStore().searchDocuments(wheresql, nb, start);
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public String getTemplate(String template, XWikiContext context) {
        try {
            String skin = getSkin(context);
            String path = "/skins/" + skin + "/" + template;
            File file = new File(getRealPath(path));
            if (file.exists())
                return path;
        } catch (Exception e) {
        }
        return "/templates/" + template;
    }

    public String getSkin(XWikiContext context) {
        try {
            // Try to get it from context
            String skin = (String) context.get("skin");
            if (skin!=null)
                return skin;

            XWikiDocInterface doc = getDocument("XWiki.XWikiPreferences");
            skin = doc.getxWikiObject().get("skin").toString();
            context.put("skin",skin);
            return skin;
        } catch (Exception e) {
            context.put("skin","default");
            return "default";
        }
    }

    public String getWebCopyright(XWikiContext context) {
        try {
            XWikiDocInterface doc = getDocument("XWiki.XWikiPreferences");
            return doc.getxWikiObject().get("webcopyright").toString();
        } catch (Exception e) {
            return "Copyright 2003,2004 (c) Ludovic Dubost";
        }
    }

    public String getXWikiPreference(String prefname, XWikiContext context) {
        try {
            XWikiDocInterface doc = getDocument("XWiki.XWikiPreferences");
            return doc.getxWikiObject().get(prefname).toString();
        } catch (Exception e) {
            return "";
        }
    }

    public String getWebPreference(String prefname, XWikiContext context) {
        try {
            XWikiDocInterface currentdoc = (XWikiDocInterface) context.get("doc");
            XWikiDocInterface doc = getDocument(currentdoc.getWeb() + ".WebPreferences");
            return doc.getxWikiObject().get(prefname).toString();
        } catch (Exception e) {
            return getXWikiPreference(prefname, context);
        }
    }

    public String getUserPreference(String prefname, XWikiContext context) {
        try {
            // XWikiUser user = (XWikiUser) context.get("user");
            // return user.getPreference(prefname, context);
            return getWebPreference(prefname, context);
        } catch (Exception e) {
            return getWebPreference(prefname, context);
        }
    }

    public void flushCache() {
        if (getStore() instanceof XWikiCacheInterface) {
            ((XWikiCacheInterface)getStore()).flushCache();
        }
    }

    public HttpServlet getServlet() {
        return servlet;
    }

    public void setServlet(HttpServlet servlet) {
        this.servlet = servlet;
    }

    public XWikiPluginManager getPluginManager() {
        return pluginManager;
    }

    public void setPluginManager(XWikiPluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void setConfig(XWikiConfig config) {
        this.config = config;
    }

    public void setStore(XWikiStoreInterface store) {
        this.store = store;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public XWikiNotificationManager getNotificationManager() {
        return notificationManager;
    }

    public void setNotificationManager(XWikiNotificationManager notificationManager) {
        this.notificationManager = notificationManager;
    }

    public void notify(XWikiNotificationRule rule, XWikiDocInterface newdoc, XWikiDocInterface olddoc, int event, XWikiContext context) {
        if (newdoc.getFullName().equals("XWiki.XWikiPreferences")) {
            setPluginManager(new XWikiPluginManager(getXWikiPreference("plugins", context), context));
        }
    }

    public BaseClass getUserClass() throws XWikiException {
        XWikiDocInterface doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiUsers");
        } catch (Exception e) {
            doc = new XWikiSimpleDoc();
            doc.setWeb("XWiki");
            doc.setName("XWikiUsers");
        }
        BaseClass bclass = doc.getxWikiClass();
        bclass.setName("XWiki.XWikiUsers");
        if (bclass.get("fullname")==null) {
            needsUpdate = true;
            StringClass fullname_class = new StringClass();
            fullname_class.setName("fullname");
            fullname_class.setPrettyName("Full Name");
            fullname_class.setSize(30);
            fullname_class.setObject(bclass);
            bclass.put("fullname", fullname_class);
        }

        if (bclass.get("email")==null) {
            needsUpdate = true;
            StringClass email_class = new StringClass();
            email_class.setName("email");
            email_class.setPrettyName("e-Mail");
            email_class.setSize(30);
            email_class.setObject(bclass);
            bclass.put("email", email_class);
        }

        if (bclass.get("password")==null) {
            needsUpdate = true;
            PasswordClass passwd_class = new PasswordClass();
            passwd_class.setName("password");
            passwd_class.setPrettyName("Password");
            passwd_class.setSize(10);
            passwd_class.setObject(bclass);
            bclass.put("password", passwd_class);
        }

        if (bclass.get("comment")==null) {
            needsUpdate = true;
            TextAreaClass comment_class = new TextAreaClass();
            comment_class.setName("comment");
            comment_class.setPrettyName("Comment");
            comment_class.setSize(40);
            comment_class.setRows(5);
            comment_class.setObject(bclass);
            bclass.put("comment", comment_class);
        }

        String content = doc.getContent();
        if ((content==null)||(content.equals("")))
            doc.setContent("---+ XWiki Users");

        if (needsUpdate)
            saveDocument(doc);
        return bclass;
    }

    public BaseClass getGroupClass() throws XWikiException {
        XWikiDocInterface doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiGroups");
        } catch (Exception e) {
            doc = new XWikiSimpleDoc();
            doc.setWeb("XWiki");
            doc.setName("XWikiGroups");
        }
        BaseClass bclass = doc.getxWikiClass();
        bclass.setName("XWiki.XWikiGroups");
        if (bclass.get("member")==null) {
            needsUpdate = true;
            StringClass member_class = new StringClass();
            member_class.setName("member");
            member_class.setPrettyName("Member");
            member_class.setSize(30);
            member_class.setObject(bclass);
            bclass.put("member", member_class);
        }

        String content = doc.getContent();
        if ((content==null)||(content.equals("")))
            doc.setContent("---+ XWiki Groups");

        if (needsUpdate)
            saveDocument(doc);
        return bclass;
    }

    public UserManager getUserManager() {
        UserManager um = UserManager.getInstance();
        ((XWikiUserProvider) um.getProfileProviders().toArray()[0]).setxWiki(this);
        ((XWikiGroupProvider) um.getAccessProviders().toArray()[0]).setxWiki(this);
        return um;
    }

    public int createUser(XWikiContext context) throws XWikiException {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        BaseClass baseclass = getUserClass();
        String xwikiname = request.getParameter("xwikiname");
        String password2 = request.getParameter("register2_password");

        try {
            // TODO: Verify existing user
            XWikiDocInterface doc = getDocument("XWiki." + xwikiname);
            if (!doc.isNew()) {
                // TODO: throws Exception
                return 0;
            }
            // TODO: Verify XWiki Name


            // Read map
            Map map = Util.getObject(request, "register");
            String password = ((String[])map.get("password"))[0];
            if (!password.equals(password2)) {
                // TODO: throw wrong password exception
                return -1;
            }

            BaseObject newobject = (BaseObject) baseclass.fromMap(map);
            newobject.setName(xwikiname);
            doc.addObject(baseclass.getName(), newobject);
            saveDocument(doc, null, context);

            // TODO: send a notification email
            return 1;
        }
        catch (Exception e) {
            Object[] args = { xwikiname };
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER,
                    XWikiException.ERROR_XWIKI_USER_CREATE,
                    "Cannot create user {0}", e, args);
        }
    }

    public User getUser(XWikiContext context) {
        return null;
    }


    public Authenticator getAuthenticator() throws XWikiException {
        if (authenticator!=null)
            return authenticator;

        try {
            authenticator = new BasicAuthenticator();
            realm = new CatalinaRealmAdapter();
            ((CatalinaRealmAdapter)realm).setRealm(new OSUserRealm());
            SecurityConfig sconfig = new SecurityConfig(false);
            sconfig.setRealmName("XWiki");
            sconfig.addRealm(realm);
            sconfig.setAuthMethod("BASIC");
            authenticator.init(null, sconfig);
            return authenticator;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER,
                    XWikiException.ERROR_XWIKI_USER_INIT,
                    "Cannot initialize authentication system",e);
        }
    }

    public Principal checkAuth(XWikiContext context) throws XWikiException {
        HttpServletRequest request = context.getRequest();
        HttpServletResponse response = context.getResponse();
        Authenticator auth = getAuthenticator();
        SecurityRequestWrapper wrappedRequest = new SecurityRequestWrapper(request, null,
                realm, auth.getAuthMethod());
        try {
            if (auth.processLogin(wrappedRequest, response)) {
                return null;
            }
            else {
                Principal user = wrappedRequest.getUserPrincipal();
                if (user==null)
                    auth.showLogin(request, response);
                else {
                    return user;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            try {
                auth.showLogin(request, response);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

    public boolean checkAccess(String action, XWikiDocInterface doc, XWikiContext context)
                    throws XWikiException {
        boolean needsAuth = false;
        if (action.equals("view")) {
            needsAuth = getXWikiPreference("authenticate_view", context).toLowerCase().equals("yes");
        } else {
            needsAuth = getXWikiPreference("authenticate_edit", context).toLowerCase().equals("yes");
        }
        if (needsAuth) {
            Principal user = checkAuth(context);
            if (user==null) {
                return false;
            }
            // Save the user
            context.put("user", user);
        }

        // Now we can verify the rights

        return true;
    }


}
