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
 * Date: 26 nov. 2003
 * Time: 13:52:39
 */

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
 * Date: 26 nov. 2003
 * Time: 13:52:39
 */

package com.xpn.xwiki;

import com.opensymphony.module.access.AccessManager;
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
import com.xpn.xwiki.test.Utils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ecs.Filter;
import org.apache.ecs.filter.CharacterFilter;
import org.apache.ecs.xhtml.textarea;
import org.apache.struts.upload.MultipartRequestWrapper;
import org.apache.velocity.VelocityContext;
import org.apache.tomcat.util.http.Cookies;
import org.securityfilter.authenticator.Authenticator;
import org.securityfilter.config.SecurityConfig;
import org.securityfilter.filter.SecurityRequestWrapper;
import org.securityfilter.filter.URLPatternMatcher;
import org.securityfilter.realm.SecurityRealmInterface;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.Principal;
import java.util.*;

public class XWiki implements XWikiNotificationInterface {
    private static final Log log = LogFactory.getLog(XWiki.class);


    private XWikiConfig config;
    private XWikiStoreInterface store;
    private XWikiRenderingEngine renderingEngine;
    private XWikiPluginManager pluginManager;
    private XWikiNotificationManager notificationManager;
    private Authenticator authenticator;
    private SecurityRealmInterface realm;

    private MetaClass metaclass = MetaClass.getMetaClass();
    private boolean test = false;
    private String version = null;
    private HttpServlet servlet;
    private String database;

    private URLPatternMatcher urlPatternMatcher = new URLPatternMatcher();

    public static XWiki getMainXWiki(XWikiContext context) throws XWikiException {
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
        return xwiki;
    }

