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

import com.opensymphony.module.access.AccessManager;
import com.opensymphony.module.access.NotFoundException;
import com.opensymphony.module.user.User;
import com.opensymphony.module.user.UserManager;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocInterface;
import com.xpn.xwiki.doc.XWikiSimpleDoc;
import com.xpn.xwiki.notify.PropertyChangedRule;
import com.xpn.xwiki.notify.XWikiNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.*;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.store.XWikiCache;
import com.xpn.xwiki.store.XWikiCacheInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.*;
import com.xpn.xwiki.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.ecs.Filter;
import org.apache.ecs.filter.CharacterFilter;
import org.apache.ecs.xhtml.textarea;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.velocity.VelocityContext;
import org.securityfilter.authenticator.Authenticator;
import org.securityfilter.authenticator.persistent.DefaultPersistentLoginManager;
import org.securityfilter.config.SecurityConfig;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.filter.URLPatternMatcher;
import org.securityfilter.realm.SecurityRealmInterface;


import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.FilterConfig;
import java.io.File;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.Principal;
import java.util.List;
import java.util.Map;

public class XWiki implements XWikiNotificationInterface {

    private XWikiConfig config;
    private XWikiStoreInterface store;
    private XWikiRenderingEngine renderingEngine;
    private XWikiPluginManager pluginManager;
    private XWikiNotificationManager notificationManager;

    private UserManager usermanager;
    private AccessManager accessmanager;
    private Authenticator authenticator;
    private SecurityRealmInterface realm;

    private MetaClass metaclass = MetaClass.getMetaClass();
    private boolean test = false;
    private String version = null;
    private HttpServlet servlet;
    private String database;

    private SecurityManager defaultSecurityManager;
    private SecurityManager secureSecurityManager;
    private URLPatternMatcher urlPatternMatcher = new URLPatternMatcher();

    public static XWiki getXWiki(XWikiContext context) throws XWikiException {
        String xwikicfg = "WEB-INF/xwiki.cfg";
        String xwikiname = "xwiki";

        HttpServlet servlet = context.getServlet();
        XWiki xwiki = (XWiki) servlet.getServletContext().getAttribute(xwikiname);

        if (xwiki == null) {
            String path = servlet.getServletContext().getRealPath(xwikicfg);
            xwiki = new XWiki(path, context, servlet);
            servlet.getServletContext().setAttribute(xwikiname, xwiki);
        }
        context.setWiki(xwiki);
        xwiki.setDatabase(context.getDatabase());
        if ("1".equals(xwiki.Param("xwiki.virtual"))) {
            HttpServletRequest request = context.getRequest();
            String host = "";
            try {
                String requestURL = getRequestURL(request);
                host = new URL(requestURL).getHost();
            } catch (Exception e) {};

            String appname = findWikiServer(host, context);

            if (appname==null) {
                String uri = request.getRequestURI();
                int i1 = host.indexOf(".");
                String servername = (i1!=-1) ? host.substring(0, i1) : host;
                appname = uri.substring(1,uri.indexOf("/",2));
                if ((servername.equals("www"))
                        ||(context.getUtil().match("m|[0-9]+\\.|[0-9]+\\.[0-9]+\\.[0-9]|", host))) {
                    if (appname.equals("xwiki"))
                        return xwiki;
                } else {
                    appname = servername;
                }
            }

            // Check if this appname exists in the Database
            String serverwikipage = getServerWikiPage(appname);
            XWikiDocInterface doc = xwiki.getDocument(serverwikipage, context);
            if (doc.isNew()) {
                throw new XWikiException(XWikiException.MODULE_XWIKI,
                        XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                        "This wiki does not exist");
            }
            context.setVirtual(true);
            context.setDatabase(appname);
        }
        return xwiki;
    }

    public static String getRequestURL(HttpServletRequest request) {
        StringBuffer requestURL = request.getRequestURL();
        if ((requestURL==null)&&(request instanceof MultipartRequestWrapper))
            requestURL = ((MultipartRequestWrapper) request).getRequest().getRequestURL();

        String qs = request.getQueryString();
        if ((qs!=null)&&(!qs.equals("")))
            return requestURL.toString() + "?" + qs;
        else
            return requestURL.toString();
    }

