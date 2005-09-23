/**
 * ===================================================================
 *
 * Copyright (c) 2003-2005 Ludovic Dubost, All rights reserved.
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
 */
package com.xpn.xwiki;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.Vector;
import java.util.zip.ZipOutputStream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.ecs.Filter;
import org.apache.ecs.filter.CharacterFilter;
import org.apache.ecs.xhtml.textarea;
import org.apache.velocity.VelocityContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.RootContainer;
import org.hibernate.HibernateException;
import org.securityfilter.filter.URLPatternMatcher;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import com.xpn.xwiki.cache.impl.OSCacheService;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.PropertyChangedRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.SearchEngineRule;
import com.xpn.xwiki.stats.impl.XWikiStatsServiceImpl;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl;
import com.xpn.xwiki.user.impl.exo.ExoAuthServiceImpl;
import com.xpn.xwiki.user.impl.exo.ExoGroupServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.web.XWikiURLFactoryService;
import com.xpn.xwiki.web.XWikiURLFactoryServiceImpl;
import com.xpn.xwiki.web.includeservletasstring.IncludeServletAsString;

public class XWiki implements XWikiDocChangeNotificationInterface, XWikiInterface {
    private static final Log log = LogFactory.getLog(XWiki.class);

    private XWikiConfig config;
    private XWikiStoreInterface store;
    private XWikiRenderingEngine renderingEngine;
    private XWikiPluginManager pluginManager;
    private XWikiNotificationManager notificationManager;
    private XWikiAuthService authService;
    private XWikiRightService rightService;
    private XWikiGroupService groupService;
    private XWikiStatsService statsService;
    private XWikiCacheService cacheService;
    private XWikiURLFactoryService urlFactoryService;

    private MetaClass metaclass = MetaClass.getMetaClass();
    private boolean test = false;
    private String version = null;
    private XWikiEngineContext engine_context;
    private String database;
    private String fullNameSQL;

    private URLPatternMatcher urlPatternMatcher = new URLPatternMatcher();

    // These are caches in order to improve finding virtual wikis
    private List virtualWikiList = new ArrayList();
    private static Map virtualWikiMap = new HashMap();
    private static Map threadMap = new HashMap();

    private boolean isReadOnly = false;

    public static final String CFG_ENV_NAME = "XWikiConfig";

    /* i don't like using static variables like, but this avoid making a JNDI lookup with
       each request ...
    */
    private static String configPath = null;
    private static String getConfigPath () throws NamingException {
        if (configPath == null) {
            try {
                Context envContext = (Context) new InitialContext().lookup("java:comp/env");
                configPath = (String) envContext.lookup(CFG_ENV_NAME);
            } catch (Exception e) {
                // Allow a config path from WEB-INF
                if (log.isInfoEnabled())
                 log.info("xwiki.cfg taken from /WEB-INF/xwiki.cfg because the XWikiConfig variable is not set in the context");
                configPath = "/WEB-INF/xwiki.cfg";
            }

        }
        return configPath;
   }

   public static XWiki getMainXWiki(XWikiContext context) throws XWikiException {
        String xwikicfg = null;
        String xwikiname = "xwiki";
        XWiki xwiki = null;
        XWikiEngineContext econtext = context.getEngineContext();

        try {
            xwikicfg = getConfigPath();
            xwiki = (XWiki) econtext.getAttribute(xwikiname);
            if (xwiki == null) {
                InputStream xwikicfgis = null;

                // first try to load the file pointed by the given path
                // if it does not exist, look for it relative to the classpath
                File f = new File (xwikicfg);
                if (f.exists()) {
                    xwikicfgis = new FileInputStream(f);
                } else {
                    xwikicfgis = econtext.getResourceAsStream(xwikicfg);
                }
                xwiki = new XWiki(xwikicfgis, context, context.getEngineContext());
                econtext.setAttribute(xwikiname, xwiki);
            }
            context.setWiki(xwiki);
            xwiki.setDatabase(context.getDatabase());
            return xwiki;
        } catch (Exception e) {
            e.printStackTrace();
            throw new XWikiException(XWikiException.MODULE_XWIKI,
                    XWikiException.ERROR_XWIKI_INIT_FAILED,
                    "Could not initialize main XWiki context", e);
        }
    }

    public XWikiHibernateStore getHibernateStore() {
        XWikiStoreInterface store = getStore();
        if (store instanceof XWikiHibernateStore)
            return (XWikiHibernateStore) store;
        else if (store instanceof XWikiCacheStoreInterface) {
            store = ((XWikiCacheStoreInterface) store).getStore();
            if (store instanceof XWikiHibernateStore)
                return (XWikiHibernateStore) store;
            else
                return null;
        } else
            return null;
    }

    public synchronized void updateDatabase(String appname, XWikiContext context) throws HibernateException, XWikiException {
        updateDatabase(appname, false, context);
    }
    public synchronized void updateDatabase(String appname, boolean force, XWikiContext context) throws HibernateException, XWikiException {
        String database = context.getDatabase()
                ;
        try {
            List wikilist = getVirtualWikiList();
            context.setDatabase(appname);
            if (!wikilist.contains(appname)) {
                wikilist.add(appname);
                XWikiHibernateStore store = getHibernateStore();
                if (store != null)
                    store.updateSchema(context, force);
            }

            // Make sure these classes exists
            getPrefsClass(context);
            getUserClass(context);
            getGroupClass(context);
            getRightsClass(context);
            getCommentsClass(context);
            getSkinClass(context);
            getGlobalRightsClass(context);
            getPluginManager().virtualInit(context);

            // Add initdone which will allow to
            // bypass some initializations
            context.put("initdone", "1");
        } finally {
            context.setDatabase(database);
        }

    }

    public List getVirtualWikiList() {
        return virtualWikiList;
    }

    public static XWiki getXWiki(XWikiContext context) throws XWikiException {
        XWiki xwiki = getMainXWiki(context);

        if (xwiki.isVirtual()) {
            XWikiRequest request = context.getRequest();
            String host = "";
            try {
                URL requestURL = context.getURL();
                host = requestURL.getHost();
            } catch (Exception e) {
            }
            ;

            if (host.equals(""))
                return xwiki;

            String appname = findWikiServer(host, context);

            if (appname == null) {
                String uri = request.getRequestURI();
                int i1 = host.indexOf(".");
                String servername = (i1 != -1) ? host.substring(0, i1) : host;
                appname = uri.substring(1, uri.indexOf("/", 2));
                if ("0".equals(xwiki.Param("xwiki.virtual.autowww"))) {
                    appname = servername;
                } else {
                    if ((servername.equals("www"))
                            || (context.getUtil().match("m|[0-9]+\\.|[0-9]+\\.[0-9]+\\.[0-9]|", host))) {
                        if (appname.equals("xwiki"))
                            return xwiki;
                    } else {
                        appname = servername;
                    }
                }
            }

            // Check if this appname exists in the Database
            String serverwikipage = getServerWikiPage(appname);
            XWikiDocument doc = xwiki.getDocument(serverwikipage, context);
            if (doc.isNew()) {
                throw new XWikiException(XWikiException.MODULE_XWIKI,
                        XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                        "The wiki " + appname + " does not exist");
            }

            // Set the wiki owner
            String wikiOwner = doc.getStringValue("XWiki.XWikiServerClass", "owner");
            if (wikiOwner.indexOf(":") == -1)
                wikiOwner = xwiki.getDatabase() + ":" + wikiOwner;
            context.setWikiOwner(wikiOwner);
            context.setWikiServer(doc);
            context.setVirtual(true);
            context.setDatabase(appname);
            context.setOriginalDatabase(appname);
            try {
                // Let's make sure the virtaul wikis are upgraded to the latest database version
                xwiki.updateDatabase(appname, false, context);
            } catch (HibernateException e) {
                // Just to report it
                e.printStackTrace();
            }
        }
        return xwiki;
    }