    public static XWiki getXWiki(XWikiContext context) throws XWikiException {
        XWiki xwiki = getMainXWiki(context);

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

    public long ParamAsLong(String key) {
        String param = getConfig().getProperty(key);
        return Long.parseLong(param);
    }

    public long ParamAsLong(String key, long default_value) {
        try {
            return ParamAsLong(key);
        } catch (NumberFormatException e) {
            return default_value;
        }
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
        String server = null, database = null;
        try {
            XWikiDocInterface doc = new XWikiSimpleDoc();
            doc.setFullName(fullname, context);
            server = doc.getDatabase();

            if (server!=null) {
                database = context.getDatabase();
                context.setDatabase(server);
            }

            doc = getDocument(doc, context);
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

    public static String getFormEncoded(String content) {
        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String scontent = filter.process(content);
        return scontent;
    }

    public static String getXMLEncoded(String content) {
        Filter filter = new CharacterFilter();
        String scontent = filter.process(content);
        return scontent;
    }

    public static String getTextArea(String content) {
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
        return getXWikiPreference(prefname, "", context);
    }

    public String getXWikiPreference(String prefname, String default_value, XWikiContext context) {
        try {
            XWikiDocInterface doc = getDocument("XWiki.XWikiPreferences", context);
            return ((BaseProperty)doc.getxWikiObject().get(prefname)).getValue().toString();
        } catch (Exception e) {
            return default_value;
        }
    }

    public String getWebPreference(String prefname, XWikiContext context) {
        return getWebPreference(prefname, "", context);
    }

    public String getWebPreference(String prefname, String default_value, XWikiContext context) {
        try {
            XWikiDocInterface currentdoc = (XWikiDocInterface) context.get("doc");
            XWikiDocInterface doc = getDocument(currentdoc.getWeb() + ".WebPreferences", context);
            String result = doc.getStringValue("XWiki.XWikiUsers", prefname);
            if (!result.equals(""))
                return result;
        } catch (Exception e) {
        }
        return getXWikiPreference(prefname, default_value, context);
    }

    public String getUserPreference(String prefname, XWikiContext context) {
        try {
             String user = context.getUser();
             XWikiDocInterface userdoc = getDocument(user, context);
             if (userdoc!=null) {
                String result = userdoc.getStringValue("XWiki.XWikiUsers", prefname);
                if (!result.equals(""))
                     return result;
             }
            }
        catch (Exception e) {
        }

        return getWebPreference(prefname, context);
    }


    public String getUserPreferenceFromCookie(String prefname, XWikiContext context) {
        Cookie[] cookies = context.getRequest().getCookies();
        if (cookies==null)
            return null;
        for (int i=0;i<cookies.length;i++) {
            String name = cookies[i].getName();
            if (name.equals(prefname)) {
                String value = cookies[i].getValue();
                if (!value.trim().equals(""))
                    return value;
                else
                    break;
            }
        }
        return null;
    }

    public String getUserPreference(String prefname, boolean useCookie, XWikiContext context) {
        // First we look in the cookies
        if (useCookie) {
            String result = getUserPreferenceFromCookie(prefname, context);
            if (result!=null)
                return result;
        }
        return getUserPreference(prefname, context);
    }

    public String getLanguagePreference(XWikiContext context) {
        // First we get the language from the request
        String result;
        try {
            result = context.getRequest().getParameter("language");
            if ((result!=null)&&(!result.equals("")))
              return result;
        } catch (Exception e) {}

        try {
        // First we get the language from the cookie
        result = getUserPreferenceFromCookie("language", context);
        if ((result!=null)&&(!result.equals("")))
            return result;
        } catch (Exception e) {}

        // Next from the default user preference
        try {
            String user = context.getUser();
            XWikiDocInterface userdoc = null;
            userdoc = getDocument(user, context);
            if (userdoc!=null) {
                 result = userdoc.getStringValue("XWiki.XWikiUsers", "default_language");
                    if (!result.equals(""))
                         return result;
                 }
        } catch (XWikiException e) {
        }

        // Then from the navigator language setting
        if (context.getRequest()!=null) {
            String accept = context.getRequest().getHeader("Accept-Language");
            if ((accept==null)||(accept.equals("")))
                return "";

            String[] alist = StringUtils.split(accept, ",;");
            if ((alist==null)||(alist.length==0))
                return "";
            else
                return alist[0];
        }

        return "";
    }

    public long getXWikiPreferenceAsLong(String prefname, XWikiContext context) {
        return Long.parseLong(getXWikiPreference(prefname, context));
    }

    public long getWebPreferenceAsLong(String prefname, XWikiContext context) {
        return Long.parseLong(getWebPreference(prefname, context));
    }

    public long getXWikiPreferenceAsLong(String prefname, long default_value, XWikiContext context) {
        try {
            return Long.parseLong(getXWikiPreference(prefname, context));
        } catch (NumberFormatException e) {
            return default_value;
        }
    }

    public long getWebPreferenceAsLong(String prefname, long default_value, XWikiContext context) {
        try {
           return Long.parseLong(getWebPreference(prefname, context));
        } catch (NumberFormatException e) {
           return default_value;
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

        if (bclass.get("first_name")==null) {
            needsUpdate = true;
            StringClass firstname_class = new StringClass();
            firstname_class.setName("first_name");
            firstname_class.setPrettyName("First Name");
            firstname_class.setSize(30);
            firstname_class.setObject(bclass);
            bclass.put("first_name", firstname_class);
        }

        if (bclass.get("last_name")==null) {
            needsUpdate = true;
            StringClass lastname_class = new StringClass();
            lastname_class.setName("last_name");
            lastname_class.setPrettyName("Last Name");
            lastname_class.setSize(30);
            lastname_class.setObject(bclass);
            bclass.put("last_name", lastname_class);
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

        if (bclass.get("validkey")==null) {
            needsUpdate = true;
            StringClass validkey_class = new StringClass();
            validkey_class.setName("validkey");
            validkey_class.setPrettyName("Validation Key");
            validkey_class.setSize(10);
            validkey_class.setObject(bclass);
            bclass.put("validkey", validkey_class);
        }

        if (bclass.get("active")==null) {
            needsUpdate = true;
            BooleanClass active_class = new BooleanClass();
            active_class.setName("active");
            active_class.setPrettyName("Active");
            active_class.setDisplayType("yesno");
            active_class.setObject(bclass);
            bclass.put("active", active_class);
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

    public UserManager getUserManager(XWikiContext context) {
        UserManager um = (UserManager) context.get("usermanager");
        if (um==null) {
           um = UserManager.getInstance();
          ((XWikiUserProvider) um.getProfileProviders().toArray()[0]).setXWiki(this);
          ((XWikiUserProvider) um.getProfileProviders().toArray()[0]).setXWikiContext(context);
          ((XWikiGroupProvider) um.getAccessProviders().toArray()[0]).setXWiki(this);
          ((XWikiGroupProvider) um.getAccessProviders().toArray()[0]).setXWikiContext(context);
          context.put("usermanager", um);
        }
        return um;
    }

    public AccessManager getAccessManager(XWikiContext context) {
        AccessManager am = (AccessManager) context.get("accessmanager");
        if (am==null) {
            am = AccessManager.getInstance();
           ((XWikiResourceProvider) am.getResourceProviders().toArray()[0]).setXWiki(this);
           ((XWikiResourceProvider) am.getResourceProviders().toArray()[0]).setXWikiContext(context);
           ((XWikiAccessUserProvider) am.getAccessProviders().toArray()[0]).setXWiki(this);
           ((XWikiAccessUserProvider) am.getAccessProviders().toArray()[0]).setXWikiContext(context);
           context.put("accessmanager", am);
        }
        return am;
    }

    public int createUser(XWikiContext context) throws XWikiException {
        return createUser(false, context);
    }

    public int validateUser(boolean withConfirmEmail, XWikiContext context) throws XWikiException {
       try {
          HttpServletRequest request = (HttpServletRequest) context.getRequest();
          String xwikiname = request.getParameter("xwikiname");
          String validkey = request.getParameter("validkey");

          if (xwikiname.indexOf(".")==-1)
               xwikiname = "XWiki." + xwikiname;

          XWikiDocInterface docuser = getDocument(xwikiname, context);
          BaseObject userobj = docuser.getObject("XWiki.XWikiUsers", 0);
          String validkey2 = userobj.getStringValue("validkey");
          String email = userobj.getStringValue("email");
          String password = userobj.getStringValue("password");

          if ((!validkey2.equals("")&&(validkey2.equals(validkey)))) {
              userobj.setIntValue("active", 1);
              saveDocument(docuser, context);

              if (withConfirmEmail)
               sendValidationEmail(xwikiname, password, email, validkey, "confirmation_email_content", context);

              return 0;
          } else
           return -1;
       } catch (Exception e) {
           e.printStackTrace();
           throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_VALIDATE_USER,
                                  "Exception while validating user", e, null);
       }
    }

    public int createUser(boolean withValidation, XWikiContext context) throws XWikiException {
        try {
        HttpServletRequest request = (HttpServletRequest) context.getRequest();
        Map map = Util.getObject(request, "register");
        String content = "#includeForm(\"XWiki.XWikiUserTemplate\")";
        String xwikiname = request.getParameter("xwikiname");
        String password2 = request.getParameter("register2_password");
        String password = ((String[])map.get("password"))[0];
        String email = ((String[])map.get("email"))[0];
        String template = request.getParameter("template");
        String parent = request.getParameter("parent");
        String validkey = null;

        if (!password.equals(password2)) {
            // TODO: throw wrong password exception
            return -2;
        }

        if ((template!=null)&&(!template.equals(""))) {
                XWikiDocInterface tdoc = getDocument(template, context);
                if ((!tdoc.isNew()))
                  content = tdoc.getContent();
            }

        if ((parent==null)||(parent.equals(""))) {
            parent = "XWiki.XWikiUsers";
        }

        if (withValidation) {
            map.put("active", "0");
            validkey = generateValidationKey(16);
            map.put("validkey", validkey);

        } else {
            // Mark user active
            map.put("active", "1");
        }

          int result = createUser(xwikiname, map, parent, content, context);

          if ((result>0)&&(withValidation)) {
            // Send the validation email
            sendValidationEmail(xwikiname, password, email, validkey, "validation_email_content", context);
          }

          return result;
        } catch (XWikiException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_CREATE_USER,
                                   "Exception while creating user", e, null);
        }
    }

    public void sendValidationEmail(String xwikiname, String password, String email, String validkey, String contentfield, XWikiContext context) throws XWikiException {
     String sender;
     String content;

     try {
         sender = getXWikiPreference("admin_email", context);
         content = getXWikiPreference(contentfield, context);
     } catch (Exception e) {
         throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_CANNOT_GET_VALIDATION_CONFIG,
                                "Exception while reading the validation email config", e, null);

     }

     try {
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        vcontext.put("validkey", validkey);
        vcontext.put("email", email);
        vcontext.put("password", password);
        vcontext.put("sender", sender);
        vcontext.put("xwikiname", xwikiname);
        content = parseContent(content, context);
    } catch (Exception e) {
        throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_CANNOT_PREPARE_VALIDATION_EMAIL,
                               "Exception while preparing the validation email", e, null);

    }

        // Let's now send the message
        sendMessage(sender, email, content, context);
    }

    public void sendMessage(String sender, String[] recipient, String message, XWikiContext context) throws XWikiException {
        SMTPClient smtpc = null;
        try {
            String server = getXWikiPreference("smtp_server", context);
            String port = getXWikiPreference("smtp_port", context);
            String login = getXWikiPreference("smtp_login", context);

            if ((server==null)||server.equals(""))
             server = "127.0.0.1";
            if ((port==null)||(port.equals("")))
             port = "25";
            if ((login==null)||login.equals(""))
             login = "XWiki version " + getVersion();

            smtpc = new SMTPClient();
            smtpc.connect(server, Integer.parseInt(port));
            int reply = smtpc.getReplyCode();
            if (!SMTPReply.isPositiveCompletion(reply)) {
                Object[] args = { server, port, new Integer(reply), smtpc.getReplyString() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_CONNECT_FAILED,
                        "Could not connect to server {0} port {1} error code {2} ({3})", null, args);
            }

            if (smtpc.login(login)==false) {
                reply = smtpc.getReplyCode();
                Object[] args = { server, port, new Integer(reply), smtpc.getReplyString() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_LOGIN_FAILED,
                        "Could not login to mail server {0} port {1} error code {2} ({3})", null, args);
            }

            if (smtpc.sendSimpleMessage(sender, recipient, message)==false) {
                reply = smtpc.getReplyCode();
                Object[] args = { server, port, new Integer(reply), smtpc.getReplyString() };
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_SEND_FAILED,
                        "Could not send mail to server {0} port {1} error code {2} ({3})", null, args);
            }
        } catch (IOException e) {
            Object[] args = { sender, recipient };
            throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_ERROR_SENDING_EMAIL,
                    "Exception while sending email from {0} to {1}", e, args);
        } finally {
            if ((smtpc != null)&&(smtpc.isConnected())) {
                try {
                    smtpc.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendMessage(String sender, String recipient, String message, XWikiContext context) throws XWikiException {
        String[] recip = { recipient };
        sendMessage(sender, recip, message, context);
    }


    public String generateValidationKey(int size) {
        return RandomStringUtils.randomAlphanumeric(16);
    }

    public int createUser(String xwikiname, Map map, String parent, String content, XWikiContext context) throws XWikiException {
        BaseClass baseclass = getUserClass(context);
        BaseClass rclass = getRightsClass(context);

        try {
            // TODO: Verify existing user
            XWikiDocInterface doc = getDocument("XWiki." + xwikiname, context);
            if (!doc.isNew()) {
                // TODO: throws Exception
                return -3;
            }

            BaseObject newobject = (BaseObject) baseclass.fromMap(map);
            newobject.setName("XWiki." + xwikiname);
            doc.addObject(baseclass.getName(), newobject);
            doc.setParent(parent);
            doc.setContent(content);

            // Add protection to the page
            BaseObject newrightsobject = (BaseObject) rclass.newObject();
            newrightsobject.setClassName(rclass.getName());
            newrightsobject.setName("XWiki." + xwikiname);
            newrightsobject.setStringValue("groups", "XWiki.XWikiAdminGroup");
            newrightsobject.setStringValue("levels", "view, edit");
            newrightsobject.setIntValue("allow", 1);
            doc.addObject(rclass.getName(), newrightsobject);

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
                    persistent.setCookiePath(Param("xwiki.authentication.cookiepath"));
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
            boolean result = (password.equals(passwd));

            if (log.isDebugEnabled()) {
                if (result)
                 log.debug("Password check for user " + username + " successfull");
                else
                 log.debug("Password check for user " + username + " failed");
            }
            return result;
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
                if (log.isInfoEnabled()) log.info("User " + context.getUser() + " has been logged-out");
                wrappedRequest.setUserPrincipal(null);
                return null;
            }

            // In case of virtual server we never use osuser
            // because the fact that it is "static" doesn't
            // allow to dynamically switch databases
            if (("1".equals(Param("xwiki.virtual")))
                    || !("1".equals(Param("xwiki.authentication.osuser")))) {
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
           if (log.isInfoEnabled()) {
               if (user!=null)
                   log.info("User " + user.getName() + " is authentified");
           }
            return user;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void logAllow(String username, String page, String action, String info) {
        if (log.isDebugEnabled())
          log.debug("Access has been granted for (" + username + "," + page + "," + action + "): " + info);
    }

    public void logDeny(String username, String page, String action, String info) {
        if (log.isInfoEnabled())
          log.info("Access has been denied for (" + username + "," + page + "," + action + "): " + info);
    }

    public boolean checkAccess(String action, XWikiDocInterface doc, XWikiContext context)
            throws XWikiException {
        Principal user = null;
        boolean needsAuth = false;
        String right = "edit";

        if ((action.equals("logout"))||(action.equals("login"))||(action.equals("loginerror"))) {
            user = checkAuth(context);
            String username;
            if (user==null)
                username = "XWiki.XWikiGuest";
            else
                username = user.getName();

            // Save the user
            context.setUser(username);
            logAllow(username, doc.getFullName(), action, "login/logout pages");
            return true;
        }

        if (action.equals("view")||(action.equals("plain")||(action.equals("raw"))))
            right = "view";

        if (action.equals("skin")||(action.equals("download")))
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

                logDeny("unauthentified", doc.getFullName(), action, "Authentication needed");
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
            username = user.getName();

        // Save the user
        context.setUser(username);

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

            if (getAccessManager(context).userHasAccessLevel(username, docname, right)) {
                logAllow(username, docname, action, "access manager granted right");
                return true;
            }
        } catch (Exception e) {
            // This should not happen..
            logDeny(username, doc.getFullName(), action, "access manager exception " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        if (user==null) {
            // Denied Guest need to be authenticated
            logDeny("unauthentified", doc.getFullName(), action, "Guest has been denied - Redirecting to authentication");
            try {
                if (context.getRequest()!=null)
                    getAuthenticator().showLogin(context.getRequest(), context.getResponse());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        else {
            logDeny(username, doc.getFullName(), action, "access manager denied right");
            return false;
        }
    }

    public String includeTopic(String topic, XWikiContext context) {
        return include(topic, context, false);
    }

    public String includeForm(String topic, XWikiContext context) {
        return include(topic, context, true);
    }

    public String include(String topic, XWikiContext context, boolean isForm) {
        String database = null, incdatabase = null;
        Document currentdoc = null, currentcdoc = null, currenttdoc = null;
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        if (vcontext!=null) {
            currentdoc = (Document) vcontext.get("doc");
            currentcdoc = (Document) vcontext.get("cdoc");
            currenttdoc = (Document) vcontext.get("tdoc");
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
            if (currenttdoc!=null)
                vcontext.put("tdoc", currentcdoc);
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
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(obj);
        } catch (NoSuchFieldException e) {
            return null;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            return null;
        } finally {
        }
    }

    public Object callPrivateMethod(Object obj, String methodName) {
        return callPrivateMethod(obj, methodName, null, null);
    }

    public Object callPrivateMethod(Object obj, String methodName, Class[] classes, Object[] args) {
        try {
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
        }

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

    public boolean copyDocument(String docname, String sourceWiki, String targetWiki, XWikiContext context) throws XWikiException {
        String db = context.getDatabase();
        try {
            context.setDatabase(sourceWiki);
            XWikiDocInterface sdoc = getDocument(docname, context);
            if (!sdoc.isNew()) {
                context.setDatabase(targetWiki);
                XWikiDocInterface tdoc = getDocument(docname, context);
                // There is already an existing document
                if (!tdoc.isNew())
                    return false;

                tdoc = (XWikiDocInterface) sdoc.clone();
                // forget past versions
                tdoc.setVersion("1.1");
                tdoc.setRCSArchive(null);
                saveDocument(tdoc, context);
            }
        return true;
        } finally {
            context.setDatabase(db);
        }
    }

    public void copyWikiWeb(String web, String sourceWiki, String targetWiki, XWikiContext context) throws XWikiException {
        String db = context.getDatabase();
        try {
            String sql = "";
            if (web!=null)
                sql = "where doc.web = '" + web + "'";

            List list = searchDocuments(sql, context);
            for (Iterator it=list.iterator();it.hasNext();) {
                String docname = (String) it.next();
                copyDocument(docname, sourceWiki, targetWiki, context);
            }

        } finally {
            context.setDatabase(db);
        }
    }

    public void copyWiki(String sourceWiki, String targetWiki, XWikiContext context) throws XWikiException {
        copyWikiWeb(null, sourceWiki, targetWiki, context);
    }

    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName, boolean failOnExist, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();

        try {
            XWikiDocInterface userdoc = getDocument(wikiAdmin, context);

            // User does not exist
            if (userdoc.isNew()) {
                if (log.isErrorEnabled()) log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "user does not exist");
                return -2;
            }

            // User is not active
            if (!(userdoc.getIntValue("XWiki.XWikiUsers", "active")==1)) {
                if (log.isErrorEnabled()) log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "user is not active");
                return -3;
            }


            String wikiForbiddenList = Param("xwiki.virtual.reserved_wikis");
            if (Util.contains(wikiName, wikiForbiddenList, ", ")) {
                if (log.isErrorEnabled()) log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki name is forbidden");
                return -4;
            }

            String wikiServerPage = "XWikiServer" + wikiName;
            // Verify is server page already exist
            XWikiDocInterface serverdoc = getDocument("XWiki", wikiServerPage, context);
            if (serverdoc.isNew()) {
                // Create Wiki Server page
                serverdoc.setStringValue("XWiki.XWikiServerClass", "server", wikiUrl);
                serverdoc.setStringValue("XWiki.XWikiServerClass", "owner", wikiAdmin);
                saveDocument(serverdoc, context);
            } else {
                // If we are not allowed to continue if server page already exists

                if (failOnExist) {
                    if (log.isErrorEnabled()) log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki server page already exists");
                    return -5;
                } else
                    if (log.isWarnEnabled()) log.warn("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki server page already exists");

            }

            // Create wiki database
            try {
             context.setDatabase(getDatabase());
             getStore().createWiki(wikiName, context);
            } catch (XWikiException e) {
                if (log.isErrorEnabled()) {
                  if (e.getCode()==10010)
                   log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database already exists");
                  else if (e.getCode()==10011)
                   log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database creation failed");
                  else
                   log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database creation threw exception", e);
                }
            } catch (Exception e) {
                log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database creation threw exception", e);
            }


            // Copy base wiki
            copyWiki(baseWikiName, wikiName, context);

            // Create user page in his wiki
            copyDocument(wikiAdmin, getDatabase(), wikiName, context);

            // Modify rights in user wiki
            XWikiDocInterface wikiprefdoc = getDocument(wikiName + ":XWiki.XWikiPreferences", context);
            wikiprefdoc.setStringValue("XWiki.XWikiPreferences", "user", getDatabase() + ":" + wikiAdmin);
            saveDocument(wikiprefdoc, context);
            return 1;
        } catch (Exception e) {
            log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki creation threw exception", e);
            return -10;
        } finally {
            context.setDatabase(database);
        }
    }

    public String getEncoding() {
        return Param("xwiki.encoding", "ISO-8859-1");
    }

    public String getBaseUrl(String database, XWikiContext context) {
        String baseurl = null;
        if (database!=null) {
            String db = context.getDatabase();
            try {
              context.setDatabase(getDatabase());
              XWikiDocInterface doc = getDocument("XWiki.XWikiServer"
                            + database.substring(0,1).toUpperCase()
                            + database.substring(1), context);
              BaseObject serverobject = doc.getObject("XWiki.XWikiServerClass",0);
              String server = serverobject.getStringValue("server");
              int mode = serverobject.getIntValue("secure");
              if (server!=null) {
                  baseurl = ((mode==1) ? "https://" : "http://")
                            + server + "/xwiki/bin/";
              }

            } catch (Exception e) {
            } finally {
                context.setDatabase(db);
            }
        }

       if (baseurl==null)
         return getBase(context);
       else
         return baseurl;
    }

     public String getActionUrl(String fullname, String action, XWikiContext context) {
        StringBuffer url = new StringBuffer();
        XWikiDocInterface doc = new XWikiSimpleDoc();
        doc.setFullName(fullname, context);

        String baseurl = getBaseUrl(doc.getDatabase(), context);
        url.append(baseurl);
        url.append(action);
        url.append("/");
        url.append(doc.getWeb());
        url.append("/");
        url.append(doc.getName());
        return url.toString();
    }

    // Usefull date functions
    public Date getCurrentDate() {
        return new Date();
    }

    public int getTimeDelta(long time) {
        Date ctime = new Date();
        return (int)(ctime.getTime() - time);
    }

    public Date getDate(long time) {
        return new Date(time);
    }

    public boolean isMultiLingual(XWikiContext context) {
        return "1".equals(getXWikiPreference("multilingual", "1", context));
    }

    public boolean isVirtual() {
        return "1".equals(Param("xwiki.virtual"));
    }
}