    private static String findWikiServer(String host, XWikiContext context) {
        String hql = ", BaseObject as obj, StringProperty as prop where obj.name=CONCAT(XWD_WEB,'.',XWD_NAME) "
                + "and obj.className='XWiki.XWikiServerClass' and prop.id.id = obj.id "
                + "and prop.id.name = 'server' and prop.value='" + host + "'";
        try {
            List list = context.getWiki().searchDocuments(hql, context);
            if ((list==null)||(list.size()==0))
                return null;
            String docname = (String) list.get(0);
            if (!docname.startsWith("XWiki.XWikiServer"))
                return null;
            String wikiserver = docname.substring("XWiki.XWikiServer".length());
            return wikiserver.toLowerCase();
        } catch (XWikiException e) {
            return null;
        }
    }

    public static String getServerWikiPage(String servername) {
        return "XWiki.XWikiServer"
                + servername.substring(0,1).toUpperCase()
                + servername.substring(1);
    }


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
        getUserClass(context);
        getGroupClass(context);
        getRightsClass(context);
        getGlobalRightsClass(context);

        // Initialize User Manager
        initUserManager();
        initAccessManager();

        // setDefaultSecurityManager(new XWikiSecurityManager(false));
        // setSecureSecurityManager(new XWikiSecurityManager(true));
        // System.setSecurityManager(getDefaultSecurityManager());
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

    public void saveDocument(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        getStore().saveXWikiDoc(doc, context);
    }

    public void saveDocument(XWikiDocInterface doc, XWikiDocInterface olddoc, XWikiContext context) throws XWikiException {
        getStore().saveXWikiDoc(doc, context);
        getNotificationManager().verify(doc, olddoc, 0, context);
    }

    private XWikiDocInterface getDocument(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        return getStore().loadXWikiDoc(doc, context);
    }

    public XWikiDocInterface getDocument(XWikiDocInterface doc, String revision, XWikiContext context) throws XWikiException {
        try {
            doc = getStore().loadXWikiDoc(doc, revision, context);
        }  catch (XWikiException e) {
            // TODO: log error for document version that does not exist.
        }
        return doc;
    }

    public XWikiDocInterface getDocument(String fullname, XWikiContext context) throws XWikiException {
        String server = null, web = null, name = null, database = null;
        try {

            int i0 = fullname.lastIndexOf(":");
            int i1 = fullname.lastIndexOf(".");

            if (i0!=-1) {
                server = fullname.substring(0,i0);
                web = fullname.substring(i0+1,i1);
                name = fullname.substring(i1+1);
            } else {
                server = null;
                web = fullname.substring(0,i1);
                name = fullname.substring(i1+1);
            }

            if (name.equals(""))
                name = "WebHome";

            if (server!=null) {
                database = context.getDatabase();
                context.setDatabase(server);
            }

            XWikiDocInterface doc = getDocument(new XWikiSimpleDoc(web, name), context);
            return doc;
        }
        finally {
            if ((server!=null)&&(database!=null)) {
                context.setDatabase(database);
            }
        }
    }

    public XWikiDocInterface getDocument(String web, String fullname, XWikiContext context) throws XWikiException {
        int i1 = fullname.lastIndexOf(".");
        if (i1!=-1) {
            String web2 = fullname.substring(0,i1);
            String name = fullname.substring(i1+1);
            if (name.equals(""))
                name = "WebHome";
            return getDocument(web2 + "." + name, context);
        } else {
            return getDocument(web + "." + fullname, context);
        }
    }


    public XWikiDocInterface getDocumentFromPath(String path, XWikiContext context) throws XWikiException {
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
        return getDocument(web + "." + name, context);
    }

