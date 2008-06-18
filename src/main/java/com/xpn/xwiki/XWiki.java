/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.DateFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.zip.ZipOutputStream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
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
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentSaveEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.criteria.api.XWikiCriteriaService;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiAttachmentArchive;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.notify.DocObjectChangedRule;
import com.xpn.xwiki.notify.PropertyChangedRule;
import com.xpn.xwiki.notify.XWikiActionRule;
import com.xpn.xwiki.notify.XWikiDocChangeNotificationInterface;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.notify.XWikiNotificationRule;
import com.xpn.xwiki.notify.XWikiPageNotification;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.classes.LevelsClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.UsersClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.plugin.query.QueryPlugin;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.render.DefaultXWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiRenderingEngine;
import com.xpn.xwiki.render.XWikiVelocityRenderer;
import com.xpn.xwiki.render.groovy.XWikiGroovyRenderer;
import com.xpn.xwiki.render.groovy.XWikiPageClassLoader;
import com.xpn.xwiki.stats.api.XWikiStatsService;
import com.xpn.xwiki.stats.impl.SearchEngineRule;
import com.xpn.xwiki.stats.impl.XWikiStatsServiceImpl;
import com.xpn.xwiki.store.AttachmentRecycleBinStore;
import com.xpn.xwiki.store.AttachmentVersioningStore;
import com.xpn.xwiki.store.VoidAttachmentVersioningStore;
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.store.hibernate.HibernateAttachmentVersioningStore;
import com.xpn.xwiki.store.jcr.XWikiJcrStore;
import com.xpn.xwiki.store.migration.AbstractXWikiMigrationManager;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl;
import com.xpn.xwiki.user.impl.exo.ExoAuthServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import com.xpn.xwiki.util.MenuSubstitution;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiServletURLFactory;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.web.XWikiURLFactoryService;
import com.xpn.xwiki.web.XWikiURLFactoryServiceImpl;
import com.xpn.xwiki.web.includeservletasstring.IncludeServletAsString;

public class XWiki implements XWikiDocChangeNotificationInterface
{
    protected static final Log LOG = LogFactory.getLog(XWiki.class);

    private XWikiConfig config;

    private XWikiStoreInterface store;

    private XWikiAttachmentStoreInterface attachmentStore;

    /** Store for attachment archives. */
    private AttachmentVersioningStore attachmentVersioningStore;

    private XWikiVersioningStoreInterface versioningStore;

    /** store for deleted documents */
    private XWikiRecycleBinStoreInterface recycleBinStore;

    /**
     * Storage for deleted attachment.
     * 
     * @since 1.4M1
     */
    private AttachmentRecycleBinStore attachmentRecycleBinStore;

    private XWikiRenderingEngine renderingEngine;

    private XWikiPluginManager pluginManager;

    private XWikiNotificationManager notificationManager;

    private XWikiAuthService authService;

    private XWikiRightService rightService;

    private XWikiGroupService groupService;

    private XWikiStatsService statsService;

    private XWikiURLFactoryService urlFactoryService;

    private XWikiCriteriaService criteriaService;

    private final Object AUTH_SERVICE_LOCK = new Object();

    private final Object RIGHT_SERVICE_LOCK = new Object();

    private final Object GROUP_SERVICE_LOCK = new Object();

    private final Object STATS_SERVICE_LOCK = new Object();

    private final Object URLFACTORY_SERVICE_LOCK = new Object();

    private MetaClass metaclass = MetaClass.getMetaClass();

    private boolean test = false;

    private String version = null;

    private XWikiEngineContext engine_context;

    private String database;

    private String fullNameSQL;

    private URLPatternMatcher urlPatternMatcher = new URLPatternMatcher();

    // These are caches in order to improve finding virtual wikis
    private List<String> virtualWikiList = new ArrayList<String>();

    /**
     * The cache containing the names of the wikis already initialized.
     */
    private Cache<String> virtualWikiMap;

    private boolean isReadOnly = false;

    public static final String CFG_ENV_NAME = "XWikiConfig";

    public static final String MACROS_FILE = "/templates/macros.txt";

    /**
     * File containing XWiki's version, in the format: <version name>.<SVN revision number>.
     */
    private static final String VERSION_FILE = "/WEB-INF/version.properties";

    /**
     * Property containing the version value in the {@link #VERSION_FILE} file.
     */
    private static final String VERSION_FILE_PROPERTY = "version";

    /*
     * i don't like using static variables like, but this avoid making a JNDI lookup with each request ...
     */
    private static String configPath = null;

    /*
     * Work directory
     */
    private static File workDir = null;

    /*
     * Temp directory
     */
    private static File tempDir = null;

    private static String getConfigPath() throws NamingException
    {
        if (configPath == null) {
            try {
                Context envContext = (Context) new InitialContext().lookup("java:comp/env");
                configPath = (String) envContext.lookup(CFG_ENV_NAME);
            } catch (Exception e) {
                configPath = "/WEB-INF/xwiki.cfg";
                LOG.debug("The xwiki.cfg file will be read from [" + configPath + "] because "
                    + "its location couldn't be read from the JNDI [" + CFG_ENV_NAME + "] "
                    + "variable in [java:comp/env].");
            }
        }
        return configPath;
    }