    public static URL getRequestURL(XWikiRequest request) throws XWikiException {
        try {
            StringBuffer requestURL = request.getRequestURL();
            String qs = request.getQueryString();
            if ((qs != null) && (!qs.equals("")))
                return new URL(requestURL.toString() + "?" + qs);
            else
                return new URL(requestURL.toString());
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION,
                    "Exception while getting URL from request", e);
        }
    }

    private static String findWikiServer(String host, XWikiContext context) {
        String wikiserver = (String) virtualWikiMap.get(host);
        if (wikiserver != null)
            return wikiserver;

        String hql = ", BaseObject as obj, StringProperty as prop where obj.name=" + context.getWiki().getFullNameSQL()
                + " and obj.className='XWiki.XWikiServerClass' and prop.id.id = obj.id "
                + "and prop.id.name = 'server' and prop.value='" + host + "'";
        try {
            List list = context.getWiki().getStore().searchDocumentsNames(hql, context);
            if ((list == null) || (list.size() == 0))
                return null;
            String docname = (String) list.get(0);
            if (!docname.startsWith("XWiki.XWikiServer"))
                return null;
            wikiserver = docname.substring("XWiki.XWikiServer".length()).toLowerCase();
            virtualWikiMap.put(host, wikiserver);
            return wikiserver;
        } catch (XWikiException e) {
            return null;
        }
    }

    public static String getServerWikiPage(String servername) {
        return "XWiki.XWikiServer"
                + servername.substring(0, 1).toUpperCase()
                + servername.substring(1);
    }

    public XWiki(XWikiConfig config, XWikiContext context) throws XWikiException {
        this(config, context, null, false);
    }

    public XWiki(XWikiConfig config, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate) throws XWikiException {
        initXWiki(config, context, engine_context, noupdate);
    }

    /**
     * @deprecated use {@link #XWiki(XWikiConfig, XWikiContext)} instead
     */
    public XWiki(String xwikicfgpath, XWikiContext context) throws XWikiException {
        this(xwikicfgpath, context, null, false);
    }

    /**
     * @deprecated use {@link #XWiki(XWikiConfig, XWikiContext, XWikiEngineContext, boolean)} instead
     */
    public XWiki(String xwikicfgpath, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate) throws XWikiException {
        try {
            initXWiki(new XWikiConfig(new FileInputStream(xwikicfgpath)), context, engine_context, noupdate);
        } catch (FileNotFoundException e) {
            Object[] args = {xwikicfgpath};
            throw new XWikiException(XWikiException.MODULE_XWIKI_CONFIG,
                    XWikiException.ERROR_XWIKI_CONFIG_FILENOTFOUND,
                    "Configuration file {0} not found", e, args);
        }
    }

    /**
     * @deprecated use {@link #XWiki(XWikiConfig, XWikiContext, XWikiEngineContext, boolean)} instead
     */
    public XWiki(InputStream is, XWikiContext context, XWikiEngineContext engine_context) throws XWikiException {
        initXWiki(new XWikiConfig(is), context, engine_context, true);
    }

    public void initXWiki(XWikiConfig config, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate) throws XWikiException {
        setEngineContext(engine_context);
        context.setWiki(this);

        // Create the notification manager
        setNotificationManager(new XWikiNotificationManager());

        // Prepare the store
        XWikiStoreInterface basestore;
        setConfig(config);
        String storeclass = Param("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore");
        try {
            Class[] classes = new Class[]{this.getClass(), context.getClass()};
            Object[] args = new Object[]{this, context};
            basestore = (XWikiStoreInterface) Class.forName(storeclass).getConstructor(classes).newInstance(args);
        } catch (InvocationTargetException e) {
            Object[] args = {storeclass};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR,
                    "Cannot load store class {0}", e.getTargetException(), args);
        } catch (Exception e) {
            Object[] args = {storeclass};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                    XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR,
                    "Cannot load store class {0}", e, args);
        }

        // Check if we need to use the cache store..
        boolean nocache = "0".equals(Param("xwiki.store.cache", "1"));
        if (!nocache) {
            XWikiCacheStoreInterface cachestore = new XWikiCacheStore(basestore, context);
            try {
                String capacity = Param("xwiki.store.cache.capacity");
                if (capacity != null)
                    cachestore.setCacheCapacity(Integer.parseInt(capacity));
            } catch (Exception e) {
            }
            try {
                String capacity = Param("xwiki.store.cache.pageexistcapacity");
                if (capacity != null)
                    cachestore.setPageExistCacheCapacity(Integer.parseInt(capacity));
            } catch (Exception e) {
            }
            setStore(cachestore);
        } else
            setStore(basestore);

        resetRenderingEngine(context);

        // Prepare the Plugin Engine
        preparePlugins(context);

        // Add a notification rule if the preference property plugin is modified
        getNotificationManager().addNamedRule("XWiki.XWikiPreferences",
                new PropertyChangedRule(this, "XWiki.XWikiPreferences", "plugin"));

        // Make sure these classes exists
        if (noupdate) {
            getPrefsClass(context);
            getUserClass(context);
            getGroupClass(context);
            getRightsClass(context);
            getCommentsClass(context);
            getSkinClass(context);
            getGlobalRightsClass(context);
            getStatsService(context);
        }

        String ro = Param ("xwiki.readonly", "no");

        isReadOnly = ("yes".equalsIgnoreCase(ro) || "true".equalsIgnoreCase(ro) || "1".equalsIgnoreCase(ro));

    }

    public void resetRenderingEngine(XWikiContext context) throws XWikiException {
        // Prepare the Rendering Engine
        setRenderingEngine(new XWikiRenderingEngine(this, context));
    }

    private void preparePlugins(XWikiContext context) {
        setPluginManager(new XWikiPluginManager(getXWikiPreference("plugins", context), context));
        String plugins = Param("xwiki.plugins", "");
        if (!plugins.equals("")) {
            getPluginManager().addPlugins(StringUtils.split(plugins, " ,"), context);
        }
    }

    public String getVersion() {
        if (version == null) {
            version = Param("xwiki.version", "");
            String bnb = null;
            try {
                InputStream is = getResourceAsStream("/WEB-INF/version.properties");
                XWikiConfig vprop = new XWikiConfig(is);
                bnb = vprop.getProperty("build.number");
            } catch (Exception e) {
            }
            if (bnb != null)
                version = version + "." + bnb;
        }
        return version;
    }

    public URL getResource(String s) throws MalformedURLException {
        return getEngineContext().getResource(s);
    }

    public InputStream getResourceAsStream(String s) throws MalformedURLException {
        return getEngineContext().getResourceAsStream(s);
    }

    public String getResourceContent(String name) throws IOException {
        InputStream is = null;
        if (getEngineContext() != null) {

            try {
                is = getResourceAsStream(name);
            } catch (Exception e) {
            }
        }

        if (is == null)
            return Util.getFileContent(new File(name));

        return Util.getFileContent(new InputStreamReader(is));
    }

    public boolean resourceExists(String name) {
        InputStream ris = null;
        if (getEngineContext() != null) {
            try {
                ris = getResourceAsStream(name);
                if (ris != null)
                    return true;
            } catch (IOException e) {
            }
        }
        File file = new File(name);
        return file.exists();
    }

    public XWikiConfig getConfig() {
        return config;
    }

    public String getRealPath(String path) {
        return getEngineContext().getRealPath(path);
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
        if (path == null)
            return null;

        fpath = new File(path);
        if (fpath.exists()) {
            return path;
        }

        path = getRealPath(path);
        if (path == null)
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

    public void saveDocument(XWikiDocument doc, XWikiContext context) throws XWikiException {
        getStore().saveXWikiDoc(doc, context);
    }

    public void saveDocument(XWikiDocument doc, XWikiDocument olddoc, XWikiContext context) throws XWikiException {
        getStore().saveXWikiDoc(doc, context);
        getNotificationManager().verify(doc, olddoc, XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
    }

    private XWikiDocument getDocument(XWikiDocument doc, XWikiContext context) throws XWikiException {
        return getStore().loadXWikiDoc(doc, context);
    }

    public XWikiDocument getDocument(XWikiDocument doc, String revision, XWikiContext context) throws XWikiException {
        XWikiDocument newdoc;
        try {
            if ((revision == null) || revision.equals("")) {
                newdoc = new XWikiDocument(doc.getWeb(), doc.getName());
            } else if (revision.equals(doc.getVersion())) {
                newdoc = doc;
            } else {
                newdoc = getStore().loadXWikiDoc(doc, revision, context);
            }
        } catch (XWikiException e) {
            if (revision.equals("1.1") || revision.equals("1.0"))
                newdoc = new XWikiDocument(doc.getWeb(), doc.getName());
            else
                throw e;
        }
        return newdoc;
    }

    public XWikiDocument getDocument(String fullname, XWikiContext context) throws XWikiException {
        String server = null, database = null;
        try {
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(fullname, context);
            server = doc.getDatabase();

            if (server != null) {
                database = context.getDatabase();
                context.setDatabase(server);
            }

            doc = getDocument(doc, context);
            return doc;
        } finally {
            if ((server != null) && (database != null)) {
                context.setDatabase(database);
            }
        }
    }

    public XWikiDocument getDocument(String web, String fullname, XWikiContext context) throws XWikiException {
        int i1 = fullname.lastIndexOf(".");
        if (i1 != -1) {
            String web2 = fullname.substring(0, i1);
            String name = fullname.substring(i1 + 1);
            if (name.equals(""))
                name = "WebHome";
            return getDocument(web2 + "." + name, context);
        } else {
            return getDocument(web + "." + fullname, context);
        }
    }


    public XWikiDocument getDocumentFromPath(String path, XWikiContext context) throws XWikiException {
        String web, name;
        int i1 = path.indexOf("/", 1);
        int i2 = path.indexOf("/", i1 + 1);
        int i3 = path.indexOf("/", i2 + 1);
        web = path.substring(i1 + 1, i2);
        if (i3 == -1)
            name = path.substring(i2 + 1);
        else
            name = path.substring(i2 + 1, i3);
        if (name.equals(""))
            name = "WebHome";

        web = Utils.decode(web, context);
        name = Utils.decode(name, context);
        return getDocument(web + "." + name, context);
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

    public static String getURLEncoded(String content) {
        try {
            return URLEncoder.encode(content, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return content;
        }
    }


    public static String getXMLEncoded(String content) {
        Filter filter = new CharacterFilter();
        String scontent = filter.process(content);
        return scontent;
    }

    public static String getTextArea(String content, XWikiContext context) {
        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String scontent = filter.process(content);

        textarea textarea = new textarea();
        textarea.setFilter(filter);

        int rows = 25;
        try {
            rows = context.getWiki().getUserPreferenceAsInt("editbox_height", context);
        } catch (Exception e) {
        }
        textarea.setRows(rows);

        int cols = 80;
        try {
            context.getWiki().getUserPreferenceAsInt("editbox_width", context);
        } catch (Exception e) {
        }
        textarea.setCols(cols);
        textarea.setName("content");
        textarea.addElement(scontent);
        return textarea.toString();
    }

    public String getHTMLArea(String content, XWikiContext context) {
        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String scontent = filter.process(content);

        scontent = context.getUtil().substitute("s/\\r\\n/<br class=\"htmlarea\"\\/>/g", scontent);
        textarea textarea = new textarea();

        int rows = 25;
        try {
            rows = context.getWiki().getUserPreferenceAsInt("editbox_height", context);
        } catch (Exception e) {
        }
        textarea.setRows(rows);

        int cols = 80;
        try {
            context.getWiki().getUserPreferenceAsInt("editbox_width", context);
        } catch (Exception e) {
        }
        textarea.setCols(cols);
        textarea.setFilter(filter);
        textarea.setName("content");
        textarea.setID("content");
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

    public List search(String sql, XWikiContext context) throws XWikiException {
        return getStore().search(sql, 0, 0, context);
    }

    public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException {
        return getStore().search(sql, nb, start, context);
    }

    public List search(String sql, Object[][] whereParams, XWikiContext context) throws XWikiException {
        return getStore().search(sql, 0, 0, whereParams, context);
    }

    public List search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context) throws XWikiException {
        return getStore().search(sql, nb, start, whereParams, context);
    }

    public boolean isTest() {
        return test;
    }

    public void setTest(boolean test) {
        this.test = test;
    }

    public String parseContent(String content, XWikiContext context) {
        if ((content != null) && (!content.equals("")))
        // Let's use this template
            return XWikiVelocityRenderer.evaluate(content, context.getDoc().getFullName(), (VelocityContext) context.get("vcontext"), context);
        else
            return "";
    }

    public String parseTemplate(String template, XWikiContext context) {
        try {
            String skin = getSkin(context);
            String result = parseTemplate(template, skin, context);
            if (result != null)
                return result;

            // If we could not find the template in the skin
            // let's try in the base skin (as long as the base skin is not the same as the skin)
            String baseskin = getBaseSkin(context);
            if (!skin.equals(baseskin)) {
                result = parseTemplate(template, baseskin, context);
                if (result != null)
                    return result;
            }

            // If we still could not find the template in the skin or in the base skin
            // let's try in the default base skin (as long as the default base skin is not the same as the skin or the base skin
            String defaultbaseskin = getDefaultBaseSkin(context);
            if ((!baseskin.equals(defaultbaseskin)) && (!skin.equals(defaultbaseskin))) {
                result = parseTemplate(template, defaultbaseskin, context);
                if (result != null)
                    return result;
            }
        } catch (Exception e) {
        }

        try {
            String content = getResourceContent("/templates/" + template);
            return XWikiVelocityRenderer.evaluate(content, "", (VelocityContext) context.get("vcontext"), context);
        } catch (Exception e) {
            return "";
        }
    }

    public String parseTemplate(String template, String skin, XWikiContext context) {
        try {
            String path = "/skins/" + skin + "/" + template;
            String content = getResourceContent(path);
            return XWikiVelocityRenderer.evaluate(content, "", (VelocityContext) context.get("vcontext"), context);
        } catch (Exception e) {
        }

        try {
            XWikiDocument doc = getDocument(skin, context);
            if (!doc.isNew()) {
                BaseObject object = doc.getObject("XWiki.XWikiSkins", 0);
                if (object != null) {
                    String content = object.getStringValue(template);
                    if ((content != null) && (!content.equals(""))) {
                        // Let's use this template
                        return XWikiVelocityRenderer.evaluate(content, "", (VelocityContext) context.get("vcontext"), context);
                    }
                }
            }
        } catch (Exception e) {
        }

        return null;
    }


    /**
     * Designed to include dynamic content, such as Servlets or JSPs, inside Velocity
     * templates; works by creating a RequestDispatcher, buffering the output,
     * then returning it as a string.
     *
     * @author LBlaze
     */
    public String invokeServletAndReturnAsString(String url, XWikiContext xwikiContext) {

        HttpServletRequest servletRequest = xwikiContext.getRequest();
        HttpServletResponse servletResponse = xwikiContext.getResponse();

        try
        {
            return IncludeServletAsString.invokeServletAndReturnAsString(
                    url,
                    servletRequest,
                    servletResponse);
        }
        catch(Exception e)
        {
            log.warn("Exception including url: "+url, e);
            return "Exception including \""+url+"\", see logs for details.";
        }

    }


    public String getSkinFile(String filename, XWikiContext context) {
        XWikiURLFactory urlf = context.getURLFactory();

        try {
            String skin = getSkin(context);
            String result = getSkinFile(filename, skin, context);
            if (result != null)
                return result;
            String baseskin = getBaseSkin(context);
            if (!skin.equals(baseskin)) {
                result = getSkinFile(filename, baseskin, context);
                if (result != null)
                    return result;
            }
            URL url = urlf.createSkinURL(filename, "default", context);
            return urlf.getURL(url, context);
        } catch (Exception e) {
        }

        return "../../../skins/default/" + filename;
    }

    public String getSkinFile(String filename, String skin, XWikiContext context) {
        XWikiURLFactory urlf = context.getURLFactory();
        try {
            String path = "skins/" + skin + "/" + filename;
            if (resourceExists(path)) {
                URL url = urlf.createSkinURL(filename, skin, context);
                return urlf.getURL(url, context);
            }

        } catch (Exception e) {
        }

        try {
            XWikiDocument doc = getDocument(skin, context);
            if (!doc.isNew()) {
                BaseObject object = doc.getObject("XWiki.XWikiSkins", 0);
                if (object != null) {
                    String content = object.getStringValue(filename);
                    if ((content != null) && (!content.equals(""))) {
                        URL url = urlf.createSkinURL(filename, doc.getWeb(), doc.getName(), context);
                        return urlf.getURL(url, context);

                    }
                }

                // Read XWikiAttachment
                XWikiAttachment attachment = null;
                List list = doc.getAttachmentList();
                String shortname = filename.substring(0, filename.indexOf(".") + 1);
                attachment = doc.getAttachment(shortname);

                if (attachment != null) {
                    URL url = urlf.createSkinURL(filename, doc.getWeb(), doc.getName(), context);
                    return urlf.getURL(url, context);
                }
            }
        } catch (Exception e) {
        }

        return null;
    }


    public String getSkin(XWikiContext context) {
        String skin = "";
        try {
            // Try to get it from context
            skin = (String) context.get("skin");
            if (skin != null)
                return skin;

            // Try to get it from URL
            if (context.getRequest() != null) {
                skin = context.getRequest().getParameter("skin");
            }

            if ((skin == null) || (skin.equals(""))) {
                skin = getWebPreference("skin", "", context);
            }
            if (skin.equals("")) {
                skin = Param("xwiki.defaultskin", "default");
            }
        } catch (Exception e) {
            skin = "default";
        }
        context.put("skin", skin);
        return skin;
    }


    public String getDefaultBaseSkin(XWikiContext context) {
        String defaultbaseskin = Param("xwiki.defaultbaseskin", "");
        if (defaultbaseskin.equals("")) {
            defaultbaseskin = Param("xwiki.defaultskin", "default");
        }
        return defaultbaseskin;
    }

    public String getBaseSkin(XWikiContext context) {
        return getBaseSkin(context, false);
    }

    public String getBaseSkin(XWikiContext context, boolean fromRenderSkin) {
        String baseskin = "";
        try {
            // Try to get it from context
            baseskin = (String) context.get("baseskin");
            if (baseskin != null)
                return baseskin;
            else
                baseskin = "";

            // Let's get the base skin doc itself
            if (fromRenderSkin) {
                baseskin = context.getDoc().getStringValue("XWiki.XWikiSkins", "baseskin");
            }

            if (baseskin.equals("")) {
                // Let's get the base skin from the skin itself
                String skin = getSkin(context);
                skin = getSkin(context);
                XWikiDocument doc = getDocument(skin, context);
                baseskin = doc.getStringValue("XWiki.XWikiSkins", "baseskin");
            }
            if (baseskin.equals("")) {
                baseskin = getDefaultBaseSkin(context);
            }
        } catch (Exception e) {
            baseskin = "default";
        }
        context.put("baseskin", baseskin);
        return baseskin;
    }

    public String getWebCopyright(XWikiContext context) {
        try {
            String result = getXWikiPreference("webcopyright", "", context);
            if (!result.trim().equals(""))
                return result;
        } catch (Exception e) {
        }
        return "Copyright 2004 (c) Contributing Authors";
    }

    public String getXWikiPreference(String prefname, XWikiContext context) {
        return getXWikiPreference(prefname, "", context);
    }

    public String getXWikiPreference(String prefname, String default_value, XWikiContext context) {
        try {
            XWikiDocument doc = getDocument("XWiki.XWikiPreferences", context);
            // First we try to get a translated preference object
            BaseObject object = doc.getObject("XWiki.XWikiPreferences", "language", context.getLanguage(), true);
            String result = "";

            try {
                result = object.getStringValue(prefname);
            } catch (Exception e) {
            }
            // If empty we take it from the default pref object
            if (result.equals(""))
                result = doc.getxWikiObject().getStringValue(prefname);

            if (!result.equals(""))
                return result;
        } catch (Exception e) {
        }
        return default_value;
    }

    public String getWebPreference(String prefname, XWikiContext context) {
        return getWebPreference(prefname, "", context);
    }

    public String getWebPreference(String prefname, String default_value, XWikiContext context) {
        try {
            XWikiDocument currentdoc = (XWikiDocument) context.get("doc");
            XWikiDocument doc = getDocument(currentdoc.getWeb() + ".WebPreferences", context);

            // First we try to get a translated preference object
            BaseObject object = doc.getObject("XWiki.XWikiPreferences", "language", context.getLanguage());
            String result = "";
            try {
                result = object.getStringValue(prefname);
                // If empty we take it from the default pref object
            } catch (Exception e) {
            }

            if (result.equals(""))
                result = doc.getxWikiObject().getStringValue(prefname);

            if (!result.equals(""))
                return result;
        } catch (Exception e) {
        }
        return getXWikiPreference(prefname, default_value, context);
    }

    public String getUserPreference(String prefname, XWikiContext context) {
        try {
            String user = context.getUser();
            XWikiDocument userdoc = getDocument(user, context);
            if (userdoc != null) {
                String result = userdoc.getStringValue("XWiki.XWikiUsers", prefname);
                if (!result.equals(""))
                    return result;
            }
        } catch (Exception e) {
        }

        return getWebPreference(prefname, context);
    }


    public String getUserPreferenceFromCookie(String prefname, XWikiContext context) {
        Cookie[] cookies = context.getRequest().getCookies();
        if (cookies == null)
            return null;
        for (int i = 0; i < cookies.length; i++) {
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
            if (result != null)
                return result;
        }
        return getUserPreference(prefname, context);
    }

    public String getLanguagePreference(XWikiContext context) {
        // First we get the language from the request
        String language;

        language = context.getLanguage();
        if (language != null)
            return language;

        if (!context.getWiki().isMultiLingual(context)) {
            language = context.getWiki().getXWikiPreference("language", "", context);
            context.setLanguage(language);
            return language;
        }

        try {
            language = context.getRequest().getParameter("language");
            if ((language != null) && (!language.equals(""))) {
                if (language.equals("default")) {
                    // forgetting language cookie
                    Cookie cookie = new Cookie("language", "");
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    context.getResponse().addCookie(cookie);
                    language = "";
                } else {
                    // setting language cookie
                    Cookie cookie = new Cookie("language", language);
                    cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
                    cookie.setPath("/");
                    context.getResponse().addCookie(cookie);
                }
                context.setLanguage(language);
                return language;
            }
        } catch (Exception e) {
        }

        try {
            // First we get the language from the cookie
            language = getUserPreferenceFromCookie("language", context);
            if ((language != null) && (!language.equals(""))) {
                context.setLanguage(language);
                return language;
            }
        } catch (Exception e) {
        }

        // Next from the default user preference
        try {
            String user = context.getUser();
            XWikiDocument userdoc = null;
            userdoc = getDocument(user, context);
            if (userdoc != null) {
                language = userdoc.getStringValue("XWiki.XWikiUsers", "default_language");
                if (!language.equals("")) {
                    context.setLanguage(language);
                    return language;
                }
            }
        } catch (XWikiException e) {
        }

        // Then from the navigator language setting
        if (context.getRequest() != null) {
            String accept = context.getRequest().getHeader("Accept-Language");
            if ((accept == null) || (accept.equals(""))) {
                context.setLanguage("");
                return "";
            }

            String[] alist = StringUtils.split(accept, ",;-");
            if ((alist == null) || (alist.length == 0)) {
                context.setLanguage("");
                return "";
            } else {
                context.setLanguage(alist[0]);
                return alist[0];
            }
        }

        context.setLanguage("");
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

    public long getUserPreferenceAsLong(String prefname, XWikiContext context) {
        return Long.parseLong(getUserPreference(prefname, context));
    }

    public int getXWikiPreferenceAsInt(String prefname, XWikiContext context) {
        return Integer.parseInt(getXWikiPreference(prefname, context));
    }

    public int getWebPreferenceAsInt(String prefname, XWikiContext context) {
        return Integer.parseInt(getWebPreference(prefname, context));
    }

    public int getXWikiPreferenceAsInt(String prefname, int default_value, XWikiContext context) {
        try {
            return Integer.parseInt(getXWikiPreference(prefname, context));
        } catch (NumberFormatException e) {
            return default_value;
        }
    }

    public int getWebPreferenceAsInt(String prefname, int default_value, XWikiContext context) {
        try {
            return Integer.parseInt(getWebPreference(prefname, context));
        } catch (NumberFormatException e) {
            return default_value;
        }
    }

    public int getUserPreferenceAsInt(String prefname, XWikiContext context) {
        return Integer.parseInt(getUserPreference(prefname, context));
    }

    public void flushCache() {
        // We need to flush the virtual wiki list
        virtualWikiList = new ArrayList();
        // We need to flush the server Cache
        virtualWikiMap = new HashMap();

        // We need to flush the group service cache
        if (groupService != null)
            groupService.flushCache();

        // If we use the Cache Store layer.. we need to flush it
        XWikiStoreInterface store = getStore();
        if ((store != null) && (store instanceof XWikiCacheStoreInterface)) {
            ((XWikiCacheStoreInterface) getStore()).flushCache();
        }
        // Flush renderers.. Groovy renderer has a cache
        XWikiRenderingEngine rengine = getRenderingEngine();
        if (rengine != null)
            rengine.flushCache();

        XWikiPluginManager pmanager = getPluginManager();
        if (pmanager != null)
            pmanager.flushCache();

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

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event, XWikiContext context) {
        if (!isVirtual()) {
            if (newdoc.getFullName().equals("XWiki.XWikiPreferences")) {
                preparePlugins(context);
            }
        }
    }

    public BaseClass getUserClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiUsers", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setWeb("XWiki");
            doc.setName("XWikiUsers");
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null)
            return bclass;

        bclass.setName("XWiki.XWikiUsers");

        needsUpdate |= bclass.addTextField("first_name", "First Name", 30);
        needsUpdate |= bclass.addTextField("last_name", "Last Name", 30);
        needsUpdate |= bclass.addTextField("email", "e-Mail", 30);
        needsUpdate |= bclass.addPasswordField("password", "Password", 10);
        needsUpdate |= bclass.addPasswordField("validkey", "Validation Key", 10);
        needsUpdate |= bclass.addBooleanField("active", "Active", "active");
        needsUpdate |= bclass.addTextField("default_language", "Default Language", 30);
        needsUpdate |= bclass.addTextField("company", "Company", 30);
        needsUpdate |= bclass.addTextField("blog", "Blog", 60);
        needsUpdate |= bclass.addTextField("blogfeed", "Blog Feed", 60);
        needsUpdate |= bclass.addTextAreaField("comment", "Comment", 40, 5);
        needsUpdate |= bclass.addStaticListField("imtype", "IM Type", "---|AIM|Yahoo|Jabber|MSN|Skype|ICQ");
        needsUpdate |= bclass.addTextField("imaccount", "imaccount", 30);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Users");
        }

        if (needsUpdate)
            saveDocument(doc, context);
        return bclass;
    }

    public BaseClass getPrefsClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiPreferences", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setWeb("XWiki");
            doc.setName("XWikiPreferences");
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null)
            return bclass;

        bclass.setName("XWiki.XWikiPreferences");

        needsUpdate |= bclass.addBooleanField("multilingual", "Multi-Lingual", "yesno");
        needsUpdate |= bclass.addTextField("language", "Language", 5);
        needsUpdate |= bclass.addTextField("default_language", "Default Language", 5);
        needsUpdate |= bclass.addBooleanField("authenticate_edit", "Authenticated Edit", "yesno");
        needsUpdate |= bclass.addBooleanField("authenticate_view", "Authenticated View", "yesno");
        needsUpdate |= bclass.addBooleanField("backlinks", "Backlinks", "yesno");

        needsUpdate |= bclass.addTextField("skin", "Skin", 30);
        // This one should not be in the prefs
        PropertyInterface baseskinProp = bclass.get("baseskin");
        if (baseskinProp != null) {
            bclass.removeField("baseskin");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addTextField("stylesheet", "Default Stylesheet", 30);
        needsUpdate |= bclass.addTextField("stylesheets", "Alternative Stylesheet", 60);

        needsUpdate |= bclass.addStaticListField("editor", "Default Editor", "Text|Wysiwyg");
        needsUpdate |= bclass.addTextField("editbox_width", "Editbox Width", 5);
        needsUpdate |= bclass.addTextField("editbox_height", "Editbox Height", 5);

        needsUpdate |= bclass.addTextField("webcopyright", "Copyright", 30);
        needsUpdate |= bclass.addTextField("title", "Title", 30);
        needsUpdate |= bclass.addTextField("version", "Version", 30);
        needsUpdate |= bclass.addTextAreaField("menu", "Menu", 60, 8);
        needsUpdate |= bclass.addTextAreaField("meta", "HTTP Meta Info", 60, 8);

        needsUpdate |= bclass.addBooleanField("use_email_verification", "Use eMail Verification", "yesno");
        needsUpdate |= bclass.addTextField("smtp_server", "SMTP Server", 30);
        needsUpdate |= bclass.addTextField("admin_email", "Admin eMail", 30);
        needsUpdate |= bclass.addTextAreaField("validation_email_content", "Validation eMail Content", 72, 10);
        needsUpdate |= bclass.addTextAreaField("confirmation_email_content", "Confirmation eMail Content", 72, 10);


        needsUpdate |= bclass.addTextField("macros_languages", "Macros Languages", 60);
        needsUpdate |= bclass.addTextField("macros_velocity", "Macros for Velocity", 60);
        needsUpdate |= bclass.addTextField("macros_groovy", "Macros for Groovy", 60);
        needsUpdate |= bclass.addTextField("macros_wiki2", "Macros for new Wiki Parser", 60);
        needsUpdate |= bclass.addTextAreaField("macros_mapping", "Macros Mapping", 60, 15);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Users");
        }

        if (needsUpdate)
            saveDocument(doc, context);
        return bclass;
    }

    public BaseClass getGroupClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiGroups", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setWeb("XWiki");
            doc.setName("XWikiGroups");
        }
        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null)
            return bclass;

        bclass.setName("XWiki.XWikiGroups");

        needsUpdate |= bclass.addTextField("member", "Member", 30);

        String content = doc.getContent();
        if ((content == null) || (content.equals("")))
            doc.setContent("1 XWiki Groups");

        if (needsUpdate)
            saveDocument(doc, context);
        return bclass;
    }


    public BaseClass getRightsClass(String pagename, XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki." + pagename, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setWeb("XWiki");
            doc.setName(pagename);
        }
        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null)
            return bclass;

        bclass.setName("XWiki." + pagename);

        needsUpdate |= bclass.addTextField("users", "Users", 80);
        needsUpdate |= bclass.addTextField("groups", "Groups", 80);
        needsUpdate |= bclass.addTextField("levels", "Levels", 80);

        PropertyInterface allowProp = bclass.get("allow");
        if ((allowProp != null) && (allowProp instanceof NumberClass)) {
            bclass.removeField("allow");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addBooleanField("allow", "Allow/Deny", "allow");

        String content = doc.getContent();
        if ((content == null) || (content.equals("")))
            doc.setContent("1 XWiki " + pagename + " Class");

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

    public BaseClass getCommentsClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiComments", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setWeb("XWiki");
            doc.setName("XWikiComments");
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null)
            return bclass;

        bclass.setName("XWiki.XWikiComments");

        needsUpdate |= bclass.addTextField("author", "Author", 30);
        needsUpdate |= bclass.addTextAreaField("highlight", "Highlighted Text", 40, 2);
        needsUpdate |= bclass.addNumberField("replyto", "Reply To", 5, "integer");
        needsUpdate |= bclass.addDateField("date", "Date");
        needsUpdate |= bclass.addTextAreaField("comment", "Comment", 40, 5);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Comment Class");
        }

        if (needsUpdate)
            saveDocument(doc, context);
        return bclass;
    }

    public BaseClass getSkinClass(XWikiContext context) throws XWikiException {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiSkins", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setWeb("XWiki");
            doc.setName("XWikiSkins");
            needsUpdate = true;
        }

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null)
            return bclass;

        bclass.setName("XWiki.XWikiSkins");

        needsUpdate |= bclass.addTextField("name", "Name", 30);
        needsUpdate |= bclass.addTextField("baseskin", "Base Skin", 30);
        needsUpdate |= bclass.addTemplateField("style.css", "Style");
        needsUpdate |= bclass.addTemplateField("header.vm", "Header");
        needsUpdate |= bclass.addTemplateField("footer.vm", "Footer");
        needsUpdate |= bclass.addTemplateField("viewheader.vm", "View Header");
        needsUpdate |= bclass.addTemplateField("view.vm", "View");
        needsUpdate |= bclass.addTemplateField("edit.vm", "Edit");

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Skin Class");
        }

        if (needsUpdate)
            saveDocument(doc, context);
        return bclass;
    }


    public int createUser(XWikiContext context) throws XWikiException {
        return createUser(false, "edit", context);
    }

    public int validateUser(boolean withConfirmEmail, XWikiContext context) throws XWikiException {
        try {
            XWikiRequest request = context.getRequest();
            String xwikiname = request.getParameter("xwikiname");
            String validkey = request.getParameter("validkey");

            if (xwikiname.indexOf(".") == -1)
                xwikiname = "XWiki." + xwikiname;

            XWikiDocument docuser = getDocument(xwikiname, context);
            BaseObject userobj = docuser.getObject("XWiki.XWikiUsers", 0);
            String validkey2 = userobj.getStringValue("validkey");
            String email = userobj.getStringValue("email");
            String password = userobj.getStringValue("password");

            if ((!validkey2.equals("") && (validkey2.equals(validkey)))) {
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

    public int createUser(boolean withValidation, String userRights, XWikiContext context) throws XWikiException {
        try {
            XWikiRequest request = context.getRequest();
            Map map = Util.getObject(request, "register");
            String content = "#includeForm(\"XWiki.XWikiUserTemplate\")";
            String xwikiname = request.getParameter("xwikiname");
            String password2 = request.getParameter("register2_password");
            String password = ((String[]) map.get("password"))[0];
            String email = ((String[]) map.get("email"))[0];
            String template = request.getParameter("template");
            String parent = request.getParameter("parent");
            String validkey = null;

            if (!password.equals(password2)) {
                // TODO: throw wrong password exception
                return -2;
            }

            if ((template != null) && (!template.equals(""))) {
                XWikiDocument tdoc = getDocument(template, context);
                if ((!tdoc.isNew()))
                    content = tdoc.getContent();
            }

            if ((parent == null) || (parent.equals(""))) {
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

            int result = createUser(xwikiname, map, parent, content, userRights, context);

            if ((result > 0) && (withValidation)) {
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

            if ((server == null) || server.equals(""))
                server = "127.0.0.1";
            if ((port == null) || (port.equals("")))
                port = "25";
            if ((login == null) || login.equals(""))
                login = "XWiki version " + getVersion();

            smtpc = new SMTPClient();
            smtpc.connect(server, Integer.parseInt(port));
            int reply = smtpc.getReplyCode();
            if (!SMTPReply.isPositiveCompletion(reply)) {
                Object[] args = {server, port, new Integer(reply), smtpc.getReplyString()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_CONNECT_FAILED,
                        "Could not connect to server {0} port {1} error code {2} ({3})", null, args);
            }

            if (smtpc.login(login) == false) {
                reply = smtpc.getReplyCode();
                Object[] args = {server, port, new Integer(reply), smtpc.getReplyString()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_LOGIN_FAILED,
                        "Could not login to mail server {0} port {1} error code {2} ({3})", null, args);
            }

            if (smtpc.sendSimpleMessage(sender, recipient, message) == false) {
                reply = smtpc.getReplyCode();
                Object[] args = {server, port, new Integer(reply), smtpc.getReplyString()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_SEND_FAILED,
                        "Could not send mail to server {0} port {1} error code {2} ({3})", null, args);
            }

        } catch (IOException e) {
            Object[] args = {sender, recipient};
            throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL, XWikiException.ERROR_XWIKI_EMAIL_ERROR_SENDING_EMAIL,
                    "Exception while sending email from {0} to {1}", e, args);
        } finally {
            if ((smtpc != null) && (smtpc.isConnected())) {
                try {
                    smtpc.disconnect();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendMessage(String sender, String recipient, String message, XWikiContext context) throws XWikiException {
        String[] recip = {recipient};
        sendMessage(sender, recip, message, context);
    }


    public String generateValidationKey(int size) {
        return RandomStringUtils.randomAlphanumeric(16);
    }

    public int createUser(String xwikiname, Map map, String parent, String content, String userRights, XWikiContext context) throws XWikiException {
        BaseClass baseclass = getUserClass(context);

        try {
            String fullwikiname = "XWiki." + xwikiname;

            // TODO: Verify existing user
            XWikiDocument doc = getDocument(fullwikiname, context);
            if (!doc.isNew()) {
                // TODO: throws Exception
                return -3;
            }

            BaseObject newobject = (BaseObject) baseclass.fromMap(map, context);
            newobject.setName(fullwikiname);
            doc.addObject(baseclass.getName(), newobject);
            doc.setParent(parent);
            doc.setContent(content);

            ProtectUserPage(context, fullwikiname, userRights, doc);

            saveDocument(doc, null, context);

            if (log.isWarnEnabled())
                log.warn("createUser: before get All Group");

            // Now let's add the user to XWiki.XWikiAllGroup
            SetUserDefaultGroup(context, fullwikiname);


            return 1;
        } catch (Exception e) {
            Object[] args = {"XWiki." + xwikiname};
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER,
                    XWikiException.ERROR_XWIKI_USER_CREATE,
                    "Cannot create user {0}", e, args);
        }
    }

    public void SetUserDefaultGroup(XWikiContext context, String fullwikiname) throws XWikiException {
        BaseClass gclass = getGroupClass(context);

        XWikiDocument allgroupdoc = getDocument("XWiki.XWikiAllGroup", context);

        BaseObject memberobj = (BaseObject) gclass.newObject(context);
        memberobj.setClassName(gclass.getName());
        memberobj.setName(allgroupdoc.getFullName());
        memberobj.setStringValue("member", fullwikiname);
        allgroupdoc.addObject(gclass.getName(), memberobj);
        if (allgroupdoc.isNew()) {
            saveDocument(allgroupdoc, null, context);
        } else {
            getHibernateStore().saveXWikiObject(memberobj, context, true);
        }

        try {
            XWikiGroupService gservice = (XWikiGroupService) getGroupService();
            gservice.addUserToGroup(fullwikiname, context.getDatabase(), "XWiki.XWikiAllGroup");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ProtectUserPage(XWikiContext context, String fullwikiname, String userRights, XWikiDocument doc) throws XWikiException {
        BaseClass rclass = getRightsClass(context);
        // Add protection to the page
        BaseObject newrightsobject = (BaseObject) rclass.newObject(context);
        newrightsobject.setClassName(rclass.getName());
        newrightsobject.setName(fullwikiname);
        newrightsobject.setStringValue("groups", "XWiki.XWikiAdminGroup");
        newrightsobject.setStringValue("levelsye", userRights);
        newrightsobject.setIntValue("allow", 1);
        doc.addObject(rclass.getName(), newrightsobject);

        BaseObject newuserrightsobject = (BaseObject) rclass.newObject(context);
        newuserrightsobject.setClassName(rclass.getName());
        newuserrightsobject.setName(fullwikiname);
        newuserrightsobject.setStringValue("users", fullwikiname);
        newuserrightsobject.setStringValue("levels", userRights);
        newuserrightsobject.setIntValue("allow", 1);
        doc.addObject(rclass.getName(), newuserrightsobject);
    }

    public User getUser(XWikiContext context) {
        return null;
    }

    public void prepareResources(XWikiContext context) {
        if (context.get("msg") == null) {
            String language = getLanguagePreference(context);
            if (context.getResponse() != null)
                context.getResponse().setLocale(new Locale(language));
            ResourceBundle bundle = ResourceBundle.getBundle("ApplicationResources", new Locale(language));
            if (bundle == null)
                bundle = ResourceBundle.getBundle("ApplicationResources");
            XWikiMessageTool msg = new XWikiMessageTool(bundle);
            context.put("msg", msg);
            VelocityContext vcontext = ((VelocityContext) context.get("vcontext"));
            if (vcontext!=null)
                vcontext.put("msg", msg);
        }
    }


    public XWikiUser checkAuth(XWikiContext context) throws XWikiException {
        return getAuthService().checkAuth(context);
    }

    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context)
            throws XWikiException {
        return getRightService().checkAccess(action, doc, context);
    }

    public String include(String topic, XWikiContext context, boolean isForm) throws XWikiException {
        String database = null, incdatabase = null;
        Document currentdoc = null, currentcdoc = null, currenttdoc = null;
        Document gcurrentdoc = null, gcurrentcdoc = null, gcurrenttdoc = null;
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        if (vcontext != null) {
            currentdoc = (Document) vcontext.get("doc");
            currentcdoc = (Document) vcontext.get("cdoc");
            currenttdoc = (Document) vcontext.get("tdoc");
        }
        Map gcontext = (Map) context.get("gcontext");
        if (gcontext != null) {
            gcurrentdoc = (Document) gcontext.get("doc");
            gcurrentcdoc = (Document) gcontext.get("cdoc");
            gcurrenttdoc = (Document) gcontext.get("tdoc");
        }


        try {

            XWikiDocument doc = null;
            try {
                log.debug("Including Topic " + topic);

                int i0 = topic.indexOf(":");
                if (i0 != -1) {
                    incdatabase = topic.substring(0, i0);
                    topic = topic.substring(i0 + 1);
                    database = context.getDatabase();
                    context.setDatabase(incdatabase);
                }

                try {
                    Integer includecounter = ((Integer) context.get("include_counter"));
                    if (includecounter != null) {
                        context.put("include_counter", new Integer(1 + includecounter.intValue()));
                    } else {
                        includecounter = new Integer(1);
                        context.put("include_counter", includecounter);
                    }

                    if ((includecounter.intValue() > 30)
                            || ((database.equals(incdatabase) && (topic.equals(currentdoc.getFullName()))))) {
                        log.warn("Error on too many recursive includes for topic " + topic);
                        return "Cannot make recursive include";
                    }
                } catch (Exception e) {
                }

                doc = getDocument(((XWikiDocument) context.get("doc")).getWeb(), topic, context);

                if (checkAccess("view", doc, context) == false) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                            XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                            "Access to this document is denied");
                }

            } catch (XWikiException e) {
                log.warn("Exception Including Topic " + topic, e);
                return "Topic " + topic + " does not exist";
            }

            XWikiDocument contentdoc = doc.getTranslatedDocument(context);

            if (isForm) {
                // We do everything in the context of the including document
                if (database != null)
                    context.setDatabase(database);

                return getRenderingEngine().renderText(contentdoc.getContent(), contentdoc, (XWikiDocument) context.get("doc"), context);
            } else {
                // We stay in the context included document
                return getRenderingEngine().renderText(contentdoc.getContent(), contentdoc, doc, context);
            }
        } finally {
            if (database != null)
                context.setDatabase(database);

            try {
                Integer includecounter = ((Integer) context.get("include_counter"));
                if (includecounter != null) {
                    context.put("include_counter", new Integer(includecounter.intValue() - 1));
                }
            } catch (Exception e) {
            }

            if (currentdoc != null) {
                if (vcontext != null)
                    vcontext.put("doc", currentdoc);
            }
            if (gcurrentdoc != null) {
                if (gcontext != null)
                    gcontext.put("doc", gcurrentdoc);
            }
            if (currentcdoc != null) {
                if (vcontext != null)
                    vcontext.put("cdoc", currentcdoc);
            }
            if (gcurrentcdoc != null) {
                if (gcontext != null)
                    gcontext.put("cdoc", gcurrentcdoc);
            }
            if (currenttdoc != null) {
                if (vcontext != null)
                    vcontext.put("tdoc", currenttdoc);
            }
            if (gcurrenttdoc != null) {
                if (gcontext != null)
                    gcontext.put("tdoc", gcurrenttdoc);
            }
        }
    }

    public void deleteDocument(XWikiDocument doc, XWikiContext context) throws XWikiException {
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

    public static Object getPrivateField(Object obj, String fieldName) {
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

    public static Object callPrivateMethod(Object obj, String methodName) {
        return callPrivateMethod(obj, methodName, null, null);
    }

    public static Object callPrivateMethod(Object obj, String methodName, Class[] classes, Object[] args) {
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

    public boolean copyDocument(String docname, String targetdocname, XWikiContext context) throws XWikiException {
        return copyDocument(docname, targetdocname, null, null, null, false, context);
    }

    public boolean copyDocument(String docname, String targetdocname, String wikilanguage, XWikiContext context) throws XWikiException {
        return copyDocument(docname, targetdocname, null, null, wikilanguage, false, context);
    }

    public boolean copyDocument(String docname, String sourceWiki, String targetWiki, String wikilanguage, XWikiContext context) throws XWikiException {
        return copyDocument(docname, docname, sourceWiki, targetWiki, wikilanguage, true, context);
    }

    public boolean copyDocument(String docname, String targetdocname, String sourceWiki, String targetWiki, String wikilanguage, boolean reset, XWikiContext context) throws XWikiException {
        String db = context.getDatabase();
        try {
            if (sourceWiki != null)
                context.setDatabase(sourceWiki);
            XWikiDocument sdoc = getDocument(docname, context);
            if (!sdoc.isNew()) {
                if (log.isInfoEnabled())
                    log.info("Copying document: " + docname + " (default) to " + targetdocname + " on wiki " + targetWiki);

                if (targetWiki != null)
                    context.setDatabase(targetWiki);
                XWikiDocument tdoc = getDocument(targetdocname, context);
                // There is already an existing document
                if (!tdoc.isNew())
                    return false;

                if (wikilanguage == null) {
                    if (docname.equals(targetdocname))
                        tdoc = (XWikiDocument) sdoc.clone();
                    else
                        tdoc = (XWikiDocument) sdoc.renameDocument(targetdocname, context);
                    // forget past versions
                    if (reset) {
                        tdoc.setVersion("1.1");
                        tdoc.setRCSArchive(null);
                    }
                    saveDocument(tdoc, context);

                    List attachlist = tdoc.getAttachmentList();
                    if (attachlist.size() > 0) {
                        for (int i = 0; i < attachlist.size(); i++) {
                            XWikiAttachment attachment = (XWikiAttachment) attachlist.get(i);
                            getStore().saveAttachmentContent(attachment, false, context, true);
                        }
                    }

                    // Now we need to copy the translations
                    if (sourceWiki != null)
                        context.setDatabase(sourceWiki);
                    List tlist = sdoc.getTranslationList(context);
                    for (int i = 0; i < tlist.size(); i++) {
                        String clanguage = (String) tlist.get(i);
                        XWikiDocument stdoc = sdoc.getTranslatedDocument(clanguage, context);
                        if (log.isInfoEnabled())
                            log.info("Copying document: " + docname + "(" + clanguage + ") to " + targetdocname + " on wiki " + targetWiki);

                        if (targetWiki != null)
                            context.setDatabase(targetWiki);
                        XWikiDocument ttdoc = tdoc.getTranslatedDocument(clanguage, context);

                        // There is already an existing document
                        if (ttdoc != tdoc)
                            return false;

                        if (docname.equals(targetdocname))
                            ttdoc = (XWikiDocument) stdoc.clone();
                        else
                            ttdoc = stdoc.renameDocument(targetdocname, context);

                        // forget past versions
                        if (reset) {
                            ttdoc.setVersion("1.1");
                            ttdoc.setRCSArchive(null);
                        }
                        saveDocument(ttdoc, context);
                    }
                } else {
                    // We want only one language in the end
                    if (sourceWiki != null)
                        context.setDatabase(sourceWiki);
                    XWikiDocument stdoc = sdoc.getTranslatedDocument(wikilanguage, context);
                    if (targetWiki != null)
                        context.setDatabase(targetWiki);
                    if (docname.equals(targetdocname))
                        tdoc = (XWikiDocument) stdoc.clone();
                    else
                        tdoc = stdoc.renameDocument(targetdocname, context);
                    // forget language
                    tdoc.setDefaultLanguage(wikilanguage);
                    tdoc.setLanguage("");
                    // forget past versions
                    if (reset) {
                        tdoc.setVersion("1.1");
                        tdoc.setRCSArchive(null);
                    }
                    saveDocument(tdoc, context);
                    List attachlist = tdoc.getAttachmentList();
                    if (attachlist.size() > 0) {
                        for (int i = 0; i < attachlist.size(); i++) {
                            XWikiAttachment attachment = (XWikiAttachment) attachlist.get(i);
                            getStore().saveAttachmentContent(attachment, false, context, true);
                        }
                    }
                }
            }
            return true;
        } finally {
            context.setDatabase(db);
        }
    }

    public int copyWikiWeb(String web, String sourceWiki, String targetWiki, String wikilanguage, XWikiContext context) throws XWikiException {
        String db = context.getDatabase();
        int nb = 0;
        try {
            String sql = "";
            if (web != null)
                sql = "where doc.web = '" + Utils.SQLFilter(web) + "'";

            context.setDatabase(sourceWiki);
            List list = getStore().searchDocumentsNames(sql, context);
            if (log.isInfoEnabled())
                log.info("Copying " + list.size() + " documents from wiki " + sourceWiki + " to wiki " + targetWiki);

            for (Iterator it = list.iterator(); it.hasNext();) {
                String docname = (String) it.next();
                copyDocument(docname, sourceWiki, targetWiki, wikilanguage, context);
                nb++;
            }
            return nb;
        } finally {
            context.setDatabase(db);
        }
    }

    public int copyWiki(String sourceWiki, String targetWiki, String language, XWikiContext context) throws XWikiException {
        return copyWikiWeb(null, sourceWiki, targetWiki, language, context);
    }

    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName, String description, String wikilanguage, boolean failOnExist, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();
        wikiName = wikiName.toLowerCase();

        try {
            XWikiDocument userdoc = getDocument(wikiAdmin, context);

            // User does not exist
            if (userdoc.isNew()) {
                if (log.isErrorEnabled()) log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "user does not exist");
                return -2;
            }

            // User is not active
            if (!(userdoc.getIntValue("XWiki.XWikiUsers", "active") == 1)) {
                if (log.isErrorEnabled()) log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "user is not active");
                return -3;
            }


            String wikiForbiddenList = Param("xwiki.virtual.reserved_wikis");
            if (Util.contains(wikiName, wikiForbiddenList, ", ")) {
                if (log.isErrorEnabled()) log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki name is forbidden");
                return -4;
            }

            String wikiServerPage = "XWikiServer" + wikiName.substring(0, 1).toUpperCase() + wikiName.substring(1);
            // Verify is server page already exist
            XWikiDocument serverdoc = getDocument("XWiki", wikiServerPage, context);
            if (serverdoc.isNew()) {
                // Create Wiki Server page
                serverdoc.setStringValue("XWiki.XWikiServerClass", "server", wikiUrl);
                serverdoc.setLargeStringValue("XWiki.XWikiServerClass", "owner", wikiAdmin);
                if (description != null)
                    serverdoc.setStringValue("XWiki.XWikiServerClass", "description", description);
                if (wikilanguage != null)
                    serverdoc.setStringValue("XWiki.XWikiServerClass", "language", wikilanguage);
                serverdoc.setContent("#includeForm(\"XWiki.XWikiServerForm\")\n");
                serverdoc.setParent("XWiki.XWikiServerClass");
                saveDocument(serverdoc, context);
            } else {
                // If we are not allowed to continue if server page already exists

                if (failOnExist) {
                    if (log.isErrorEnabled()) log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki server page already exists");
                    return -5;
                } else if (log.isWarnEnabled()) log.warn("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki server page already exists");

            }

            // Create wiki database
            try {
                context.setDatabase(getDatabase());
                getStore().createWiki(wikiName, context);
            } catch (XWikiException e) {
                if (log.isErrorEnabled()) {
                    if (e.getCode() == 10010)
                        log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database already exists");
                    else if (e.getCode() == 10011)
                        log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database creation failed");
                    else
                        log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database creation threw exception", e);
                }
            } catch (Exception e) {
                log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database creation threw exception", e);
            }

            try {
                updateDatabase(wikiName, true, context);
            } catch (Exception e) {
                log.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: " + "wiki database shema update threw exception", e);
                return -6;
            }


            // Copy base wiki
            int nb = copyWiki(baseWikiName, wikiName, wikilanguage, context);
            // Save the number of docs copied in the context
            context.put("nbdocs", new Integer(nb));

            // Create user page in his wiki
            // Let's not create it anymore.. this makes the creator loose
            // super admin rights on his wiki
            // copyDocument(wikiAdmin, getDatabase(), wikiName, wikilanguage, context);

            // Modify rights in user wiki
            context.setDatabase(wikiName);
            XWikiDocument wikiprefdoc = getDocument("XWiki.XWikiPreferences", context);
            wikiprefdoc.setStringValue("XWiki.XWikiGlobalRights", "users", wikiAdmin);
            wikiprefdoc.setStringValue("XWiki.XWikiGlobalRights", "levels", "admin, edit");
            wikiprefdoc.setIntValue("XWiki.XWikiGlobalRights", "allow", 1);
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


    public URL getServerURL(String database, XWikiContext context) throws MalformedURLException {
        String serverurl = null;
        if (database != null) {
            String db = context.getDatabase();
            try {
                context.setDatabase(getDatabase());
                XWikiDocument doc = getDocument("XWiki.XWikiServer"
                        + database.substring(0, 1).toUpperCase()
                        + database.substring(1), context);
                BaseObject serverobject = doc.getObject("XWiki.XWikiServerClass", 0);
                String server = serverobject.getStringValue("server");
                int mode = serverobject.getIntValue("secure");
                if (server != null) {
                    serverurl = ((mode == 1) ? "https://" : "http://")
                            + server + "/";
                }

            } catch (Exception e) {
            } finally {
                context.setDatabase(db);
            }
        }

        return new URL(serverurl);
    }

    public String getURL(String fullname, String action, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();
        try {
            fullname = Util.getName(fullname, context);
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(fullname, context);

            URL url = context.getURLFactory().createURL(doc.getWeb(), doc.getName(), action, context);
            return context.getURLFactory().getURL(url, context);
        } finally {
            context.setDatabase(database);
        }
    }

    public String getURL(String fullname, String action, String querystring, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();
        try {
            fullname = Util.getName(fullname, context);
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(fullname, context);

            URL url = context.getURLFactory().createURL(doc.getWeb(), doc.getName(),
                    action, querystring, null, context);
            return context.getURLFactory().getURL(url, context);
        } finally {
            context.setDatabase(database);
        }
    }

    public String getAttachmentURL(String fullname, String filename, XWikiContext context) throws XWikiException {
        String database = context.getDatabase();
        try {
            fullname = Util.getName(fullname, context);
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(fullname, context);
            return doc.getAttachmentURL(filename, "download", context);
        } finally {
            context.setDatabase(database);
        }
    }

    // Usefull date functions
    public Date getCurrentDate() {
        return new Date();
    }

    public int getTimeDelta(long time) {
        Date ctime = new Date();
        return (int) (ctime.getTime() - time);
    }

    public Date getDate(long time) {
        return new Date(time);
    }

    public boolean isMultiLingual(XWikiContext context) {
        return "1".equals(getXWikiPreference("multilingual", "1", context));
    }

    public boolean isVirtual() {
        // With exo we can't be using virtual mode
        if (isExo())
            return false;

        return "1".equals(Param("xwiki.virtual"));
    }

    public boolean isExo() {
        return "1".equals(Param("xwiki.exo"));
    }

    public boolean isLDAP() {
        return "1".equals(Param("xwiki.authentication.ldap"));
    }

    public int checkActive(XWikiContext context) throws XWikiException {
        int active = 1;
        String checkactivefield = getXWikiPreference("auth_active_check", context);
        if (checkactivefield.equals("1")) {
            String username = context.getUser();
            XWikiDocument userdoc = getDocument(username, context);
            active = userdoc.getIntValue("XWiki.XWikiUsers", "active");
        }
        return active;
    }

    public boolean prepareDocuments(XWikiRequest request, XWikiContext context, VelocityContext vcontext) throws XWikiException {
        // From there we will try to catch any exceptions and show a nice page
        XWikiDocument doc = null;

        if (context.getMode() == XWikiContext.MODE_PORTLET) {
            if (request.getParameter("topic") != null)
                doc = getDocument(request.getParameter("topic"), context);
            else
                doc = getDocument("Main.WebHome", context);
        } else if (context.getMode() == XWikiContext.MODE_XMLRPC) {
            doc = context.getDoc();
        } else
            doc = getDocumentFromPath(request.getPathInfo(), context);

        context.put("doc", doc);
        vcontext.put("doc", new Document(doc, context));
        vcontext.put("cdoc", vcontext.get("doc"));

        // We need to check rights before we look for translations
        // Otherwise we don't have the user language
        if (checkAccess(context.getAction(), doc, context) == false) {
            Object[] args = {doc.getFullName(), context.getUser()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                    "Access to document {0} has been denied to user {1}", null, args);
        } else if (checkActive(context) == 0) {
            Object[] args = {context.getUser()};
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INACTIVE,
                    "User {0} account is inactive", null, args);
        }

        if (isMultiLingual(context)) {
            XWikiDocument tdoc = doc.getTranslatedDocument(context);
            context.put("tdoc", tdoc);
            vcontext.put("tdoc", new Document(tdoc, context));
        } else {
            context.put("tdoc", doc);
            vcontext.put("tdoc", new Document(doc, context));
        }
        return true;
    }

    public XWikiEngineContext getEngineContext() {
        return engine_context;
    }

    public void setEngineContext(XWikiEngineContext engine_context) {
        this.engine_context = engine_context;
    }

    public URLPatternMatcher getUrlPatternMatcher() {
        return urlPatternMatcher;
    }

    public void setUrlPatternMatcher(URLPatternMatcher urlPatternMatcher) {
        this.urlPatternMatcher = urlPatternMatcher;
    }

    public void setAuthService(XWikiAuthService authService) {
        this.authService = authService;
    }

    public void setRightService(XWikiRightService rightService) {
        this.rightService = rightService;
    }

    public XWikiGroupService getGroupService() {
        if (groupService == null) {
            String groupClass;
            if (isExo())
                groupClass = Param("xwiki.authentication.groupclass", "com.xpn.xwiki.user.impl.exo.ExoGroupServiceImpl");
            else
                groupClass = Param("xwiki.authentication.groupclass", "com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl");

            try {
                groupService = (XWikiGroupService) Class.forName(groupClass).newInstance();
                groupService.init(this);
            } catch (Exception e) {
                e.printStackTrace();
                if (isExo())
                    groupService = new ExoGroupServiceImpl();
                else
                    groupService = new XWikiGroupServiceImpl();
            }
        }
        return groupService;
    }

    public void setGroupService(XWikiGroupService groupService) {
        this.groupService = groupService;
    }

    // added some log statements to make debugging easier - LBlaze 2005.06.02
    public XWikiAuthService getAuthService() {
        if (authService == null) {

            log.info("Initializing AuthService...");

            String authClass = Param("xwiki.authentication.authclass");
            if (authClass != null) {
                if (log.isDebugEnabled()) log.debug("Using custom AuthClass " + authClass + ".");
            } else {

                if (isExo())
                    authClass = "com.xpn.xwiki.user.impl.exo.ExoAuthServiceImpl";
                else if (isLDAP())
                    authClass = "com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl";
                else
                    authClass = "com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl";

                if (log.isDebugEnabled()) log.debug("Using default AuthClass " + authClass + ".");

            }

            try {

                authService = (XWikiAuthService) Class.forName(authClass).newInstance();

                log.debug("Initialized AuthService using Relfection.");

            } catch (Exception e) {

                log.warn("Failed to initialize AuthService " + authClass + " using Reflection, trying default implementations using 'new'.", e);

                // e.printStackTrace(); - not needed? -LBlaze

                // LDAP support wasn't here before, I assume it should be? -LBlaze

                if (isExo())
                    authService = new ExoAuthServiceImpl();
                else if (isLDAP())
                    authService = new LDAPAuthServiceImpl();
                else
                    authService = new XWikiAuthServiceImpl();

                if (log.isDebugEnabled()) log.debug("Initialized AuthService " + authService.getClass().getName() + " using 'new'.");

            }
        }
        return authService;
    }

    // added some log statements to make debugging easier - LBlaze 2005.06.02
    public XWikiRightService getRightService() {
        if (rightService == null) {

            log.info("Initializing RightService...");

            String rightsClass = Param("xwiki.authentication.rightsclass");
            if (rightsClass != null) {
                if (log.isDebugEnabled()) log.debug("Using custom RightsClass " + rightsClass + ".");
            } else {
                rightsClass = "com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl";
                if (log.isDebugEnabled()) log.debug("Using default RightsClass " + rightsClass + ".");
            }


            try {

                rightService = (XWikiRightService) Class.forName(rightsClass).newInstance();
                log.debug("Initialized RightService using Reflection.");

            } catch (Exception e) {

                log.warn("Failed to initialize RightService " + rightsClass + " using Reflection, trying default implementation using 'new'.", e);

                //e.printStackTrace(); - not needed? -LBlaze

                rightService = new XWikiRightServiceImpl();

                if (log.isDebugEnabled()) log.debug("Initialized RightService " + authService.getClass().getName() + " using 'new'.");

            }
        }
        return rightService;
    }

    public XWikiStatsService getStatsService(XWikiContext context) {
        if (statsService == null) {
            if ("1".equals(Param("xwiki.stats", "1"))) {
                String storeClass = Param("xwiki.stats.class", "com.xpn.xwiki.stats.impl.XWikiStatsServiceImpl");
                try {
                    statsService = (XWikiStatsService) Class.forName(storeClass).newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    statsService = new XWikiStatsServiceImpl();
                }
                statsService.init(context);
            }
        }
        return statsService;
    }

    public XWikiURLFactoryService getURLFactoryService()
    {
        if (urlFactoryService == null)
        {

            log.info("Initializing URLFactory Service...");

            String urlFactoryServiceClass = Param("xwiki.urlfactory.serviceclass");

            if (urlFactoryServiceClass != null)
            {
                try
                {
                    if (log.isDebugEnabled()) log.debug("Using custom URLFactory Service Class " + urlFactoryServiceClass + ".");
                    urlFactoryService = (XWikiURLFactoryService) Class.forName(urlFactoryServiceClass).newInstance();
                    urlFactoryService.init(this);
                    log.debug("Initialized URLFactory Service using Reflection.");
                }
                catch (Exception e)
                {
                    urlFactoryService = null;
                    log.warn("Failed to initialize URLFactory Service  " + urlFactoryServiceClass + " using Reflection, trying default implementation using 'new'.", e);
                }
            }
            if (urlFactoryService == null)
            {
                if (log.isDebugEnabled()) log.debug("Using default URLFactory Service Class " + urlFactoryServiceClass + ".");
                urlFactoryService = new XWikiURLFactoryServiceImpl();
                urlFactoryService.init(this);
            }
        }
        return urlFactoryService;
    }

    public Object getService(String className) throws XWikiException {
        try {
            RootContainer manager = RootContainer.getInstance();
            return manager.getComponentInstanceOfType(Class.forName(className));
        } catch (Exception e) {
            Object[] args = {className};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SERVICE_NOT_FOUND,
                    "Service {0} not found", e, args);
        }
    }

    public Object getPortalService(String className) throws XWikiException {
        try {
            PortalContainer manager = PortalContainer.getInstance();
            return manager.getComponentInstanceOfType(Class.forName(className));
        } catch (Exception e) {
            Object[] args = {className};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SERVICE_NOT_FOUND,
                    "Service {0} not found", e, args);
        }
    }


    public ZipOutputStream getZipOutputStream(XWikiContext context) throws IOException {
        return new ZipOutputStream(context.getResponse().getOutputStream());
    }

    private Map getSearchEngineRules(XWikiContext context) {
        // We currently hardcode the rules
        // We will put them in the preferences soon
        Map map = new HashMap();
        map.put("Google", new SearchEngineRule("google.", "s/(^|.*&)q=(.*?)(&.*|$)/$2/"));
        map.put("MSN", new SearchEngineRule("search.msn.", "s/(^|.*&)q=(.*?)(&.*|$)/$2/"));
        map.put("Yahoo", new SearchEngineRule("search.yahoo.", "s/(^|.*&)p=(.*?)(&.*|$)/$2/"));
        map.put("Voila", new SearchEngineRule("voila.fr", "s/(^|.*&)kw=(.*?)(&.*|$)/$2/"));
        return map;
    }

    public String getRefererText(String referer, XWikiContext context) {
        try {
            URL url = new URL(referer);
            Map searchengines = getSearchEngineRules(context);
            if (searchengines != null) {
                Iterator seit = searchengines.keySet().iterator();
                while (seit.hasNext()) {
                    String sengine = (String) seit.next();
                    SearchEngineRule senginerule = (SearchEngineRule) searchengines.get(sengine);
                    String host = url.getHost();
                    int i1 = host.indexOf(senginerule.getHost());
                    if (i1 != -1) {
                        String query = context.getUtil().substitute(senginerule.getRegEx(), url.getQuery());
                        if ((query != null) || (!query.equals(""))) {
                            // We return the query text instead of the full referer
                            return host.substring(i1) + ":" + query;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        String result = referer.substring(referer.indexOf("://") + 3);
        if (result.endsWith("/"))
            return result.substring(0, result.length() - 1);
        else
            return result;
    }

    public boolean isMySQL() {
        return "net.sf.hibernate.dialect.MySQLDialect".equals(getHibernateStore().getConfiguration().getProperties().get("dialect"));
    }

    public String getFullNameSQL() {
        return getFullNameSQL(true);
    }

    public String getFullNameSQL(boolean newFullName) {
        if (newFullName)
            return "doc.fullName";

        if (fullNameSQL==null) {
            if (isMySQL())
                fullNameSQL = "CONCAT(doc.web,'.',doc.name)";
            else
                fullNameSQL = "doc.web||'.'||doc.name";
        }
        return fullNameSQL;
    }

    public String getDocName(String docname) {
        return docname.substring(docname.indexOf(".") + 1);
    }

    public String getUserName(String user, XWikiContext context) {
        return getUserName(user, null, true, context);
    }

    public String getUserName(String user, String format, XWikiContext context) {
        return getUserName(user, format, true, context);
    }

    public String getUserName(String user, String format, boolean link, XWikiContext context) {
        XWikiDocument userdoc = null;
        try {
            userdoc = getDocument(user, context);
            if (userdoc == null)
                return user;

            BaseObject userobj = userdoc.getObject("XWiki.XWikiUsers");
            if (userobj == null)
                return userdoc.getName();

            Set proplist = userobj.getPropertyList();
            String text;

            if (format == null) {
                text = userobj.getStringValue("first_name") + " " + userobj.getStringValue("last_name");
                if (text.trim().equals("")) {
                    text = userdoc.getName();
                }
            } else {

                VelocityContext vcontext = new VelocityContext();
                for (Iterator it = proplist.iterator(); it.hasNext();) {
                    String propname = (String) it.next();
                    vcontext.put(propname, userobj.getStringValue(propname));
                }
                text = XWikiVelocityRenderer.evaluate(format, "", vcontext, context);
            }

            if (link == false)
                return text;
            else {
                return "<span class=\"wikilink\"><a href=\"" + userdoc.getURL("view", context) + "\">" + text + "</a></span>";
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (userdoc != null)
                return userdoc.getName();
            else
                return user;
        }
    }

    public String getLocalUserName(String user, XWikiContext context) {
        return getUserName(user.substring(user.indexOf(":") + 1), null, true, context);
    }

    public String getLocalUserName(String user, String format, XWikiContext context) {
        return getUserName(user.substring(user.indexOf(":") + 1), format, true, context);
    }

    public String getLocalUserName(String user, String format, boolean link, XWikiContext context) {
        return getUserName(user.substring(user.indexOf(":") + 1), format, link, context);
    }

    public String formatDate(Date date, String format, XWikiContext context) {
        String xformat = format;
        String defaultFormat = "yyyy/MM/dd HH:mm";

        if (format == null) {
            xformat = getXWikiPreference("dateformat", defaultFormat, context);
        }
        try {
            DateFormatSymbols formatSymbols = null;
            try {
                String language = getLanguagePreference(context);
                formatSymbols = new DateFormatSymbols(new Locale(language));
            } catch (Exception e2) {
                String language = getXWikiPreference("language", context);
                if ((language != null) && (!language.equals("")))
                    formatSymbols = new DateFormatSymbols(new Locale(language));
            }

            if (formatSymbols != null)
                return (new SimpleDateFormat(xformat, formatSymbols)).format(date);
            else
                return (new SimpleDateFormat(xformat)).format(date);
        } catch (Exception e) {
            e.printStackTrace();
            if (format == null) {
                if (xformat.equals(defaultFormat))
                    return date.toString();
                else
                    return formatDate(date, defaultFormat, context);
            } else {
                return formatDate(date, null, context);
            }
        }
    }

    public boolean exists(String fullname, XWikiContext context) {
        String server = null, database = null;
        try {
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(fullname, context);
            server = doc.getDatabase();

            if (server != null) {
                database = context.getDatabase();
                context.setDatabase(server);
            }

            return getStore().exists(doc, context);
        } catch (XWikiException e) {
            return false;
        } finally {
            if ((server != null) && (database != null)) {
                context.setDatabase(database);
            }
        }
    }

    public String getAdType(XWikiContext context) {
        String adtype = "";
        if (isVirtual()) {
            XWikiDocument wikiServer = context.getWikiServer();
            if (wikiServer != null) {
                adtype = wikiServer.getStringValue("XWiki.XWikiServerClass", "adtype");
            }
        } else {
            adtype = getXWikiPreference("adtype", "", context);
        }
        if (adtype.equals(""))
            adtype = Param("xwiki.ad.type", "");
        if (adtype.equals(""))
            adtype = "google";
        return adtype;
    }

    public String getAdClientId(XWikiContext context) {
        final String defaultadclientid = "pub-2778691407285481";
        String adclientid = "";
        if (isVirtual()) {
            XWikiDocument wikiServer = context.getWikiServer();
            if (wikiServer != null) {
                adclientid = wikiServer.getStringValue("XWiki.XWikiServerClass", "adclientid");
            }
        } else {
            adclientid = getXWikiPreference("adclientid", "", context);
        }

        if (adclientid.equals(""))
            adclientid = Param("xwiki.ad.clientid", "");
        if (adclientid.equals(""))
            adclientid = defaultadclientid;
        return adclientid;
    }

    public XWikiPluginInterface getPlugin(String name, XWikiContext context) {
        XWikiPluginManager plugins = getPluginManager();
        Vector pluginlist = plugins.getPlugins();
        for (int i = 0; i < pluginlist.size(); i++) {
            String pluginname = (String) pluginlist.get(i);
            if (pluginname.equals(name)) {
                return (XWikiPluginInterface) plugins.getPlugin(pluginname);
            }
        }
        return null;
    }

    public Api getPluginApi(String name, XWikiContext context) {
        XWikiPluginInterface plugin = (XWikiPluginInterface) getPlugin(name, context);
        if (plugin != null)
            return plugin.getPluginApi(plugin, context);
        else
            return null;
    }

    public static Map getThreadMap() {
        return threadMap;
    }

    public static void setThreadMap(Map threadMap) {
        XWiki.threadMap = threadMap;
    }


    /*  public XWikiExecutionInfo getExecutionInfo(XWikiContext context) {
        Thread thread = Thread.currentThread();
        XWikiExecutionInfo info = getThreadMap().get(thread);
        if (info==null) {
            info = new XWikiExecutionInfo(thread, getRequestURL(context.getRequest()))
        }
    }
    */

    public XWikiCacheService getCacheService() {
        if (cacheService == null) {
            String cacheClass;
            cacheClass = Param("xwiki.cache.cacheclass", "com.xpn.xwiki.cache.impl.OSCacheService");

            try {
                cacheService = (XWikiCacheService) Class.forName(cacheClass).newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                cacheService = new OSCacheService();
            }
        }
        return cacheService;
    }

    public String getURLContent(String surl) throws IOException {
        HttpClient client = new HttpClient();

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // execute the GET
            int status = client.executeMethod(get);

            // print the status and response
            return get.getResponseBodyAsString();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public String getURLContent(String surl, String username, String password) throws IOException {
        HttpClient client = new HttpClient();

        // pass our credentials to HttpClient, they will only be used for
        // authenticating to servers with realm "realm", to authenticate agains
        // an arbitrary realm change this to null.
        client.getState().setCredentials(null,
                null,
                new UsernamePasswordCredentials(username, password));

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // Tell the GET method to automatically handle authentication. The
            // method will use any appropriate credentials to handle basic
            // authentication requests.  Setting this value to false will cause
            // any request for authentication to return with a status of 401.
            // It will then be up to the client to handle the authentication.
            get.setDoAuthentication(true);

            // execute the GET
            int status = client.executeMethod(get);

            // print the status and response
            return get.getResponseBodyAsString();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public byte[] getURLContentAsBytes(String surl) throws IOException {
        HttpClient client = new HttpClient();

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // execute the GET
            int status = client.executeMethod(get);

            // print the status and response
            return get.getResponseBody();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public byte[] getURLContentAsBytes(String surl, String username, String password) throws IOException {
        HttpClient client = new HttpClient();

        // pass our credentials to HttpClient, they will only be used for
        // authenticating to servers with realm "realm", to authenticate agains
        // an arbitrary realm change this to null.
        client.getState().setCredentials(null,
                null,
                new UsernamePasswordCredentials(username, password));

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // Tell the GET method to automatically handle authentication. The
            // method will use any appropriate credentials to handle basic
            // authentication requests.  Setting this value to false will cause
            // any request for authentication to return with a status of 401.
            // It will then be up to the client to handle the authentication.
            get.setDoAuthentication(true);

            // execute the GET
            int status = client.executeMethod(get);

            // print the status and response
            return get.getResponseBody();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public List getSpaces(XWikiContext context) throws XWikiException {

        List webs = this.search("select distinct doc.web from XWikiDocument doc", context);
        return webs;
    }

    public List getSpaceDocsName(String spaceName, XWikiContext context) throws XWikiException {

        List docs = this.search("select distinct doc.name from XWikiDocument doc", new Object[][]{{"doc.web", spaceName}}, context);
        return docs;
    }

    public List getIncludedMacros(String defaultweb, String content, XWikiContext context) {
        try {
            String pattern = "#includeMacros\\(\"(.*?)\"\\)";
            List list = context.getUtil().getMatches(content, pattern, 1);
            for (int i = 0; i < list.size(); i++) {
                try {
                    String name = (String) list.get(i);
                    if (name.indexOf(".") == -1) {
                        list.set(i, defaultweb + "." + name);
                    }
                } catch (Exception e) {
                    // This should never happen
                    e.printStackTrace();
                    return null;
                }
            }

            return list;
        } catch (Exception e) {
            // This should never happen
            e.printStackTrace();
            return null;
        }
    }

    public String getFlash(String url, String width, String height, XWikiContext context) {
        VelocityContext vorigcontext = ((VelocityContext) context.get("vcontext"));
        try {
            VelocityContext vcontext = (VelocityContext) vorigcontext.clone();
            vcontext.put("flashurl", url);
            vcontext.put("width", width);
            vcontext.put("height", height);
            context.put("vcontext", vcontext);
            return parseTemplate("flash.vm", context);
        } finally {
            context.put("vcontext", vorigcontext);
        }
    }

    /**
     * accessor for the isReadOnly instance var.
     *
     * @see #isReadOnly
     *
     */
    public boolean isReadOnly () {
        return isReadOnly;
    }

    public void setReadOnly (boolean readOnly) {
        isReadOnly = readOnly;
    }

    public void deleteAllDocuments(XWikiDocument doc, XWikiContext context) throws XWikiException {
        // Delete all documents
        List list = doc.getTranslationList(context);
        for (int i=0;i<list.size();i++) {
            String lang = (String) list.get(i);
            XWikiDocument tdoc = doc.getTranslatedDocument(lang, context);
            deleteDocument(tdoc, context);
        }
        deleteDocument(doc, context);
    }

    public void refreshLinks(XWikiContext context)throws XWikiException{
       // refreshes all Links of each doc of the wiki
        List docs = this.search("select doc.fullName from XWikiDocument as doc", context);
        for (int i=0;i<docs.size();i++){
            XWikiDocument myDoc = this.getDocument((String)docs.get(i), context);
            myDoc.getStore().saveLinks(myDoc, context, true);
        }
    }

    public boolean hasBacklinks(XWikiContext context) {
        String bl = getXWikiPreference("backlinks", "", context);
        if ("1".equals(bl))
            return true;
        if ("0".equals(bl))
            return false;
        return "1".equals(Param("xwiki.backlinks"));
    }

    public XWikiDocument renamePage (XWikiDocument doc, XWikiContext context, String newFullName) throws XWikiException {
        XWikiDocument renamedDoc = doc.renameDocument(newFullName, context);
        saveDocument(renamedDoc, context);
        deleteDocument(doc, context);
        refreshLinks(context);
        return renamedDoc;
    }
}