    public String getBase(XWikiContext context) {
        return context.getBaseUrl();
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

    public String getXMLEncoded(String content) {
        Filter filter = new CharacterFilter();
        String scontent = filter.process(content);
        return scontent;
    }

    public String getTextArea(String content) {
        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String scontent = filter.process(content);

        textarea textarea = new textarea();
        textarea.setFilter(filter);
        textarea.setRows(20);
        textarea.setCols(80);
        textarea.setName("content");
        textarea.addElement(scontent);
        return textarea.toString();
    }

    public List getClassList(XWikiContext context) throws XWikiException {
        return getStore().getClassList(context);
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

    public List search(String wheresql, XWikiContext context) throws XWikiException {
        return getStore().search(wheresql, 0, 0, context);
    }

    public List search(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
        return getStore().search(wheresql, nb, start, context);
    }


    public List searchDocuments(String wheresql, XWikiContext context) throws XWikiException {
        return getStore().searchDocuments(wheresql, context);
    }

    public List searchDocuments(String wheresql, int nb, int start, XWikiContext context) throws XWikiException {
        return getStore().searchDocuments(wheresql, nb, start, context);
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public String parseContent(String content, XWikiContext context) {
        if ((content!=null)&&(!content.equals("")))
        // Let's use this template
            return XWikiVelocityRenderer.evaluate(content, "", (VelocityContext)context.get("vcontext"));
        else
            return "";
    }

    public String parseTemplate(String template, XWikiContext context) {
        String skin = "default";
        try {
            skin = getSkin(context);
        } catch (Exception e) {}

        try {
            String path = "/skins/" + skin + "/" + template;
            File file = new File(getRealPath(path));
            if (file.exists()) {
                String content = Util.getFileContent(file);
                return XWikiVelocityRenderer.evaluate(content, skin + "/" + template, (VelocityContext)context.get("vcontext"));
            }
        } catch (Exception e) {}

        try {
            XWikiDocInterface doc = getDocument(skin, context);
            if (!doc.isNew()) {
                BaseObject object = doc.getObject("XWiki.XWikiSkinClass", 0);
                if (object!=null) {
                    String content = object.getStringValue(template);
                    if ((content!=null)&&(!content.equals(""))) {
                        // Let's use this template
                        return XWikiVelocityRenderer.evaluate(content, skin + "/" + template, (VelocityContext)context.get("vcontext"));
                    }
                }
            }
        } catch (Exception e) {}

        try {
            File file = new File(getRealPath("/templates/" + template));
            String content = Util.getFileContent(file);
            return XWikiVelocityRenderer.evaluate(content, skin + "/" + template, (VelocityContext)context.get("vcontext"));
        } catch (Exception e) {
            return "";
        }
    }


    public String getSkinFile(String filename, XWikiContext context) {
        String skin = "default";
        try {
            skin = getSkin(context);
        } catch (Exception e) {}

        try {
            String path = "skins/" + skin + "/" + filename;
            File file = new File(getRealPath(path));
            if (file.exists())
                return context.getBaseUrl() + "../" + path;
        } catch (Exception e) {}

        try {
            XWikiDocInterface doc = getDocument(skin, context);
            if (!doc.isNew()) {
                BaseObject object = doc.getObject("XWiki.XWikiSkinClass", 0);
                if (object!=null) {
                    String content = object.getStringValue(filename);
                    if ((content!=null)&&(!content.equals(""))) {
                        return context.getBaseUrl() + "skin/" + StringUtils.replace(skin, ".","/", 1) + "/" + filename;
                    }
                }

                // Read XWikiAttachment
                XWikiAttachment attachment = null;
                List list = doc.getAttachmentList();
                String shortname = filename.substring(0, filename.indexOf(".")+1);
                attachment = doc.getAttachment(shortname);

                if (attachment!=null) {
                    return "../../skin/" + StringUtils.replace(skin, ".","/", 1) + "/" + attachment.getFilename();
                }

            }
        } catch (Exception e) {}

        return "../../../skins/default/" + filename;
    }


    public String getSkin(XWikiContext context) {
        try {
            // Try to get it from context
            String skin = (String) context.get("skin");
            if (skin!=null)
                return skin;

            // Try to get it from URL
            if (context.getRequest()!=null) {
                skin = context.getRequest().getParameter("skin");
                if ((skin!=null)&&(!skin.equals(""))) {
                    context.put("skin",skin);
                    return skin;
                }
            }

            XWikiDocInterface doc = getDocument("XWiki.XWikiPreferences", context);
            skin = ((BaseProperty)doc.getxWikiObject().get("skin")).getValue().toString();
            context.put("skin",skin);
            return skin;
        } catch (Exception e) {
            context.put("skin","default");
            return "default";
        }
    }

    public String getWebCopyright(XWikiContext context) {
        try {
            XWikiDocInterface doc = getDocument("XWiki.XWikiPreferences", context);
            return ((BaseProperty)doc.getxWikiObject().get("webcopyright")).getValue().toString();
        } catch (Exception e) {
            return "Copyright 2003,2004 (c) Ludovic Dubost";
        }
    }

    public String getXWikiPreference(String prefname, XWikiContext context) {
        try {
            XWikiDocInterface doc = getDocument("XWiki.XWikiPreferences", context);
            return ((BaseProperty)doc.getxWikiObject().get(prefname)).getValue().toString();
        } catch (Exception e) {
            return "";
        }
    }

    public String getWebPreference(String prefname, XWikiContext context) {
        try {
            XWikiDocInterface currentdoc = (XWikiDocInterface) context.get("doc");
            XWikiDocInterface doc = getDocument(currentdoc.getWeb() + ".WebPreferences", context);
            return ((BaseProperty)doc.getObject("XWiki.XWikiPreferences", 0).get(prefname)).getValue().toString();
        } catch (Exception e) {
            return getXWikiPreference(prefname, context);
        }
    }

    public String getUserPreference(String prefname, XWikiContext context) {
        try {
            return getWebPreference(prefname, context);
        } catch (Exception e) {
            return getXWikiPreference(prefname, context);
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

    public BaseClass getUserClass(XWikiContext context) throws XWikiException {
        XWikiDocInterface doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiUsers", context);
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
            saveDocument(doc, context);
        return bclass;
    }

    public BaseClass getGroupClass(XWikiContext context) throws XWikiException {
        XWikiDocInterface doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiGroups", context);
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
            saveDocument(doc, context);
        return bclass;
    }


    public BaseClass getRightsClass(String pagename,XWikiContext context) throws XWikiException {
        XWikiDocInterface doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki." + pagename, context);
        } catch (Exception e) {
            doc = new XWikiSimpleDoc();
            doc.setWeb("XWiki");
            doc.setName(pagename);
        }
        BaseClass bclass = doc.getxWikiClass();
        bclass.setName("XWiki." + pagename);
        if (bclass.get("users")==null) {
            needsUpdate = true;
            StringClass users_class = new StringClass();
            users_class.setName("users");
            users_class.setPrettyName("Users");
            users_class.setSize(80);
            users_class.setObject(bclass);
            bclass.put("Users", users_class);
        }
        if (bclass.get("groups")==null) {
            needsUpdate = true;
            StringClass groups_class = new StringClass();
            groups_class.setName("groups");
            groups_class.setPrettyName("Groups");
            groups_class.setSize(80);
            groups_class.setObject(bclass);
            bclass.put("groups", groups_class);
        }
        if (bclass.get("levels")==null) {
            needsUpdate = true;
            StringClass levels_class = new StringClass();
            levels_class.setName("levels");
            levels_class.setPrettyName("Access Levels");
            levels_class.setSize(80);
            levels_class.setObject(bclass);
            bclass.put("levels", levels_class);
        }
        if (bclass.get("allow")==null) {
            needsUpdate = true;
            NumberClass allow_class = new NumberClass();
            allow_class.setName("allow");
            allow_class.setPrettyName("Allow/Deny");
            allow_class.setSize(2);
            allow_class.setNumberType("integer");
            allow_class.setObject(bclass);
            bclass.put("allow", allow_class);
        }

        String content = doc.getContent();
        if ((content==null)||(content.equals("")))
            doc.setContent("---+ XWiki " + pagename);

        if (needsUpdate)
            saveDocument(doc, context);
        return bclass;
    }

    public BaseClass getRightsClass(XWikiContext context) throws XWikiException {
        return getRightsClass("XWikiRights", context);
    }

    public BaseClass getGlobalRightsClass(XWikiContext context) throws XWikiException {
        return getRightsClass("XWikiGlobalRights", context);
    }

    public void initUserManager() {
        UserManager um = UserManager.getInstance();
        ((XWikiUserProvider) um.getProfileProviders().toArray()[0]).setxWiki(this);
        ((XWikiGroupProvider) um.getAccessProviders().toArray()[0]).setxWiki(this);
        setUsermanager(um);
    }

    public void initAccessManager() {
        AccessManager am = AccessManager.getInstance();
        ((XWikiResourceProvider) am.getResourceProviders().toArray()[0]).setxWiki(this);
        setAccessmanager(am);
    }

    public int createUser(XWikiContext context) throws XWikiException {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        Map map = Util.getObject(request, "register");
        String xwikiname = request.getParameter("xwikiname");
        String password2 = request.getParameter("register2_password");
        String password = ((String[])map.get("password"))[0];

        if (!password.equals(password2)) {
            // TODO: throw wrong password exception
            return -1;
        }

        return createUser(xwikiname, map, context);
    }

    public int createUser(String xwikiname, Map map, XWikiContext context) throws XWikiException {
        BaseClass baseclass = getUserClass(context);

        try {
            // TODO: Verify existing user
            XWikiDocInterface doc = getDocument("XWiki." + xwikiname, context);
            if (!doc.isNew()) {
                // TODO: throws Exception
                return 0;
            }

            BaseObject newobject = (BaseObject) baseclass.fromMap(map);
            newobject.setName("XWiki." + xwikiname);
            doc.addObject(baseclass.getName(), newobject);
            saveDocument(doc, null, context);
            return 1;
        }
        catch (Exception e) {
            Object[] args = { "XWiki." + xwikiname };
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
            if ("basic".equals(Param("xwiki.authentication"))) {
                authenticator = new MyBasicAuthenticator();
                realm = new XWikiRealmAdapter(this);
                SecurityConfig sconfig = new SecurityConfig(false);
                sconfig.addRealm(realm);
                sconfig.setAuthMethod("BASIC");
                if (Param("xwiki.authentication.realname")!=null)
                    sconfig.setRealmName(Param("xwiki.authentication.realname"));
                else
                    sconfig.setRealmName("XWiki");
                authenticator.init(null, sconfig);
            } else {
                authenticator =  new MyFormAuthenticator();
                realm = new XWikiRealmAdapter(this);
                SecurityConfig sconfig = new SecurityConfig(false);
                sconfig.setAuthMethod("FORM");
                sconfig.addRealm(realm);
                if (Param("xwiki.authentication.realname")!=null)
                    sconfig.setRealmName(Param("xwiki.authentication.realname"));
                else
                    sconfig.setRealmName("XWiki");
                if (Param("xwiki.authentication.defaultpage")!=null)
                    sconfig.setDefaultPage(Param("xwiki.authentication.defaultpage"));
                else
                    sconfig.setDefaultPage("/bin/view/Main/WebHome");
                if (Param("xwiki.authentication.loginpage")!=null)
                    sconfig.setLoginPage(Param("xwiki.authentication.loginpage"));
                else
                    sconfig.setLoginPage("/bin/login/XWiki/XWikiLogin");
                if (Param("xwiki.authentication.logoutpage")!=null)
                    sconfig.setLogoutPage(Param("xwiki.authentication.logoutpage"));
                else
                    sconfig.setLogoutPage("/bin/logout/XWiki/XWikiLogout");
                if (Param("xwiki.authentication.errorpage")!=null)
                    sconfig.setErrorPage(Param("xwiki.authentication.errorpage"));
                else
                    sconfig.setErrorPage("/bin/loginerror/XWiki/XWikiLoginError");


                MyPersistentLoginManager persistent = new MyPersistentLoginManager();
                if (Param("xwiki.authentication.cookiepath")!=null)
                    persistent.setCookieLife(Param("xwiki.authentication.cookiepath"));
                if (Param("xwiki.authentication.cookielife")!=null)
                    persistent.setCookieLife(Param("xwiki.authentication.cookielife"));
                if (Param("xwiki.authentication.protection")!=null)
                    persistent.setProtection(Param("xwiki.authentication.protection"));
                if (Param("xwiki.authentication.useip")!=null)
                    persistent.setUseIP(Param("xwiki.authentication.useip"));
                if (Param("xwiki.authentication.encryptionalgorithm")!=null)
                    persistent.setEncryptionAlgorithm(Param("xwiki.authentication.encryptionalgorithm"));
                if (Param("xwiki.authentication.encryptionmode")!=null)
                    persistent.setEncryptionMode(Param("xwiki.authentication.encryptionmode"));
                if (Param("xwiki.authentication.encryptionpadding")!=null)
                    persistent.setEncryptionPadding(Param("xwiki.authentication.encryptionpadding"));
                if (Param("xwiki.authentication.validationKey")!=null)
                    persistent.setValidationKey(Param("xwiki.authentication.validationKey"));
                if (Param("xwiki.authentication.encryptionKey")!=null)
                    persistent.setEncryptionKey(Param("xwiki.authentication.encryptionKey"));
                sconfig.setPersistentLoginManager(persistent);

                MyFilterConfig fconfig = new MyFilterConfig();
                if (Param("xwiki.authentication.loginsubmitpage")!=null)
                    fconfig.setInitParameter("loginSubmitPattern", Param("xwiki.authentication.loginsubmitpage"));
                else
                    fconfig.setInitParameter("loginSubmitPattern", "/bin/login/XWiki/XWikiLogin");
                authenticator.init(fconfig, sconfig);
            }

            return authenticator;
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER,
                    XWikiException.ERROR_XWIKI_USER_INIT,
                    "Cannot initialize authentication system",e);
        }
    }

    public boolean checkPassword(String username, String password, XWikiContext context) throws XWikiException {
        try {
            XWikiDocInterface doc = getDocument(username, context);
            String passwd = ((BaseProperty)doc.getObject("XWiki.XWikiUsers", 0).get("password")).toText();
            return (password.equals(passwd));
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }


    public Principal checkAuth(XWikiContext context) throws XWikiException {
        HttpServletRequest request = context.getRequest();
        HttpServletResponse response = context.getResponse();

        if (request==null)
            return null;

        Authenticator auth = getAuthenticator();
        SecurityRequestWrapper wrappedRequest = new SecurityRequestWrapper(request, null,
                realm, auth.getAuthMethod());
        try {

            // Process login out (this only works with FORMS
            if (auth.processLogout(wrappedRequest, response, urlPatternMatcher)) {
                wrappedRequest.setUserPrincipal(null);
                return null;
            }

            // In case of virtual server we never use osuser
            // because the fact that it is "static" doesn't
            // allow to dynamically switch databases
            if ((Param("xwiki.virtual").equals("1"))
                    || !Param("xwiki.authentication.osuser").equals("1")) {
                if (auth.getAuthMethod().equals("BASIC")) {
                       if (((MyBasicAuthenticator)auth).processLogin(wrappedRequest, response, context)) {
                         return null;
                    }
                } else {
                       if (((MyFormAuthenticator)auth).processLogin(wrappedRequest, response, context)) {
                         return null;
                    }
                }
            } else {
                if (auth.processLogin(wrappedRequest, response)) {
                    return null;
                }
            }

            Principal user = wrappedRequest.getUserPrincipal();
            return user;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean checkAccess(String action, XWikiDocInterface doc, XWikiContext context)
            throws XWikiException {
        Principal user = null;
        boolean needsAuth = false;
        String right = "edit";

        if (action.equals("view")||(action.equals("plain")||(action.equals("raw"))))
            right = "view";

        if (action.equals("skin")||(action.equals("download"))||(action.equals("logout"))||(action.equals("login")))
            right = "view";


        needsAuth = getXWikiPreference("authenticate_" + right, context).toLowerCase().equals("yes");
        if (!needsAuth)
            needsAuth = getWebPreference("authenticate_" + right, context).toLowerCase().equals("yes");

        try {
            user = checkAuth(context);

            if ((user==null)&&(needsAuth)) {
                try {
                    if (context.getRequest()!=null)
                        getAuthenticator().showLogin(context.getRequest(), context.getResponse());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        } catch (XWikiException e) {
            if (needsAuth)
                throw e;
        }


        String username;
        if (user==null)
            username = "XWiki.XWikiGuest";
        else
            username = "XWiki." + user.getName();

        // Save the user
        context.setUser(username);

        // This correspond to the login/logout/loginerror documents
        if (doc==null)
            return true;

        // Check Rights
        try {
            // Verify access rights and return if ok
            String docname;
            if (context.getDatabase()!=null) {
                docname = context.getDatabase() + ":" + doc.getFullName();
                username = context.getDatabase() + ":" + username;
            }
            else
                docname = doc.getFullName();

            if (getAccessmanager().userHasAccessLevel(username, docname, right))
                return true;
        } catch (NotFoundException e) {
            // This should not happen..
            e.printStackTrace();
            return false;
        }

        if (user==null) {
            // Denied Guest need to be authenticated
            try {
                if (context.getRequest()!=null)
                    getAuthenticator().showLogin(context.getRequest(), context.getResponse());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        else {
            // Other user is refused
            return false;
        }
    }

    public UserManager getUsermanager() {
        return usermanager;
    }

    public void setUsermanager(UserManager usermanager) {
        this.usermanager = usermanager;
    }

    public AccessManager getAccessmanager() {
        return accessmanager;
    }

    public void setAccessmanager(AccessManager accessmanager) {
        this.accessmanager = accessmanager;
    }

    public String includeTopic(String topic, XWikiContext context) {
        return include(topic, context, false);
    }

    public String includeForm(String topic, XWikiContext context) {
        return include(topic, context, true);
    }

    public String include(String topic, XWikiContext context, boolean isForm) {
        String database = null, incdatabase = null;
        Document currentdoc = null, currentcdoc = null;
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        if (vcontext!=null) {
            currentdoc = (Document) vcontext.get("doc");
            currentcdoc = (Document) vcontext.get("cdoc");
        }

        try {

            XWikiDocInterface doc = null;
            try {

                int i0 = topic.indexOf(":");
                if (i0!=-1) {
                    incdatabase = topic.substring(0,i0);
                    topic = topic.substring(i0+1);
                    database = context.getDatabase();
                    context.setDatabase(incdatabase);
                }

                doc = getDocument(((XWikiDocInterface) context.get("doc")).getWeb(), topic, context);

                if (checkAccess("view", doc, context)==false) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                            XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                            "Access to this document is denied");
                }

            } catch (XWikiException e) {
                return "Topic " + topic + " does not exist";
            }

            if (isForm) {
                // We do everything in the context of the including document
                if (database!=null)
                    context.setDatabase(database);

                return "{pre}" + getRenderingEngine().renderDocument(doc, (XWikiDocInterface)context.get("doc"), context) + "{/pre}";
            }
            else {
                // We stay in the context included document
                return "{pre}" + getRenderingEngine().renderDocument(doc, context) + "{/pre}";
            }
        } finally {
            if (database!=null)
                context.setDatabase(database);
            if (currentdoc!=null)
                vcontext.put("doc", currentdoc);
            if (currentcdoc!=null)
                vcontext.put("cdoc", currentcdoc);
        }
    }

    public void deleteDocument(XWikiDocInterface doc, XWikiContext context) throws XWikiException {
        getStore().deleteXWikiDoc(doc, context);
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public void gc() {
        System.gc();
    }

    public long freeMemory() {
        return Runtime.getRuntime().freeMemory();
    }

    public long totalMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    public long maxMemory() {
        return Runtime.getRuntime().maxMemory();
    }

    public Object getPrivateField(Object obj, String fieldName) {
        // SecurityManager secmng = null;
        try {
            // secmng = System.getSecurityManager();
            // System.setSecurityManager(getDefaultSecurityManager());
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } finally {
            // System.setSecurityManager(secmng);
        }
    }

    public Object callPrivateMethod(Object obj, String methodName) {
        return callPrivateMethod(obj, methodName, null, null);
    }

    public Object callPrivateMethod(Object obj, String methodName, Class[] classes, Object[] args) {
        // SecurityManager secmng = null;
        try {
            // secmng = System.getSecurityManager();
            // System.setSecurityManager(getDefaultSecurityManager());
            Method method = obj.getClass().getDeclaredMethod(methodName, classes);
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } catch (NoSuchMethodException e) {
            return null;
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            return null;
        } finally {
            // System.setSecurityManager(secmng);
        }

    }

    public SecurityManager getDefaultSecurityManager() {
        return defaultSecurityManager;
    }

    public void setDefaultSecurityManager(SecurityManager defaultSecurityManager) {
        this.defaultSecurityManager = defaultSecurityManager;
    }

    public SecurityManager getSecureSecurityManager() {
        return secureSecurityManager;
    }

    public void setSecureSecurityManager(SecurityManager secureSecurityManager) {
        this.secureSecurityManager = secureSecurityManager;
    }

    public String[] split(String str, String sep) {
        return StringUtils.split(str, sep);
    }

    public String printStrackTrace(Throwable e) {
        StringWriter strwriter = new StringWriter();
        PrintWriter writer = new PrintWriter(strwriter);
        e.printStackTrace(writer);
        return strwriter.toString();
    }
}