    public static XWiki getMainXWiki(XWikiContext context) throws XWikiException
    {
        String xwikiname = "xwiki";
        XWiki xwiki = null;
        XWikiEngineContext econtext = context.getEngineContext();

        try {
            if (context.getRequest().getRequestURL().indexOf("/testbin/") != -1) {
                xwikiname = "xwikitest";
                context.setDatabase("xwikitest");
                context.setOriginalDatabase("xwikitest");
            }
        } catch (Exception e) {
        }

        context.setMainXWiki(xwikiname);

        try {
            xwiki = (XWiki) econtext.getAttribute(xwikiname);
            if (xwiki == null) {
                synchronized (XWiki.class) {
                    xwiki = (XWiki) econtext.getAttribute(xwikiname);
                    if (xwiki == null) {
                        InputStream xwikicfgis = XWiki.readXWikiConfiguration(getConfigPath(), econtext, context);
                        xwiki = new XWiki(xwikicfgis, context, context.getEngineContext());
                        econtext.setAttribute(xwikiname, xwiki);
                    }
                }
            }
            context.setWiki(xwiki);
            xwiki.setDatabase(context.getDatabase());
            return xwiki;
        } catch (Exception e) {
            e.printStackTrace();
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_INIT_FAILED,
                "Could not initialize main XWiki context", e);
        }
    }

    /**
     * First try to find the configuration file pointed by the passed location as a file. If it does not exist or if the
     * file cannot be read (for example if the security manager doesn't allow it), then try to load the file as a
     * resource using the Servlet Context and failing that from teh classpath.
     * 
     * @param configurationLocation the location where the XWiki configuration file is located (either an absolute or
     *            relative file path or a resource location)
     * @return the stream containing the configuration data or null if not found
     * @todo this code should be moved to a Configuration class proper
     */
    private static InputStream readXWikiConfiguration(String configurationLocation, XWikiEngineContext econtext,
        XWikiContext context)
    {
        InputStream xwikicfgis = null;

        // First try loading from a file.
        File f = new File(configurationLocation);
        try {
            if (f.exists()) {
                xwikicfgis = new FileInputStream(f);
            }
        } catch (Exception e) {
            // Error loading the file. Most likely, the Security Manager prevented it.
            // We'll try loading it as a resource below.
            LOG.debug("Failed to load the file [" + configurationLocation + "] using direct "
                + "file access. The error was [" + e.getMessage() + "]. Trying to load it "
                + "as a resource using the Servlet Context...");
        }
        // Second, try loading it as a resource using the Servlet Context
        if (xwikicfgis == null) {
            xwikicfgis = econtext.getResourceAsStream(configurationLocation);
            LOG.debug("Failed to load the file [" + configurationLocation + "] as a resource "
                + "using the Servlet Context. Trying to load it as classpath resource...");
        }

        // Third, try loading it from the classloader used to load this current class
        if (xwikicfgis == null) {
            // TODO: Verify if checking on MODE_GWT is correct. I think we should only check for
            // the debug mode and even for that we need to find some better way of doing it so
            // that we don't have hardcoded code only for development debugging purposes.
            if (context.getMode() == XWikiContext.MODE_GWT || context.getMode() == XWikiContext.MODE_GWT_DEBUG) {
                xwikicfgis = XWiki.class.getClassLoader().getResourceAsStream("xwiki-gwt.cfg");
            } else {
                xwikicfgis = XWiki.class.getClassLoader().getResourceAsStream("xwiki.cfg");
            }
        }

        LOG.debug("Failed to load the file [" + configurationLocation + "] using any method.");

        // TODO: Should throw an exception instead of return null...

        return xwikicfgis;
    }

    public static XWiki getXWiki(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = getMainXWiki(context);

        if (xwiki.isVirtualMode()) {
            XWikiRequest request = context.getRequest();
            String host = "";
            try {
                URL requestURL = context.getURL();
                host = requestURL.getHost();
            } catch (Exception e) {
            }

            if (host.equals("")) {
                return xwiki;
            }

            String appname = xwiki.findWikiServer(host, context);

            if (appname.equals("")) {
                String uri = request.getRequestURI();
                int i1 = host.indexOf(".");
                String servername = (i1 != -1) ? host.substring(0, i1) : host;

                XWikiURLFactory urlf = context.getURLFactory();
                if ((urlf != null) && (urlf instanceof XWikiServletURLFactory)
                    && ("".equals(((XWikiServletURLFactory) urlf).getContextPath()))) {
                    appname = context.getMainXWiki();
                } else {
                    appname = uri.substring(1, uri.indexOf("/", 2));
                }

                if ("0".equals(xwiki.Param("xwiki.virtual.autowww"))) {
                    appname = servername;
                } else {
                    // As a convenience, allow sites starting with www, localhost or using an
                    // IP address not to have to create a XWikiServerXwiki page since we consider
                    // in that case that they're pointing to the main wiki.
                    if ((servername.equals("www"))
                        || (host.equals("localhost") || (context.getUtil().match(
                            "m|[0-9]+\\.|[0-9]+\\.[0-9]+\\.[0-9]|", host)))) {
                        if (appname.equals("xwiki")) {
                            return xwiki;
                        }
                    } else {
                        appname = servername;
                    }
                }
            }

            synchronized (appname) {
                // Check if this appname exists in the Database
                String serverwikipage = getServerWikiPage(appname);
                XWikiDocument doc = xwiki.getDocument(serverwikipage, context);
                if (doc.isNew()) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                        "The wiki " + appname + " does not exist");
                }

                // Set the wiki owner
                String wikiOwner = doc.getStringValue("XWiki.XWikiServerClass", "owner");
                if (wikiOwner.indexOf(":") == -1) {
                    wikiOwner = xwiki.getDatabase() + ":" + wikiOwner;
                }
                context.setWikiOwner(wikiOwner);
                context.setWikiServer(doc);
                context.setDatabase(appname);
                context.setOriginalDatabase(appname);

                try {
                    // Let's make sure the virtual wikis are upgraded to the latest database version
                    xwiki.updateDatabase(appname, false, context);
                } catch (HibernateException e) {
                    // Just to report it
                    e.printStackTrace();
                }
            }
        }
        return xwiki;
    }

    public static URL getRequestURL(XWikiRequest request) throws XWikiException
    {
        try {
            StringBuffer requestURL = request.getRequestURL();
            String qs = request.getQueryString();
            if ((qs != null) && (!qs.equals(""))) {
                return new URL(requestURL.toString() + "?" + qs);
            } else {
                return new URL(requestURL.toString());
            }
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION,
                "Exception while getting URL from request", e);
        }
    }

    public static Object callPrivateMethod(Object obj, String methodName)
    {
        return callPrivateMethod(obj, methodName, null, null);
    }

    public static Object callPrivateMethod(Object obj, String methodName, Class< ? >[] classes, Object[] args)
    {
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

    public static String getFormEncoded(String content)
    {
        Filter filter = new CharacterFilter();
        filter.removeAttribute("'");
        String scontent = filter.process(content);
        return scontent;
    }

    public static HttpClient getHttpClient(int timeout, String userAgent)
    {
        HttpClient client = new HttpClient();

        if (timeout != 0) {
            client.getParams().setSoTimeout(timeout);
            client.getParams().setParameter("http.connection.timeout", new Integer(timeout));
        }

        client.getParams().setParameter("http.useragent", userAgent);

        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPort = System.getProperty("http.proxyPort");
        if ((proxyHost != null) && (!proxyHost.equals(""))) {
            int port = 3128;
            if ((proxyPort != null) && (!proxyPort.equals(""))) {
                port = Integer.parseInt(proxyPort);
            }
            client.getHostConfiguration().setProxy(proxyHost, port);
        }

        String proxyUser = System.getProperty("http.proxyUser");
        if ((proxyUser != null) && (!proxyUser.equals(""))) {
            String proxyPassword = System.getProperty("http.proxyPassword");
            Credentials defaultcreds = new UsernamePasswordCredentials(proxyUser, proxyPassword);
            client.getState().setProxyCredentials(AuthScope.ANY, defaultcreds);
        }

        return client;
    }

    public static Object getPrivateField(Object obj, String fieldName)
    {
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

    public static String getServerWikiPage(String servername)
    {
        return "XWiki.XWikiServer" + servername.substring(0, 1).toUpperCase() + servername.substring(1);
    }

    public static String getXMLEncoded(String content)
    {
        Filter filter = new CharacterFilter();
        String scontent = filter.process(content);
        return scontent;
    }

    public static String getTextArea(String content, XWikiContext context)
    {
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
            cols = context.getWiki().getUserPreferenceAsInt("editbox_width", context);
        } catch (Exception e) {
        }
        textarea.setCols(cols);
        textarea.setName("content");
        textarea.setID("content");
        // Forcing a new line after the <textarea> tag, as
        // http://www.w3.org/TR/html4/appendix/notes.html#h-B.3.1 causes an empty line at the start
        // of the document content to be trimmed.
        textarea.addElement("\n" + scontent);

        return textarea.toString();
    }

    /**
     * This provide a way to create an XWiki object without initializing the whole XWiki (including plugins, storage,
     * etc.).
     * <p>
     * Needed for tools or tests which need XWiki because it is used everywhere in the API.
     */
    public XWiki()
    {

    }

    public XWiki(XWikiConfig config, XWikiContext context) throws XWikiException
    {
        this(config, context, null, false);
    }

    public XWiki(XWikiConfig config, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate)
        throws XWikiException
    {
        initXWiki(config, context, engine_context, noupdate);
    }

    /**
     * @deprecated use {@link #XWiki(XWikiConfig, XWikiContext)} instead
     */
    @Deprecated
    public XWiki(String xwikicfgpath, XWikiContext context) throws XWikiException
    {
        this(xwikicfgpath, context, null, false);
    }

    /**
     * @deprecated use {@link #XWiki(XWikiConfig, XWikiContext, XWikiEngineContext, boolean)} instead
     */
    @Deprecated
    public XWiki(String xwikicfgpath, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate)
        throws XWikiException
    {
        try {
            initXWiki(new XWikiConfig(new FileInputStream(xwikicfgpath)), context, engine_context, noupdate);
        } catch (FileNotFoundException e) {
            Object[] args = {xwikicfgpath};
            throw new XWikiException(XWikiException.MODULE_XWIKI_CONFIG,
                XWikiException.ERROR_XWIKI_CONFIG_FILENOTFOUND, "Configuration file {0} not found", e, args);
        }
    }

    /**
     * @deprecated use {@link #XWiki(XWikiConfig, XWikiContext, XWikiEngineContext, boolean)} instead
     */
    @Deprecated
    public XWiki(InputStream is, XWikiContext context, XWikiEngineContext engine_context) throws XWikiException
    {
        initXWiki(new XWikiConfig(is), context, engine_context, true);
    }

    /**
     * Initialize all xwiki subsystems.
     */
    public void initXWiki(XWikiConfig config, XWikiContext context, XWikiEngineContext engine_context, boolean noupdate)
        throws XWikiException
    {
        setEngineContext(engine_context);
        context.setWiki(this);

        // Create the notification manager
        setNotificationManager(new XWikiNotificationManager());

        // Prepare the store
        setConfig(config);
        XWikiStoreInterface basestore =
            (XWikiStoreInterface) createClassFromConfig("xwiki.store.class", "com.xpn.xwiki.store.XWikiHibernateStore",
                context);
        // Check if we need to use the cache store..
        boolean nocache = "0".equals(Param("xwiki.store.cache", "1"));
        if (!nocache) {
            XWikiCacheStoreInterface cachestore = new XWikiCacheStore(basestore, context);
            setStore(cachestore);
        } else {
            setStore(basestore);
        }

        setCriteriaService((XWikiCriteriaService) createClassFromConfig("xwiki.criteria.class",
            "com.xpn.xwiki.criteria.impl.XWikiCriteriaServiceImpl", context));

        setAttachmentStore((XWikiAttachmentStoreInterface) createClassFromConfig("xwiki.store.attachment.class",
            "com.xpn.xwiki.store.XWikiHibernateAttachmentStore", context));

        if (hasVersioning(null, context)) {
            setVersioningStore((XWikiVersioningStoreInterface) createClassFromConfig("xwiki.store.versioning.class",
                "com.xpn.xwiki.store.XWikiHibernateVersioningStore", context));
        }

        setAttachmentVersioningStore((AttachmentVersioningStore) createClassFromConfig(
            "xwiki.store.attachment.versioning.class", hasAttachmentVersioning(context)
                ? HibernateAttachmentVersioningStore.class.getName() : VoidAttachmentVersioningStore.class.getName(),
            context));

        if (hasRecycleBin(context)) {
            setRecycleBinStore((XWikiRecycleBinStoreInterface) createClassFromConfig("xwiki.store.recyclebin.class",
                "com.xpn.xwiki.store.XWikiHibernateRecycleBinStore", context));
        }

        if (hasAttachmentRecycleBin(context)) {
            setAttachmentRecycleBinStore((AttachmentRecycleBinStore) createClassFromConfig(
                "storage.attachment.recyclebin.class",
                "com.xpn.xwiki.store.hibernate.HibernateAttachmentRecycleBinStore", context));
        }

        // Run migrations
        if ("1".equals(Param("xwiki.store.migration", "0"))) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Running storage migrations");
            }
            AbstractXWikiMigrationManager manager =
                (AbstractXWikiMigrationManager) createClassFromConfig("xwiki.store.migration.manager.class",
                    "com.xpn.xwiki.store.migration.hibernate.XWikiHibernateMigrationManager", context);
            manager.startMigrations(context);
            if ("1".equals(Param("xwiki.store.migration.exitAfterEnd", "0"))) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Exiting because xwiki.store.migration.exitAfterEnd is set");
                }
                System.exit(0);
            }
        }

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
            getTagClass(context);
            getGroupClass(context);
            getRightsClass(context);
            getCommentsClass(context);
            getSkinClass(context);
            getGlobalRightsClass(context);
            getStatsService(context);
            if (context.getDatabase().equals(context.getMainXWiki())
                && "1".equals(context.getWiki().Param("xwiki.preferences.redirect"))) {
                getRedirectClass(context);
            }
        }

        // Add a notification for notifications
        getNotificationManager().addGeneralRule(new XWikiActionRule(new XWikiPageNotification()));

        // Add rule to get informed of new servers
        getNotificationManager().addGeneralRule(new DocObjectChangedRule(this, "XWiki.XWikiServerClass"));

        String ro = Param("xwiki.readonly", "no");
        this.isReadOnly = ("yes".equalsIgnoreCase(ro) || "true".equalsIgnoreCase(ro) || "1".equalsIgnoreCase(ro));
    }

    public XWikiStoreInterface getNotCacheStore()
    {
        XWikiStoreInterface store = getStore();
        if (store instanceof XWikiCacheStoreInterface) {
            store = ((XWikiCacheStoreInterface) store).getStore();
        }
        return store;
    }

    public XWikiHibernateStore getHibernateStore()
    {
        XWikiStoreInterface store = getStore();
        if (store instanceof XWikiHibernateStore) {
            return (XWikiHibernateStore) store;
        } else if (store instanceof XWikiCacheStoreInterface) {
            store = ((XWikiCacheStoreInterface) store).getStore();
            if (store instanceof XWikiHibernateStore) {
                return (XWikiHibernateStore) store;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void updateDatabase(String appname, XWikiContext context) throws HibernateException, XWikiException
    {
        updateDatabase(appname, false, context);
    }

    public void updateDatabase(String appname, boolean force, XWikiContext context) throws HibernateException,
        XWikiException
    {
        updateDatabase(appname, force, true, context);
    }

    public void updateDatabase(String appname, boolean force, boolean initClasses, XWikiContext context)
        throws HibernateException, XWikiException
    {
        synchronized (appname) {
            String database = context.getDatabase();

            try {
                List wikilist = getVirtualWikiList();
                context.setDatabase(appname);
                if (!wikilist.contains(appname)) {
                    wikilist.add(appname);
                    XWikiHibernateStore store = getHibernateStore();
                    if (store != null) {
                        store.updateSchema(context, force);
                    }
                }

                // Make sure these classes exists
                if (initClasses) {
                    getPrefsClass(context);
                    getUserClass(context);
                    getGroupClass(context);
                    getRightsClass(context);
                    getCommentsClass(context);
                    getSkinClass(context);
                    getGlobalRightsClass(context);
                    getTagClass(context);
                    getPluginManager().virtualInit(context);
                    getRenderingEngine().virtualInit(context);
                }

                // Add initdone which will allow to
                // bypass some initializations
                context.put("initdone", "1");
            } finally {
                context.setDatabase(database);
            }
        }
    }

    /**
     * @return a cached list of all active virtual wikis (i.e. wikis who have been hit by a user request). To get a full
     *         list of all virtual wikis database names use {@link #getVirtualWikisDatabaseNames(XWikiContext)}.
     */
    public List<String> getVirtualWikiList()
    {
        return this.virtualWikiList;
    }

    /**
     * @return the full list of all database names of all defined virtual wikis. The database names are computed from
     *         the names of documents having a XWiki.XWikiServerClass object attached to them by removing the
     *         "XWiki.XWikiServer" prefix and making it lower case. For example a page named
     *         "XWiki.XWikiServerMyDatabase" would return "mydatabase" as the database name.
     */
    public List<String> getVirtualWikisDatabaseNames(XWikiContext context) throws XWikiException
    {
        List<String> databaseNames = new ArrayList<String>();

        String database = context.getDatabase();
        try {
            context.setDatabase(context.getMainXWiki());

            String hql =
                ", BaseObject as obj, StringProperty as prop where obj.name=doc.fullName"
                    + " and obj.name <> 'XWiki.XWikiServerClassTemplate' and obj.className='XWiki.XWikiServerClass' "
                    + "and prop.id.id = obj.id ";
            List<String> list = getStore().searchDocumentsNames(hql, context);

            for (String docname : list) {
                if (docname.startsWith("XWiki.XWikiServer")) {
                    databaseNames.add(docname.substring("XWiki.XWikiServer".length()).toLowerCase());
                }
            }
        } finally {
            context.setDatabase(database);
        }

        return databaseNames;
    }

    /**
     * @return the cache containing the names of the wikis already initialized.
     * @since 1.5M2.
     */
    public Cache<String> getVirtualWikiCache()
    {
        return this.virtualWikiMap;
    }

    private String findWikiServer(String host, XWikiContext context) throws XWikiException
    {
        synchronized (this) {
            if (this.virtualWikiMap == null) {
                int iCapacity = 1000;
                try {
                    String capacity = Param("xwiki.virtual.cache.capacity");
                    if (capacity != null) {
                        iCapacity = Integer.parseInt(capacity);
                    }
                } catch (Exception e) {
                }
                try {
                    CacheConfiguration configuration = new CacheConfiguration();
                    configuration.setConfigurationId("xwiki.virtualwikimap");
                    LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
                    lru.setMaxEntries(iCapacity);
                    configuration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

                    this.virtualWikiMap = getCacheFactory().newCache(configuration);
                } catch (CacheException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_CACHE,
                        XWikiException.ERROR_CACHE_INITIALIZING, "Failed to create new cache", e);
                }
            }
        }
        synchronized (host) {
            String wikiserver = this.virtualWikiMap.get(host);

            if (wikiserver == null) {
                String hql =
                    ", BaseObject as obj, StringProperty as prop where obj.name=doc.fullName"
                        + " and obj.className='XWiki.XWikiServerClass' and prop.id.id = obj.id "
                        + "and prop.id.name = 'server' and prop.value='" + host + "'";
                try {
                    List<String> list = context.getWiki().getStore().searchDocumentsNames(hql, context);
                    if ((list != null) && (list.size() > 0)) {
                        String docname = list.get(0);
                        if (docname.startsWith("XWiki.XWikiServer")) {
                            wikiserver = docname.substring("XWiki.XWikiServer".length()).toLowerCase();
                        }
                    }

                    this.virtualWikiMap.set(host, wikiserver);
                } catch (XWikiException e2) {
                    wikiserver = null;
                }
            }

            return wikiserver;
        }
    }

    public String getWikiOwner(String servername, XWikiContext context) throws XWikiException
    {
        String wikiOwner = context.getWikiOwner();

        if (!context.isMainWiki(servername)) {
            String serverwikipage = getServerWikiPage(servername);

            String currentdatabase = context.getDatabase();

            try {
                context.setDatabase(context.getMainXWiki());

                XWikiDocument doc = getDocument(serverwikipage, context);

                if (doc.isNew()) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                        "The wiki " + servername + " does not exist");
                }

                wikiOwner = doc.getStringValue("XWiki.XWikiServerClass", "owner");
                if (wikiOwner.indexOf(":") == -1) {
                    wikiOwner = context.getMainXWiki() + ":" + wikiOwner;
                }
            } finally {
                context.setDatabase(currentdatabase);
            }
        }

        return wikiOwner;
    }

    protected Object createClassFromConfig(String param, String defClass, XWikiContext context) throws XWikiException
    {
        String storeclass = Param(param, defClass);
        try {
            Class< ? >[] classes = new Class< ? >[] {XWikiContext.class};
            Object[] args = new Object[] {context};
            Object result = Class.forName(storeclass).getConstructor(classes).newInstance(args);
            return result;
        } catch (Exception e) {
            Throwable ecause = e;
            if (e instanceof InvocationTargetException) {
                ecause = ((InvocationTargetException) e).getTargetException();
            }
            Object[] args = {param, storeclass};
            throw new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_CLASSINVOCATIONERROR, "Cannot load class {1} from param {0}", ecause,
                args);
        }
    }

    public void resetRenderingEngine(XWikiContext context) throws XWikiException
    {
        // Prepare the Rendering Engine
        setRenderingEngine(new DefaultXWikiRenderingEngine(this, context));
    }

    private void preparePlugins(XWikiContext context)
    {
        setPluginManager(new XWikiPluginManager(getXWikiPreference("plugins", context), context));
        String plugins = Param("xwiki.plugins", "");
        if (!plugins.equals("")) {
            getPluginManager().addPlugins(StringUtils.split(plugins, " ,"), context);
        }
    }

    /**
     * @return the XWiki core version as specified in the {@link #VERSION_FILE} file
     */
    public String getVersion()
    {
        if (this.version == null) {
            try {
                InputStream is = getResourceAsStream(VERSION_FILE);
                XWikiConfig properties = new XWikiConfig(is);
                this.version = properties.getProperty(VERSION_FILE_PROPERTY);
            } catch (Exception e) {
                // Failed to retrieve the version, log a warning and default to "Unknown"
                LOG.warn("Failed to retrieve XWiki's version from [" + VERSION_FILE + "], using the ["
                    + VERSION_FILE_PROPERTY + "] property.", e);
                this.version = "Unknown version";
            }
        }
        return this.version;
    }

    public URL getResource(String s) throws MalformedURLException
    {
        return getEngineContext().getResource(s);
    }

    public InputStream getResourceAsStream(String s) throws MalformedURLException
    {
        InputStream is = getEngineContext().getResourceAsStream(s);
        if (is == null) {
            is = getEngineContext().getResourceAsStream("/" + s);
        }
        return is;
    }

    public String getResourceContent(String name) throws IOException
    {
        InputStream is = null;
        if (getEngineContext() != null) {

            try {
                is = getResourceAsStream(name);
            } catch (Exception e) {
            }
        }

        if (is == null) {
            return Util.getFileContent(new File(name));
        }

        return Util.getFileContent(new InputStreamReader(is));
    }

    public Date getResourceLastModificationDate(String name)
    {
        try {
            if (getEngineContext() != null) {
                return Util.getFileLastModificationDate(getEngineContext().getRealPath(name));
            }
        } catch (Exception ex) {
            // Probably a SecurityException or the file is not accessible (inside a war)
            LOG.info("Failed to get file modification date: " + ex.getMessage());
        }
        return new Date();
    }

    public byte[] getResourceContentAsBytes(String name) throws IOException
    {
        InputStream is = null;
        if (getEngineContext() != null) {

            try {
                is = getResourceAsStream(name);
            } catch (Exception e) {
            }
        }

        if (is == null) {
            return Util.getFileContentAsBytes(new File(name));
        }

        return Util.getFileContentAsBytes(is);
    }

    public boolean resourceExists(String name)
    {
        if (getEngineContext() != null) {
            try {
                if (getResourceAsStream(name) != null) {
                    return true;
                }
            } catch (IOException e) {
            }
        }
        try {
            File file = new File(name);
            return file.exists();
        } catch (Exception e) {
            // Could be running under -security, which prevents calling file.exists().
        }
        return false;
    }

    public XWikiConfig getConfig()
    {
        return this.config;
    }

    public String getRealPath(String path)
    {
        return getEngineContext().getRealPath(path);
    }

    public String Param(String key)
    {
        return getConfig().getProperty(key);
    }

    public String ParamAsRealPath(String key)
    {
        String param = Param(key);
        try {
            return getRealPath(param);
        } catch (Exception e) {
            return param;
        }
    }

    public String ParamAsRealPath(String key, XWikiContext context)
    {
        return ParamAsRealPath(key);
    }

    public String ParamAsRealPathVerified(String param)
    {
        String path;
        File fpath;

        path = Param(param);
        if (path == null) {
            return null;
        }

        fpath = new File(path);
        if (fpath.exists()) {
            return path;
        }

        path = getRealPath(path);
        if (path == null) {
            return null;
        }

        fpath = new File(path);
        if (fpath.exists()) {
            return path;
        } else {
        }
        return null;
    }

    public String Param(String key, String default_value)
    {
        return getConfig().getProperty(key, default_value);
    }

    public long ParamAsLong(String key)
    {
        String param = getConfig().getProperty(key);
        return Long.parseLong(param);
    }

    public long ParamAsLong(String key, long default_value)
    {
        try {
            return ParamAsLong(key);
        } catch (NumberFormatException e) {
            return default_value;
        }
    }

    public XWikiStoreInterface getStore()
    {
        return this.store;
    }

    public XWikiAttachmentStoreInterface getAttachmentStore()
    {
        return this.attachmentStore;
    }

    public AttachmentVersioningStore getAttachmentVersioningStore()
    {
        return this.attachmentVersioningStore;
    }

    public XWikiVersioningStoreInterface getVersioningStore()
    {
        return this.versioningStore;
    }

    public XWikiRecycleBinStoreInterface getRecycleBinStore()
    {
        return this.recycleBinStore;
    }

    public AttachmentRecycleBinStore getAttachmentRecycleBinStore()
    {
        return this.attachmentRecycleBinStore;
    }

    public void saveDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        // If no comment is provided we should use an empty comment
        saveDocument(doc, "", context);
    }

    public void saveDocument(XWikiDocument doc, String comment, XWikiContext context) throws XWikiException
    {
        saveDocument(doc, comment, false, context);
    }

    public void saveDocument(XWikiDocument doc, String comment, boolean isMinorEdit, XWikiContext context)
        throws XWikiException
    {
        String server = null, database = null;
        try {
            server = doc.getDatabase();

            if (server != null) {
                database = context.getDatabase();
                context.setDatabase(server);
            }

            // Setting comment & minoredit before saving
            doc.setComment(StringUtils.defaultString(comment));
            doc.setMinorEdit(isMinorEdit);

            // We need to save the original document since saveXWikiDoc() will reset it and we
            // need that original document for the notification below.
            XWikiDocument originalDocument = doc.getOriginalDocument();
            // Always use an originalDocument, to provide a consistent behavior. The cases where
            // originalDocument is null are rare (specifically when the XWikiDocument object is
            // manually constructed, and not obtained using the API).
            if (originalDocument == null) {
                originalDocument = new XWikiDocument(doc.getSpace(), doc.getName());
            }

            // Notify listeners about the document change
            if (originalDocument.isNew()) {
                getNotificationManager().preverify(doc, originalDocument,
                    XWikiDocChangeNotificationInterface.EVENT_NEW, context);
            } else {
                getNotificationManager().preverify(doc, originalDocument,
                    XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
            }

            getStore().saveXWikiDoc(doc, context);

            // Since the store#saveXWikiDoc resets originalDocument, we need to temporarily put it
            // back to send notifications.
            XWikiDocument newOriginal = doc.getOriginalDocument();
            try {
                doc.setOriginalDocument(originalDocument);
                ObservationManager om = (ObservationManager) Utils.getComponent(ObservationManager.ROLE);
                // Notify listeners about the document change
                // The first call is for the old notification mechanism. It is kept here because it
                // is in a deprecation stage. It will be removed later.
                //
                // The second is the new notification mechanism, implemented as a Plexus Component.
                // For the moment we're sending the XWiki context as the data, but this will be
                // changed in the future, when the whole platform will be written using components
                // and there won't be a need for the context. The old version is available using
                // doc.getOriginalDocument()
                if (originalDocument.isNew()) {
                    getNotificationManager().verify(doc, originalDocument,
                        XWikiDocChangeNotificationInterface.EVENT_NEW, context);
                    if (om != null) {
                        om.notify(new DocumentSaveEvent(doc.getFullName()), doc, context);
                    }
                } else {
                    getNotificationManager().verify(doc, originalDocument,
                        XWikiDocChangeNotificationInterface.EVENT_CHANGE, context);
                    if (om != null) {
                        om.notify(new DocumentUpdateEvent(doc.getFullName()), doc, context);
                    }
                }
            } catch (Exception ex) {
                LOG.error("Failed to send document save notifications for document [" + doc.getFullName() + "]", ex);
            } finally {
                doc.setOriginalDocument(newOriginal);
            }
        } finally {
            if ((server != null) && (database != null)) {
                context.setDatabase(database);
            }
        }
    }

    private XWikiDocument getDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String server = null, database = null;
        try {
            server = doc.getDatabase();

            if (server != null) {
                database = context.getDatabase();
                context.setDatabase(server);
            }

            return getStore().loadXWikiDoc(doc, context);
        } finally {
            if ((server != null) && (database != null)) {
                context.setDatabase(database);
            }
        }
    }

    public XWikiDocument getDocument(XWikiDocument doc, String revision, XWikiContext context) throws XWikiException
    {
        XWikiDocument newdoc;
        try {
            if ((revision == null) || revision.equals("")) {
                newdoc = new XWikiDocument(doc.getSpace(), doc.getName());
            } else if (revision.equals(doc.getVersion())) {
                newdoc = doc;
            } else {
                newdoc = getVersioningStore().loadXWikiDoc(doc, revision, context);
            }
        } catch (XWikiException e) {
            if (revision.equals("1.1") || revision.equals("1.0")) {
                newdoc = new XWikiDocument(doc.getSpace(), doc.getName());
            } else {
                throw e;
            }
        }
        return newdoc;
    }

    public XWikiDocument getDocument(String fullname, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        try {
            doc.setFullName(fullname, context);
        } catch (Exception ex) {
            // The name isn't good. The most frequent cause for this is a kind of spam attempt which
            // gives some ugly URLs.
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_URL_EXCEPTION,
                "Invalid document name: " + fullname);
        }
        return getDocument(doc, context);
    }

    public XWikiDocument getDocument(String web, String fullname, XWikiContext context) throws XWikiException
    {
        int i1 = fullname.lastIndexOf(".");
        if (i1 != -1) {
            String web2 = fullname.substring(0, i1);
            String name = fullname.substring(i1 + 1);
            if (name.equals("")) {
                name = "WebHome";
            }
            return getDocument(web2 + "." + name, context);
        } else {
            return getDocument(web + "." + fullname, context);
        }
    }

    public XWikiDocument getDocumentFromPath(String path, XWikiContext context) throws XWikiException
    {
        String fullname = getDocumentNameFromPath(path, context);
        return getDocument(fullname, context);
    }

    public String getDocumentNameFromPath(String path, XWikiContext context)
    {
        if (StringUtils.countMatches(path, "/") == 0) {
            return context.getWiki().getDefaultWeb(context) + "." + context.getWiki().getDefaultPage(context);
        }

        String web, name;
        int i1 = 0;
        int i2;
        if (StringUtils.countMatches(path, "/") > 2) {
            i1 = path.indexOf("/", 1);
        }

        if (StringUtils.countMatches(path, "/") > 1) {
            i2 = path.indexOf("/", i1 + 1);
            web = path.substring(i1 + 1, i2);
        } else {
            i2 = 0;
            web = context.getWiki().getDefaultWeb(context);
        }
        int i3 = path.indexOf("/", i2 + 1);
        if (i3 == -1) {
            name = path.substring(i2 + 1);
        } else {
            name = path.substring(i2 + 1, i3);
        }
        if (name.equals("")) {
            name = getDefaultPage(context);
        }

        web = Util.decodeURI(web, context);
        name = Util.decodeURI(name, context);
        if (StringUtils.contains(web, ':')) {
            web = StringUtils.substringAfterLast(web, ":");
        }
        if (StringUtils.contains(name, ':')) {
            name = StringUtils.substringAfterLast(name, ":");
        }
        String fullname = web + "." + name;
        return fullname;
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#getDeletedDocuments(String, String)
     */
    public XWikiDeletedDocument[] getDeletedDocuments(String fullname, String lang, XWikiContext context)
        throws XWikiException
    {
        if (hasRecycleBin(context)) {
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(fullname, context);
            doc.setLanguage(lang);
            return getRecycleBinStore().getAllDeletedDocuments(doc, context, true);
        } else {
            return null;
        }
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#getDeletedDocument(String, String, String)
     */
    public XWikiDeletedDocument getDeletedDocument(String fullname, String lang, int index, XWikiContext context)
        throws XWikiException
    {
        if (hasRecycleBin(context)) {
            XWikiDocument doc = new XWikiDocument();
            doc.setFullName(fullname, context);
            doc.setLanguage(lang);
            return getRecycleBinStore().getDeletedDocument(doc, index, context, true);
        } else {
            return null;
        }
    }

    public XWikiRenderingEngine getRenderingEngine()
    {
        return this.renderingEngine;
    }

    public void setRenderingEngine(XWikiRenderingEngine renderingEngine)
    {
        this.renderingEngine = renderingEngine;
    }

    public MetaClass getMetaclass()
    {
        return this.metaclass;
    }

    public void setMetaclass(MetaClass metaclass)
    {
        this.metaclass = metaclass;
    }

    public String getHTMLArea(String content, XWikiContext context)
    {
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
            cols = context.getWiki().getUserPreferenceAsInt("editbox_width", context);
        } catch (Exception e) {
        }
        textarea.setCols(cols);
        textarea.setFilter(filter);
        textarea.setName("content");
        textarea.setID("content");
        textarea.addElement(scontent);
        return textarea.toString();
    }

    public List<String> getClassList(XWikiContext context) throws XWikiException
    {
        return getStore().getClassList(context);
    }

    /*
     * public String[] getClassList() throws XWikiException { List list = store.getClassList(); String[] array = new
     * String[list.size()]; for (int i=0;i<list.size();i++) array[i] = (String)list.get(i); return array; }
     */

    public List search(String sql, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, 0, 0, context);
    }

    public List search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, nb, start, context);
    }

    public List search(String sql, Object[][] whereParams, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, 0, 0, whereParams, context);
    }

    public List search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        return getStore().search(sql, nb, start, whereParams, context);
    }

    public boolean isTest()
    {
        return this.test;
    }

    public void setTest(boolean test)
    {
        this.test = test;
    }

    public String parseContent(String content, XWikiContext context)
    {
        String parsedContent;

        if ((content != null) && (!content.equals(""))) {
            parsedContent = context.getWiki().getRenderingEngine().interpretText(content, context.getDoc(), context);
        } else {
            parsedContent = "";
        }

        return parsedContent;
    }

    public String parseTemplate(String template, XWikiContext context)
    {
        try {
            String skin = getSkin(context);
            String result = parseTemplate(template, skin, context);
            if (result != null) {
                return result;
            }

            // If we could not find the template in the skin
            // let's try in the base skin (as long as the base skin is not the same as the skin)
            String baseskin = getBaseSkin(context);
            if (!skin.equals(baseskin)) {
                result = parseTemplate(template, baseskin, context);
                if (result != null) {
                    return result;
                }
            }

            // If we still could not find the template in the skin or in the base skin
            // let's try in the default base skin (as long as the default base skin is not the same
            // as the skin or the base skin
            String defaultbaseskin = getDefaultBaseSkin(context);
            if ((!baseskin.equals(defaultbaseskin)) && (!skin.equals(defaultbaseskin))) {
                result = parseTemplate(template, defaultbaseskin, context);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception e) {
        }

        try {
            String content = getResourceContent("/templates/" + template);
            return XWikiVelocityRenderer.evaluate(content, "/templates/" + template, (VelocityContext) context
                .get("vcontext"), context);
        } catch (Exception e) {
            return "";
        }
    }

    public String parseTemplate(String template, String skin, XWikiContext context)
    {
        try {
            XWikiDocument doc = getDocument(skin, context);
            if (!doc.isNew()) {
                // Try parsing the object property
                BaseObject object = doc.getObject("XWiki.XWikiSkins", 0);
                if (object != null) {
                    String content = object.getStringValue(template);
                    if (!StringUtils.isBlank(content)) {
                        // Let's use this template
                        // Use "" as namespace to register macros in global namespace. That way it
                        // can be used in a renderer content not parsed at the same level.
                        return XWikiVelocityRenderer.evaluate(content, "", (VelocityContext) context.get("vcontext"),
                            context);
                    }
                }
                // Try parsing a document attachment
                XWikiAttachment attachment = doc.getAttachment(template);
                if (attachment != null) {
                    String content = new String(attachment.getContent(context));
                    if (!StringUtils.isBlank(content)) {
                        // Let's use this template
                        // Use "" as namespace to register macros in global namespace. That way it
                        // can be used in a renderer content not parsed at the same level.
                        return XWikiVelocityRenderer.evaluate(content, "", (VelocityContext) context.get("vcontext"),
                            context);
                    }
                }
            }
        } catch (Exception e) {
        }

        // Try parsing a file located in the directory with the same name.
        try {
            String path = "/skins/" + skin + "/" + template;
            // We must make sure the file is taken from the skins directory, otherwise people might
            // try to read things from WEB-INF.
            path = URI.create(path).normalize().toString();
            // This is a safe assumption, as templates found under /templates/ are treated
            // separately, and there is no need to have templates in another place.
            if (path.startsWith("/skins/")) {
                String content = getResourceContent(path);
                // Use "" as namespace to register macros in global namespace. That way it can be
                // used in a renderer content not parsed at the same level.
                return XWikiVelocityRenderer.evaluate(content, "", (VelocityContext) context.get("vcontext"), context);
            } else {
                LOG.warn("Illegal access, tried to use file [" + path + "] as a template."
                    + " Possible break-in attempt!");
            }
        } catch (Exception e) {
        }

        return null;
    }

    public String renderTemplate(String template, String skin, XWikiContext context)
    {
        try {
            return getRenderingEngine().getRenderer("wiki").render(parseTemplate(template, skin, context),
                context.getDoc(), context.getDoc(), context);
        } catch (Exception ex) {
            LOG.error(ex);
            return parseTemplate(template, skin, context);
        }
    }

    public String renderTemplate(String template, XWikiContext context)
    {
        try {
            return getRenderingEngine().getRenderer("wiki").render(parseTemplate(template, context), context.getDoc(),
                context.getDoc(), context);
        } catch (Exception ex) {
            LOG.error(ex);
            return parseTemplate(template, context);
        }
    }

    /**
     * Designed to include dynamic content, such as Servlets or JSPs, inside Velocity templates; works by creating a
     * RequestDispatcher, buffering the output, then returning it as a string.
     * 
     * @author LBlaze
     */
    public String invokeServletAndReturnAsString(String url, XWikiContext xwikiContext)
    {

        HttpServletRequest servletRequest = xwikiContext.getRequest();
        HttpServletResponse servletResponse = xwikiContext.getResponse();

        try {
            return IncludeServletAsString.invokeServletAndReturnAsString(url, servletRequest, servletResponse);
        } catch (Exception e) {
            LOG.warn("Exception including url: " + url, e);
            return "Exception including \"" + url + "\", see logs for details.";
        }

    }

    public String getSkinFile(String filename, XWikiContext context)
    {
        return getSkinFile(filename, false, context);
    }

    public String getSkinFile(String filename, boolean forceSkinAction, XWikiContext context)
    {
        XWikiURLFactory urlf = context.getURLFactory();

        try {
            // Try in the specified skin
            String skin = getSkin(context);
            String result = getSkinFile(filename, skin, forceSkinAction, context);
            if (result != null) {
                return result;
            }
            // Try in the parent skin
            String baseskin = getBaseSkin(context);
            if (!skin.equals(baseskin)) {
                result = getSkinFile(filename, baseskin, forceSkinAction, context);
                if (result != null) {
                    return result;
                }
            }
        } catch (Exception e) {
        }

        // If all else fails, use the default base skin, even if the URLs could be invalid.
        URL url;
        if (forceSkinAction) {
            url = urlf.createSkinURL(filename, "skins", getDefaultBaseSkin(context), context);
        } else {
            url = urlf.createSkinURL(filename, getDefaultBaseSkin(context), context);
        }
        return urlf.getURL(url, context);
    }

    public String getSkinFile(String filename, String skin, XWikiContext context)
    {
        return getSkinFile(filename, skin, false, context);
    }

    public String getSkinFile(String filename, String skin, boolean forceSkinAction, XWikiContext context)
    {
        XWikiURLFactory urlf = context.getURLFactory();
        try {
            XWikiDocument doc = getDocument(skin, context);
            if (!doc.isNew()) {
                // Look for an object property
                BaseObject object = doc.getObject("XWiki.XWikiSkins");
                if (object != null) {
                    String content = object.getStringValue(filename);
                    if (!StringUtils.isBlank(content)) {
                        URL url =
                            urlf.createSkinURL(filename, doc.getSpace(), doc.getName(), doc.getDatabase(), context);
                        return urlf.getURL(url, context);
                    }
                }

                // Look for an attachment
                String shortName = StringUtils.replaceChars(filename, '/', '.');
                XWikiAttachment attachment = doc.getAttachment(shortName);
                if (attachment != null) {
                    return doc.getAttachmentURL(shortName, context);
                }
            }

            // Look for a filesystem file
            String path = "/skins/" + skin + "/" + filename;
            if (resourceExists(path)) {
                URL url;

                if (forceSkinAction) {
                    url = urlf.createSkinURL(filename, "skins", skin, context);
                } else {
                    url = urlf.createSkinURL(filename, skin, context);
                }
                return urlf.getURL(url, context);
            }
        } catch (Exception e) {
        }

        return null;
    }

    public String getSkin(XWikiContext context)
    {
        String skin = "";
        try {
            // Try to get it from context
            skin = (String) context.get("skin");
            if (skin != null) {
                return skin;
            }

            // Try to get it from URL
            if (context.getRequest() != null) {
                skin = context.getRequest().getParameter("skin");
            }

            if ((skin == null) || (skin.equals(""))) {
                skin = getUserPreference("skin", context);
            }
            if (skin.equals("")) {
                skin = Param("xwiki.defaultskin", getDefaultBaseSkin(context));
            }
        } catch (Exception e) {
            skin = getDefaultBaseSkin(context);
        }
        try {
            if (skin.indexOf(".") != -1) {
                if (!getRightService().hasAccessLevel("view", context.getUser(), skin, context)) {
                    skin = Param("xwiki.defaultskin", getDefaultBaseSkin(context));
                }
            }
        } catch (XWikiException e) {
            // if it fails here, let's just ignore it
        }

        context.put("skin", skin);
        return skin;
    }

    public String getSkinPreference(String prefname, XWikiContext context)
    {
        return getSkinPreference(prefname, "", context);
    }

    public String getSkinPreference(String prefname, String default_value, XWikiContext context)
    {
        try {
            String skin = getSkin(context);
            String oldskin = skin;
            String value = context.getWiki().getDocument(skin, context).getStringValue("XWiki.XWikiSkins", prefname);
            if (value == null || "".equals(value)) {
                skin = getBaseSkin(context);
                if (!oldskin.equals(skin)) {
                    value = context.getWiki().getDocument(skin, context).getStringValue("XWiki.XWikiSkins", prefname);
                    oldskin = skin;
                }
            }
            if (value == null || "".equals(value)) {
                skin = getDefaultBaseSkin(context);
                if (!oldskin.equals(skin)) {
                    value = context.getWiki().getDocument(skin, context).getStringValue("XWiki.XWikiSkins", prefname);
                }
            }
            if (value == null || "".equals(value)) {
                value = default_value;
            }
            return value;
        } catch (XWikiException ex) {
            LOG.warn("", ex);
        }
        return default_value;
    }

    public String getDefaultBaseSkin(XWikiContext context)
    {
        String defaultbaseskin = Param("xwiki.defaultbaseskin", "");
        if (defaultbaseskin.equals("")) {
            defaultbaseskin = Param("xwiki.defaultskin", "albatross");
        }
        return defaultbaseskin;
    }

    public String getBaseSkin(XWikiContext context)
    {
        return getBaseSkin(context, false);
    }

    public String getBaseSkin(XWikiContext context, boolean fromRenderSkin)
    {
        String baseskin = "";
        try {
            // Try to get it from context
            baseskin = (String) context.get("baseskin");
            if (baseskin != null) {
                return baseskin;
            } else {
                baseskin = "";
            }

            // Let's get the base skin doc itself
            if (fromRenderSkin) {
                baseskin = context.getDoc().getStringValue("XWiki.XWikiSkins", "baseskin");
            }

            if (baseskin.equals("")) {
                // Let's get the base skin from the skin itself
                String skin = getSkin(context);
                XWikiDocument doc = getDocument(skin, context);
                baseskin = doc.getStringValue("XWiki.XWikiSkins", "baseskin");
            }
            if (baseskin.equals("")) {
                baseskin = getDefaultBaseSkin(context);
            }
        } catch (Exception e) {
            baseskin = getDefaultBaseSkin(context);
        }
        context.put("baseskin", baseskin);
        return baseskin;
    }

    public String getWebCopyright(XWikiContext context)
    {
        try {
            String result = getWebPreference("webcopyright", "", context);
            if (!result.trim().equals("")) {
                return result;
            }
        } catch (Exception e) {
        }
        return "Copyright 2004-2007 (c) XPertNet and Contributing Authors";
    }

    public String getXWikiPreference(String prefname, XWikiContext context)
    {
        return getXWikiPreference(prefname, "", context);
    }

    public String getXWikiPreference(String prefname, String default_value, XWikiContext context)
    {
        try {
            XWikiDocument doc = getDocument("XWiki.XWikiPreferences", context);
            // First we try to get a translated preference object
            BaseObject object =
                doc.getObject("XWiki.XWikiPreferences", "default_language", context.getLanguage(), true);
            String result = "";

            try {
                result = object.getStringValue(prefname);
            } catch (Exception e) {
            }
            // If empty we take it from the default pref object
            if (result.equals("")) {
                result = doc.getxWikiObject().getStringValue(prefname);
            }

            if (!result.equals("")) {
                return result;
            }
        } catch (Exception e) {
        }
        return default_value;
    }

    public String getWebPreference(String prefname, XWikiContext context)
    {
        return getWebPreference(prefname, "", context);
    }

    public String getWebPreference(String prefname, String default_value, XWikiContext context)
    {
        XWikiDocument currentdoc = (XWikiDocument) context.get("doc");
        return getWebPreference(prefname, (currentdoc == null) ? null : currentdoc.getSpace(), default_value, context);
    }

    public String getWebPreference(String prefname, String space, String default_value, XWikiContext context)
    {
        try {
            XWikiDocument doc = getDocument(space + ".WebPreferences", context);

            // First we try to get a translated preference object
            BaseObject object =
                doc.getObject("XWiki.XWikiPreferences", "default_language", context.getLanguage(), true);
            String result = "";
            try {
                result = object.getStringValue(prefname);
            } catch (Exception e) {
            }

            if (!result.equals("")) {
                return result;
            }
        } catch (Exception e) {
        }
        return getXWikiPreference(prefname, default_value, context);
    }

    public String getUserPreference(String prefname, XWikiContext context)
    {
        try {
            String user = context.getUser();
            XWikiDocument userdoc = getDocument(user, context);
            if (userdoc != null) {
                String result = userdoc.getStringValue("XWiki.XWikiUsers", prefname);
                if ((!result.equals("")) && (!result.equals("---"))) {
                    return result;
                }
            }
        } catch (Exception e) {
        }

        return getWebPreference(prefname, context);
    }

    public String getUserPreferenceFromCookie(String prefname, XWikiContext context)
    {
        Cookie[] cookies = context.getRequest().getCookies();
        if (cookies == null) {
            return null;
        }
        for (int i = 0; i < cookies.length; i++) {
            String name = cookies[i].getName();
            if (name.equals(prefname)) {
                String value = cookies[i].getValue();
                if (!value.trim().equals("")) {
                    return value;
                } else {
                    break;
                }
            }
        }
        return null;
    }

    public String getUserPreference(String prefname, boolean useCookie, XWikiContext context)
    {
        // First we look in the cookies
        if (useCookie) {
            String result = getUserPreferenceFromCookie(prefname, context);
            if (result != null) {
                return result;
            }
        }
        return getUserPreference(prefname, context);
    }

    /**
     * @deprecated use {@link #getLanguagePreference(XWikiContext)} instead
     */
    @Deprecated
    public String getDocLanguagePreference(XWikiContext context)
    {
        return getLanguagePreference(context);
    }

    /**
     * First try to find the current language in use from the XWiki context. If none is used and if the wiki is not
     * multilingual use the default language defined in the XWiki preferences. If the wiki is multilingual try to get
     * the language passed in the request. If none was passed try to get it from a cookie. If no language cookie exists
     * then use the user default language and barring that use the browser's "Accept-Language" header sent in HTTP
     * request. If none is defined use the default language.
     * 
     * @return the language to use
     */
    public String getLanguagePreference(XWikiContext context)
    {
        // First we try to get the language from the XWiki Context. This is the current language
        // being used.
        String language = context.getLanguage();
        if (language != null) {
            return language;
        }

        String defaultLanguage = getDefaultLanguage(context);

        // If the wiki is non multilingual then the language is the default language.
        if (!context.getWiki().isMultiLingual(context)) {
            language = defaultLanguage;
            context.setLanguage(language);
            return language;
        }

        // As the wiki is multilingual try to find the language to use from the request by looking
        // for a language parameter. If the language value is "default" use the default language
        // from the XWiki preferences settings. Otherwise set a cookie to remember the language
        // in use.
        try {
            language = context.getRequest().getParameter("language");
            if ((language != null) && (!language.equals(""))) {
                language = language.toLowerCase();
                if (language.equals("default")) {
                    // forgetting language cookie
                    Cookie cookie = new Cookie("language", "");
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    context.getResponse().addCookie(cookie);
                    language = defaultLanguage;
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

        // As no language parameter was passed in the request, try to get the language to use
        // from a cookie.
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
                language = userdoc.getStringValue("XWiki.XWikiUsers", "default_language").toLowerCase();
                if (!language.equals("")) {
                    context.setLanguage(language);
                    return language;
                }
            }
        } catch (XWikiException e) {
        }

        // If the default language is prefered, and since the user didn't explicitely ask for a
        // language already, then use the default wiki language.
        if (Param("xwiki.language.preferDefault", "0").equals("1")
            || getWebPreference("preferDefaultLanguage", "0", context).equals("1")) {
            language = defaultLanguage;
            context.setLanguage(language);
            return language;
        }

        // Then from the navigator language setting
        if (context.getRequest() != null) {
            String acceptHeader = context.getRequest().getHeader("Accept-Language");
            // If the client didn't specify some languages, skip this phase
            if ((acceptHeader != null) && (!acceptHeader.equals(""))) {
                List<String> acceptedLanguages = getAcceptedLanguages(context.getRequest());
                // We can force one of the configured languages to be accepted
                if (Param("xwiki.language.forceSupported", "0").equals("1")) {
                    List<String> available = Arrays.asList(getXWikiPreference("languages", context).split(", |"));
                    // Filter only configured languages
                    acceptedLanguages.retainAll(available);
                }
                if (acceptedLanguages.size() > 0) {
                    // Use the "most-preferred" language, as requested by the client.
                    context.setLanguage(acceptedLanguages.get(0));
                    return acceptedLanguages.get(0);
                }
                // If none of the languages requested by the client is acceptable, skip to next
                // phase (use default language).
            }
        }

        // Finally, use the default language from the global preferences.
        context.setLanguage(defaultLanguage);
        return defaultLanguage;
    }

    /**
     * Construct a list of language codes (ISO 639-1) from the Accept-Languages header. This method filters out some
     * bugs in different browsers or containers, like returning '*' as a language (Jetty) or using '_' as a
     * language--country delimiter (some versions of Opera).
     * 
     * @param request The client request.
     * @return A list of language codes, in the client preference order; might be empty if the header is not well
     *         formed.
     */
    @SuppressWarnings("unchecked")
    private List<String> getAcceptedLanguages(XWikiRequest request)
    {
        List<String> result = new ArrayList<String>();
        Enumeration<Locale> e = request.getLocales();
        while (e.hasMoreElements()) {
            String language = e.nextElement().getLanguage().toLowerCase();
            // All language codes should have 2 letters.
            if (StringUtils.isAlpha(language)) {
                result.add(language);
            }
        }
        return result;
    }

    public String getDefaultLanguage(XWikiContext context)
    {
        // Find out what is the default language from the XWiki preferences settings.
        String defaultLanguage = context.getWiki().getXWikiPreference("default_language", "", context);
        if (defaultLanguage == null || defaultLanguage.equals("")) {
            defaultLanguage = "en";
        }
        return defaultLanguage.toLowerCase();
    }

    public String getDocLanguagePreferenceNew(XWikiContext context)
    {
        // Get context language
        String contextLanguage = context.getLanguage();
        // If the language exists in the context, it was previously set by another call
        if (contextLanguage != null && contextLanguage != "") {
            return contextLanguage;
        }

        String language = "", requestLanguage = "", userPreferenceLanguage = "", navigatorLanguage = "", cookieLanguage =
            "";
        boolean setCookie = false;

        if (!context.getWiki().isMultiLingual(context)) {
            language = context.getWiki().getXWikiPreference("default_language", "", context);
            context.setLanguage(language);
            return language;
        }

        // Get request language
        try {
            requestLanguage = context.getRequest().getParameter("language");
        } catch (Exception ex) {
        }

        // Get user preference
        try {
            String user = context.getUser();
            XWikiDocument userdoc = getDocument(user, context);
            if (userdoc != null) {
                userPreferenceLanguage = userdoc.getStringValue("XWiki.XWikiUsers", "default_language");
            }
        } catch (XWikiException e) {
        }

        // Get navigator language setting
        if (context.getRequest() != null) {
            String accept = context.getRequest().getHeader("Accept-Language");
            if ((accept != null) && (!accept.equals(""))) {
                String[] alist = StringUtils.split(accept, ",;-");
                if ((alist != null) && !(alist.length == 0)) {
                    context.setLanguage(alist[0]);
                    navigatorLanguage = alist[0];
                }
            }
        }

        // Get language from cookie
        try {
            cookieLanguage = getUserPreferenceFromCookie("language", context);
        } catch (Exception e) {
        }

        // Determine which language to use
        // First we get the language from the request
        if ((requestLanguage != null) && (!requestLanguage.equals(""))) {
            if (requestLanguage.equals("default")) {
                setCookie = true;
            } else {
                language = requestLanguage;
                context.setLanguage(language);
                Cookie cookie = new Cookie("language", language);
                cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
                cookie.setPath("/");
                context.getResponse().addCookie(cookie);
                return language;
            }
        }
        // Next we get the language from the cookie
        if (cookieLanguage != null && cookieLanguage != "") {
            language = cookieLanguage;
        }
        // Next from the default user preference
        else if (userPreferenceLanguage != null && userPreferenceLanguage != "") {
            language = userPreferenceLanguage;
        }
        // Then from the navigator language setting
        else if (navigatorLanguage != null && navigatorLanguage != "") {
            language = navigatorLanguage;
        }
        context.setLanguage(language);
        if (setCookie) {
            Cookie cookie = new Cookie("language", language);
            cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
            cookie.setPath("/");
            context.getResponse().addCookie(cookie);
        }
        return language;
    }

    public String getInterfaceLanguagePreference(XWikiContext context)
    {
        String language = "", requestLanguage = "", userPreferenceLanguage = "", navigatorLanguage = "", cookieLanguage =
            "", contextLanguage = "";
        boolean setCookie = false;

        if (!context.getWiki().isMultiLingual(context)) {
            language = context.getWiki().getXWikiPreference("default_language", "", context);
            context.setInterfaceLanguage(language);
            return language;
        }

        // Get request language
        try {
            requestLanguage = context.getRequest().getParameter("interfacelanguage");
        } catch (Exception ex) {
        }

        // Get context language
        contextLanguage = context.getInterfaceLanguage();

        // Get user preference
        try {
            String user = context.getUser();
            XWikiDocument userdoc = null;
            userdoc = getDocument(user, context);
            if (userdoc != null) {
                userPreferenceLanguage = userdoc.getStringValue("XWiki.XWikiUsers", "default_interface_language");
            }
        } catch (XWikiException e) {
        }

        // Get navigator language setting
        if (context.getRequest() != null) {
            String accept = context.getRequest().getHeader("Accept-Language");
            if ((accept != null) && (!accept.equals(""))) {
                String[] alist = StringUtils.split(accept, ",;-");
                if ((alist != null) && !(alist.length == 0)) {
                    context.setLanguage(alist[0]);
                    navigatorLanguage = alist[0];
                }
            }
        }

        // Get language from cookie
        try {
            cookieLanguage = getUserPreferenceFromCookie("interfacelanguage", context);
        } catch (Exception e) {
        }

        // Determine which language to use
        // First we get the language from the request
        if ((requestLanguage != null) && (!requestLanguage.equals(""))) {
            if (requestLanguage.equals("default")) {
                setCookie = true;
            } else {
                language = requestLanguage;
                context.setLanguage(language);
                Cookie cookie = new Cookie("interfacelanguage", language);
                cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
                cookie.setPath("/");
                context.getResponse().addCookie(cookie);
                return language;
            }
        }
        // Next we get the language from the context
        if (contextLanguage != null && contextLanguage != "") {
            language = contextLanguage;
        }
        // Next we get the language from the cookie
        else if (cookieLanguage != null && cookieLanguage != "") {
            language = cookieLanguage;
        }
        // Next from the default user preference
        else if (userPreferenceLanguage != null && userPreferenceLanguage != "") {
            language = userPreferenceLanguage;
        }
        // Then from the navigator language setting
        else if (navigatorLanguage != null && navigatorLanguage != "") {
            language = navigatorLanguage;
        }
        context.setLanguage(language);
        if (setCookie) {
            Cookie cookie = new Cookie("interfacelanguage", language);
            cookie.setMaxAge(60 * 60 * 24 * 365 * 10);
            cookie.setPath("/");
            context.getResponse().addCookie(cookie);
        }
        return language;
    }

    public long getXWikiPreferenceAsLong(String prefname, XWikiContext context)
    {
        return Long.parseLong(getXWikiPreference(prefname, context));
    }

    public long getWebPreferenceAsLong(String prefname, XWikiContext context)
    {
        return Long.parseLong(getWebPreference(prefname, context));
    }

    public long getXWikiPreferenceAsLong(String prefname, long default_value, XWikiContext context)
    {
        try {
            return Long.parseLong(getXWikiPreference(prefname, context));
        } catch (NumberFormatException e) {
            return default_value;
        }
    }

    public long getWebPreferenceAsLong(String prefname, long default_value, XWikiContext context)
    {
        try {
            return Long.parseLong(getWebPreference(prefname, context));
        } catch (NumberFormatException e) {
            return default_value;
        }
    }

    public long getUserPreferenceAsLong(String prefname, XWikiContext context)
    {
        return Long.parseLong(getUserPreference(prefname, context));
    }

    public int getXWikiPreferenceAsInt(String prefname, XWikiContext context)
    {
        return Integer.parseInt(getXWikiPreference(prefname, context));
    }

    public int getWebPreferenceAsInt(String prefname, XWikiContext context)
    {
        return Integer.parseInt(getWebPreference(prefname, context));
    }

    public int getXWikiPreferenceAsInt(String prefname, int default_value, XWikiContext context)
    {
        try {
            return Integer.parseInt(getXWikiPreference(prefname, context));
        } catch (NumberFormatException e) {
            return default_value;
        }
    }

    public int getWebPreferenceAsInt(String prefname, int default_value, XWikiContext context)
    {
        try {
            return Integer.parseInt(getWebPreference(prefname, context));
        } catch (NumberFormatException e) {
            return default_value;
        }
    }

    public int getUserPreferenceAsInt(String prefname, XWikiContext context)
    {
        return Integer.parseInt(getUserPreference(prefname, context));
    }

    public void flushCache()
    {
        flushCache(null);
    }

    public void flushCache(XWikiContext context)
    {
        // We need to flush the virtual wiki list
        this.virtualWikiList = new ArrayList<String>();
        // We need to flush the server Cache
        if (this.virtualWikiMap != null) {
            this.virtualWikiMap.removeAll();
            this.virtualWikiMap = null;
        }

        // We need to flush the group service cache
        if (this.groupService != null) {
            this.groupService.flushCache();
        }

        // If we use the Cache Store layer.. we need to flush it
        XWikiStoreInterface store = getStore();
        if ((store != null) && (store instanceof XWikiCacheStoreInterface)) {
            ((XWikiCacheStoreInterface) getStore()).flushCache();
        }
        // Flush renderers.. Groovy renderer has a cache
        XWikiRenderingEngine rengine = getRenderingEngine();
        if (rengine != null) {
            rengine.flushCache();
        }

        XWikiPluginManager pmanager = getPluginManager();
        if (pmanager != null) {
            pmanager.flushCache(context);
        }

        // Make sure we call all classes flushCache function
        try {
            List<String> classes = getClassList(context);
            for (int i = 0; i < classes.size(); i++) {
                String className = classes.get(i);
                try {
                    getClass(className, context).flushCache();
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
        }

    }

    public XWikiPluginManager getPluginManager()
    {
        return this.pluginManager;
    }

    public void setPluginManager(XWikiPluginManager pluginManager)
    {
        this.pluginManager = pluginManager;
    }

    public void setConfig(XWikiConfig config)
    {
        this.config = config;
    }

    public void setStore(XWikiStoreInterface store)
    {
        this.store = store;
    }

    public void setAttachmentStore(XWikiAttachmentStoreInterface attachmentStore)
    {
        this.attachmentStore = attachmentStore;
    }

    public void setAttachmentVersioningStore(AttachmentVersioningStore avStore)
    {
        this.attachmentVersioningStore = avStore;
    }

    public void setVersioningStore(XWikiVersioningStoreInterface versioningStore)
    {
        this.versioningStore = versioningStore;
    }

    public void setRecycleBinStore(XWikiRecycleBinStoreInterface recycleBinStore)
    {
        this.recycleBinStore = recycleBinStore;
    }

    public void setAttachmentRecycleBinStore(AttachmentRecycleBinStore attachmentRecycleBinStore)
    {
        this.attachmentRecycleBinStore = attachmentRecycleBinStore;
    }

    public void setCriteriaService(XWikiCriteriaService criteriaService)
    {
        this.criteriaService = criteriaService;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public XWikiNotificationManager getNotificationManager()
    {
        return this.notificationManager;
    }

    public void setNotificationManager(XWikiNotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }

    public void notify(XWikiNotificationRule rule, XWikiDocument newdoc, XWikiDocument olddoc, int event,
        XWikiContext context)
    {
        if (!isVirtualMode()) {
            if (newdoc.getFullName().equals("XWiki.XWikiPreferences")) {
                preparePlugins(context);
            }
        }

        if (olddoc != null) {
            flushVirtualWikis(olddoc);
        }
        flushVirtualWikis(newdoc);
    }

    private void flushVirtualWikis(XWikiDocument doc)
    {
        List<BaseObject> bobjects = doc.getObjects("XWiki.XWikiServerClass");
        if (bobjects != null) {
            for (BaseObject bobj : bobjects) {
                if (bobj != null) {
                    String host = bobj.getStringValue("server");
                    if ((host != null) && (!"".equals(host))) {
                        if (this.virtualWikiMap != null) {
                            if (this.virtualWikiMap.get(host) != null)
                                this.virtualWikiMap.remove(host);
                        }
                    }
                }
            }
        }
    }

    /**
     * Verify if the <code>XWiki.TagClass</code> page exists and that it contains all the required configuration
     * properties to make the tag feature work properly. If some properties are missing they are created and saved in
     * the database.
     * 
     * @param context the XWiki Context
     * @return the TagClass Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getTagClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument(XWikiConstant.TAG_CLASS, context);

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        bclass.setName(XWikiConstant.TAG_CLASS);

        needsUpdate |= bclass.addStaticListField(XWikiConstant.TAG_CLASS_PROP_TAGS, "Tags", 30, true, "", "checkbox");
        StaticListClass tagClass = (StaticListClass) bclass.get(XWikiConstant.TAG_CLASS_PROP_TAGS);
        if (tagClass.isRelationalStorage() == false) {
            tagClass.setRelationalStorage(true);
            needsUpdate = true;
        }
        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki TagClass");
        }

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    /**
     * Verify if the <code>XWiki.XWikiUsers</code> page exists and that it contains all the required configuration
     * properties to make the user feature work properly. If some properties are missing they are created and saved in
     * the database.
     * 
     * @param context the XWiki Context
     * @return the XWikiUsers Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getUserClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument("XWiki.XWikiUsers", context);

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

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
        needsUpdate |= bclass.addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");
        needsUpdate |= bclass.addStaticListField("usertype", "User type", "Basic|Advanced");

        // New fields for the XWiki 1.0 skin
        needsUpdate |= bclass.addTextField("skin", "skin", 30);
        needsUpdate |= bclass.addStaticListField("pageWidth", "Preferred page width", "default|640|800|1024|1280|1600");
        needsUpdate |= bclass.addTextField("avatar", "Avatar", 30);

        // Only used by LDAP authentication service
        needsUpdate |= bclass.addTextField("ldap_dn", "LDAP DN", 80);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Users");
        }

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    /**
     * Verify if the <code>XWiki.GlobalRedirect</code> page exists and that it contains all the required configuration
     * properties to make the redirection feature work properly. If some properties are missing they are created and
     * saved in the database.
     * 
     * @param context the XWiki Context
     * @return the GlobalRedirect Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getRedirectClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument("XWiki.GlobalRedirect", context);

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        bclass.setName("XWiki.GlobalRedirect");
        needsUpdate |= bclass.addTextField("pattern", "Pattern", 30);
        needsUpdate |= bclass.addTextField("destination", "Destination", 30);
        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Global Redirect Class");
        }

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    /**
     * Verify if the <code>XWiki.XWikiPreferences</code> page exists and that it contains all the required
     * configuration properties to make XWiki work properly. If some properties are missing they are created and saved
     * in the database.
     * 
     * @param context the XWiki Context
     * @return the XWiki Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getPrefsClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument("XWiki.XWikiPreferences", context);

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        bclass.setName("XWiki.XWikiPreferences");
        if (!"internal".equals(bclass.getCustomMapping())) {
            needsUpdate = true;
            bclass.setCustomMapping("internal");
        }

        needsUpdate |= bclass.addTextField("parent", "Parent Space", 30);
        needsUpdate |= bclass.addBooleanField("multilingual", "Multi-Lingual", "yesno");
        needsUpdate |= bclass.addTextField("default_language", "Default Language", 5);
        needsUpdate |= bclass.addBooleanField("authenticate_edit", "Authenticated Edit", "yesno");
        needsUpdate |= bclass.addBooleanField("authenticate_view", "Authenticated View", "yesno");
        needsUpdate |= bclass.addBooleanField("auth_active_check", "Authentication Active Check", "yesno");

        needsUpdate |= bclass.addTextField("skin", "Skin", 30);
        // This one should not be in the prefs
        PropertyInterface baseskinProp = bclass.get("baseskin");
        if (baseskinProp != null) {
            bclass.removeField("baseskin");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addTextField("stylesheet", "Default Stylesheet", 30);
        needsUpdate |= bclass.addTextField("stylesheets", "Alternative Stylesheet", 60);

        needsUpdate |= bclass.addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");
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
        needsUpdate |= bclass.addTextAreaField("invitation_email_content", "Invitation eMail Content", 72, 10);

        needsUpdate |= bclass.addTextField("macros_languages", "Macros Languages", 60);
        needsUpdate |= bclass.addTextField("macros_velocity", "Macros for Velocity", 60);
        needsUpdate |= bclass.addTextField("macros_groovy", "Macros for Groovy", 60);
        needsUpdate |= bclass.addTextField("macros_wiki", "Macros for the Wiki Parser", 60);
        needsUpdate |= bclass.addTextAreaField("macros_mapping", "Macros Mapping", 60, 15);

        needsUpdate |= bclass.addStaticListField("registration_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("registration_registered", "Registered", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("edit_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("edit_registered", "Registered", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("comment_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("comment_registered", "Registered", "---|Image|Text");

        needsUpdate |= bclass.addTextField("notification_pages", "Notification Pages", 60);

        needsUpdate |= bclass.addBooleanField("renderXWikiVelocityRenderer", "Render velocity code", "yesno");
        needsUpdate |= bclass.addBooleanField("renderXWikiGroovyRenderer", "Render Groovy code", "yesno");
        needsUpdate |= bclass.addBooleanField("renderXWikiRadeoxRenderer", "Render Wiki syntax", "yesno");

        needsUpdate |= bclass.addNumberField("upload_maxsize", "Maximum Upload Size", 5, "long");

        // for tags
        needsUpdate |= bclass.addBooleanField("tags", "Activate the tagging", "yesno");

        // for backlinks
        needsUpdate |= bclass.addBooleanField("backlinks", "Activate the backlinks", "yesno");

        // New fields for the XWiki 1.0 skin
        needsUpdate |= bclass.addTextField("leftPanels", "Panels displayed on the left", 60);
        needsUpdate |= bclass.addTextField("rightPanels", "Panels displayed on the right", 60);
        needsUpdate |= bclass.addBooleanField("showLeftPanels", "Display the left panel column", "yesno");
        needsUpdate |= bclass.addBooleanField("showRightPanels", "Display the right panel column", "yesno");
        needsUpdate |= bclass.addStaticListField("pageWidth", "Preferred page width", "default|640|800|1024|1280|1600");
        needsUpdate |= bclass.addTextField("languages", "Supported languages", 30);
        needsUpdate |= bclass.addTextField("convertmail", "convert email type", 1);
        needsUpdate |= bclass.addTextField("documentBundles", "Internationalization Document Bundles", 60);

        // Only used by LDAP authentication service

        needsUpdate |= bclass.addBooleanField("ldap", "Ldap", "yesno");
        needsUpdate |= bclass.addTextField("ldap_server", "Ldap server adress", 60);
        needsUpdate |= bclass.addTextField("ldap_port", "Ldap server port", 60);
        needsUpdate |= bclass.addTextField("ldap_bind_DN", "Ldap login matching", 60);
        needsUpdate |= bclass.addTextField("ldap_bind_pass", "Ldap password matching", 60);
        needsUpdate |= bclass.addBooleanField("ldap_validate_password", "Validate Ldap user/password", "yesno");
        needsUpdate |= bclass.addTextField("ldap_user_group", "Ldap group filter", 60);
        needsUpdate |= bclass.addTextField("ldap_base_DN", "Ldap base DN", 60);
        needsUpdate |= bclass.addTextField("ldap_UID_attr", "Ldap UID attribute name", 60);
        needsUpdate |= bclass.addTextField("ldap_fields_mapping", "Ldap user fiels mapping", 60);
        needsUpdate |= bclass.addBooleanField("ldap_update_user", "Update user from LDAP", "yesno");
        needsUpdate |= bclass.addTextField("ldap_group_mapping", "Ldap groups mapping", 80);
        needsUpdate |= bclass.addTextField("ldap_groupcache_expiration", "LDAP groups members cache", 60);
        needsUpdate |= bclass.addStaticListField("ldap_mode_group_sync", "LDAP groups sync mode", "|always|create");
        needsUpdate |= bclass.addBooleanField("ldap_trylocal", "Try local login", "yesno");

        if (((BooleanClass) bclass.get("showLeftPanels")).getDisplayType().equals("checkbox")) {
            ((BooleanClass) bclass.get("showLeftPanels")).setDisplayType("yesno");
            ((BooleanClass) bclass.get("showRightPanels")).setDisplayType("yesno");
            needsUpdate = true;
        }

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            needsUpdate = true;
            doc.setContent("1 XWiki Preferences");
        }

        String menu = doc.getStringValue("XWiki.XWikiPreferences", "menu");
        if (menu.indexOf("../..") != -1) {
            MenuSubstitution msubst = new MenuSubstitution(context.getUtil());
            menu = msubst.substitute(menu);
            doc.setLargeStringValue("XWiki.XWikiPreferences", "menu", menu);
            needsUpdate = true;
        }

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    public BaseClass getGroupClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        XWikiDocument template = null;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki.XWikiGroups", context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace("XWiki");
            doc.setName("XWikiGroups");
        }
        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        bclass.setName("XWiki.XWikiGroups");

        needsUpdate |= bclass.addTextField("member", "Member", 30);

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            doc.setContent("1 XWiki Groups");
        }

        if (needsUpdate) {
            saveDocument(doc, context);
        }

        // Create the group template document and attach a XWiki.XWikiGroupClass object
        try {
            template = getDocument("XWiki.XWikiGroupTemplate", context);
        } catch (Exception e) {
            template = new XWikiDocument();
            template.setSpace("XWiki");
            template.setName("XWikiGroupTemplate");
        } finally {
            if (template.isNew()) {
                template.setContent("#includeForm(\"XWiki.XWikiGroupSheet\")");
                template.createNewObject(bclass.getName(), context);
                template.setCreator("XWiki.Admin");
                template.setAuthor("XWiki.Admin");
                List<String> args = new ArrayList<String>(1);
                args.add("Group");
                saveDocument(template, context.getMessageTool().get("core.comment.createdTemplate", args), context);
            }
        }

        return bclass;
    }

    public BaseClass getRightsClass(String pagename, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        try {
            doc = getDocument("XWiki." + pagename, context);
        } catch (Exception e) {
            doc = new XWikiDocument();
            doc.setSpace("XWiki");
            doc.setName(pagename);
        }
        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        bclass.setName("XWiki." + pagename);

        PropertyInterface groupsProp = bclass.get("groups");
        if ((groupsProp != null) && !(groupsProp instanceof GroupsClass)) {
            bclass.removeField("groups");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addGroupsField("groups", "Groups");

        PropertyInterface levelsProp = bclass.get("levels");
        if ((levelsProp != null) && !(levelsProp instanceof LevelsClass)) {
            bclass.removeField("levels");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addLevelsField("levels", "Levels");

        PropertyInterface usersProp = bclass.get("users");
        if ((usersProp != null) && !(usersProp instanceof UsersClass)) {
            bclass.removeField("users");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addUsersField("users", "Users");

        PropertyInterface allowProp = bclass.get("allow");
        if ((allowProp != null) && (allowProp instanceof NumberClass)) {
            bclass.removeField("allow");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addBooleanField("allow", "Allow/Deny", "allow");
        BooleanClass afield = (BooleanClass) bclass.get("allow");
        if (afield.getDefaultValue() != 1) {
            afield.setDefaultValue(1);
            needsUpdate = true;
        }

        String content = doc.getContent();
        if ((content == null) || (content.equals(""))) {
            doc.setContent("1 XWiki " + pagename + " Class");
        }

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    public BaseClass getRightsClass(XWikiContext context) throws XWikiException
    {
        return getRightsClass("XWikiRights", context);
    }

    public BaseClass getGlobalRightsClass(XWikiContext context) throws XWikiException
    {
        return getRightsClass("XWikiGlobalRights", context);
    }

    public BaseClass getCommentsClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument("XWiki.XWikiComments", context);

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        bclass.setName("XWiki.XWikiComments");
        /*
         * if (!"internal".equals(bclass.getCustomMapping())) { needsUpdate = true; bclass.setCustomMapping("internal"); }
         */
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

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    public BaseClass getSkinClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument("XWiki.XWikiSkins", context);

        BaseClass bclass = doc.getxWikiClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

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

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    public int createUser(XWikiContext context) throws XWikiException
    {
        return createUser(false, "edit", context);
    }

    public int validateUser(boolean withConfirmEmail, XWikiContext context) throws XWikiException
    {
        try {
            XWikiRequest request = context.getRequest();
            String xwikiname = convertUsername(request.getParameter("xwikiname"), context);
            String validkey = request.getParameter("validkey");

            if (xwikiname.indexOf(".") == -1) {
                xwikiname = "XWiki." + xwikiname;
            }

            XWikiDocument docuser = getDocument(xwikiname, context);
            BaseObject userobj = docuser.getObject("XWiki.XWikiUsers", 0);
            String validkey2 = userobj.getStringValue("validkey");
            String email = userobj.getStringValue("email");
            String password = userobj.getStringValue("password");

            if ((!validkey2.equals("") && (validkey2.equals(validkey)))) {
                userobj.setIntValue("active", 1);
                saveDocument(docuser, context);

                if (withConfirmEmail) {
                    sendValidationEmail(xwikiname, password, email, validkey, "confirmation_email_content", context);
                }

                return 0;
            } else {
                return -1;
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_VALIDATE_USER,
                "Exception while validating user", e, null);
        }
    }

    public int createUser(boolean withValidation, String userRights, XWikiContext context) throws XWikiException
    {
        try {
            XWikiRequest request = context.getRequest();
            Map map = Util.getObject(request, "register");
            String content = "#includeForm(\"XWiki.XWikiUserSheet\")";
            String xwikiname = request.getParameter("xwikiname");
            String password2 = request.getParameter("register2_password");
            String password = ((String[]) map.get("password"))[0];
            String email = ((String[]) map.get("email"))[0];
            String template = request.getParameter("template");
            String parent = request.getParameter("parent");
            String validkey = null;

            if ("superadmin".equals(xwikiname)) {
                return -8;
            }
            try {
                if (!context.getUtil().match(this.Param("xwiki.validusername", "/^[a-zA-Z0-9_]+$/"), xwikiname)) {
                    return -4;
                }
            } catch (RuntimeException ex) {
                LOG.warn("Invalid regular expression for xwiki.validusername", ex);
                if (!context.getUtil().match("/^[a-zA-Z0-9_]+$/", xwikiname)) {
                    return -4;
                }
            }

            if ((!password.equals(password2)) || (password.trim().equals(""))) {
                // TODO: throw wrong password exception
                return -2;
            }

            if ((template != null) && (!template.equals(""))) {
                XWikiDocument tdoc = getDocument(template, context);
                if ((!tdoc.isNew())) {
                    content = tdoc.getContent();
                }
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

    /**
     * Method allows to create an empty user with no password (he won't be able to login) This method is usefull for
     * authentication like LDAP or App Server trusted
     * 
     * @param xwikiname
     * @param userRights
     * @param context
     * @return true if success
     * @throws XWikiException
     */
    public boolean createEmptyUser(String xwikiname, String userRights, XWikiContext context) throws XWikiException
    {
        HashMap map = new HashMap();
        map.put("active", "1");
        map.put("first_name", xwikiname);
        String content = "#includeForm(\"XWiki.XWikiUserSheet\")";
        if (createUser(xwikiname, map, "XWiki.XWikiUsers", content, userRights, context) == 1) {
            return true;
        } else {
            return false;
        }
    }

    public void sendConfirmationEmail(String xwikiname, String password, String email, String message,
        String contentfield, XWikiContext context) throws XWikiException
    {
        sendValidationEmail(xwikiname, password, email, "message", message, contentfield, context);
    }

    public void sendValidationEmail(String xwikiname, String password, String email, String validkey,
        String contentfield, XWikiContext context) throws XWikiException
    {
        sendValidationEmail(xwikiname, password, email, "validkey", validkey, contentfield, context);
    }

    public void sendValidationEmail(String xwikiname, String password, String email, String addfieldname,
        String addfieldvalue, String contentfield, XWikiContext context) throws XWikiException
    {
        String sender;
        String content;

        try {
            sender = getXWikiPreference("admin_email", context);
            content = getXWikiPreference(contentfield, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                XWikiException.ERROR_XWIKI_EMAIL_CANNOT_GET_VALIDATION_CONFIG,
                "Exception while reading the validation email config", e, null);

        }

        try {
            VelocityContext vcontext = (VelocityContext) context.get("vcontext");
            vcontext.put(addfieldname, addfieldvalue);
            vcontext.put("email", email);
            vcontext.put("password", password);
            vcontext.put("sender", sender);
            vcontext.put("xwikiname", xwikiname);
            content = parseContent(content, context);
        } catch (Exception e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                XWikiException.ERROR_XWIKI_EMAIL_CANNOT_PREPARE_VALIDATION_EMAIL,
                "Exception while preparing the validation email", e, null);

        }

        // Let's now send the message
        sendMessage(sender, email, content, context);
    }

    /**
     * @deprecated replaced by the <a href="http://code.xwiki.org/xwiki/bin/view/Plugins/MailSenderPlugin">Mail Sender
     *             Plugin</a>
     */
    @Deprecated
    public void sendMessage(String sender, String[] recipient, String message, XWikiContext context)
        throws XWikiException
    {
        SMTPClient smtpc = null;
        try {
            String server = getXWikiPreference("smtp_server", context);
            String port = getXWikiPreference("smtp_port", context);
            String login = getXWikiPreference("smtp_login", context);

            if (context.get("debugMail") != null) {
                StringBuffer msg = new StringBuffer(message);
                msg.append("\n Recipient: ");
                msg.append(recipient);
                recipient = ((String) context.get("debugMail")).split(",");
                message = msg.toString();
            }

            if ((server == null) || server.equals("")) {
                server = "127.0.0.1";
            }
            if ((port == null) || (port.equals(""))) {
                port = "25";
            }
            if ((login == null) || login.equals("")) {
                login = InetAddress.getLocalHost().getHostName();
            }

            smtpc = new SMTPClient();
            smtpc.connect(server, Integer.parseInt(port));
            int reply = smtpc.getReplyCode();
            if (!SMTPReply.isPositiveCompletion(reply)) {
                Object[] args = {server, port, new Integer(reply), smtpc.getReplyString()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                    XWikiException.ERROR_XWIKI_EMAIL_CONNECT_FAILED,
                    "Could not connect to server {0} port {1} error code {2} ({3})", null, args);
            }

            if (smtpc.login(login) == false) {
                reply = smtpc.getReplyCode();
                Object[] args = {server, port, new Integer(reply), smtpc.getReplyString()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                    XWikiException.ERROR_XWIKI_EMAIL_LOGIN_FAILED,
                    "Could not login to mail server {0} port {1} error code {2} ({3})", null, args);
            }

            if (smtpc.sendSimpleMessage(sender, recipient, message) == false) {
                reply = smtpc.getReplyCode();
                Object[] args = {server, port, new Integer(reply), smtpc.getReplyString()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                    XWikiException.ERROR_XWIKI_EMAIL_SEND_FAILED,
                    "Could not send mail to server {0} port {1} error code {2} ({3})", null, args);
            }

        } catch (IOException e) {
            Object[] args = {sender, recipient};
            throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                XWikiException.ERROR_XWIKI_EMAIL_ERROR_SENDING_EMAIL, "Exception while sending email from {0} to {1}",
                e, args);
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

    /**
     * @deprecated replaced by the <a href="http://code.xwiki.org/xwiki/bin/view/Plugins/MailSenderPlugin">Mail Sender
     *             Plugin</a>
     */
    @Deprecated
    public void sendMessage(String sender, String recipient, String message, XWikiContext context)
        throws XWikiException
    {
        String[] recip = recipient.split(",");
        sendMessage(sender, recip, message, context);
    }

    public String generateRandomString(int size)
    {
        return RandomStringUtils.randomAlphanumeric(size);
    }

    public String generateValidationKey(int size)
    {
        return generateRandomString(size);
    }

    public int createUser(String xwikiname, Map map, String parent, String content, String userRights,
        XWikiContext context) throws XWikiException
    {
        BaseClass baseclass = getUserClass(context);

        try {
            String fullwikiname = "XWiki." + xwikiname;

            // TODO: Verify existing user
            XWikiDocument doc = getDocument(fullwikiname, context);

            if (!doc.isNew()) {
                // TODO: throws Exception
                return -3;
            }

            /*
             * if(!this.checkAccess("register", doc, context)){ return -1; }
             */

            BaseObject newobject = (BaseObject) baseclass.fromMap(map, context);
            newobject.setName(fullwikiname);
            doc.addObject(baseclass.getName(), newobject);
            doc.setParent(parent);
            doc.setContent(content);
            doc.setCreator(doc.getFullName());
            doc.setAuthor(doc.getFullName());

            protectUserPage(fullwikiname, userRights, doc, context);

            saveDocument(doc, context.getMessageTool().get("core.comment.createdUser"), context);

            // Now let's add the user to XWiki.XWikiAllGroup
            setUserDefaultGroup(fullwikiname, context);

            return 1;
        } catch (Exception e) {
            Object[] args = {"XWiki." + xwikiname};
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_CREATE,
                "Cannot create user {0}", e, args);
        }
    }

    public void setUserDefaultGroup(String fullwikiname, XWikiContext context) throws XWikiException
    {
        String groupsPreference = this.Param("xwiki.users.initialGroups", "XWiki.XWikiAllGroup");

        if (groupsPreference != null) {
            String[] groups = groupsPreference.split(",");
            for (int i = 0; i < groups.length; ++i) {
                if (groups[i].trim().equals("")) {
                    continue;
                }
                addUserToGroup(fullwikiname, groups[i].trim(), context);
            }
        }
    }

    protected void addUserToGroup(String userName, String groupName, XWikiContext context) throws XWikiException
    {
        BaseClass groupClass = getGroupClass(context);
        XWikiDocument groupDoc = getDocument(groupName, context);

        BaseObject memberObject = (BaseObject) groupClass.newObject(context);
        memberObject.setClassName(groupClass.getName());
        memberObject.setName(groupDoc.getFullName());
        memberObject.setStringValue("member", userName);
        groupDoc.addObject(groupClass.getName(), memberObject);
        if (groupDoc.isNew()) {
            saveDocument(groupDoc, context.getMessageTool().get("core.comment.addedUserToGroup"), context);
        } else {
            getHibernateStore().saveXWikiObject(memberObject, context, true);
        }

        try {
            XWikiGroupService gservice = getGroupService(context);
            gservice.addUserToGroup(userName, context.getDatabase(), groupName, context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @deprecated replaced by {@link #setUserDefaultGroup(String fullwikiname, XWikiContext context)}
     * @param context
     * @param fullwikiname
     * @throws XWikiException
     */
    @Deprecated
    public void SetUserDefaultGroup(XWikiContext context, String fullwikiname) throws XWikiException
    {
        setUserDefaultGroup(fullwikiname, context);
    }

    public void protectUserPage(String fullwikiname, String userRights, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        BaseClass rclass = getRightsClass(context);
        // Add protection to the page
        BaseObject newrightsobject = (BaseObject) rclass.newObject(context);
        newrightsobject.setClassName(rclass.getName());
        newrightsobject.setName(fullwikiname);
        newrightsobject.setLargeStringValue("groups", "XWiki.XWikiAdminGroup");
        newrightsobject.setStringValue("levels", userRights);
        newrightsobject.setIntValue("allow", 1);
        doc.addObject(rclass.getName(), newrightsobject);

        BaseObject newuserrightsobject = (BaseObject) rclass.newObject(context);
        newuserrightsobject.setClassName(rclass.getName());
        newuserrightsobject.setName(fullwikiname);
        newuserrightsobject.setLargeStringValue("users", fullwikiname);
        newuserrightsobject.setStringValue("levels", userRights);
        newuserrightsobject.setIntValue("allow", 1);
        doc.addObject(rclass.getName(), newuserrightsobject);
    }

    /**
     * @deprecated replaced by {@link #protectUserPage(String,String,XWikiDocument,XWikiContext)}
     * @param context
     * @param fullwikiname
     * @param userRights
     * @param doc
     * @throws XWikiException
     */
    @Deprecated
    public void ProtectUserPage(XWikiContext context, String fullwikiname, String userRights, XWikiDocument doc)
        throws XWikiException
    {
        protectUserPage(fullwikiname, userRights, doc, context);
    }

    public User getUser(XWikiContext context)
    {
        XWikiUser xwikiUser = context.getXWikiUser();
        User user = new User(xwikiUser, context);
        return user;
    }

    public User getUser(String username, XWikiContext context)
    {
        XWikiUser xwikiUser = new XWikiUser(username);
        User user = new User(xwikiUser, context);
        return user;
    }

    /**
     * Prepares the localized resources, according to the selected language. From any point in the code (java, velocity
     * or groovy) the "msg" parameter holds an instance of the localized resource bundle, and the "locale" parameter
     * holds the current locale settings.
     * 
     * @param context The request context.
     */
    public void prepareResources(XWikiContext context)
    {
        if (context.get("msg") == null) {
            // String ilanguage = getInterfaceLanguagePreference(context);
            String dlanguage = getLanguagePreference(context);
            Locale locale = new Locale(dlanguage);
            context.put("locale", locale);
            if (context.getResponse() != null) {
                context.getResponse().setLocale(locale);
            }
            ResourceBundle bundle = ResourceBundle.getBundle("ApplicationResources", locale);
            if (bundle == null) {
                bundle = ResourceBundle.getBundle("ApplicationResources");
            }
            XWikiMessageTool msg = new XWikiMessageTool(bundle, context);
            context.put("msg", msg);
            VelocityContext vcontext = ((VelocityContext) context.get("vcontext"));
            if (vcontext != null) {
                vcontext.put("msg", msg);
                vcontext.put("locale", locale);
            }
            Map gcontext = (Map) context.get("gcontext");
            if (gcontext != null) {
                gcontext.put("msg", msg);
                gcontext.put("locale", locale);
            }
        }
    }

    public XWikiUser checkAuth(XWikiContext context) throws XWikiException
    {
        return getAuthService().checkAuth(context);
    }

    public boolean checkAccess(String action, XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        if (action.equals("skin") && doc.getSpace().equals("skins")) {
            return true;
        }
        return getRightService().checkAccess(action, doc, context);
    }

    /**
     * @deprecated replaced by {@link #include(String topic, boolean isForm, XWikiContext context)}
     * @param topic
     * @param context
     * @param isForm
     * @return
     * @throws XWikiException
     */
    @Deprecated
    public String include(String topic, XWikiContext context, boolean isForm) throws XWikiException
    {
        return include(topic, isForm, context);
    }

    @SuppressWarnings("unchecked")
    public String include(String topic, boolean isForm, XWikiContext context) throws XWikiException
    {
        String database = null, incdatabase = null;
        String prefixedTopic, localTopic;
        Document currentdoc = null, currentcdoc = null, currenttdoc = null;
        Document gcurrentdoc = null, gcurrentcdoc = null, gcurrenttdoc = null;
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        String currentDocName = context.getDatabase() + ":" + context.getDoc().getFullName();
        if (vcontext != null) {
            currentdoc = (Document) vcontext.get("doc");
            currentcdoc = (Document) vcontext.get("cdoc");
            currenttdoc = (Document) vcontext.get("tdoc");
        }
        Map<Object, Object> gcontext = (Map<Object, Object>) context.get("gcontext");
        if (gcontext != null) {
            gcurrentdoc = (Document) gcontext.get("doc");
            gcurrentcdoc = (Document) gcontext.get("cdoc");
            gcurrenttdoc = (Document) gcontext.get("tdoc");
        }

        try {
            int i0 = topic.indexOf(":");
            if (i0 != -1) {
                incdatabase = topic.substring(0, i0);
                database = context.getDatabase();
                context.setDatabase(incdatabase);
                prefixedTopic = topic;
                localTopic = topic.substring(i0 + 1);
            } else {
                prefixedTopic = context.getDatabase() + ":" + topic;
                localTopic = topic;
            }

            XWikiDocument doc = null;
            try {
                LOG.debug("Including Topic " + topic);
                try {
                    Set<String> includedDocs = (Set<String>) context.get("included_docs");
                    if (includedDocs == null) {
                        includedDocs = new HashSet<String>();
                        context.put("included_docs", includedDocs);
                    }

                    if (includedDocs.contains(prefixedTopic) || currentDocName.equals(prefixedTopic)) {
                        LOG.warn("Error on too many recursive includes for topic " + topic);
                        return "Cannot make recursive include";
                    }
                    includedDocs.add(prefixedTopic);
                } catch (Exception e) {
                }

                doc = getDocument(((XWikiDocument) context.get("doc")).getSpace(), localTopic, context);

                if (checkAccess("view", doc, context) == false) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                        XWikiException.ERROR_XWIKI_ACCESS_DENIED, "Access to this document is denied");
                }
            } catch (XWikiException e) {
                LOG.warn("Exception Including Topic " + topic, e);
                return "Topic " + topic + " does not exist";
            }

            XWikiDocument contentdoc = doc.getTranslatedDocument(context);

            String result;
            if (isForm) {
                // We do everything in the context of the including document
                if (database != null) {
                    context.setDatabase(database);
                }
                result =
                    getRenderingEngine().renderText(contentdoc.getContent(), contentdoc,
                        (XWikiDocument) context.get("doc"), context);
            } else {
                // We stay in the context included document
                result = getRenderingEngine().renderText(contentdoc.getContent(), contentdoc, doc, context);
            }
            try {
                Set<String> includedDocs = (Set<String>) context.get("included_docs");
                if (includedDocs != null) {
                    includedDocs.remove(prefixedTopic);
                }
            } catch (Exception e) {
            }
            return result;
        } finally {
            if (database != null) {
                context.setDatabase(database);
            }

            if (currentdoc != null) {
                if (vcontext != null) {
                    vcontext.put("doc", currentdoc);
                }
            }
            if (gcurrentdoc != null) {
                if (gcontext != null) {
                    gcontext.put("doc", gcurrentdoc);
                }
            }
            if (currentcdoc != null) {
                if (vcontext != null) {
                    vcontext.put("cdoc", currentcdoc);
                }
            }
            if (gcurrentcdoc != null) {
                if (gcontext != null) {
                    gcontext.put("cdoc", gcurrentcdoc);
                }
            }
            if (currenttdoc != null) {
                if (vcontext != null) {
                    vcontext.put("tdoc", currenttdoc);
                }
            }
            if (gcurrenttdoc != null) {
                if (gcontext != null) {
                    gcontext.put("tdoc", gcurrenttdoc);
                }
            }
        }
    }

    public void deleteDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        deleteDocument(doc, true, context);
    }

    public void deleteDocument(XWikiDocument doc, boolean totrash, XWikiContext context) throws XWikiException
    {
        getNotificationManager().preverify(doc, new XWikiDocument(doc.getSpace(), doc.getName()),
            XWikiDocChangeNotificationInterface.EVENT_DELETE, context);
        if (hasRecycleBin(context) && totrash) {
            getRecycleBinStore().saveToRecycleBin(doc, context.getUser(), new Date(), context, true);
        }
        getStore().deleteXWikiDoc(doc, context);
        try {
            // This is the old notification mechanism. It is kept here because it is in a
            // deprecation stage. It will be removed later.
            getNotificationManager().verify(new XWikiDocument(doc.getSpace(), doc.getName()), doc,
                XWikiDocChangeNotificationInterface.EVENT_DELETE, context);
            // This is the new notification mechanism, implemented as a Plexus Component.
            // For the moment we're sending the XWiki context as the data, but this will be
            // changed in the future, when the whole platform will be written using components
            // and there won't be a need for the context. The old version is available using
            // doc.getOriginalDocument()
            ObservationManager om = (ObservationManager) Utils.getComponent(ObservationManager.ROLE);
            if (om != null) {
                XWikiDocument blankDoc = new XWikiDocument(doc.getSpace(), doc.getName());
                blankDoc.setOriginalDocument(doc);
                om.notify(new DocumentDeleteEvent(doc.getFullName()), blankDoc, context);
            }
        } catch (Exception ex) {
            LOG.error("Failed to send document delete notifications for document [" + doc.getFullName() + "]", ex);
        }
    }

    public String getDatabase()
    {
        return this.database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
    }

    public void gc()
    {
        System.gc();
    }

    public long freeMemory()
    {
        return Runtime.getRuntime().freeMemory();
    }

    public long totalMemory()
    {
        return Runtime.getRuntime().totalMemory();
    }

    public long maxMemory()
    {
        return Runtime.getRuntime().maxMemory();
    }

    public String[] split(String str, String sep)
    {
        return StringUtils.split(str, sep);
    }

    public String printStrackTrace(Throwable e)
    {
        StringWriter strwriter = new StringWriter();
        PrintWriter writer = new PrintWriter(strwriter);
        e.printStackTrace(writer);
        return strwriter.toString();
    }

    public boolean copyDocument(String docname, String targetdocname, XWikiContext context) throws XWikiException
    {
        return copyDocument(docname, targetdocname, null, null, null, true, context);
    }

    public boolean copyDocument(String docname, String targetdocname, boolean reset, XWikiContext context)
        throws XWikiException
    {
        return copyDocument(docname, targetdocname, null, null, null, reset, context);
    }

    public boolean copyDocument(String docname, String targetdocname, String wikilanguage, XWikiContext context)
        throws XWikiException
    {
        return copyDocument(docname, targetdocname, null, null, wikilanguage, true, context);
    }

    public boolean copyDocument(String docname, String sourceWiki, String targetWiki, String wikilanguage,
        XWikiContext context) throws XWikiException
    {
        return copyDocument(docname, docname, sourceWiki, targetWiki, wikilanguage, true, context);
    }

    public boolean copyDocument(String docname, String targetdocname, String sourceWiki, String targetWiki,
        String wikilanguage, boolean reset, XWikiContext context) throws XWikiException
    {
        return copyDocument(docname, targetdocname, sourceWiki, targetWiki, wikilanguage, reset, false, context);
    }

    public boolean copyDocument(String docname, String targetdocname, String sourceWiki, String targetWiki,
        String wikilanguage, boolean reset, boolean force, XWikiContext context) throws XWikiException
    {
        return copyDocument(docname, targetdocname, sourceWiki, targetWiki, wikilanguage, reset, force, false, context);
    }

    public boolean copyDocument(String docname, String targetdocname, String sourceWiki, String targetWiki,
        String wikilanguage, boolean reset, boolean force, boolean resetCreationData, XWikiContext context)
        throws XWikiException
    {
        String db = context.getDatabase();
        if (sourceWiki == null) {
            sourceWiki = db;
        }

        try {
            if (sourceWiki != null) {
                context.setDatabase(sourceWiki);
            }
            XWikiDocument sdoc = getDocument(docname, context);
            if (!sdoc.isNew()) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Copying document: " + docname + " (default) to " + targetdocname + " on wiki "
                        + targetWiki);
                }

                // Let's switch to the other database to verify if the document already exists
                if (targetWiki != null) {
                    context.setDatabase(targetWiki);
                }
                XWikiDocument tdoc = getDocument(targetdocname, context);
                // There is already an existing document
                if (!tdoc.isNew()) {
                    if (force) {
                        // We need to delete the previous document
                        deleteDocument(tdoc, context);
                    } else {
                        return false;
                    }
                }

                // Let's switch back again to the original db
                if (sourceWiki != null) {
                    context.setDatabase(sourceWiki);
                }

                if (wikilanguage == null) {
                    tdoc = sdoc.copyDocument(targetdocname, context);
                    // forget past versions
                    if (reset) {
                        tdoc.setVersion("1.1");
                    }
                    if (resetCreationData) {
                        Date now = new Date();
                        tdoc.setCreationDate(now);
                        tdoc.setContentUpdateDate(now);
                        tdoc.setDate(now);
                        tdoc.setCreator(context.getUser());
                        tdoc.setAuthor(context.getUser());
                    }
                    if (targetWiki != null) {
                        tdoc.setDatabase(targetWiki);
                    }

                    // we don't want to trigger a new version
                    // otherwise the version number will be wrong
                    tdoc.setMetaDataDirty(false);
                    tdoc.setContentDirty(false);

                    saveDocument(tdoc, context);

                    if (!reset) {
                        if (sourceWiki != null) {
                            context.setDatabase(sourceWiki);
                        }
                        XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                        if (targetWiki != null) {
                            context.setDatabase(targetWiki);
                        }
                        txda = txda.clone(tdoc.getId(), context);
                        getVersioningStore().saveXWikiDocArchive(txda, true, context);
                    } else {
                        getVersioningStore().resetRCSArchive(tdoc, true, context);
                    }

                    if (targetWiki != null) {
                        context.setDatabase(targetWiki);
                    }
                    for (XWikiAttachment attachment : tdoc.getAttachmentList()) {
                        getAttachmentStore().saveAttachmentContent(attachment, false, context, true);
                    }

                    // Now we need to copy the translations
                    if (sourceWiki != null) {
                        context.setDatabase(sourceWiki);
                    }
                    List<String> tlist = sdoc.getTranslationList(context);
                    for (String clanguage : tlist) {
                        XWikiDocument stdoc = sdoc.getTranslatedDocument(clanguage, context);
                        if (LOG.isInfoEnabled()) {
                            LOG.info("Copying document: " + docname + "(" + clanguage + ") to " + targetdocname
                                + " on wiki " + targetWiki);
                        }

                        if (targetWiki != null) {
                            context.setDatabase(targetWiki);
                        }
                        XWikiDocument ttdoc = tdoc.getTranslatedDocument(clanguage, context);

                        // There is already an existing document
                        if (ttdoc != tdoc) {
                            return false;
                        }

                        // Let's switch back again to the original db
                        if (sourceWiki != null) {
                            context.setDatabase(sourceWiki);
                        }

                        ttdoc = stdoc.copyDocument(targetdocname, context);

                        // forget past versions
                        if (reset) {
                            ttdoc.setVersion("1.1");
                        }
                        if (resetCreationData) {
                            Date now = new Date();
                            ttdoc.setCreationDate(now);
                            ttdoc.setContentUpdateDate(now);
                            ttdoc.setDate(now);
                            ttdoc.setCreator(context.getUser());
                            ttdoc.setAuthor(context.getUser());
                        }
                        if (targetWiki != null) {
                            ttdoc.setDatabase(targetWiki);
                        }

                        // we don't want to trigger a new version
                        // otherwise the version number will be wrong
                        tdoc.setMetaDataDirty(false);
                        tdoc.setContentDirty(false);

                        saveDocument(ttdoc, context);

                        if (!reset) {
                            if (sourceWiki != null) {
                                context.setDatabase(sourceWiki);
                            }
                            XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                            if (targetWiki != null) {
                                context.setDatabase(targetWiki);
                            }
                            txda = txda.clone(tdoc.getId(), context);
                            getVersioningStore().saveXWikiDocArchive(txda, true, context);
                        } else {
                            getVersioningStore().resetRCSArchive(tdoc, true, context);
                        }
                    }
                } else {
                    // We want only one language in the end
                    XWikiDocument stdoc = sdoc.getTranslatedDocument(wikilanguage, context);

                    tdoc = stdoc.copyDocument(targetdocname, context);

                    // forget language
                    tdoc.setDefaultLanguage(wikilanguage);
                    tdoc.setLanguage("");
                    // forget past versions
                    if (reset) {
                        tdoc.setVersion("1.1");
                    }
                    if (resetCreationData) {
                        Date now = new Date();
                        tdoc.setCreationDate(now);
                        tdoc.setContentUpdateDate(now);
                        tdoc.setDate(now);
                        tdoc.setCreator(context.getUser());
                        tdoc.setAuthor(context.getUser());
                    }

                    if (targetWiki != null) {
                        tdoc.setDatabase(targetWiki);
                    }

                    // we don't want to trigger a new version
                    // otherwise the version number will be wrong
                    tdoc.setMetaDataDirty(false);
                    tdoc.setContentDirty(false);

                    saveDocument(tdoc, context);

                    if (!reset) {
                        if (sourceWiki != null) {
                            context.setDatabase(sourceWiki);
                        }
                        XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                        if (targetWiki != null) {
                            context.setDatabase(targetWiki);
                        }
                        txda = txda.clone(tdoc.getId(), context);
                        getVersioningStore().saveXWikiDocArchive(txda, true, context);
                    } else {
                        getVersioningStore().resetRCSArchive(tdoc, true, context);
                    }

                    if (targetWiki != null) {
                        context.setDatabase(targetWiki);
                    }
                    for (XWikiAttachment attachment : tdoc.getAttachmentList()) {
                        getAttachmentStore().saveAttachmentContent(attachment, false, context, true);
                    }
                }
            }
            return true;
        } finally {
            context.setDatabase(db);
        }
    }

    public int copyWikiWeb(String web, String sourceWiki, String targetWiki, String wikilanguage, XWikiContext context)
        throws XWikiException
    {
        return copyWikiWeb(web, sourceWiki, targetWiki, wikilanguage, false, context);
    }

    public int copyWikiWeb(String web, String sourceWiki, String targetWiki, String wikilanguage, boolean clean,
        XWikiContext context) throws XWikiException
    {
        String db = context.getDatabase();
        int nb = 0;
        String sql = "";
        if (web != null) {
            sql = "where doc.space = '" + Utils.SQLFilter(web) + "'";
        }

        if (clean) {
            try {
                context.setDatabase(targetWiki);
                List<String> list = getStore().searchDocumentsNames(sql, context);
                if (LOG.isInfoEnabled()) {
                    LOG.info("Deleting " + list.size() + " documents from wiki " + targetWiki);
                }

                for (String docname : list) {
                    XWikiDocument doc = getDocument(docname, context);
                    deleteDocument(doc, context);
                }
            } finally {
                context.setDatabase(db);
            }
        }

        try {
            context.setDatabase(sourceWiki);
            List<String> list = getStore().searchDocumentsNames(sql, context);
            if (LOG.isInfoEnabled()) {
                LOG.info("Copying " + list.size() + " documents from wiki " + sourceWiki + " to wiki " + targetWiki);
            }

            for (String docname : list) {
                copyDocument(docname, sourceWiki, targetWiki, wikilanguage, context);
                nb++;
            }
            return nb;
        } finally {
            context.setDatabase(db);
        }
    }

    public int copyWiki(String sourceWiki, String targetWiki, String language, XWikiContext context)
        throws XWikiException
    {
        return copyWikiWeb(null, sourceWiki, targetWiki, language, context);
    }

    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName,
        String description, String wikilanguage, boolean failOnExist, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        wikiName = wikiName.toLowerCase();

        try {
            XWikiDocument userdoc = getDocument(wikiAdmin, context);

            // User does not exist
            if (userdoc.isNew()) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                        + "user does not exist");
                }
                return -2;
            }

            // User is not active
            if (!(userdoc.getIntValue("XWiki.XWikiUsers", "active") == 1)) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                        + "user is not active");
                }
                return -3;
            }

            String wikiForbiddenList = Param("xwiki.virtual.reserved_wikis");
            if (Util.contains(wikiName, wikiForbiddenList, ", ")) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                        + "wiki name is forbidden");
                }
                return -4;
            }

            String wikiServerPage = "XWikiServer" + wikiName.substring(0, 1).toUpperCase() + wikiName.substring(1);
            // Verify is server page already exist
            XWikiDocument serverdoc = getDocument("XWiki", wikiServerPage, context);
            if (serverdoc.isNew()) {
                // clear entry in virtual wiki cache
                this.virtualWikiMap.remove(wikiUrl);

                // Create Wiki Server page
                serverdoc.setStringValue("XWiki.XWikiServerClass", "server", wikiUrl);
                serverdoc.setStringValue("XWiki.XWikiServerClass", "owner", wikiAdmin);
                if (description != null) {
                    serverdoc.setLargeStringValue("XWiki.XWikiServerClass", "description", description);
                }
                if (wikilanguage != null) {
                    serverdoc.setStringValue("XWiki.XWikiServerClass", "language", wikilanguage);
                }
                serverdoc.setContent("#includeForm(\"XWiki.XWikiServerForm\")\n");
                serverdoc.setParent("XWiki.XWikiServerClass");
                saveDocument(serverdoc, context);
            } else {
                // If we are not allowed to continue if server page already exists

                if (failOnExist) {
                    if (LOG.isErrorEnabled()) {
                        LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                            + "wiki server page already exists");
                    }
                    return -5;
                } else if (LOG.isWarnEnabled()) {
                    LOG.warn("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                        + "wiki server page already exists");
                }

            }

            // Create wiki database
            try {
                context.setDatabase(getDatabase());
                getStore().createWiki(wikiName, context);
            } catch (XWikiException e) {
                if (LOG.isErrorEnabled()) {
                    if (e.getCode() == 10010) {
                        LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                            + "wiki database already exists");
                    } else if (e.getCode() == 10011) {
                        LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                            + "wiki database creation failed");
                    } else {
                        LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                            + "wiki database creation threw exception", e);
                    }
                }
            } catch (Exception e) {
                LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                    + "wiki database creation threw exception", e);
            }

            try {
                updateDatabase(wikiName, true, false, context);
            } catch (Exception e) {
                LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                    + "wiki database shema update threw exception", e);
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
            /*
             * XWikiDocument wikiprefdoc = getDocument("XWiki.XWikiPreferences", context);
             * wikiprefdoc.setStringValue("XWiki.XWikiGlobalRights", "users", wikiAdmin);
             * wikiprefdoc.setStringValue("XWiki.XWikiGlobalRights", "levels", "admin, edit");
             * wikiprefdoc.setIntValue("XWiki.XWikiGlobalRights", "allow", 1); saveDocument(wikiprefdoc, context);
             */
            return 1;
        } catch (Exception e) {
            LOG.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                + "wiki creation threw exception", e);
            return -10;
        } finally {
            context.setDatabase(database);
        }
    }

    public String getEncoding()
    {
        return Param("xwiki.encoding", "UTF-8");
    }

    public URL getServerURL(String database, XWikiContext context) throws MalformedURLException
    {
        String serverurl = null;
        if (database != null) {
            String db = context.getDatabase();
            try {
                context.setDatabase(getDatabase());
                XWikiDocument doc =
                    getDocument("XWiki.XWikiServer" + database.substring(0, 1).toUpperCase() + database.substring(1),
                        context);
                BaseObject serverobject = doc.getObject("XWiki.XWikiServerClass");
                if (serverobject != null) {
                    String server = serverobject.getStringValue("server");
                    if (server != null) {
                        int mode = serverobject.getIntValue("secure");
                        int port = (context.getURL().getPort() == -1) ? 80 : context.getURL().getPort();
                        if (mode == 1) {
                            if (port != 443) {
                                serverurl = "https://" + server + ":" + port + "/";
                            } else {
                                serverurl = "https://" + server + "/";
                            }
                        } else {
                            if (port != 80) {
                                serverurl = "http://" + server + ":" + port + "/";
                            } else {
                                serverurl = "http://" + server + "/";
                            }
                        }
                    }
                }
            } catch (Exception ex) {
            } finally {
                context.setDatabase(db);
            }
        }

        if (serverurl != null) {
            return new URL(serverurl);
        } else {
            return null;
        }
    }

    public String getURL(String fullname, String action, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullname, context);

        URL url =
            context.getURLFactory().createURL(doc.getSpace(), doc.getName(), action, null, null, doc.getDatabase(),
                context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getExternalURL(String fullname, String action, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullname, context);

        URL url =
            context.getURLFactory().createExternalURL(doc.getSpace(), doc.getName(), action, null, null,
                doc.getDatabase(), context);
        return url.toString();
    }

    public String getExternalURL(String fullname, String action, String querystring, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullname, context);

        URL url =
            context.getURLFactory().createExternalURL(doc.getSpace(), doc.getName(), action, querystring, null,
                doc.getDatabase(), context);
        return url.toString();
    }

    public String getURL(String fullname, String action, String querystring, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullname, context);

        URL url =
            context.getURLFactory().createURL(doc.getSpace(), doc.getName(), action, querystring, null,
                doc.getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getAttachmentURL(String fullname, String filename, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullname, context);
        return doc.getAttachmentURL(filename, "download", context);
    }

    // Usefull date functions

    public Date getCurrentDate()
    {
        return new Date();
    }

    public int getTimeDelta(long time)
    {
        Date ctime = new Date();
        return (int) (ctime.getTime() - time);
    }

    public Date getDate(long time)
    {
        return new Date(time);
    }

    public boolean isMultiLingual(XWikiContext context)
    {
        return "1".equals(getXWikiPreference("multilingual", "1", context));
    }

    /**
     * @return true for multi-wiki/false for mono-wiki
     */
    public boolean isVirtualMode()
    {
        // With exo we can't be using virtual mode
        if (isExo()) {
            return false;
        }

        return "1".equals(Param("xwiki.virtual"));
    }

    public boolean isExo()
    {
        return "1".equals(Param("xwiki.exo"));
    }

    public boolean isLDAP()
    {
        return "1".equals(Param("xwiki.authentication.ldap"));
    }

    public int checkActive(XWikiContext context) throws XWikiException
    {
        return checkActive(context.getUser(), context);
    }

    public int checkActive(String user, XWikiContext context) throws XWikiException
    {
        int active = 1;

        // These users are necessarly active
        if (user.equals("XWiki.XWikiGuest") || (user.equals("XWiki.superadmin"))) {
            return active;
        }

        String checkactivefield = getXWikiPreference("auth_active_check", context);
        if (checkactivefield.equals("1")) {
            String username = context.getUser();
            XWikiDocument userdoc = getDocument(username, context);
            active = userdoc.getIntValue("XWiki.XWikiUsers", "active");
        }
        return active;
    }

    public String getDocumentName(XWikiRequest request, XWikiContext context)
    {
        String docname;
        if (context.getMode() == XWikiContext.MODE_PORTLET) {
            if (request.getParameter("topic") != null) {
                docname = request.getParameter("topic");
            } else {
                docname = "Main.WebHome";
            }
        } else if (context.getMode() == XWikiContext.MODE_XMLRPC) {
            docname = context.getDoc().getFullName();
        } else {
            String action = context.getAction();
            if ((request.getParameter("topic") != null) && (action.equals("edit") || action.equals("inline"))) {
                docname = request.getParameter("topic");
            } else {
                // TODO: Introduce a XWikiURL class in charge of getting the information relevant
                // to XWiki from a request URL (action, space, document name, file, etc)

                // Important: We cannot use getPathInfo() as the container encodes it and different
                // containers encode it differently, depending on their internal behavior and how
                // they are configured. Thus to make this container-proof we use the
                // getRequestURI() which isn't modified by the container and is thus only
                // URL-encoded.

                // Note: Ideally we should modify the getDocumentNameFromPath method but in order
                // not to introduce any new bug right now we're reconstructing a path info that we
                // pass to it using the following algorithm:
                // path info = requestURI - (contextPath + servletPath)
                String path;
                if (!request.getRequestURI().startsWith(request.getContextPath() + request.getServletPath())) {
                    LOG.warn("Request URI [" + request.getRequestURI() + "] should have matched " + "context path ["
                        + request.getContextPath() + "] and servlet path [" + request.getServletPath() + "]");
                    // Even though this branch shouldn't get executed we never know what containers
                    // will return and thus in order to be safe we fall back to the previous
                    // behavior which was to use getPathInfo() for getting the path (with the
                    // potential issue with i18n encoding as stated above).
                    path = request.getPathInfo();
                } else {
                    path =
                        request.getRequestURI().substring(
                            request.getContextPath().length() + request.getServletPath().length());
                }

                // Fix error in some containers, which don't hide the jsessionid parameter from the
                // URL
                if (path.indexOf(";jsessionid=") != -1) {
                    path = path.substring(0, path.indexOf(";jsessionid="));
                }
                docname = getDocumentNameFromPath(path, context);
            }
        }
        return (docname.indexOf(":") < 0) ? context.getDatabase() + ":" + docname : docname;
    }

    public boolean prepareDocuments(XWikiRequest request, XWikiContext context, VelocityContext vcontext)
        throws XWikiException
    {
        XWikiDocument doc;
        context.getWiki().prepareResources(context);
        String docName = getDocumentName(request, context);
        if (context.getAction().equals("register")) {
            setPhonyDocument(docName, context, vcontext);
            doc = context.getDoc();
        } else {
            try {
                doc = getDocument(docName, context);
            } catch (XWikiException e) {
                doc = context.getDoc();
                if (context.getAction().equals("delete")) {
                    if (doc == null) {
                        setPhonyDocument(docName, context, vcontext);
                    }
                    if (!checkAccess("admin", doc, context)) {
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }

        // We need to check rights before we look for translations
        // Otherwise we don't have the user language
        if (checkAccess(context.getAction(), doc, context) == false) {
            Object[] args = {doc.getFullName(), context.getUser()};
            setPhonyDocument(docName, context, vcontext);
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access to document {0} has been denied to user {1}", null, args);
        } else if (checkActive(context) == 0) {
            boolean allow = false;
            String allowed = Param("xwiki.inactiveuser.allowedpages", "");
            if (context.getAction().equals("view") && !allowed.equals("")) {
                String[] allowedList = StringUtils.split(allowed, " ,");
                for (int i = 0; i < allowedList.length; i++) {
                    if (allowedList[i].equals(doc.getFullName())) {
                        allow = true;
                        break;
                    }
                }
            }
            if (!allow) {
                Object[] args = {context.getUser()};
                setPhonyDocument(docName, context, vcontext);
                throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INACTIVE,
                    "User {0} account is inactive", null, args);
            }
        }

        context.put("doc", doc);
        vcontext.put("doc", doc.newDocument(context));
        vcontext.put("cdoc", vcontext.get("doc"));
        XWikiDocument tdoc = doc.getTranslatedDocument(context);
        context.put("tdoc", tdoc);
        vcontext.put("tdoc", tdoc.newDocument(context));
        return true;
    }

    public void setPhonyDocument(String docName, XWikiContext context, VelocityContext vcontext)
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(docName);
        doc.setElements(XWikiDocument.HAS_ATTACHMENTS | XWikiDocument.HAS_OBJECTS);
        doc.setStore(getStore());
        context.put("doc", doc);
        vcontext.put("doc", doc.newDocument(context));
        vcontext.put("cdoc", vcontext.get("doc"));
        vcontext.put("tdoc", vcontext.get("doc"));
    }

    public XWikiEngineContext getEngineContext()
    {
        return this.engine_context;
    }

    public void setEngineContext(XWikiEngineContext engine_context)
    {
        this.engine_context = engine_context;
    }

    public URLPatternMatcher getUrlPatternMatcher()
    {
        return this.urlPatternMatcher;
    }

    public void setUrlPatternMatcher(URLPatternMatcher urlPatternMatcher)
    {
        this.urlPatternMatcher = urlPatternMatcher;
    }

    public void setAuthService(XWikiAuthService authService)
    {
        this.authService = authService;
    }

    public void setRightService(XWikiRightService rightService)
    {
        this.rightService = rightService;
    }

    public XWikiGroupService getGroupService(XWikiContext context) throws XWikiException
    {
        synchronized (this.GROUP_SERVICE_LOCK) {
            if (this.groupService == null) {
                String groupClass;
                if (isExo()) {
                    groupClass =
                        Param("xwiki.authentication.groupclass", "com.xpn.xwiki.user.impl.exo.ExoGroupServiceImpl");
                } else {
                    groupClass =
                        Param("xwiki.authentication.groupclass", "com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl");
                }

                try {
                    this.groupService = (XWikiGroupService) Class.forName(groupClass).newInstance();
                } catch (Exception e) {
                    e.printStackTrace();
                    // If eXo we still want to use instanciation to not have class dependency
                    // for java 1.4 compatibility
                    if (isExo()) {
                        try {
                            this.groupService =
                                (XWikiGroupService) Class.forName("com.xpn.xwiki.user.impl.exo.ExoGroupServiceImpl")
                                    .newInstance();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    } else {
                        this.groupService = new XWikiGroupServiceImpl();
                    }
                }
                this.groupService.init(this, context);
            }

            return this.groupService;
        }
    }

    public void setGroupService(XWikiGroupService groupService)
    {
        this.groupService = groupService;
    }

    // added some log statements to make debugging easier - LBlaze 2005.06.02
    public XWikiAuthService getAuthService()
    {
        synchronized (this.AUTH_SERVICE_LOCK) {
            if (this.authService == null) {

                LOG.info("Initializing AuthService...");

                String authClass = Param("xwiki.authentication.authclass");
                if (authClass != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Using custom AuthClass " + authClass + ".");
                    }
                } else {

                    if (isExo()) {
                        authClass = "com.xpn.xwiki.user.impl.exo.ExoAuthServiceImpl";
                    } else if (isLDAP()) {
                        authClass = "com.xpn.xwiki.user.impl.LDAP.LDAPAuthServiceImpl";
                    } else {
                        authClass = "com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl";
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Using default AuthClass " + authClass + ".");
                    }

                }

                try {

                    this.authService = (XWikiAuthService) Class.forName(authClass).newInstance();

                    LOG.debug("Initialized AuthService using Relfection.");

                } catch (Exception e) {

                    LOG.warn("Failed to initialize AuthService " + authClass
                        + " using Reflection, trying default implementations using 'new'.", e);

                    // e.printStackTrace(); - not needed? -LBlaze

                    // LDAP support wasn't here before, I assume it should be? -LBlaze

                    if (isExo()) {
                        this.authService = new ExoAuthServiceImpl();
                    } else if (isLDAP()) {
                        this.authService = new LDAPAuthServiceImpl();
                    } else {
                        this.authService = new XWikiAuthServiceImpl();
                    }

                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Initialized AuthService " + this.authService.getClass().getName() + " using 'new'.");
                    }

                }
            }
            return this.authService;
        }
    }

    // added some log statements to make debugging easier - LBlaze 2005.06.02
    public XWikiRightService getRightService()
    {
        synchronized (this.RIGHT_SERVICE_LOCK) {

            if (this.rightService == null) {

                LOG.info("Initializing RightService...");

                String rightsClass = Param("xwiki.authentication.rightsclass");
                if (rightsClass != null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Using custom RightsClass " + rightsClass + ".");
                    }
                } else {
                    rightsClass = "com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl";
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Using default RightsClass " + rightsClass + ".");
                    }
                }

                try {

                    this.rightService = (XWikiRightService) Class.forName(rightsClass).newInstance();
                    LOG.debug("Initialized RightService using Reflection.");

                } catch (Exception e) {

                    LOG.warn("Failed to initialize RightService " + rightsClass
                        + " using Reflection, trying default implementation using 'new'.", e);

                    // e.printStackTrace(); - not needed? -LBlaze

                    this.rightService = new XWikiRightServiceImpl();

                    if (LOG.isDebugEnabled()) {
                        LOG
                            .debug("Initialized RightService " + this.authService.getClass().getName()
                                + " using 'new'.");
                    }

                }
            }
            return this.rightService;
        }
    }

    public XWikiStatsService getStatsService(XWikiContext context)
    {
        synchronized (this.STATS_SERVICE_LOCK) {
            if (this.statsService == null) {
                if ("1".equals(Param("xwiki.stats", "1"))) {
                    String storeClass = Param("xwiki.stats.class", "com.xpn.xwiki.stats.impl.XWikiStatsServiceImpl");
                    try {
                        this.statsService = (XWikiStatsService) Class.forName(storeClass).newInstance();
                    } catch (Exception e) {
                        e.printStackTrace();
                        this.statsService = new XWikiStatsServiceImpl();
                    }
                    this.statsService.init(context);
                }
            }
            return this.statsService;
        }
    }

    public XWikiURLFactoryService getURLFactoryService()
    {
        if (this.urlFactoryService == null) {
            synchronized (this.URLFACTORY_SERVICE_LOCK) {
                if (this.urlFactoryService == null) {
                    LOG.info("Initializing URLFactory Service...");

                    XWikiURLFactoryService factoryService = null;

                    String urlFactoryServiceClass = Param("xwiki.urlfactory.serviceclass");
                    if (urlFactoryServiceClass != null) {
                        try {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Using custom URLFactory Service Class [" + urlFactoryServiceClass + "]");
                            }
                            factoryService =
                                (XWikiURLFactoryService) Class.forName(urlFactoryServiceClass).getConstructor(
                                    new Class[] {XWiki.class}).newInstance(new Object[] {this});
                        } catch (Exception e) {
                            factoryService = null;
                            LOG.warn("Failed to initialize URLFactory Service [" + urlFactoryServiceClass + "]", e);
                        }
                    }
                    if (factoryService == null) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("Using default URLFactory Service Class [" + urlFactoryServiceClass + "]");
                        }
                        factoryService = new XWikiURLFactoryServiceImpl(this);
                    }

                    // Set the urlFactoryService object in one assignment to prevent threading
                    // issues when checking for
                    // null above.
                    this.urlFactoryService = factoryService;
                }
            }
        }
        return this.urlFactoryService;
    }

    public XWikiCriteriaService getCriteriaService(XWikiContext context)
    {
        return this.criteriaService;
    }

    /**
     * @see #getExoService(String)
     * @deprecated use {@link #getExoService(String)} instead
     */
    @Deprecated
    public Object getService(String className) throws XWikiException
    {
        return getExoService(className);
    }

    /**
     * Privileged API to access an eXo Platform service from the Wiki Engine
     * 
     * @param className eXo classname to retrieve the service from
     * @return A object representing the service
     * @throws XWikiException if the service cannot be loaded
     * @since 1.1 Beta 1
     */
    public Object getExoService(String className) throws XWikiException
    {
        try {
            RootContainer manager = RootContainer.getInstance();
            return manager.getComponentInstanceOfType(Class.forName(className));
        } catch (Exception e) {
            Object[] args = {className};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_SERVICE_NOT_FOUND,
                "Service {0} not found", e, args);
        }
    }

    /**
     * @see #getExoPortalService(String)
     * @deprecated use {@link #getExoPortalService(String)} instead
     */
    @Deprecated
    public Object getPortalService(String className) throws XWikiException
    {
        return getExoPortalService(className);
    }

    /**
     * Privileged API to access an eXo Platform Portal service from the Wiki Engine
     * 
     * @param className eXo classname to retrieve the service from
     * @return A object representing the service
     * @throws XWikiException if the service cannot be loaded
     * @since 1.1 Beta 1
     */
    public Object getExoPortalService(String className) throws XWikiException
    {
        try {
            PortalContainer manager = PortalContainer.getInstance();
            return manager.getComponentInstanceOfType(Class.forName(className));
        } catch (Exception e) {
            Object[] args = {className};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP, XWikiException.ERROR_XWIKI_APP_SERVICE_NOT_FOUND,
                "Service {0} not found", e, args);
        }
    }

    public ZipOutputStream getZipOutputStream(XWikiContext context) throws IOException
    {
        return new ZipOutputStream(context.getResponse().getOutputStream());
    }

    private Map<String, SearchEngineRule> getSearchEngineRules(XWikiContext context)
    {
        // We currently hardcode the rules
        // We will put them in the preferences soon
        Map<String, SearchEngineRule> map = new HashMap<String, SearchEngineRule>();
        map.put("Google", new SearchEngineRule("google.", "s/(^|.*&)q=(.*?)(&.*|$)/$2/"));
        map.put("MSN", new SearchEngineRule("search.msn.", "s/(^|.*&)q=(.*?)(&.*|$)/$2/"));
        map.put("Yahoo", new SearchEngineRule("search.yahoo.", "s/(^|.*&)p=(.*?)(&.*|$)/$2/"));
        map.put("Voila", new SearchEngineRule("voila.fr", "s/(^|.*&)kw=(.*?)(&.*|$)/$2/"));
        return map;
    }

    public String getRefererText(String referer, XWikiContext context)
    {
        try {
            URL url = new URL(referer);
            Map<String, SearchEngineRule> searchengines = getSearchEngineRules(context);
            if (searchengines != null) {
                for (SearchEngineRule senginerule : searchengines.values()) {
                    String host = url.getHost();
                    int i1 = host.indexOf(senginerule.getHost());
                    if (i1 != -1) {
                        String query = context.getUtil().substitute(senginerule.getRegEx(), url.getQuery());
                        if ((query != null) && (!query.equals(""))) {
                            // We return the query text instead of the full referer
                            return host.substring(i1) + ":" + query;
                        }
                    }
                }
            }
        } catch (Exception e) {
        }

        String result = referer.substring(referer.indexOf("://") + 3);
        if (result.endsWith("/")) {
            return result.substring(0, result.length() - 1);
        } else {
            return result;
        }
    }

    public boolean isMySQL()
    {
        if (getHibernateStore() == null) {
            return false;
        }
        return "org.hibernate.dialect.MySQLDialect".equals(getHibernateStore().getConfiguration().getProperties().get(
            "dialect"))
            || "net.sf.hibernate.dialect.MySQLDialect".equals(getHibernateStore().getConfiguration().getProperties()
                .get("dialect"));
    }

    public String getFullNameSQL()
    {
        return getFullNameSQL(true);
    }

    public String getFullNameSQL(boolean newFullName)
    {
        if (newFullName) {
            return "doc.fullName";
        }

        if (this.fullNameSQL == null) {
            if (isMySQL()) {
                this.fullNameSQL = "CONCAT(doc.space,'.',doc.name)";
            } else {
                this.fullNameSQL = "doc.space||'.'||doc.name";
            }
        }
        return this.fullNameSQL;
    }

    public String getDocName(String docname)
    {
        return docname.substring(docname.indexOf(".") + 1);
    }

    public String getUserName(String user, XWikiContext context)
    {
        return getUserName(user, null, true, context);
    }

    public String getUserName(String user, String format, XWikiContext context)
    {
        return getUserName(user, format, true, context);
    }

    public String getUserName(String user, String format, boolean link, XWikiContext context)
    {
        if (user == null || user.trim().equals("")) {
            return "";
        }
        XWikiDocument userdoc = null;
        try {
            userdoc = getDocument(user, context);
            if (userdoc == null) {
                return user;
            }

            BaseObject userobj = userdoc.getObject("XWiki.XWikiUsers");
            if (userobj == null) {
                return userdoc.getName();
            }

            Set proplist = userobj.getPropertyList();
            String text;

            if (format == null) {
                text = userobj.getStringValue("first_name") + " " + userobj.getStringValue("last_name");
                if (StringUtils.isBlank(text)) {
                    text = userdoc.getName();
                }
            } else {
                VelocityContext vcontext = new VelocityContext();
                for (Iterator it = proplist.iterator(); it.hasNext();) {
                    String propname = (String) it.next();
                    vcontext.put(propname, userobj.getStringValue(propname));
                }
                text =
                    XWikiVelocityRenderer.evaluate(format, "<username formatting code in "
                        + context.getDoc().getFullName() + ">", vcontext, context);
            }

            if (link == false) {
                return text.trim();
            } else {
                return "<span class=\"wikilink\"><a href=\"" + userdoc.getURL("view", context) + "\">" + text.trim()
                    + "</a></span>";
            }
        } catch (Exception e) {
            e.printStackTrace();
            if (userdoc != null) {
                return userdoc.getName();
            }
            return user;
        }
    }

    public boolean hasCentralizedAuthentication(XWikiContext context)
    {
        String bl = getXWikiPreference("authentication_centralized", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.authentication.centralized", "0"));
    }

    public String getLocalUserName(String user, XWikiContext context)
    {
        if (hasCentralizedAuthentication(context)) {
            return getUserName(user, null, true, context);
        } else {
            return getUserName(user.substring(user.indexOf(":") + 1), null, true, context);
        }
    }

    public String getLocalUserName(String user, String format, XWikiContext context)
    {
        if (hasCentralizedAuthentication(context)) {
            return getUserName(user, format, true, context);
        } else {
            return getUserName(user.substring(user.indexOf(":") + 1), format, true, context);
        }
    }

    public String getLocalUserName(String user, String format, boolean link, XWikiContext context)
    {
        if (hasCentralizedAuthentication(context)) {
            return getUserName(user, format, link, context);
        } else {
            return getUserName(user.substring(user.indexOf(":") + 1), format, link, context);
        }
    }

    public String formatDate(Date date, String format, XWikiContext context)
    {
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
                String language = getXWikiPreference("default_language", context);
                if ((language != null) && (!language.equals(""))) {
                    formatSymbols = new DateFormatSymbols(new Locale(language));
                }
            }

            SimpleDateFormat sdf;
            if (formatSymbols != null) {
                sdf = new SimpleDateFormat(xformat, formatSymbols);
            } else {
                sdf = new SimpleDateFormat(xformat);
            }

            try {
                sdf.setTimeZone(TimeZone.getTimeZone(getUserTimeZone(context)));
            } catch (Exception e) {
            }
            return sdf.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            if (format == null) {
                if (xformat.equals(defaultFormat)) {
                    return date.toString();
                } else {
                    return formatDate(date, defaultFormat, context);
                }
            } else {
                return formatDate(date, null, context);
            }
        }
    }

    /*
     * Allow to read user setting providing the user timezone All dates will be expressed with this timezone
     */
    public String getUserTimeZone(XWikiContext context)
    {
        String tz = getUserPreference("timezone", context);
        if ((tz == null) || (tz.equals(""))) {
            String defaultTz = TimeZone.getDefault().getID();
            return Param("xwiki.timezone", defaultTz);
        } else {
            return tz;
        }
    }

    public boolean exists(String fullname, XWikiContext context)
    {
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

    public String getAdType(XWikiContext context)
    {
        String adtype = "";
        if (isVirtualMode()) {
            XWikiDocument wikiServer = context.getWikiServer();
            if (wikiServer != null) {
                adtype = wikiServer.getStringValue("XWiki.XWikiServerClass", "adtype");
            }
        } else {
            adtype = getXWikiPreference("adtype", "", context);
        }
        if (adtype.equals("")) {
            adtype = Param("xwiki.ad.type", "");
        }
        return adtype;
    }

    public String getAdClientId(XWikiContext context)
    {
        final String defaultadclientid = "pub-2778691407285481";
        String adclientid = "";
        if (isVirtualMode()) {
            XWikiDocument wikiServer = context.getWikiServer();
            if (wikiServer != null) {
                adclientid = wikiServer.getStringValue("XWiki.XWikiServerClass", "adclientid");
            }
        } else {
            adclientid = getXWikiPreference("adclientid", "", context);
        }

        if (adclientid.equals("")) {
            adclientid = Param("xwiki.ad.clientid", "");
        }
        if (adclientid.equals("")) {
            adclientid = defaultadclientid;
        }
        return adclientid;
    }

    public XWikiPluginInterface getPlugin(String name, XWikiContext context)
    {
        XWikiPluginManager plugins = getPluginManager();
        Vector pluginlist = plugins.getPlugins();
        for (int i = 0; i < pluginlist.size(); i++) {
            String pluginname = (String) pluginlist.get(i);
            if (pluginname.equals(name)) {
                return plugins.getPlugin(pluginname);
            }
        }
        return null;
    }

    public Api getPluginApi(String name, XWikiContext context)
    {
        XWikiPluginInterface plugin = getPlugin(name, context);
        if (plugin != null) {
            return plugin.getPluginApi(plugin, context);
        }
        return null;
    }

    /*
     * public XWikiExecutionInfo getExecutionInfo(XWikiContext context) { Thread thread = Thread.currentThread();
     * XWikiExecutionInfo info = getThreadMap().get(thread); if (info==null) { info = new XWikiExecutionInfo(thread,
     * getRequestURL(context.getRequest())) } }
     */

    /**
     * @return the cache factory.
     * @since 1.5M2.
     */
    public CacheFactory getCacheFactory()
    {
        String cacheHint = Param("xwiki.cache.cachefactory.hint", "default");

        return (CacheFactory) Utils.getComponent(CacheFactory.ROLE, cacheHint);
    }

    /**
     * @return the cache factory creating local caches.
     * @since 1.5M2.
     */
    public CacheFactory getLocalCacheFactory()
    {
        String localCacheHint = Param("xwiki.cache.cachefactory.local.hint", "default");

        return (CacheFactory) Utils.getComponent(CacheFactory.ROLE, localCacheHint);
    }

    public int getHttpTimeout(XWikiContext context)
    {
        int defaulttimeout = 60000;
        return (context == null) ? defaulttimeout : (int) context.getWiki().ParamAsLong("xwiki.http.timeout",
            defaulttimeout);
    }

    public String getHttpUserAgent(XWikiContext context)
    {
        if (context != null) {
            return context.getWiki().Param("xwiki.http.useragent", "XWikiBot/1.0");
        } else {
            return "XWikiBot/1.0";
        }
    }

    public String getURLContent(String surl, XWikiContext context) throws IOException
    {
        return getURLContent(surl, getHttpTimeout(context), getHttpUserAgent(context));
    }

    public String getURLContent(String surl, int timeout, String userAgent) throws IOException
    {
        String content;
        HttpClient client = getHttpClient(timeout, userAgent);
        GetMethod get = new GetMethod(surl);

        try {
            client.executeMethod(get);
            content = get.getResponseBodyAsString();
        } finally {
            // Release any connection resources used by the method
            get.releaseConnection();
        }
        return content;
    }

    public String getURLContent(String surl, String username, String password, XWikiContext context) throws IOException
    {
        return getURLContent(surl, username, password, getHttpTimeout(context), getHttpUserAgent(context));
    }

    public String getURLContent(String surl, String username, String password, int timeout, String userAgent)
        throws IOException
    {
        HttpClient client = getHttpClient(timeout, userAgent);

        // pass our credentials to HttpClient, they will only be used for
        // authenticating to servers with realm "realm", to authenticate agains
        // an arbitrary realm change this to null.
        client.getState().setCredentials(new AuthScope(null, -1, null),
            new UsernamePasswordCredentials(username, password));

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // Tell the GET method to automatically handle authentication. The
            // method will use any appropriate credentials to handle basic
            // authentication requests. Setting this value to false will cause
            // any request for authentication to return with a status of 401.
            // It will then be up to the client to handle the authentication.
            get.setDoAuthentication(true);

            // execute the GET
            client.executeMethod(get);

            // print the status and response
            return get.getResponseBodyAsString();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public byte[] getURLContentAsBytes(String surl, XWikiContext context) throws IOException
    {
        return getURLContentAsBytes(surl, getHttpTimeout(context), getHttpUserAgent(context));
    }

    public byte[] getURLContentAsBytes(String surl, int timeout, String userAgent) throws IOException
    {
        HttpClient client = getHttpClient(timeout, userAgent);

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // execute the GET
            client.executeMethod(get);

            // print the status and response
            return get.getResponseBody();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public byte[] getURLContentAsBytes(String surl, String username, String password, XWikiContext context)
        throws IOException
    {
        return getURLContentAsBytes(surl, username, password, getHttpTimeout(context), getHttpUserAgent(context));
    }

    public byte[] getURLContentAsBytes(String surl, String username, String password, int timeout, String userAgent)
        throws IOException
    {
        HttpClient client = getHttpClient(timeout, userAgent);

        // pass our credentials to HttpClient, they will only be used for
        // authenticating to servers with realm "realm", to authenticate agains
        // an arbitrary realm change this to null.
        client.getState().setCredentials(new AuthScope(null, -1, null),
            new UsernamePasswordCredentials(username, password));

        // create a GET method that reads a file over HTTPS, we're assuming
        // that this file requires basic authentication using the realm above.
        GetMethod get = new GetMethod(surl);

        try {
            // Tell the GET method to automatically handle authentication. The
            // method will use any appropriate credentials to handle basic
            // authentication requests. Setting this value to false will cause
            // any request for authentication to return with a status of 401.
            // It will then be up to the client to handle the authentication.
            get.setDoAuthentication(true);

            // execute the GET
            client.executeMethod(get);

            // print the status and response
            return get.getResponseBody();
        } finally {
            // release any connection resources used by the method
            get.releaseConnection();
        }
    }

    public List<String> getSpaces(XWikiContext context) throws XWikiException
    {
        List<String> webs = null;
        if (getNotCacheStore() instanceof XWikiHibernateStore) {
            webs = this.search("select distinct doc.space from XWikiDocument doc", context);
        } else if (getNotCacheStore() instanceof XWikiJcrStore) {
            webs = ((XWikiJcrStore) getNotCacheStore()).getSpaces(context);
        }
        return webs;
    }

    public List<String> getSpaceDocsName(String spaceName, XWikiContext context) throws XWikiException
    {
        List<String> docs = null;
        if (getNotCacheStore() instanceof XWikiHibernateStore) {
            docs =
                this.search("select distinct doc.name from XWikiDocument doc",
                    new Object[][] {{"doc.space", spaceName}}, context);
        } else if (getNotCacheStore() instanceof XWikiJcrStore) {
            docs = ((XWikiJcrStore) getNotCacheStore()).getSpaceDocsName(spaceName, context);
        }
        return docs;
    }

    public List<String> getIncludedMacros(String defaultweb, String content, XWikiContext context)
    {
        try {
            String pattern = "#includeMacros\\(\"(.*?)\"\\)";
            List<String> list = context.getUtil().getUniqueMatches(content, pattern, 1);
            for (int i = 0; i < list.size(); i++) {
                try {
                    String name = list.get(i);
                    if (name.indexOf(".") == -1) {
                        list.set(i, defaultweb + "." + name);
                    }
                } catch (Exception e) {
                    // This should never happen
                    // TODO: log a nice error message instead
                    e.printStackTrace();
                    return Collections.emptyList();
                }
            }

            return list;
        } catch (Exception e) {
            // This should never happen
            // TODO: log a nice error message instead
            e.printStackTrace();
            // @todo Shouldn't we return an empty list instead!!!!
            return Collections.emptyList();
        }
    }

    public String getFlash(String url, String width, String height, XWikiContext context)
    {
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
     */
    public boolean isReadOnly()
    {
        return this.isReadOnly;
    }

    public void setReadOnly(boolean readOnly)
    {
        this.isReadOnly = readOnly;
    }

    public void deleteAllDocuments(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        deleteAllDocuments(doc, true, context);
    }

    public void deleteAllDocuments(XWikiDocument doc, boolean totrash, XWikiContext context) throws XWikiException
    {
        // Delete all documents
        for (String lang : doc.getTranslationList(context)) {
            XWikiDocument tdoc = doc.getTranslatedDocument(lang, context);
            deleteDocument(tdoc, totrash, context);
        }
        deleteDocument(doc, context);
    }

    public void refreshLinks(XWikiContext context) throws XWikiException
    {
        // refreshes all Links of each doc of the wiki
        List<String> docs = null;
        if (getNotCacheStore() instanceof XWikiHibernateStore) {
            docs = this.search("select doc.fullName from XWikiDocument as doc", context);
        } else if (getNotCacheStore() instanceof XWikiJcrStore) {
            docs = ((XWikiJcrStore) getNotCacheStore()).getAllDocuments(context);
        } else {
            return;
        }
        for (int i = 0; i < docs.size(); i++) {
            XWikiDocument myDoc = this.getDocument(docs.get(i), context);
            myDoc.getStore().saveLinks(myDoc, context, true);
        }
    }

    public boolean hasBacklinks(XWikiContext context)
    {
        String bl = getXWikiPreference("backlinks", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.backlinks", "0"));
    }

    public boolean hasTags(XWikiContext context)
    {
        String bl = getXWikiPreference("tags", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.tags", "0"));
    }

    public boolean hasCustomMappings()
    {
        return "1".equals(Param("xwiki.store.hibernate.custommapping", "1"));
    }

    public boolean hasDynamicCustomMappings()
    {
        return "1".equals(Param("xwiki.store.hibernate.custommapping.dynamic", "0"));
    }

    public String getDefaultWeb(XWikiContext context)
    {
        String dweb = getXWikiPreference("defaultweb", "", context);
        if ("".equals(dweb)) {
            return Param("xwiki.defaultweb", "Main");
        } else {
            return dweb;
        }
    }

    public boolean useDefaultWeb(XWikiContext context)
    {
        String bl = getXWikiPreference("usedefaultweb", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.usedefaultweb", "0"));
    }

    public boolean showViewAction(XWikiContext context)
    {
        String bl = getXWikiPreference("showviewaction", "", context);
        if ("1".equals(bl)) {
            return true;
        } else if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.showviewaction", "1"));
    }

    public boolean useDefaultAction(XWikiContext context)
    {
        String bl = getXWikiPreference("usedefaultaction", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.usedefaultaction", "0"));
    }

    public String getDefaultPage(XWikiContext context)
    {
        String dweb = getXWikiPreference("defaultpage", "", context);
        if ("".equals(dweb)) {
            return Param("xwiki.defaultpage", "WebHome");
        } else {
            return dweb;
        }
    }

    public boolean hasEditComment(XWikiContext context)
    {
        String bl = getXWikiPreference("editcomment", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.editcomment", "0"));
    }

    public boolean isEditCommentFieldHidden(XWikiContext context)
    {
        String bl = getXWikiPreference("editcomment_hidden", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.editcomment.hidden", "0"));
    }

    public boolean isEditCommentSuggested(XWikiContext context)
    {
        String bl = getXWikiPreference("editcomment_suggested", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.editcomment.suggested", "0"));
    }

    public boolean isEditCommentMandatory(XWikiContext context)
    {
        String bl = getXWikiPreference("editcomment_mandatory", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.editcomment.mandatory", "0"));
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#hasMinorEdit()
     */
    public boolean hasMinorEdit(XWikiContext context)
    {
        String bl = getXWikiPreference("minoredit", "", context);
        if ("1".equals(bl)) {
            return true;
        }
        if ("0".equals(bl)) {
            return false;
        }
        return "1".equals(Param("xwiki.minoredit", "1"));
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#hasRecycleBin()
     * @param context maybe will be useful
     */
    public boolean hasRecycleBin(XWikiContext context)
    {
        return "1".equals(Param("xwiki.recyclebin", "1"));
    }

    /**
     * Indicates whether deleted attachments are stored in a recycle bin or not. This can be configured using the key
     * <var>storage.attachment.recyclebin</var>.
     * 
     * @see com.xpn.xwiki.api.XWiki#hasAttachmentRecycleBin()
     * @param context The current {@link XWikiContext context}, maybe will be useful.
     */
    public boolean hasAttachmentRecycleBin(XWikiContext context)
    {
        return "1".equals(Param("storage.attachment.recyclebin", "1"));
    }

    /**
     * @deprecated use {@link XWikiDocument#rename(String, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument renamePage(XWikiDocument doc, String newFullName, XWikiContext context) throws XWikiException
    {
        if (context.getWiki().exists(newFullName, context)) {
            XWikiDocument delDoc = context.getWiki().getDocument(newFullName, context);
            context.getWiki().deleteDocument(delDoc, context);
        }
        XWikiDocument renamedDoc = doc.copyDocument(newFullName, context);
        saveDocument(renamedDoc, context);
        renamedDoc.saveAllAttachments(context);
        deleteDocument(doc, context);
        return renamedDoc;
    }

    /**
     * @deprecated use {@link XWikiDocument#rename(String, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument renamePage(XWikiDocument doc, XWikiContext context, String newFullName) throws XWikiException
    {
        return renamePage(doc, newFullName, context);
    }

    public BaseClass getClass(String fullName, XWikiContext context) throws XWikiException
    {
        // Used to avoid recursive loading of documents if there are recursives usage of classes
        BaseClass bclass = context.getBaseClass(fullName);
        if (bclass != null) {
            return bclass;
        }
        return getDocument(fullName, context).getxWikiClass();
    }

    public String getEditorPreference(XWikiContext context)
    {
        String pref = getUserPreference("editor", context);
        if (pref.equals("---")) {
            pref = getWebPreference("editor", context);
        }
        if (pref.equals("")) {
            pref = Param("xwiki.editor", "");
        }
        return pref.toLowerCase();
    }

    /**
     * Privileged API to retrieve an object instantiated from groovy code in a String. Note that Groovy scripts
     * compilation is cached.
     * 
     * @param script the Groovy class definition string (public class MyClass { ... })
     * @return An object instantiating this class
     * @throws XWikiException
     */
    public Object parseGroovyFromString(String script, XWikiContext context) throws XWikiException
    {
        if (getRenderingEngine().getRenderer("groovy") == null) {
            return null;
        } else {
            return ((XWikiGroovyRenderer) getRenderingEngine().getRenderer("groovy")).parseGroovyFromString(script,
                context);
        }
    }

    /**
     * Privileged API to retrieve an object instantiated from groovy code in a String, using a classloader including all
     * JAR files located in the passed page as attachments. Note that Groovy scripts compilation is cached
     * 
     * @param script the Groovy class definition string (public class MyClass { ... })
     * @return An object instantiating this class
     * @throws XWikiException
     */
    public Object parseGroovyFromString(String script, String jarWikiPage, XWikiContext context) throws XWikiException
    {
        if (getRenderingEngine().getRenderer("groovy") == null) {
            return null;
        }

        XWikiPageClassLoader pcl = new XWikiPageClassLoader(jarWikiPage, context);
        Object prevParentClassLoader = context.get("parentclassloader");
        try {
            context.put("parentclassloader", pcl);
            return parseGroovyFromString(script, context);
        } finally {
            if (prevParentClassLoader == null) {
                context.remove("parentclassloader");
            } else {
                context.put("parentclassloader", prevParentClassLoader);
            }
        }
    }

    public Object parseGroovyFromPage(String fullname, XWikiContext context) throws XWikiException
    {
        return parseGroovyFromString(context.getWiki().getDocument(fullname, context).getContent(), context);
    }

    public Object parseGroovyFromPage(String fullName, String jarWikiPage, XWikiContext context) throws XWikiException
    {
        return parseGroovyFromString(context.getWiki().getDocument(fullName, context).getContent(), context);
    }

    public String getMacroList(XWikiContext context)
    {
        String macrosmapping = "";
        XWiki xwiki = context.getWiki();

        try {
            macrosmapping = getResourceContent(MACROS_FILE);
        } catch (IOException e) {
        }

        macrosmapping += "\r\n" + xwiki.getXWikiPreference("macros_mapping", "", context);
        return macrosmapping;
    }

    // This functions adds an object from an new object creation form
    public BaseObject getObjectFromRequest(String className, XWikiContext context) throws XWikiException
    {
        Map<String, String[]> map = Util.getObject(context.getRequest(), className);
        BaseClass bclass = context.getWiki().getClass(className, context);
        BaseObject newobject = (BaseObject) bclass.fromMap(map, context);
        return newobject;
    }

    public String getConvertingUserNameType(XWikiContext context)
    {
        if (context.getWiki().getXWikiPreference("convertmail", context) != null
            && context.getWiki().getXWikiPreference("convertmail", context).length() > 0) {
            return context.getWiki().getXWikiPreference("convertmail", "0", context);
        }
        return context.getWiki().Param("xwiki.authentication.convertemail", "0");
    }

    public String convertUsername(String username, XWikiContext context)
    {
        if (username == null) {
            return null;
        }
        if (getConvertingUserNameType(context).equals("1") && (username.indexOf("@") != -1)) {
            String id = "" + username.hashCode();
            id = id.replaceAll("-", "");
            if (username.length() > 1) {
                int i1 = username.indexOf('@');
                id =
                    "" + username.charAt(0) + username.substring(i1 + 1, i1 + 2)
                        + username.charAt(username.length() - 1) + id;
            }
            return id;
        } else if (getConvertingUserNameType(context).equals("2")) {
            return username.replaceAll("[\\.\\@]", "_");
        } else {
            return username;
        }
    }

    public boolean hasSectionEdit(XWikiContext context)
    {
        return (context.getWiki().ParamAsLong("xwiki.section.edit", 0) == 1);
    }

    public boolean hasCaptcha(XWikiContext context)
    {
        return (context.getWiki().ParamAsLong("xwiki.plugin.captcha", 0) == 1);
    }

    public String getWysiwygToolbars(XWikiContext context)
    {
        return context.getWiki().Param("xwiki.wysiwyg.toolbars", "");
    }

    public String clearName(String name, XWikiContext context)
    {
        return clearName(name, true, true, context);
    }

    public String clearName(String name, boolean stripDots, boolean ascii, XWikiContext context)
    {
        String temp = name;
        temp =
            temp.replaceAll(
                "[\u00c0\u00c1\u00c2\u00c3\u00c4\u00c5\u0100\u0102\u0104\u01cd\u01de\u01e0\u01fa\u0200\u0202\u0226]",
                "A");
        temp =
            temp.replaceAll(
                "[\u00e0\u00e1\u00e2\u00e3\u00e4\u00e5\u0101\u0103\u0105\u01ce\u01df\u01e1\u01fb\u0201\u0203\u0227]",
                "a");
        temp = temp.replaceAll("[\u00c6\u01e2\u01fc]", "AE");
        temp = temp.replaceAll("[\u00e6\u01e3\u01fd]", "ae");
        temp = temp.replaceAll("[\u008c\u0152]", "OE");
        temp = temp.replaceAll("[\u009c\u0153]", "oe");
        temp = temp.replaceAll("[\u00c7\u0106\u0108\u010a\u010c]", "C");
        temp = temp.replaceAll("[\u00e7\u0107\u0109\u010b\u010d]", "c");
        temp = temp.replaceAll("[\u00d0\u010e\u0110]", "D");
        temp = temp.replaceAll("[\u00f0\u010f\u0111]", "d");
        temp = temp.replaceAll("[\u00c8\u00c9\u00ca\u00cb\u0112\u0114\u0116\u0118\u011a\u0204\u0206\u0228]", "E");
        temp = temp.replaceAll("[\u00e8\u00e9\u00ea\u00eb\u0113\u0115\u0117\u0119\u011b\u01dd\u0205\u0207\u0229]", "e");
        temp = temp.replaceAll("[\u011c\u011e\u0120\u0122\u01e4\u01e6\u01f4]", "G");
        temp = temp.replaceAll("[\u011d\u011f\u0121\u0123\u01e5\u01e7\u01f5]", "g");
        temp = temp.replaceAll("[\u0124\u0126\u021e]", "H");
        temp = temp.replaceAll("[\u0125\u0127\u021f]", "h");
        temp = temp.replaceAll("[\u00cc\u00cd\u00ce\u00cf\u0128\u012a\u012c\u012e\u0130\u01cf\u0208\u020a]", "I");
        temp = temp.replaceAll("[\u00ec\u00ed\u00ee\u00ef\u0129\u012b\u012d\u012f\u0131\u01d0\u0209\u020b]", "i");
        temp = temp.replaceAll("[\u0132]", "IJ");
        temp = temp.replaceAll("[\u0133]", "ij");
        temp = temp.replaceAll("[\u0134]", "J");
        temp = temp.replaceAll("[\u0135]", "j");
        temp = temp.replaceAll("[\u0136\u01e8]", "K");
        temp = temp.replaceAll("[\u0137\u0138\u01e9]", "k");
        temp = temp.replaceAll("[\u0139\u013b\u013d\u013f\u0141]", "L");
        temp = temp.replaceAll("[\u013a\u013c\u013e\u0140\u0142\u0234]", "l");
        temp = temp.replaceAll("[\u00d1\u0143\u0145\u0147\u014a\u01f8]", "N");
        temp = temp.replaceAll("[\u00f1\u0144\u0146\u0148\u0149\u014b\u01f9\u0235]", "n");
        temp =
            temp
                .replaceAll(
                    "[\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u014c\u014e\u0150\u01d1\u01ea\u01ec\u01fe\u020c\u020e\u022a\u022c\u022e\u0230]",
                    "O");
        temp =
            temp
                .replaceAll(
                    "[\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u01d2\u01eb\u01ed\u01ff\u020d\u020f\u022b\u022d\u022f\u0231]",
                    "o");
        temp = temp.replaceAll("[\u0156\u0158\u0210\u0212]", "R");
        temp = temp.replaceAll("[\u0157\u0159\u0211\u0213]", "r");
        temp = temp.replaceAll("[\u015a\u015c\u015e\u0160\u0218]", "S");
        temp = temp.replaceAll("[\u015b\u015d\u015f\u0161\u0219]", "s");
        temp = temp.replaceAll("[\u00de\u0162\u0164\u0166\u021a]", "T");
        temp = temp.replaceAll("[\u00fe\u0163\u0165\u0167\u021b\u0236]", "t");
        temp =
            temp
                .replaceAll(
                    "[\u00d9\u00da\u00db\u00dc\u0168\u016a\u016c\u016e\u0170\u0172\u01d3\u01d5\u01d7\u01d9\u01db\u0214\u0216]",
                    "U");
        temp =
            temp
                .replaceAll(
                    "[\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173\u01d4\u01d6\u01d8\u01da\u01dc\u0215\u0217]",
                    "u");
        temp = temp.replaceAll("/[\u0174]", "W");
        temp = temp.replaceAll("[\u0175]", "w");
        temp = temp.replaceAll("[\u00dd\u0176\u0178\u0232]", "Y");
        temp = temp.replaceAll("[\u00fd\u00ff\u0177\u0233]", "y");
        temp = temp.replaceAll("[\u0179\u017b\u017d]", "Z");
        temp = temp.replaceAll("[\u017a\u017c\u017e]", "z");
        temp = temp.replaceAll("[\u00df]", "SS");
        temp = temp.replaceAll("[_':,;\\\\/]", " ");
        name = temp;
        name = name.replaceAll("\\s+", "");
        name = name.replaceAll("[\\(\\)]", " ");

        if (stripDots) {
            name = name.replaceAll("[\\.]", "");
        }

        if (ascii) {
            name = name.replaceAll("[^a-zA-Z0-9\\-_\\.]", "");
        }

        if (name.length() > 250) {
            name = name.substring(0, 250);
        }
        return name;

    }

    public String getUniquePageName(String space, XWikiContext context)
    {
        String pageName = generateRandomString(16);
        return getUniquePageName(space, pageName, context);
    }

    public String getUniquePageName(String space, String name, XWikiContext context)
    {
        String pageName = clearName(name, context);
        if (exists(space + "." + pageName, context)) {
            int i = 0;
            while (exists(space + "." + pageName + "_" + i, context)) {
                i++;
            }
            return pageName + "_" + i;
        }
        return pageName;
    }

    public String displaySearch(String fieldname, String className, XWikiCriteria criteria, XWikiContext context)
        throws XWikiException
    {
        return displaySearch(fieldname, className, "", criteria, context);
    }

    public String displaySearch(String fieldname, String className, XWikiContext context) throws XWikiException
    {
        return displaySearch(fieldname, className, "", new XWikiCriteria(), context);
    }

    public String displaySearch(String fieldname, String className, String prefix, XWikiCriteria criteria,
        XWikiContext context) throws XWikiException
    {
        BaseClass bclass = getDocument(className, context).getxWikiClass();
        PropertyClass pclass = (PropertyClass) bclass.get(fieldname);
        if (criteria == null) {
            criteria = new XWikiCriteria();
        }
        if (pclass == null) {
            return "";
        } else {
            return pclass.displaySearch(fieldname, prefix + className + "_", criteria, context);
        }
    }

    public String displaySearchColumns(String className, XWikiQuery query, XWikiContext context) throws XWikiException
    {
        return displaySearchColumns(className, "", query, context);
    }

    public String displaySearchColumns(String className, String prefix, XWikiQuery query, XWikiContext context)
        throws XWikiException
    {
        BaseClass bclass = getDocument(className, context).getxWikiClass();

        if (query == null) {
            query = new XWikiQuery();
        }
        return bclass.displaySearchColumns(className + "_" + prefix, query, context);
    }

    public String displaySearchOrder(String className, XWikiQuery query, XWikiContext context) throws XWikiException
    {
        return displaySearchOrder(className, "", query, context);
    }

    public String displaySearchOrder(String className, String prefix, XWikiQuery query, XWikiContext context)
        throws XWikiException
    {
        BaseClass bclass = getDocument(className, context).getxWikiClass();

        if (query == null) {
            query = new XWikiQuery();
        }
        return bclass.displaySearchOrder(className + "_" + prefix, query, context);
    }

    public List search(XWikiQuery query, XWikiContext context) throws XWikiException
    {
        QueryPlugin qp = (QueryPlugin) getPlugin("query", context);
        if (qp == null) {
            return null;
        }
        return qp.search(query);
    }

    public XWikiQuery createQueryFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return new XWikiQuery(context.getRequest(), className, context);
    }

    public String searchAsTable(XWikiQuery query, XWikiContext context) throws XWikiException
    {
        QueryPlugin qp = (QueryPlugin) getPlugin("query", context);
        if (qp == null) {
            return null;
        }
        List list = qp.search(query);
        String result = "{table}\r\n";
        List<String> headerColumns = new ArrayList<String>();
        List displayProperties = query.getDisplayProperties();
        Iterator displayListIt = displayProperties.iterator();
        while (displayListIt.hasNext()) {
            String propname = (String) displayListIt.next();
            PropertyClass pclass = getPropertyClassFromName(propname, context);
            if (pclass != null) {
                headerColumns.add(pclass.getPrettyName());
            } else {
                if (propname.startsWith("doc.")) {
                    propname = propname.substring(4);
                    headerColumns.add(XWikiDocument.getInternalPropertyName(propname, context));
                } else {
                    headerColumns.add(propname);
                }

            }
        }
        result += StringUtils.join(headerColumns.toArray(), " | ") + "\r\n";
        Iterator resultIt = list.iterator();
        while (resultIt.hasNext()) {
            List<String> rowColumns = new ArrayList<String>();
            String docname = (String) resultIt.next();
            XWikiDocument doc = getDocument(docname, context);
            displayListIt = displayProperties.iterator();
            while (displayListIt.hasNext()) {
                String propname = (String) displayListIt.next();
                PropertyClass pclass = getPropertyClassFromName(propname, context);
                if (pclass == null) {
                    if (propname.startsWith("doc.")) {
                        propname = propname.substring(4);
                    }
                    String value = doc.getInternalProperty(propname);
                    rowColumns.add((value == null) ? " " : value);
                } else {
                    BaseObject bobj = doc.getObject(pclass.getObject().getName());
                    rowColumns.add(doc.display(pclass.getName(), "view", bobj, context));
                }
            }
            result += StringUtils.join(rowColumns.toArray(), " | ") + "\r\n";
        }
        result += "{table}\r\n";
        return result;
    }

    public PropertyClass getPropertyClassFromName(String propPath, XWikiContext context)
    {
        int i1 = propPath.indexOf("_");
        if (i1 == -1) {
            return null;
        } else {
            String className = propPath.substring(0, i1);
            String propName = propPath.substring(i1 + 1);
            try {
                return (PropertyClass) getDocument(className, context).getxWikiClass().get(propName);
            } catch (XWikiException e) {
                return null;
            }
        }
    }

    public boolean validateDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return doc.validate(context);
    }

    public String getMessage(String item, XWikiContext context)
    {
        XWikiMessageTool msg = context.getMessageTool();
        if (msg == null) {
            return item;
        } else {
            return msg.get(item);
        }
    }

    public String parseMessage(String id, XWikiContext context)
    {
        XWikiMessageTool msg = context.getMessageTool();
        return parseContent(msg.get(id), context);
    }

    public String parseMessage(XWikiContext context)
    {
        String message = (String) context.get("message");
        if (message == null) {
            return null;
        }
        return parseMessage(message, context);
    }

    public String addTooltip(String html, String message, String params, XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<span class=\"tooltip_span\" onmouseover=\"");
        buffer.append(params);
        buffer.append("; return escape('");
        buffer.append(message.replaceAll("'", "\\'"));
        buffer.append("');\">");
        buffer.append(html);
        buffer.append("</span>");
        return buffer.toString();
    }

    public String addTooltipJS(XWikiContext context)
    {
        StringBuffer buffer = new StringBuffer();
        buffer.append("<script type=\"text/javascript\" src=\"");
        buffer.append(getSkinFile("ajax/wzToolTip.js", context));
        buffer.append("\"></script>");
        // buffer.append("<div id=\"dhtmltooltip\"></div>");
        return buffer.toString();
    }

    public String addTooltip(String html, String message, XWikiContext context)
    {
        return addTooltip(html, message, "this.WIDTH='300'", context);
    }

    public void renamePage(String fullName, String newFullName, XWikiContext context) throws XWikiException
    {
        renamePage(context.getWiki().getDocument(fullName, context), newFullName, context);
    }

    public String addMandatory(XWikiContext context)
    {
        String star =
            "<span class=\"mandatoryParenthesis\">&nbsp;(</span><span class=\"mandatoryDot\">&lowast;</span><span class=\"mandatoryParenthesis\">)&nbsp;</span>";
        return context.getWiki().getXWikiPreference("mandatory_display", star, context);
    }

    public boolean hasVersioning(String fullName, XWikiContext context)
    {
        return ("1".equals(context.getWiki().Param("xwiki.store.versioning", "1")));
    }

    public boolean hasAttachmentVersioning(XWikiContext context)
    {
        return ("1".equals(context.getWiki().Param("xwiki.store.attachment.versioning", "1")));
    }

    public String getExternalAttachmentURL(String fullName, String filename, XWikiContext context)
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullName, context);
        return doc.getExternalAttachmentURL(filename, "download", context);
    }

    public int getMaxRecursiveSpaceChecks(XWikiContext context)
    {
        int max = getXWikiPreferenceAsInt("rights_maxrecursivespacechecks", -1, context);
        if (max == -1) {
            return (int) ParamAsLong("xwiki.rights.maxrecursivespacechecks", 0);
        } else {
            return max;
        }
    }

    /**
     * Get the XWiki temporary filesystem directory (deleted on exit)
     * 
     * @param context
     * @return temporary directory
     * @since 1.1 Milestone 4
     */
    public File getTempDirectory(XWikiContext context)
    {
        // if tempDir has already been set, return it
        if (tempDir != null) {
            return tempDir;
        }

        // xwiki.cfg
        String dirPath = context.getWiki().Param("xwiki.temp.dir");
        if (dirPath != null) {
            try {
                tempDir = new File(dirPath.replaceAll("\\s+$", ""));
                if (tempDir.isDirectory() && tempDir.canWrite()) {
                    tempDir.deleteOnExit();
                    return tempDir;
                }
            } catch (Exception e) {
                tempDir = null;
                LOG.warn("xwiki.temp.dir set in xwiki.cfg : " + dirPath + " does not exist or is not writable", e);
            }
        }

        Object jsct = context.getEngineContext().getAttribute("javax.servlet.context.tempdir");

        // javax.servlet.context.tempdir (File)
        if (jsct != null && (jsct instanceof File)) {
            workDir = (File) jsct;
            if (workDir.isDirectory() && workDir.canWrite()) {
                return workDir;
            }
        }

        // javax.servlet.context.tempdir (String)
        if (jsct != null && (jsct instanceof String)) {
            workDir = new File((String) jsct);

            if (workDir.isDirectory() && workDir.canWrite()) {
                return workDir;
            }
        }

        // Let's make a tempdir in java.io.tmpdir
        workDir = new File(System.getProperty("java.io.tmpdir"), "xwikiTemp");

        if (workDir.exists()) {
            workDir.deleteOnExit();
        } else {
            workDir.mkdir();
            workDir.deleteOnExit();
        }

        return workDir;
    }

    /**
     * Get a new directory in the xwiki work directory
     * 
     * @param subdir desired directory name
     * @param context
     * @return work subdirectory
     * @since 1.1 Milestone 4
     */
    public File getWorkSubdirectory(String subdir, XWikiContext context)
    {
        File fdir = new File(this.getWorkDirectory(context).getAbsolutePath(), subdir);
        if (!fdir.exists()) {
            fdir.mkdir();
        }
        return fdir;
    }

    /**
     * Get the XWiki work directory
     * 
     * @param context
     * @return work directory
     * @since 1.1 Milestone 4
     */
    public File getWorkDirectory(XWikiContext context)
    {
        String dirPath;

        // if workDir has already been set, return it
        if (workDir != null) {
            return workDir;
        }

        // xwiki.cfg
        dirPath = context.getWiki().Param("xwiki.work.dir");
        if (dirPath != null) {
            try {
                workDir = new File(dirPath.replaceAll("\\s+$", ""));
                if (workDir.exists()) {
                    if (workDir.isDirectory() && workDir.canWrite()) {
                        return workDir;
                    }
                } else {
                    workDir.mkdir();
                    return workDir;
                }
            } catch (Exception e) {
                workDir = null;
                LOG.warn("xwiki.work.dir set in xwiki.cfg : " + dirPath + " does not exist or is not writable", e);
            }
        }

        /*
         * TODO : it would be nice to create a subdirectory xwiki in APPServer/work or WEBAPP/WEB-INF/work
         */

        // No choices left, retreiving the temp directory
        workDir = this.getTempDirectory(context);

        return workDir;
    }

    public XWikiDocument rollback(final XWikiDocument tdoc, String rev, XWikiContext context) throws XWikiException
    {
        // let's clone rolledbackDoc since we might modify it
        XWikiDocument rolledbackDoc = (XWikiDocument) getDocument(tdoc, rev, context).clone();

        if ("1".equals(context.getWiki().Param("xwiki.store.rollbackattachmentwithdocuments", "1"))) {
            // XWIKI-851 We need to check the attachments..
            // If attachment exist in both versions (current and older), rollback the attachment to
            // the one in the older document
            // If attachment does not exist anymore we can't do anything, we have to delete the
            // reference in the older document
            // If attachment does not exist in the older, readd it and roll it back to the first
            // version uploaded
            // Delete attachment that do not exist anymore but existed in the version to which we
            // rollback
            List<XWikiAttachment> rolledbackAttachList = rolledbackDoc.getAttachmentList();
            List<XWikiAttachment> toDelete = new ArrayList<XWikiAttachment>();
            for (XWikiAttachment attach : rolledbackAttachList) {
                String fileName = attach.getFilename();
                if (tdoc.getAttachment(fileName) == null) {
                    // we need to remove that attachment because it does not exist in the never one
                    // TODO: if we have a storage system which keeps track of deleted attachments
                    // then we can recover it..
                    toDelete.add(attach);
                }
            }
            for (XWikiAttachment newAttach : toDelete) {
                rolledbackAttachList.remove(newAttach);
            }

            // Let's look now for attachment that are in both or present in the most recent doc but
            // not in the rolledback doc
            // if the attachment is in both we want to rollback to the version existing at the time
            // (if it exist)
            // if the attachment is not present in the rollbacked do we don't want to explicitely
            // loose a reference
            // to that doc, since we'll never be able to resolve it
            // so we want to keep this attachment and roll it back to the first ever uploaded
            // attachment
            List<XWikiAttachment> currentAttachList = tdoc.getAttachmentList();
            Map<String, XWikiAttachment> toRollbackMap = new HashMap<String, XWikiAttachment>();
            for (XWikiAttachment attach : currentAttachList) {
                String fileName = attach.getFilename();
                XWikiAttachment rolledbackAttach = rolledbackDoc.getAttachment(fileName);
                XWikiAttachment correctAttach = null;
                if (rolledbackAttach != null) {
                    // if the attachment exist in both let's look for the revision existing at the
                    // time
                    try {
                        correctAttach = rolledbackAttach.getAttachmentRevision(rolledbackAttach.getVersion(), context);
                    } catch (Exception e) {
                    }
                }

                // if we had an attachment but correctAttach returned null, then we might be in an
                // even more complex
                // case where the attachment was uploaded, delete, uploaded and we don't have the
                // same number of versions
                // in this case we want to still keep the latest attachment we have in the store.
                if (correctAttach == null) {
                    // let's re-add the attachment and roll it back to the first ever version
                    rolledbackAttach = (XWikiAttachment) attach.clone();
                    XWikiAttachment firstAttach = rolledbackAttach.getAttachmentRevision("1.1", context);
                    firstAttach.setVersion(attach.getVersion());
                    XWikiAttachmentArchive firstArchive =
                        (XWikiAttachmentArchive) rolledbackAttach.getAttachment_archive().clone();
                    firstArchive.setAttachment(firstAttach);
                    firstAttach.setAttachment_archive(firstArchive);
                    rolledbackAttachList.add(firstAttach);
                    toRollbackMap.put(rolledbackAttach.getFilename(), firstAttach);
                } else {
                    // we need to update the version
                    correctAttach.setVersion(attach.getVersion());
                    XWikiAttachmentArchive correctArchive =
                        (XWikiAttachmentArchive) rolledbackAttach.getAttachment_archive().clone();
                    correctArchive.setAttachment(correctAttach);
                    correctAttach.setAttachment_archive(correctArchive);
                    toRollbackMap.put(rolledbackAttach.getFilename(), correctAttach);
                    rolledbackAttachList.remove(rolledbackAttach);
                    rolledbackAttachList.add(correctAttach);
                }
            }

            // now we have the list of attachments to rollback.. let's do it
            // if this fails we might have saved some attachments without saving the document itself
            int attachmentTreated = 0;
            for (String fileName : toRollbackMap.keySet()) {
                XWikiAttachment newAttach = toRollbackMap.get(fileName);
                try {
                    context.getWiki().getAttachmentStore().saveAttachmentContent(newAttach, false, context, true);
                    attachmentTreated++;
                } catch (XWikiException e) {
                    LOG.error("Error rolling back attachment " + fileName + " in document rollback for document "
                        + tdoc.getFullName());
                    // If we have already treated an attachment it's better to continue and leave
                    // only one attachment in bad state
                    // this is better then leaving the whole document in bad state...
                    if (attachmentTreated == 0) {
                        throw e;
                    }
                }
            }
        }

        // now we save the final document..
        String username = context.getUser();
        rolledbackDoc.setAuthor(username);
        rolledbackDoc.setRCSVersion(tdoc.getRCSVersion());
        rolledbackDoc.setVersion(tdoc.getVersion());
        rolledbackDoc.setContentDirty(true);
        List<Object> params = new ArrayList<Object>();
        params.add(rev);

        saveDocument(rolledbackDoc, context.getMessageTool().get("core.comment.rollback", params), context);
        return rolledbackDoc;
    }
}
