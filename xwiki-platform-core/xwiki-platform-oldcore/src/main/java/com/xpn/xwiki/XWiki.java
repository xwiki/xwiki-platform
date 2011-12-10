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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TimeZone;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.velocity.VelocityContext;
import org.hibernate.HibernateException;
import org.securityfilter.filter.URLPatternMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentCreatingEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentDeletingEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.DocumentUpdatingEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.macro.wikibridge.WikiMacroInitializer;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxFactory;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.url.XWikiEntityURL;
import org.xwiki.url.standard.XWikiURLBuilder;
import org.xwiki.xml.XMLUtils;

import com.xpn.xwiki.api.Api;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.api.User;
import com.xpn.xwiki.criteria.api.XWikiCriteriaService;
import com.xpn.xwiki.doc.DeletedAttachment;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiDocumentArchive;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.PropertyInterface;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.BooleanClass;
import com.xpn.xwiki.objects.classes.GroupsClass;
import com.xpn.xwiki.objects.classes.LevelsClass;
import com.xpn.xwiki.objects.classes.NumberClass;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.objects.classes.StaticListClass;
import com.xpn.xwiki.objects.classes.UsersClass;
import com.xpn.xwiki.objects.meta.MetaClass;
import com.xpn.xwiki.plugin.XWikiPluginInterface;
import com.xpn.xwiki.plugin.XWikiPluginManager;
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
import com.xpn.xwiki.store.XWikiAttachmentStoreInterface;
import com.xpn.xwiki.store.XWikiCacheStore;
import com.xpn.xwiki.store.XWikiCacheStoreInterface;
import com.xpn.xwiki.store.XWikiHibernateStore;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.store.XWikiVersioningStoreInterface;
import com.xpn.xwiki.store.migration.AbstractXWikiMigrationManager;
import com.xpn.xwiki.user.api.XWikiAuthService;
import com.xpn.xwiki.user.api.XWikiGroupService;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.user.impl.LDAP.XWikiLDAPAuthServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl;
import com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.util.XWikiStubContextProvider;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiURLFactory;
import com.xpn.xwiki.web.XWikiURLFactoryService;
import com.xpn.xwiki.web.XWikiURLFactoryServiceImpl;
import com.xpn.xwiki.web.includeservletasstring.IncludeServletAsString;

public class XWiki implements EventListener
{
    /** Name of the default wiki. */
    public static final String DEFAULT_MAIN_WIKI = "xwiki";

    /** Name of the default home space. */
    public static final String DEFAULT_HOME_SPACE = "Main";

    /** Name of the default system space. */
    public static final String SYSTEM_SPACE = "XWiki";

    /** Name of the default space homepage. */
    public static final String DEFAULT_SPACE_HOMEPAGE = "WebHome";

    /** Logging helper object. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(XWiki.class);

    /** Frequently used Document reference, the class which holds virtual wiki definitions. */
    private static final DocumentReference VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE = new DocumentReference(
        DEFAULT_MAIN_WIKI, SYSTEM_SPACE, "XWikiServerClass");

    /** The default encoding, and the internally used encoding when dealing with byte representation of strings. */
    public static final String DEFAULT_ENCODING = "UTF-8";

    /** XWiki configuration loaded from xwiki.cfg. */
    private XWikiConfig config;

    /** The main document storage. */
    private XWikiStoreInterface store;

    /** The attachment storage (excluding attachment history). */
    private XWikiAttachmentStoreInterface attachmentStore;

    /** Store for attachment archives. */
    private AttachmentVersioningStore attachmentVersioningStore;

    /** Document versioning storage. */
    private XWikiVersioningStoreInterface versioningStore;

    /** Deleted documents storage. */
    private XWikiRecycleBinStoreInterface recycleBinStore;

    /**
     * Storage for deleted attachment.
     * 
     * @since 1.4M1
     */
    private AttachmentRecycleBinStore attachmentRecycleBinStore;

    private XWikiRenderingEngine renderingEngine;

    private XWikiPluginManager pluginManager;

    private XWikiAuthService authService;

    private XWikiRightService rightService;

    private XWikiGroupService groupService;

    private XWikiStatsService statsService;

    private XWikiURLFactoryService urlFactoryService;

    private XWikiCriteriaService criteriaService;

    /** Lock object used for the lazy initialization of the authentication service. */
    private final Object AUTH_SERVICE_LOCK = new Object();

    /** Lock object used for the lazy initialization of the authorization service. */
    private final Object RIGHT_SERVICE_LOCK = new Object();

    /** Lock object used for the lazy initialization of the group management service. */
    private final Object GROUP_SERVICE_LOCK = new Object();

    /** Lock object used for the lazy initialization of the statistics service. */
    private final Object STATS_SERVICE_LOCK = new Object();

    /** Lock object used for the lazy initialization of the URL Factory service. */
    private final Object URLFACTORY_SERVICE_LOCK = new Object();

    private MetaClass metaclass = MetaClass.getMetaClass();

    /** Is the wiki running in test mode? Deprecated, was used when running Cactus tests. */
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
    private Cache<DocumentReference> virtualWikiMap;

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

    /**
     * List of configured syntax ids.
     */
    private List<String> configuredSyntaxes;

    /**
     * Used to convert a proper Document Reference to string (standard form).
     */
    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer = Utils
        .getComponent(EntityReferenceSerializer.class);

    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<String> localStringEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.class, "local");

    private EntityReferenceValueProvider defaultEntityReferenceValueProvider = Utils
        .getComponent(EntityReferenceValueProvider.class);

    @SuppressWarnings("unchecked")
    private EntityReferenceSerializer<EntityReference> localReferenceEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.class, "local/reference");

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead.
     */
    @SuppressWarnings("unchecked")
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.class, "currentmixed");

    @SuppressWarnings("unchecked")
    private EntityReferenceResolver<String> relativeEntityReferenceResolver = Utils.getComponent(
        EntityReferenceResolver.class, "relative");

    private SyntaxFactory syntaxFactory = Utils.getComponent(SyntaxFactory.class);

    private XWikiURLBuilder entityXWikiURLBuilder = Utils.getComponent(XWikiURLBuilder.class, "entity");

    /**
     * Whether backlinks are enabled or not (cached for performance).
     *
     * @since 3.2M2
     */
    private Boolean hasBacklinks;

    public static String getConfigPath() throws NamingException
    {
        if (configPath == null) {
            try {
                Context envContext = (Context) new InitialContext().lookup("java:comp/env");
                configPath = (String) envContext.lookup(CFG_ENV_NAME);
            } catch (Exception e) {
                configPath = "/WEB-INF/xwiki.cfg";
                LOGGER.debug("The xwiki.cfg file will be read from [" + configPath + "] because "
                    + "its location couldn't be read from the JNDI [" + CFG_ENV_NAME + "] "
                    + "variable in [java:comp/env].");
            }
        }

        return configPath;
    }

    public static XWiki getMainXWiki(XWikiContext context) throws XWikiException
    {
        String xwikiname = DEFAULT_MAIN_WIKI;
        XWiki xwiki;
        XWikiEngineContext econtext = context.getEngineContext();

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

                context.setWiki(xwiki);

                // initialize stub context here instead of during Execution context initialization because during
                // Execution context initialization, the XWikiContext is not fully initialized (does not contains XWiki
                // object) which make it unusable
                Utils.getComponent(XWikiStubContextProvider.class).initialize(context);
            } else {
                context.setWiki(xwiki);
            }

            return xwiki;
        } catch (Exception e) {
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
            LOGGER.debug("Failed to load the file [" + configurationLocation + "] using direct "
                + "file access. The error was [" + e.getMessage() + "]. Trying to load it "
                + "as a resource using the Servlet Context...");
        }
        // Second, try loading it as a resource using the Servlet Context
        if (xwikicfgis == null) {
            xwikicfgis = econtext.getResourceAsStream(configurationLocation);
            LOGGER.debug("Failed to load the file [" + configurationLocation + "] as a resource "
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

        LOGGER.debug("Failed to load the file [" + configurationLocation + "] using any method.");

        // TODO: Should throw an exception instead of return null...

        return xwikicfgis;
    }

    /**
     * Return the XWiki object (as in "the Wiki API") corresponding to the requested wiki.
     * 
     * @param context the current context
     * @return an XWiki object configured for the wiki corresponding to the current request
     * @throws XWikiException if the requested URL does not correspond to a real wiki, or if there's an error in the
     *             storage
     */
    public static XWiki getXWiki(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = getMainXWiki(context);
        if (!xwiki.isVirtualMode()) {
            return xwiki;
        }

        // Host is full.host.name in DNS-based multiwiki, and wikiname in path-based multiwiki.
        String host = "";
        // Canonical name of the wiki (database)
        String wikiName = "";
        // wikiDefinition should be the document holding the definition of the virtual wiki, a document in the main
        // wiki with a XWiki.XWikiServerClass object attached to it
        DocumentReference wikiDefinition;

        XWikiRequest request = context.getRequest();
        try {
            URL requestURL = context.getURL();
            host = requestURL.getHost();
        } catch (Exception e) {
        }

        // In path-based multi-wiki, the wiki name is an element of the request path.
        // The url is in the form /xwiki (app name)/wiki (servlet name)/wikiname/
        if ("1".equals(xwiki.Param("xwiki.virtual.usepath", "0"))) {
            String uri = request.getRequestURI();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Request uri is: " + uri);
            }
            // Remove the (eventual) context path from the URI, usually /xwiki
            uri = stripSegmentFromPath(uri, request.getContextPath());
            // Remove the (eventual) servlet path from the URI, usually /wiki
            String servletPath = request.getServletPath();
            uri = stripSegmentFromPath(uri, servletPath);

            if (servletPath.equals("/" + xwiki.Param("xwiki.virtual.usepath.servletpath", "wiki"))) {
                // Requested path corresponds to a path-based wiki, now the wiki name is between the first and
                // second "/"
                host = StringUtils.substringBefore(StringUtils.removeStart(uri, "/"), "/");
            }
        }

        if (StringUtils.isEmpty(host) || host.equals(context.getMainXWiki())) {
            // Can't find any wiki name, return the main wiki
            return xwiki;
        }

        wikiDefinition = xwiki.findWikiServer(host, context);

        if (wikiDefinition == null) {
            // No definition found based on the full domain name/path wiki name, try to use the first part of the domain
            // name as the wiki name
            String servername = StringUtils.substringBefore(host, ".");

            // As a convenience, allow sites starting with www, localhost or using an
            // IP address not to have to create a XWikiServerXwiki page since we consider
            // in that case that they're pointing to the main wiki.
            if (!"0".equals(xwiki.Param("xwiki.virtual.autowww"))
                && (servername.equals("www") || host.equals("localhost") || host
                    .matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}"))) {
                return xwiki;
            }

            wikiDefinition =
                new DocumentReference(DEFAULT_MAIN_WIKI, SYSTEM_SPACE, "XWikiServer"
                    + StringUtils.capitalize(servername));
        }

        // Check if this wiki definition exists in the Database
        XWikiDocument doc = xwiki.getDocument(wikiDefinition, context);
        if (doc.isNew()) {
            throw new XWikiException(XWikiException.MODULE_XWIKI, XWikiException.ERROR_XWIKI_DOES_NOT_EXIST,
                "The wiki " + host + " does not exist");
        }

        // Set the wiki owner
        String wikiOwner = doc.getStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "owner");
        if (wikiOwner.indexOf(':') == -1) {
            wikiOwner = xwiki.getDatabase() + ":" + wikiOwner;
        }
        context.setWikiOwner(wikiOwner);
        context.setWikiServer(doc);

        wikiName = StringUtils.removeStart(wikiDefinition.getName(), "XWikiServer").toLowerCase();
        context.setDatabase(wikiName);
        context.setOriginalDatabase(wikiName);

        try {
            // Let's make sure the virtual wikis are upgraded to the latest database version
            xwiki.updateDatabase(wikiName, false, context);
        } catch (HibernateException ex) {
            // Just report it, hopefully the database is in a good enough state
            LOGGER.error("Failed to upgrade database: " + wikiName, ex);
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
        }
    }

    public static HttpClient getHttpClient(int timeout, String userAgent)
    {
        HttpClient client = new HttpClient();

        if (timeout != 0) {
            client.getParams().setSoTimeout(timeout);
            client.getParams().setParameter("http.connection.timeout", Integer.valueOf(timeout));
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
        return "XWiki.XWikiServer" + StringUtils.capitalize(servername);
    }

    public static String getTextArea(String content, XWikiContext context)
    {
        StringBuilder result = new StringBuilder();

        // Forcing a new line after the <textarea> tag, as
        // http://www.w3.org/TR/html4/appendix/notes.html#h-B.3.1 causes an empty line at the start
        // of the document content to be trimmed.
        result.append("<textarea name=\"content\" id=\"content\" rows=\"25\" cols=\"80\">\n");
        result.append(XMLUtils.escape(content));
        result.append("</textarea>");

        return result.toString();
    }

    /**
     * This provide a way to create an XWiki object without initializing the whole XWiki (including plugins, storage,
     * etc.).
     * <p>
     * Needed for tools or tests which need XWiki because it is used everywhere in the API.
     * </p>
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
        setDatabase(context.getMainXWiki());

        setEngineContext(engine_context);
        context.setWiki(this);

        // Prepare the store
        setConfig(config);

        XWikiStoreInterface basestore = Utils.getComponent(XWikiStoreInterface.class, Param("xwiki.store.main.hint"));

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

        setAttachmentStore(Utils
            .getComponent(XWikiAttachmentStoreInterface.class, Param("xwiki.store.attachment.hint")));

        setVersioningStore(Utils
            .getComponent(XWikiVersioningStoreInterface.class, Param("xwiki.store.versioning.hint")));

        setAttachmentVersioningStore(Utils.getComponent(AttachmentVersioningStore.class,
            hasAttachmentVersioning(context) ? Param("xwiki.store.attachment.versioning.hint") : "void"));

        if (hasRecycleBin(context)) {
            setRecycleBinStore(Utils.getComponent(XWikiRecycleBinStoreInterface.class,
                Param("xwiki.store.recyclebin.hint")));
        }

        if (hasAttachmentRecycleBin(context)) {
            setAttachmentRecycleBinStore(Utils.getComponent(AttachmentRecycleBinStore.class,
                Param("xwiki.store.attachment.recyclebin.hint")));
        }

        // Run migrations
        if ("1".equals(Param("xwiki.store.migration", "0"))) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Running storage migrations");
            }
            AbstractXWikiMigrationManager manager =
                (AbstractXWikiMigrationManager) createClassFromConfig("xwiki.store.migration.manager.class",
                    "com.xpn.xwiki.store.migration.hibernate.XWikiHibernateMigrationManager", context);
            manager.startMigrations(context);
            if ("1".equals(Param("xwiki.store.migration.exitAfterEnd", "0"))) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Exiting because xwiki.store.migration.exitAfterEnd is set");
                }
                System.exit(0);
            }
        }

        resetRenderingEngine(context);

        // Prepare the Plugin Engine
        preparePlugins(context);

        // Make sure these classes exists
        if (noupdate) {
            initializeMandatoryClasses(context);
            getStatsService(context);
        }

        String ro = Param("xwiki.readonly", "no");
        this.isReadOnly = ("yes".equalsIgnoreCase(ro) || "true".equalsIgnoreCase(ro) || "1".equalsIgnoreCase(ro));

        // Save the configured syntaxes
        String syntaxes = Param("xwiki.rendering.syntaxes", "xwiki/1.0");
        this.configuredSyntaxes = Arrays.asList(StringUtils.split(syntaxes, " ,"));

        // Initialize all wiki macros.
        // TODO: This is only a temporary work around, we need to use a component-based init mechanism instead. Note
        // that we need DB access to be available (at component initialization) to make this possible.
        registerWikiMacros();

        Utils.getComponent(ObservationManager.class).addListener(this);
    }

    /**
     * Ensure that mandatory classes (ie classes XWiki needs to work properly) exist and create them if they don't
     * exist.
     */
    private void initializeMandatoryClasses(XWikiContext context) throws XWikiException
    {
        getPrefsClass(context);
        getUserClass(context);
        getTagClass(context);
        getGroupClass(context);
        getRightsClass(context);
        getCommentsClass(context);
        getSkinClass(context);
        getGlobalRightsClass(context);
        getSheetClass(context);
        getEditModeClass(context);

        try {
            WikiMacroInitializer wikiMacroInitializer = Utils.getComponentManager().lookup(WikiMacroInitializer.class);
            wikiMacroInitializer.installOrUpgradeWikiMacroClasses();
        } catch (Exception ex) {
            LOGGER.error("Error while installing / upgrading xwiki classes required for wiki macros.", ex);
        }

        if (context.getDatabase().equals(context.getMainXWiki())
            && "1".equals(context.getWiki().Param("xwiki.preferences.redirect"))) {
            getRedirectClass(context);
        }
    }

    /**
     * TODO: This is only a temporary work around, we need to use a component-based init mechanism instead. Note that we
     * need DB access to be available (at component initialization) to make this possible.
     * <p>
     * This method is protected to be able to skip it in unit tests.
     */
    protected void registerWikiMacros()
    {
        try {
            WikiMacroInitializer wikiMacroInitializer = Utils.getComponentManager().lookup(WikiMacroInitializer.class);
            wikiMacroInitializer.registerExistingWikiMacros();
        } catch (Exception ex) {
            LOGGER.error("Error while registering wiki macros.", ex);
        }
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

    public void updateDatabase(String wikiName, XWikiContext context) throws HibernateException, XWikiException
    {
        updateDatabase(wikiName, false, context);
    }

    public void updateDatabase(String wikiName, boolean force, XWikiContext context) throws HibernateException,
        XWikiException
    {
        updateDatabase(wikiName, force, true, context);
    }

    public void updateDatabase(String wikiName, boolean force, boolean initClasses, XWikiContext context)
        throws HibernateException, XWikiException
    {
        String database = context.getDatabase();
        try {
            List<String> wikiList = getVirtualWikiList();

            // Make sure the wiki is updated
            if (force) {
                wikiList.remove(wikiName);
                context.remove("initdone");
            }

            context.setDatabase(wikiName);
            synchronized (wikiName) {
                if (!wikiList.contains(wikiName)) {
                    wikiList.add(wikiName);
                    XWikiHibernateStore store = getHibernateStore();
                    if (store != null) {
                        store.updateSchema(context, force);
                    }

                    // Make sure these classes exists
                    if (initClasses) {
                        initializeMandatoryClasses(context);
                        getPluginManager().virtualInit(context);
                        getRenderingEngine().virtualInit(context);
                    }
                }
            }

            // Add initdone which will allow to
            // bypass some initializations
            context.put("initdone", "1");
        } finally {
            context.setDatabase(database);
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
        String database = context.getDatabase();
        try {
            context.setDatabase(context.getMainXWiki());

            String query =
                ", BaseObject as obj where doc.space = 'XWiki' and obj.name=doc.fullName"
                    + " and obj.name <> 'XWiki.XWikiServerClassTemplate' and obj.className='XWiki.XWikiServerClass' ";
            List<DocumentReference> documents = getStore().searchDocumentReferences(query, context);
            List<String> databaseNames = new ArrayList<String>(documents.size());

            int prefixLength = "XWikiServer".length();
            for (DocumentReference document : documents) {
                if (document.getName().startsWith("XWikiServer")) {
                    databaseNames.add(document.getName().substring(prefixLength).toLowerCase());
                }
            }

            return databaseNames;
        } finally {
            context.setDatabase(database);
        }
    }

    /**
     * @return the cache containing the names of the wikis already initialized.
     * @since 1.5M2.
     */
    public Cache<DocumentReference> getVirtualWikiCache()
    {
        return this.virtualWikiMap;
    }

    /**
     * Searches for the document containing the definition of the virtual wiki corresponding to the specified hostname.
     * 
     * @param host the hostname, as specified in the request (for example: {@code forge.xwiki.org})
     * @param context the current context
     * @return the name of the document containing the wiki definition, or {@code null} if no wiki corresponds to the
     *         hostname
     * @throws XWikiException if a problem occurs while searching the storage
     */
    private DocumentReference findWikiServer(String host, XWikiContext context) throws XWikiException
    {
        this.ensureVirtualWikiMapExists();
        DocumentReference wikiName = this.virtualWikiMap.get(host);

        if (wikiName == null) {
            // Not loaded yet, search for it in the main wiki
            String hql =
                ", BaseObject as obj, StringProperty as prop WHERE obj.name=doc.fullName"
                    + " AND doc.space='XWiki' AND doc.name LIKE 'XWikiServer%'"
                    + " AND obj.className='XWiki.XWikiServerClass' AND prop.id.id = obj.id"
                    + " AND prop.id.name = 'server' AND prop.value=?";
            List<String> parameters = new ArrayList<String>(1);
            parameters.add(host);
            try {
                List<DocumentReference> list =
                    context.getWiki().getStore().searchDocumentReferences(hql, parameters, context);
                if ((list != null) && (list.size() > 0)) {
                    wikiName = list.get(0);
                }

                this.virtualWikiMap.set(host, wikiName);
            } catch (XWikiException e) {
                LOGGER.warn("Error when searching for wiki name from URL host [" + host + "]", e);
            }
        }

        return wikiName;
    }

    private void ensureVirtualWikiMapExists() throws XWikiException
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

                wikiOwner = doc.getStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "owner");
                if (wikiOwner.indexOf(':') == -1) {
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
                LOGGER.warn("Failed to retrieve XWiki's version from [" + VERSION_FILE + "], using the ["
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
            // Resources should always be encoded as UTF-8, to reduce the dependency on the system encoding
            return FileUtils.readFileToString(new File(name), DEFAULT_ENCODING);
        }

        return IOUtils.toString(is, DEFAULT_ENCODING);
    }

    public Date getResourceLastModificationDate(String name)
    {
        try {
            if (getEngineContext() != null) {
                return Util.getFileLastModificationDate(getEngineContext().getRealPath(name));
            }
        } catch (Exception ex) {
            // Probably a SecurityException or the file is not accessible (inside a war)
            LOGGER.info("Failed to get file modification date: " + ex.getMessage());
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
            return FileUtils.readFileToByteArray(new File(name));
        }

        return IOUtils.toByteArray(is);
    }

    public boolean resourceExists(String name)
    {
        if (getEngineContext() != null) {
            try {
                if (getResource(name) != null) {
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
        return Param(key, null);
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
        if (getConfig() != null) {
            return getConfig().getProperty(key, default_value);
        }
        return default_value;
    }

    public long ParamAsLong(String key)
    {
        String param = Param(key);
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
            server = doc.getDocumentReference().getWikiReference().getName();

            if (server != null) {
                database = context.getDatabase();
                context.setDatabase(server);
            }

            // Setting comment & minor edit before saving
            doc.setComment(StringUtils.defaultString(comment));
            doc.setMinorEdit(isMinorEdit);

            // We need to save the original document since saveXWikiDoc() will reset it and we
            // need that original document for the notification below.
            XWikiDocument originalDocument = doc.getOriginalDocument();
            // Always use an originalDocument, to provide a consistent behavior. The cases where
            // originalDocument is null are rare (specifically when the XWikiDocument object is
            // manually constructed, and not obtained using the API).
            if (originalDocument == null) {
                originalDocument = new XWikiDocument(doc.getDocumentReference());
            }

            ObservationManager om = Utils.getComponent(ObservationManager.class);

            // Notify listeners about the document about to be created or updated

            // Note that for the moment the event being send is a bridge event, as we are still passing around
            // an XWikiDocument as source and an XWikiContext as data.

            if (om != null) {
                if (originalDocument.isNew()) {
                    om.notify(new DocumentCreatingEvent(doc.getDocumentReference()), doc, context);
                } else {
                    om.notify(new DocumentUpdatingEvent(doc.getDocumentReference()), doc, context);
                }
            }

            getStore().saveXWikiDoc(doc, context);

            // Since the store#saveXWikiDoc resets originalDocument, we need to temporarily put it
            // back to send notifications.
            XWikiDocument newOriginal = doc.getOriginalDocument();

            try {
                doc.setOriginalDocument(originalDocument);

                // Notify listeners about the document having been created or updated

                // First the legacy notification mechanism

                // Then the new observation module
                // Note that for the moment the event being send is a bridge event, as we are still passing around
                // an XWikiDocument as source and an XWikiContext as data.
                // The old version is made available using doc.getOriginalDocument()

                if (om != null) {
                    if (originalDocument.isNew()) {
                        om.notify(new DocumentCreatedEvent(doc.getDocumentReference()), doc, context);
                    } else {
                        om.notify(new DocumentUpdatedEvent(doc.getDocumentReference()), doc, context);
                    }
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to send document save notification for document ["
                    + this.defaultEntityReferenceSerializer.serialize(doc.getDocumentReference()) + "]", ex);
            } finally {
                doc.setOriginalDocument(newOriginal);
            }
        } finally {
            if ((server != null) && (database != null)) {
                context.setDatabase(database);
            }
        }
    }

    public XWikiDocument getDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        try {
            if (doc.getDocumentReference().getWikiReference().getName() != null) {
                context.setDatabase(doc.getDocumentReference().getWikiReference().getName());
            }

            return getStore().loadXWikiDoc(doc, context);
        } finally {
            context.setDatabase(database);
        }
    }

    public XWikiDocument getDocument(XWikiDocument doc, String revision, XWikiContext context) throws XWikiException
    {
        XWikiDocument newdoc;

        String database = context.getDatabase();
        try {
            if (doc.getDocumentReference().getWikiReference().getName() != null) {
                context.setDatabase(doc.getDocumentReference().getWikiReference().getName());
            }

            if ((revision == null) || revision.equals("")) {
                newdoc = new XWikiDocument(doc.getDocumentReference());
            } else if (revision.equals(doc.getVersion())) {
                newdoc = doc;
            } else {
                newdoc = getVersioningStore().loadXWikiDoc(doc, revision, context);
            }
        } catch (XWikiException e) {
            if (revision.equals("1.1") || revision.equals("1.0")) {
                newdoc = new XWikiDocument(doc.getDocumentReference());
            } else {
                throw e;
            }
        } finally {
            context.setDatabase(database);
        }

        return newdoc;
    }

    /**
     * @since 2.2M1
     */
    public XWikiDocument getDocument(DocumentReference reference, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(reference);
        doc.setContentDirty(true);
        return getDocument(doc, context);
    }

    /**
     * @deprecated since 2.2M1 use {@link #getDocument(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument getDocument(String fullname, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument();
        doc.setFullName(fullname, context);
        return getDocument(doc, context);
    }

    /**
     * @deprecated since 2.2M1 use {@link #getDocument(DocumentReference, XWikiContext)} instead
     */
    @Deprecated
    public XWikiDocument getDocument(String space, String fullname, XWikiContext context) throws XWikiException
    {
        int dotPosition = fullname.lastIndexOf('.');
        if (dotPosition != -1) {
            String spaceFromFullname = fullname.substring(0, dotPosition);
            String name = fullname.substring(dotPosition + 1);
            if (name.equals("")) {
                name = getDefaultPage(context);
            }
            return getDocument(spaceFromFullname + "." + name, context);
        } else {
            return getDocument(space + "." + fullname, context);
        }
    }

    public XWikiDocument getDocumentFromPath(String path, XWikiContext context) throws XWikiException
    {
        return getDocument(getDocumentReferenceFromPath(path, context), context);
    }

    /**
     * @since 2.3M1
     */
    public DocumentReference getDocumentReferenceFromPath(String path, XWikiContext context)
    {
        // TODO: Remove this and use XWikiURLFactory instead in XWikiAction and all entry points.
        List<String> segments = new ArrayList<String>();
        for (String segment : path.split("/", -1)) {
            segments.add(Util.decodeURI(segment, context));
        }
        // Remove the first segment if it's empty to cater for cases when the path starts with "/"
        if (segments.size() > 0 && segments.get(0).length() == 0) {
            segments.remove(0);
        }

        XWikiEntityURL entityURL =
            (XWikiEntityURL) this.entityXWikiURLBuilder.build(new WikiReference(context.getDatabase()), segments);

        return new DocumentReference(entityURL.getEntityReference().extractReference(EntityType.DOCUMENT));
    }

    /**
     * @deprecated since 2.3M1 use {@link #getDocumentReferenceFromPath(String, XWikiContext)} instead
     */
    @Deprecated
    public String getDocumentNameFromPath(String path, XWikiContext context)
    {
        return this.localStringEntityReferenceSerializer.serialize(getDocumentReferenceFromPath(path, context));
    }

    /**
     * @see com.xpn.xwiki.api.XWiki#getDeletedDocuments(String, String)
     */
    public XWikiDeletedDocument[] getDeletedDocuments(String fullname, String lang, XWikiContext context)
        throws XWikiException
    {
        if (hasRecycleBin(context)) {
            XWikiDocument doc = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(fullname));
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
            XWikiDocument doc = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(fullname));
            doc.setLanguage(lang);
            return getRecycleBinStore().getDeletedDocument(doc, index, context, true);
        } else {
            return null;
        }
    }

    /**
     * Retrieve all the deleted attachments that belonged to a certain document. Note that this does not distinguish
     * between different incarnations of a document name, and it does not require that the document still exists, it
     * returns all the attachments that at the time of their deletion had a document with the specified name as their
     * owner.
     * 
     * @param docName the {@link XWikiDocument#getFullName() name} of the owner document
     * @param context the current request context
     * @return A list with all the deleted attachments which belonged to the specified document. If no such attachments
     *         are found in the trash, an empty list is returned.
     * @throws XWikiException if an error occurs while loading the attachments
     */
    public List<DeletedAttachment> getDeletedAttachments(String docName, XWikiContext context) throws XWikiException
    {
        if (hasAttachmentRecycleBin(context)) {
            XWikiDocument doc = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(docName));
            return getAttachmentRecycleBinStore().getAllDeletedAttachments(doc, context, true);
        }
        return null;
    }

    /**
     * Retrieve all the deleted attachments that belonged to a certain document and had the specified name. Multiple
     * versions can be returned since the same file can be uploaded and deleted several times, creating different
     * instances in the trash. Note that this does not distinguish between different incarnations of a document name,
     * and it does not require that the document still exists, it returns all the attachments that at the time of their
     * deletion had a document with the specified name as their owner.
     * 
     * @param docName the {@link DeletedAttachment#getDocName() name of the document} the attachment belonged to
     * @param filename the {@link DeletedAttachment#getFilename() name} of the attachment to search for
     * @param context the current request context
     * @return A list with all the deleted attachments which belonged to the specified document and had the specified
     *         filename. If no such attachments are found in the trash, an empty list is returned.
     * @throws XWikiException if an error occurs while loading the attachments
     */
    public List<DeletedAttachment> getDeletedAttachments(String docName, String filename, XWikiContext context)
        throws XWikiException
    {
        if (hasAttachmentRecycleBin(context)) {
            XWikiDocument doc = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(docName));
            XWikiAttachment attachment = new XWikiAttachment(doc, filename);
            return getAttachmentRecycleBinStore().getAllDeletedAttachments(attachment, context, true);
        }
        return null;
    }

    /**
     * Retrieve a specific attachment from the trash.
     * 
     * @param id the unique identifier of the entry in the trash
     * @return specified attachment from the trash, {@code null} if not found
     * @throws XWikiException if an error occurs while loading the attachments
     */
    public DeletedAttachment getDeletedAttachment(String id, XWikiContext context) throws XWikiException
    {
        if (hasAttachmentRecycleBin(context)) {
            return getAttachmentRecycleBinStore().getDeletedAttachment(NumberUtils.toLong(id), context, true);
        }
        return null;
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

    public List<String> getClassList(XWikiContext context) throws XWikiException
    {
        List<String> result = getStore().getClassList(context);
        Collections.sort(result);
        return result;
    }

    /*
     * public String[] getClassList() throws XWikiException { List list = store.getClassList(); String[] array = new
     * String[list.size()]; for (int i=0;i<list.size();i++) array[i] = (String)list.get(i); return array; }
     */

    public <T> List<T> search(String sql, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, 0, 0, context);
    }

    public <T> List<T> search(String sql, int nb, int start, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, nb, start, context);
    }

    public <T> List<T> search(String sql, Object[][] whereParams, XWikiContext context) throws XWikiException
    {
        return getStore().search(sql, 0, 0, whereParams, context);
    }

    public <T> List<T> search(String sql, int nb, int start, Object[][] whereParams, XWikiContext context)
        throws XWikiException
    {
        return getStore().search(sql, nb, start, whereParams, context);
    }

    /**
     * Checks if the wiki is running in test mode.
     * 
     * @return {@code true} if the wiki is running Cactus tests, {@code false} otherwise
     * @deprecated No longer used.
     */
    @Deprecated
    public boolean isTest()
    {
        return this.test;
    }

    /**
     * Marks that the wiki is running in test mode.
     * 
     * @param test whether tests are being executed
     * @deprecated No longer used.
     */
    @Deprecated
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

    /**
     * @deprecated use {@link #evaluateTemplate(String, XWikiContext)} instead
     */
    @Deprecated
    public String parseTemplate(String template, XWikiContext context)
    {
        String result = "";

        try {
            result = evaluateTemplate(template, context);
        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception while parsing template [" + template + "] from /templates/", e);
            }
        }

        return result;
    }

    /**
     * Evaluate provided template content using velocity engine.
     * 
     * @param template the template to evaluate
     * @param context the XWiki context
     * @return the return of the velocity script
     * @throws IOException failed to get the template content
     * @since 2.2.2
     */
    public String evaluateTemplate(String template, XWikiContext context) throws IOException
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
        } catch (Exception ex) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception while parsing template [" + template + "] from skin", ex);
            }
        }

        // Prevent inclusion of templates from other directories
        template = URI.create("/templates/" + template).normalize().toString();
        if (!template.startsWith("/templates/")) {
            LOGGER.warn("Illegal access, tried to use file [" + template
                + "] as a template. Possible break-in attempt!");
            return "";
        }

        String content = getResourceContent(template);
        return XWikiVelocityRenderer.evaluate(content, template, (VelocityContext) context.get("vcontext"), context);
    }

    public String parseTemplate(String template, String skin, XWikiContext context)
    {
        try {
            XWikiDocument doc = getDocument(skin, context);
            if (!doc.isNew()) {
                // Try parsing the object property
                BaseObject object =
                    doc.getXObject(new DocumentReference(doc.getDocumentReference().getWikiReference().getName(),
                        SYSTEM_SPACE, "XWikiSkins"));
                if (object != null) {
                    String content = object.getStringValue(template);
                    if (StringUtils.isNotBlank(content)) {
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
                    // It's impossible to know the real attachemtn encoding, but let's assume that they respect the
                    // standard and use UTF-8 (which is required for the files located on the filesystem)
                    String content = IOUtils.toString(attachment.getContentInputStream(context), DEFAULT_ENCODING);
                    if (StringUtils.isNotBlank(content)) {
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
                LOGGER.warn("Illegal access, tried to use file [" + path + "] as a template."
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
            LOGGER.error("Failed to render template [" + template + "] for skin [" + skin + "]", ex);
            return parseTemplate(template, skin, context);
        }
    }

    public String renderTemplate(String template, XWikiContext context)
    {
        try {
            return getRenderingEngine().getRenderer("wiki").render(parseTemplate(template, context), context.getDoc(),
                context.getDoc(), context);
        } catch (Exception ex) {
            LOGGER.error("Failed to render template [" + template + "]", ex);
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
            LOGGER.warn("Exception including url: " + url, e);
            return "Exception including \"" + url + "\", see logs for details.";
        }

    }

    /**
     * @param iconName the standard name of an icon (it's not the name of the file on the filesystem, it's a generic
     *            name, for example "success" for a success icon
     * @return the URL to the icon resource
     * @since 2.6M1
     */
    public String getIconURL(String iconName, XWikiContext context)
    {
        // TODO: Do a better mapping between generic icon name and physical resource name, especially to be independent
        // of the underlying icon library. Right now we assume it's the Silk icon library.
        return getSkinFile("icons/silk/" + iconName + ".gif", context);
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception while getting skin file [" + filename + "]", e);
            }
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
                BaseObject object =
                    doc.getXObject(new DocumentReference(doc.getDocumentReference().getWikiReference().getName(),
                        SYSTEM_SPACE, "XWikiSkins"));
                if (object != null) {
                    String content = object.getStringValue(filename);
                    if (StringUtils.isNotBlank(content)) {
                        URL url =
                            urlf.createSkinURL(filename, doc.getSpace(), doc.getName(), doc.getDatabase(), context);
                        return urlf.getURL(url, context);
                    }
                }

                // Look for an attachment
                String shortName = StringUtils.replaceChars(filename, '/', '.');
                XWikiAttachment attachment = doc.getAttachment(shortName);
                if (attachment != null) {
                    return doc.getAttachmentURL(shortName, "skin", context);
                }
            }

            // Look for a skin file
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

            // Look for a resource file
            path = "/resources/" + filename;
            if (resourceExists(path)) {
                URL url;
                url = urlf.createResourceURL(filename, forceSkinAction, context);
                return urlf.getURL(url, context);
            }

        } catch (Exception e) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Exception while getting skin file [" + filename + "] from skin [" + skin + "]", e);
            }
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
                if (LOGGER.isDebugEnabled()) {
                    if (skin != null && !skin.equals("")) {
                        LOGGER.debug("Requested skin in the URL: [" + skin + "]");
                    }
                }
            }

            if ((skin == null) || (skin.equals(""))) {
                skin = getUserPreference("skin", context);
                if (LOGGER.isDebugEnabled()) {
                    if (skin != null && !skin.equals("")) {
                        LOGGER.debug("Configured skin in user preferences: [" + skin + "]");
                    }
                }
            }
            if (skin == null || skin.equals("")) {
                skin = Param("xwiki.defaultskin");
                if (LOGGER.isDebugEnabled()) {
                    if (skin != null && !skin.equals("")) {
                        LOGGER.debug("Configured default skin in preferences: [" + skin + "]");
                    }
                }
            }
            if (skin == null || skin.equals("")) {
                skin = getDefaultBaseSkin(context);
                if (LOGGER.isDebugEnabled()) {
                    if (skin != null && !skin.equals("")) {
                        LOGGER.debug("Configured default base skin in preferences: [" + skin + "]");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Exception while determining current skin", e);
            skin = getDefaultBaseSkin(context);
        }
        try {
            if (skin.indexOf('.') != -1) {
                if (!getRightService().hasAccessLevel("view", context.getUser(), skin, context)) {
                    LOGGER.debug("Cannot access configured skin due to access rights, using the default skin.");
                    skin = Param("xwiki.defaultskin", getDefaultBaseSkin(context));
                }
            }
        } catch (XWikiException e) {
            // if it fails here, let's just ignore it
            LOGGER.debug("Exception while determining current skin", e);
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
            LOGGER.warn("", ex);
        }
        return default_value;
    }

    public String getDefaultBaseSkin(XWikiContext context)
    {
        String defaultbaseskin = Param("xwiki.defaultbaseskin", "");
        if (defaultbaseskin.equals("")) {
            defaultbaseskin = Param("xwiki.defaultskin", "colibri");
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
                baseskin = getBaseSkin(skin, context);
            }
            if (baseskin.equals("")) {
                baseskin = getDefaultBaseSkin(context);
            }
        } catch (Exception e) {
            baseskin = getDefaultBaseSkin(context);
            LOGGER.debug("Exception while determining base skin", e);
        }
        context.put("baseskin", baseskin);
        return baseskin;
    }

    /**
     * @param skin the full name of the skin document for which to return the base skin. For example :
     *            <tt>XWiki.DefaultSkin</tt>
     * @param context the XWiki context
     * @return if found, the name of the base skin the asked skin inherits from. If not found, returns an empty string.
     * @since 2.0.2
     * @since 2.1M1
     */
    public String getBaseSkin(String skin, XWikiContext context)
    {
        if (context.getWiki().exists(skin, context)) {
            try {
                return getDocument(skin, context).getStringValue("XWiki.XWikiSkins", "baseskin");
            } catch (XWikiException e) {
                // Do nothing and let return the empty string.
            }
        }
        return "";
    }

    public String getSpaceCopyright(XWikiContext context)
    {
        String defaultValue = "Copyright 2004-" + Calendar.getInstance().get(Calendar.YEAR) + " XWiki";
        return getSpacePreference("webcopyright", defaultValue, context);
    }

    public String getXWikiPreference(String prefname, XWikiContext context)
    {
        return getXWikiPreference(prefname, "", context);
    }

    /**
     * Obtain a preference value for the wiki, looking up first in the XWiki.XWikiPreferences document, then fallbacking
     * on a config parameter when the first lookup gives an empty string, then returning the default value if the config
     * parameter returned itself an empty string.
     * 
     * @param prefname the parameter to look for in the XWiki.XWikiPreferences object corresponding to the context's
     *            language in the XWiki.XWikiPreferences document of the wiki (or the first XWiki.XWikiPreferences
     *            object contained, if the one for the context'ds language could not be found).
     * @param fallback_param the parameter in xwiki.cfg to fallback on, in case the XWiki.XWikiPreferences object gave
     *            no result
     * @param default_value the default value to fallback on, in case both XWiki.XWikiPreferences and the fallback
     *            xwiki.cfg parameter gave no result
     */
    public String getXWikiPreference(String prefname, String fallback_param, String default_value, XWikiContext context)
    {
        try {
            DocumentReference xwikiPreferencesReference =
                new DocumentReference("XWikiPreferences", new SpaceReference(SYSTEM_SPACE, new WikiReference(
                    context.getDatabase())));
            XWikiDocument doc = getDocument(xwikiPreferencesReference, context);
            // First we try to get a translated preference object
            BaseObject object =
                doc.getXObject(xwikiPreferencesReference, "default_language", context.getLanguage(), true);
            String result = "";

            if (object != null) {
                try {
                    result = object.getStringValue(prefname);
                } catch (Exception e) {
                    LOGGER.warn("Exception while getting wiki preference [" + prefname + "]", e);
                }
            }
            // If empty we take it from the default pref object
            if (result.equals("")) {
                object = doc.getXObject();
                if (object != null) {
                    result = object.getStringValue(prefname);
                }
            }

            if (!result.equals("")) {
                return result;
            }
        } catch (Exception e) {
            LOGGER.warn("Exception while getting wiki preference [" + prefname + "]", e);
        }
        return Param(fallback_param, default_value);
    }

    public String getXWikiPreference(String prefname, String default_value, XWikiContext context)
    {
        return getXWikiPreference(prefname, "", default_value, context);
    }

    public String getSpacePreference(String preference, XWikiContext context)
    {
        return getSpacePreference(preference, "", context);
    }

    public String getSpacePreference(String preference, String defaultValue, XWikiContext context)
    {
        XWikiDocument currentdoc = (XWikiDocument) context.get("doc");
        return getSpacePreference(preference, (currentdoc == null) ? null : currentdoc.getSpace(), defaultValue,
            context);
    }

    public String getSpacePreference(String preference, String space, String defaultValue, XWikiContext context)
    {
        // If there's no space defined then don't return space preferences (since it'll usually mean that the current
        // doc is not set).
        if (space != null) {
            try {
                XWikiDocument doc = getDocument(space + ".WebPreferences", context);

                // First we try to get a translated preference object
                DocumentReference xwikiPreferencesReference =
                    new DocumentReference("XWikiPreferences", new SpaceReference(SYSTEM_SPACE, new WikiReference(
                        context.getDatabase())));
                BaseObject object =
                    doc.getXObject(xwikiPreferencesReference, "default_language", context.getLanguage(), true);
                String result = "";
                if (object != null) {
                    try {
                        result = object.getStringValue(preference);
                    } catch (Exception e) {
                        LOGGER.warn("Exception while getting space preference [" + preference + "]", e);
                    }
                }

                if (!result.equals("")) {
                    return result;
                }
            } catch (Exception e) {
                LOGGER.warn("Exception while getting space preference [" + preference + "]", e);
            }
        }
        return getXWikiPreference(preference, defaultValue, context);
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
            LOGGER.warn("Exception while getting user preference [" + prefname + "]", e);
        }

        return getSpacePreference(prefname, context);
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
            String result = Util.normalizeLanguage(getUserPreferenceFromCookie(prefname, context));
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
            language = Util.normalizeLanguage(context.getRequest().getParameter("language"));
            if ((language != null) && (!language.equals(""))) {
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
            language = Util.normalizeLanguage(getUserPreferenceFromCookie("language", context));
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
                language = Util.normalizeLanguage(userdoc.getStringValue("XWiki.XWikiUsers", "default_language"));
                if (!language.equals("")) {
                    context.setLanguage(language);
                    return language;
                }
            }
        } catch (XWikiException e) {
        }

        // If the default language is preferred, and since the user didn't explicitly ask for a
        // language already, then use the default wiki language.
        if (Param("xwiki.language.preferDefault", "0").equals("1")
            || getSpacePreference("preferDefaultLanguage", "0", context).equals("1")) {
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
                    List<String> available = Arrays.asList(getXWikiPreference("languages", context).split("[, |]"));
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
        if (StringUtils.isBlank(defaultLanguage)) {
            defaultLanguage = "en";
        }
        return Util.normalizeLanguage(defaultLanguage);
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
            requestLanguage = Util.normalizeLanguage(context.getRequest().getParameter("language"));
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
            cookieLanguage = Util.normalizeLanguage(getUserPreferenceFromCookie("language", context));
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
            language = Util.normalizeLanguage(context.getWiki().getXWikiPreference("default_language", "", context));
            context.setInterfaceLanguage(language);
            return language;
        }

        // Get request language
        try {
            requestLanguage = Util.normalizeLanguage(context.getRequest().getParameter("interfacelanguage"));
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
            cookieLanguage = Util.normalizeLanguage(getUserPreferenceFromCookie("interfacelanguage", context));
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

    public long getXWikiPreferenceAsLong(String preference, XWikiContext context)
    {
        return Long.parseLong(getXWikiPreference(preference, context));
    }

    public long getSpacePreferenceAsLong(String preference, XWikiContext context)
    {
        return Long.parseLong(getSpacePreference(preference, context));
    }

    public long getXWikiPreferenceAsLong(String preference, long defaultValue, XWikiContext context)
    {
        return NumberUtils.toLong((getXWikiPreference(preference, context)), defaultValue);
    }

    public long getXWikiPreferenceAsLong(String preference, String fallbackParameter, long defaultValue,
        XWikiContext context)
    {
        return NumberUtils.toLong(getXWikiPreference(preference, fallbackParameter, "", context), defaultValue);
    }

    public long getSpacePreferenceAsLong(String preference, long defaultValue, XWikiContext context)
    {
        return NumberUtils.toLong(getSpacePreference(preference, context), defaultValue);
    }

    public long getUserPreferenceAsLong(String preference, XWikiContext context)
    {
        return Long.parseLong(getUserPreference(preference, context));
    }

    public int getXWikiPreferenceAsInt(String preference, XWikiContext context)
    {
        return Integer.parseInt(getXWikiPreference(preference, context));
    }

    public int getSpacePreferenceAsInt(String preference, XWikiContext context)
    {
        return Integer.parseInt(getSpacePreference(preference, context));
    }

    public int getXWikiPreferenceAsInt(String preference, int defaultValue, XWikiContext context)
    {
        return NumberUtils.toInt(getXWikiPreference(preference, context), defaultValue);
    }

    public int getXWikiPreferenceAsInt(String preference, String fallbackParameter, int defaultValue,
        XWikiContext context)
    {
        return NumberUtils.toInt(getXWikiPreference(preference, fallbackParameter, "", context), defaultValue);
    }

    public int getSpacePreferenceAsInt(String preference, int defaultValue, XWikiContext context)
    {
        return NumberUtils.toInt(getSpacePreference(preference, context), defaultValue);
    }

    public int getUserPreferenceAsInt(String prefname, XWikiContext context)
    {
        return Integer.parseInt(getUserPreference(prefname, context));
    }

    /**
     * Get XWiki context from execution context.
     * 
     * @return the XWiki context for the current thread
     */
    private XWikiContext getXWikiContext()
    {
        Execution execution = Utils.getComponent(Execution.class);

        ExecutionContext ec = execution.getContext();

        return ec != null ? (XWikiContext) ec.getProperty("xwikicontext") : null;
    }

    /**
     * @deprecated user {@link #flushCache(XWikiContext)} instead
     */
    @Deprecated
    public void flushCache()
    {
        flushCache(getXWikiContext());
    }

    public void flushCache(XWikiContext context)
    {
        // We need to flush the virtual wiki list
        this.virtualWikiList = new ArrayList<String>();
        // We need to flush the server Cache
        if (this.virtualWikiMap != null) {
            this.virtualWikiMap.dispose();
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

    private void flushVirtualWikis(XWikiDocument doc)
    {
        List<BaseObject> bobjects = doc.getXObjects(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
        if (bobjects != null) {
            for (BaseObject bobj : bobjects) {
                if (bobj != null) {
                    String host = bobj.getStringValue("server");
                    if (StringUtils.isNotEmpty(host)) {
                        if (this.virtualWikiMap != null) {
                            if (this.virtualWikiMap.get(host) != null) {
                                this.virtualWikiMap.remove(host);
                            }
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

        doc = getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "TagClass"), context);

        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        needsUpdate |=
            bclass.addStaticListField(XWikiConstant.TAG_CLASS_PROP_TAGS, "Tags", 30, true, true, "", "input", "|,");
        StaticListClass tagClass = (StaticListClass) bclass.get(XWikiConstant.TAG_CLASS_PROP_TAGS);
        if (tagClass.isRelationalStorage() == false) {
            tagClass.setRelationalStorage(true);
            needsUpdate = true;
        }
        needsUpdate |= setClassDocumentFields(doc, "XWiki Tag Class");

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    /**
     * Verify if the <code>XWiki.SheetClass</code> page exists and that it contains all the required configuration
     * properties to make the sheet feature work properly. If some properties are missing they are created and saved in
     * the database. SheetClass is used to a page as a sheet. When a page is tagged as a sheet and that page is included
     * in another page using the include macro then editing it triggers automatic inline edition (for XWiki Syntax 2.0
     * only - for XWiki Syntax 1.0 automatic inline edition is triggered using #includeForm).
     * 
     * @param context the XWiki Context
     * @return the SheetClass Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the database
     * @deprecated since 3.1M2 edit mode class should be used for this purpose, not the sheet class
     * @see #getEditModeClass(XWikiContext)
     */
    public BaseClass getSheetClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc =
            getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "SheetClass"), context);
        boolean needsUpdate = doc.isNew();

        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        // Note: Ideally we don't want a special field in the sheet class but XWiki classes must have at
        // least one field or they're not saved. Thus we are introducing a "defaultEditMode" which will
        // tell what edit mode to use. If empty it'll default to "inline".
        needsUpdate |= bclass.addTextField("defaultEditMode", "Default Edit Mode", 15);

        if (doc.isNew()) {
            needsUpdate |= setClassDocumentFields(doc, "XWiki Sheet Class");
            doc.setContent(doc.getContent() + "\n\nClass that should be used to recognize sheet pages.");
        }

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    /**
     * Verify if the {@code XWiki.EditModeClass} page exists and that it contains all the required configuration
     * properties to make the edit mode feature work properly. If some properties are missing they are created and saved
     * in the database. EditModeClass is used to specify the default edit mode of a page. It can also be used to mark a
     * page as a sheet. When a page is marked as a sheet and that page is included in another page using the include
     * macro then editing it triggers automatic inline edition (for XWiki Syntax 2.0 only - for XWiki Syntax 1.0
     * automatic inline edition is triggered using #includeForm). It replaces and enhances the SheetClass mechanism (see
     * {@link #getSheetClass(XWikiContext)}).
     * 
     * @param context the XWiki Context
     * @return the EditModeClass Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the database
     * @since 3.1M2
     */
    public BaseClass getEditModeClass(XWikiContext context) throws XWikiException
    {
        DocumentReference classReference =
            new DocumentReference(context.getDatabase(), XWikiConstant.EDIT_MODE_CLASS.getParent().getName(),
                XWikiConstant.EDIT_MODE_CLASS.getName());
        XWikiDocument doc = getDocument(classReference, context);

        boolean needsUpdate = doc.isNew();

        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        needsUpdate |= bclass.addTextField("defaultEditMode", "Default Edit Mode", 15);

        if (doc.isNew()) {
            needsUpdate |= setClassDocumentFields(doc, "XWiki Edit Mode Class");
            doc.setContent("Class that should be used to specify the edit mode of a page.");
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

        doc = getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "XWikiUsers"), context);

        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

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
        needsUpdate |= bclass.addStaticListField("usertype", "User type", "Simple|Advanced");
        needsUpdate |= bclass.addBooleanField("accessibility", "Enable extra accessibility features", "yesno");

        // New fields for the XWiki 1.0 skin
        needsUpdate |= bclass.addTextField("skin", "skin", 30);
        needsUpdate |= bclass.addTextField("avatar", "Avatar", 30);
        needsUpdate |= bclass.addTextField("phone", "Phone", 30);
        needsUpdate |= bclass.addTextAreaField("address", "Address", 40, 3);
        needsUpdate |= setClassDocumentFields(doc, "XWiki User Class");

        // Use XWikiUserSheet to display documents having XWikiUsers objects if no other class sheet is specified.
        SheetBinder classSheetBinder = Utils.getComponent(SheetBinder.class, "class");
        if (classSheetBinder.getSheets(doc).isEmpty()) {
            DocumentReference sheet = new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "XWikiUserSheet");
            needsUpdate |= classSheetBinder.bind(doc, sheet);
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

        doc = getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "GlobalRedirect"), context);

        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        needsUpdate |= bclass.addTextField("pattern", "Pattern", 30);
        needsUpdate |= bclass.addTextField("destination", "Destination", 30);
        needsUpdate |= setClassDocumentFields(doc, "XWiki Global Redirect Class");

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    /**
     * Verify if the <code>XWiki.XWikiPreferences</code> page exists and that it contains all the required configuration
     * properties to make XWiki work properly. If some properties are missing they are created and saved in the
     * database.
     * 
     * @param context the XWiki Context
     * @return the XWiki Base Class object containing the properties
     * @throws XWikiException if an error happens during the save to the datavase
     */
    public BaseClass getPrefsClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "XWikiPreferences"), context);

        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

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
        needsUpdate |=
            bclass.addDBListField("colorTheme", "Color theme",
                "select doc.fullName, doc.title from XWikiDocument as doc, BaseObject as theme "
                    + "where doc.fullName=theme.name and theme.className='ColorThemes.ColorThemeClass' "
                    + "and doc.fullName<>'ColorThemes.ColorThemeTemplate'");
        // This one should not be in the prefs
        PropertyInterface baseskinProp = bclass.get("baseskin");
        if (baseskinProp != null) {
            bclass.removeField("baseskin");
            needsUpdate = true;
        }
        needsUpdate |= bclass.addTextField("stylesheet", "Default Stylesheet", 30);
        needsUpdate |= bclass.addTextField("stylesheets", "Alternative Stylesheet", 60);
        needsUpdate |= bclass.addBooleanField("accessibility", "Enable extra accessibility features", "yesno");

        needsUpdate |= bclass.addStaticListField("editor", "Default Editor", "---|Text|Wysiwyg");

        needsUpdate |= bclass.addTextField("webcopyright", "Copyright", 30);
        needsUpdate |= bclass.addTextField("title", "Title", 30);
        needsUpdate |= bclass.addTextField("version", "Version", 30);
        needsUpdate |= bclass.addTextAreaField("meta", "HTTP Meta Info", 60, 8);
        needsUpdate |= bclass.addTextField("dateformat", "Date Format", 30);

        // mail
        needsUpdate |= bclass.addBooleanField("use_email_verification", "Use eMail Verification", "yesno");
        needsUpdate |= bclass.addTextField("admin_email", "Admin eMail", 30);
        needsUpdate |= bclass.addTextField("smtp_server", "SMTP Server", 30);
        needsUpdate |= bclass.addTextField("smtp_port", "SMTP Port", 5);
        needsUpdate |= bclass.addTextField("smtp_server_username", "Server username (optional)", 30);
        needsUpdate |= bclass.addTextField("smtp_server_password", "Server password (optional)", 30);
        needsUpdate |= bclass.addTextAreaField("javamail_extra_props", "Additional JavaMail properties", 60, 6);
        needsUpdate |= bclass.addTextAreaField("validation_email_content", "Validation eMail Content", 72, 10);
        needsUpdate |= bclass.addTextAreaField("confirmation_email_content", "Confirmation eMail Content", 72, 10);
        needsUpdate |= bclass.addTextAreaField("invitation_email_content", "Invitation eMail Content", 72, 10);

        needsUpdate |= bclass.addStaticListField("registration_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("registration_registered", "Registered", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("edit_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("edit_registered", "Registered", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("comment_anonymous", "Anonymous", "---|Image|Text");
        needsUpdate |= bclass.addStaticListField("comment_registered", "Registered", "---|Image|Text");

        needsUpdate |= bclass.addNumberField("upload_maxsize", "Maximum Upload Size", 5, "long");

        // Captcha for guest comments
        needsUpdate |=
            bclass.addBooleanField("guest_comment_requires_captcha",
                "Enable CAPTCHA in Comments for Unregistered Users", "select");

        // Document editing
        needsUpdate |= bclass.addTextField("core.defaultDocumentSyntax", "Default document syntax", 60);
        needsUpdate |= bclass.addBooleanField("xwiki.title.mandatory", "Make document title field mandatory", "yesno");

        // for tags
        needsUpdate |= bclass.addBooleanField("tags", "Activate the tagging", "yesno");

        // for backlinks
        needsUpdate |= bclass.addBooleanField("backlinks", "Activate the backlinks", "yesno");

        // New fields for the XWiki 1.0 skin
        needsUpdate |= bclass.addTextField("leftPanels", "Panels displayed on the left", 60);
        needsUpdate |= bclass.addTextField("rightPanels", "Panels displayed on the right", 60);
        needsUpdate |= bclass.addBooleanField("showLeftPanels", "Display the left panel column", "yesno");
        needsUpdate |= bclass.addBooleanField("showRightPanels", "Display the right panel column", "yesno");
        needsUpdate |= bclass.addTextField("languages", "Supported languages", 30);
        needsUpdate |= bclass.addTextField("documentBundles", "Internationalization Document Bundles", 60);

        // Only used by LDAP authentication service

        needsUpdate |= bclass.addBooleanField("ldap", "Ldap", "yesno");
        needsUpdate |= bclass.addTextField("ldap_server", "Ldap server adress", 60);
        needsUpdate |= bclass.addTextField("ldap_port", "Ldap server port", 60);
        needsUpdate |= bclass.addTextField("ldap_bind_DN", "Ldap login matching", 60);
        needsUpdate |= bclass.addTextField("ldap_bind_pass", "Ldap password matching", 60);
        needsUpdate |= bclass.addBooleanField("ldap_validate_password", "Validate Ldap user/password", "yesno");
        needsUpdate |= bclass.addTextField("ldap_user_group", "Ldap group filter", 60);
        needsUpdate |= bclass.addTextField("ldap_exclude_group", "Ldap group to exclude", 60);
        needsUpdate |= bclass.addTextField("ldap_base_DN", "Ldap base DN", 60);
        needsUpdate |= bclass.addTextField("ldap_UID_attr", "Ldap UID attribute name", 60);
        needsUpdate |= bclass.addTextField("ldap_fields_mapping", "Ldap user fiels mapping", 60);
        needsUpdate |= bclass.addBooleanField("ldap_update_user", "Update user from LDAP", "yesno");
        needsUpdate |= bclass.addTextAreaField("ldap_group_mapping", "Ldap groups mapping", 60, 5);
        needsUpdate |= bclass.addTextField("ldap_groupcache_expiration", "LDAP groups members cache", 60);
        needsUpdate |= bclass.addStaticListField("ldap_mode_group_sync", "LDAP groups sync mode", "|always|create");
        needsUpdate |= bclass.addBooleanField("ldap_trylocal", "Try local login", "yesno");

        if (((BooleanClass) bclass.get("showLeftPanels")).getDisplayType().equals("checkbox")) {
            ((BooleanClass) bclass.get("showLeftPanels")).setDisplayType("yesno");
            ((BooleanClass) bclass.get("showRightPanels")).setDisplayType("yesno");
            needsUpdate = true;
        }

        SheetBinder documentSheetBinder = Utils.getComponent(SheetBinder.class, "document");
        boolean withoutDocumentSheets = documentSheetBinder.getSheets(doc).isEmpty();
        if (withoutDocumentSheets) {
            // Bind a document sheet to prevent the default class sheet from being used.
            documentSheetBinder.bind(doc, doc.getDocumentReference());
        }
        needsUpdate |= setClassDocumentFields(doc, "XWiki Preferences");
        if (withoutDocumentSheets) {
            // Unbind the document sheet we bound earlier.
            documentSheetBinder.unbind(doc, doc.getDocumentReference());
        }

        // Use AdminSheet to display documents having XWikiPreferences objects if no other class sheet is specified.
        SheetBinder classSheetBinder = Utils.getComponent(SheetBinder.class, "class");
        if (classSheetBinder.getSheets(doc).isEmpty()) {
            DocumentReference sheet = new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "AdminSheet");
            needsUpdate |= classSheetBinder.bind(doc, sheet);
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

        doc = getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "XWikiGroups"), context);
        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        needsUpdate |= bclass.addTextField("member", "Member", 30);
        needsUpdate |= setClassDocumentFields(doc, "XWiki Group Class");

        // Use XWikiGroupSheet to display documents having XWikiGroups objects if no other class sheet is specified.
        SheetBinder classSheetBinder = Utils.getComponent(SheetBinder.class, "class");
        if (classSheetBinder.getSheets(doc).isEmpty()) {
            DocumentReference sheet = new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "XWikiGroupSheet");
            needsUpdate |= classSheetBinder.bind(doc, sheet);
        }

        if (needsUpdate) {
            saveDocument(doc, context);
        }

        return bclass;
    }

    public BaseClass getRightsClass(String pagename, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, pagename), context);
        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

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

        String title;
        if (pagename.equals("XWikiGlobalRights")) {
            title = "XWiki Global Rights Class";
        } else {
            title = "XWiki Rights Class";
        }

        needsUpdate |= setClassDocumentFields(doc, title);

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

        doc = getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "XWikiComments"), context);

        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        needsUpdate |= bclass.addTextField("author", "Author", 30);
        needsUpdate |= bclass.addTextAreaField("highlight", "Highlighted Text", 40, 2);
        needsUpdate |= bclass.addNumberField("replyto", "Reply To", 5, "integer");
        needsUpdate |= bclass.addDateField("date", "Date");
        needsUpdate |= bclass.addTextAreaField("comment", "Comment", 40, 5);
        needsUpdate |= setClassDocumentFields(doc, "XWiki Comment Class");

        if (needsUpdate) {
            saveDocument(doc, context);
        }
        return bclass;
    }

    public BaseClass getSkinClass(XWikiContext context) throws XWikiException
    {
        XWikiDocument doc;
        boolean needsUpdate = false;

        doc = getDocument(new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "XWikiSkins"), context);

        BaseClass bclass = doc.getXClass();
        if (context.get("initdone") != null) {
            return bclass;
        }

        needsUpdate |= bclass.addTextField("name", "Name", 30);
        needsUpdate |= bclass.addTextField("baseskin", "Base Skin", 30);
        needsUpdate |= bclass.addTextField("logo", "Logo", 30);
        needsUpdate |= bclass.addTemplateField("style.css", "Style");
        needsUpdate |= bclass.addTemplateField("header.vm", "Header");
        needsUpdate |= bclass.addTemplateField("footer.vm", "Footer");
        needsUpdate |= bclass.addTemplateField("viewheader.vm", "View Header");
        needsUpdate |= bclass.addTemplateField("view.vm", "View");
        needsUpdate |= bclass.addTemplateField("edit.vm", "Edit");
        needsUpdate |= setClassDocumentFields(doc, "XWiki Skin Class");

        // Use XWikiSkinsSheet to display documents having XWikiSkins objects if no other class sheet is specified.
        SheetBinder classSheetBinder = Utils.getComponent(SheetBinder.class, "class");
        if (classSheetBinder.getSheets(doc).isEmpty()) {
            DocumentReference sheet = new DocumentReference(context.getDatabase(), SYSTEM_SPACE, "XWikiSkinsSheet");
            needsUpdate |= classSheetBinder.bind(doc, sheet);
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
            // Get the user document
            String username = convertUsername(request.getParameter("xwikiname"), context);
            if (username.indexOf('.') == -1) {
                username = "XWiki." + username;
            }
            XWikiDocument userDocument = getDocument(username, context);

            // Get the stored validation key
            BaseObject userObject = userDocument.getObject("XWiki.XWikiUsers", 0);
            String storedKey = userObject.getStringValue("validkey");

            // Get the validation key from the URL
            String validationKey = request.getParameter("validkey");
            PropertyInterface validationKeyClass = getClass("XWiki.XWikiUsers", context).get("validkey");
            if (validationKeyClass instanceof PasswordClass) {
                validationKey = ((PasswordClass) validationKeyClass).getEquivalentPassword(storedKey, validationKey);
            }

            // Compare the two keys
            if ((!storedKey.equals("") && (storedKey.equals(validationKey)))) {
                userObject.setIntValue("active", 1);
                saveDocument(userDocument, context);

                if (withConfirmEmail) {
                    String email = userObject.getStringValue("email");
                    String password = userObject.getStringValue("password");
                    sendValidationEmail(username, password, email, request.getParameter("validkey"),
                        "confirmation_email_content", context);
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
            Map<String, String[]> map = Util.getObject(request, "register");

            String content;
            Syntax syntax;
            if (!getDefaultDocumentSyntax().equals(Syntax.XWIKI_1_0.toIdString())) {
                content = "{{include document=\"XWiki.XWikiUserSheet\"/}}";
                syntax = Syntax.XWIKI_2_0;
            } else {
                content = "#includeForm(\"XWiki.XWikiUserSheet\")";
                syntax = Syntax.XWIKI_1_0;
            }

            String xwikiname = request.getParameter("xwikiname");
            String password2 = request.getParameter("register2_password");
            String password = (map.get("password"))[0];
            String email = (map.get("email"))[0];
            String template = request.getParameter("template");
            String parent = request.getParameter("parent");
            String validkey = null;

            if (XWikiRightService.SUPERADMIN_USER.equalsIgnoreCase(xwikiname)) {
                return -8;
            }
            try {
                if (!context.getUtil().match(this.Param("xwiki.validusername", "/^[a-zA-Z0-9_]+$/"), xwikiname)) {
                    return -4;
                }
            } catch (RuntimeException ex) {
                LOGGER.warn("Invalid regular expression for xwiki.validusername", ex);
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
                    syntax = tdoc.getSyntax();
                }
            }

            if ((parent == null) || (parent.equals(""))) {
                parent = "XWiki.XWikiUsers";
            }

            if (withValidation) {
                map.put("active", new String[] {"0"});
                validkey = generateValidationKey(16);
                map.put("validkey", new String[] {validkey});

            } else {
                // Mark user active
                map.put("active", new String[] {"1"});
            }

            int result =
                createUser(xwikiname, map, this.relativeEntityReferenceResolver.resolve(parent, EntityType.DOCUMENT),
                    content, syntax, userRights, context);

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
        Map<String, String> map = new HashMap<String, String>();
        map.put("active", "1");
        map.put("first_name", xwikiname);

        if (createUser(xwikiname, map, userRights, context) == 1) {
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
    public void sendMessage(String sender, String[] recipients, String rawMessage, XWikiContext context)
        throws XWikiException
    {
        LOGGER.trace("Entering sendMessage()");

        // We'll be using the MailSender plugin, which has much more advanced capabilities (authentication, TLS).
        // Since the plugin is in another module, and it depends on the core, we have to use it through reflection in
        // order to avoid cyclic dependencies. This should be fixed once the mailsender becomes a clean component
        // instead of a plugin.
        Object mailSender;
        Class mailSenderClass;
        Method mailSenderSendRaw;

        try {
            mailSender = getPluginApi("mailsender", context);
            mailSenderClass = Class.forName("com.xpn.xwiki.plugin.mailsender.MailSenderPluginApi");

            // public int sendRawMessage(String from, String to, String rawMessage)
            mailSenderSendRaw =
                mailSenderClass.getMethod("sendRawMessage", new Class[] {String.class, String.class, String.class});
        } catch (Exception e) {
            LOGGER.error("Problem getting MailSender via Reflection. Using the old sendMessage mechanism.", e);
            sendMessageOld(sender, recipients, rawMessage, context);
            return;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending message = \"" + rawMessage + "\"");
        }

        String messageRecipients = StringUtils.join(recipients, ',');

        try {
            mailSenderSendRaw.invoke(mailSender, sender, messageRecipients, rawMessage);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof XWikiException) {
                throw (XWikiException) cause;
            } else {
                throw new RuntimeException(cause);
            }
        } catch (Exception e) {
            // Probably either IllegalAccessException or IllegalArgumentException
            // Shouldn't happen unless there were an incompatible code change
            throw new RuntimeException(e);
        }

        LOGGER.info("Exiting sendMessage(). It seems everything went ok.");
    }

    /**
     * @deprecated replaced by the <a href="http://code.xwiki.org/xwiki/bin/view/Plugins/MailSenderPlugin">Mail Sender
     *             Plugin</a>
     */
    @Deprecated
    private void sendMessageOld(String sender, String[] recipient, String message, XWikiContext context)
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
                Object[] args = {server, port, Integer.valueOf(reply), smtpc.getReplyString()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                    XWikiException.ERROR_XWIKI_EMAIL_CONNECT_FAILED,
                    "Could not connect to server {0} port {1} error code {2} ({3})", null, args);
            }

            if (smtpc.login(login) == false) {
                reply = smtpc.getReplyCode();
                Object[] args = {server, port, Integer.valueOf(reply), smtpc.getReplyString()};
                throw new XWikiException(XWikiException.MODULE_XWIKI_EMAIL,
                    XWikiException.ERROR_XWIKI_EMAIL_LOGIN_FAILED,
                    "Could not login to mail server {0} port {1} error code {2} ({3})", null, args);
            }

            if (smtpc.sendSimpleMessage(sender, recipient, message) == false) {
                reply = smtpc.getReplyCode();
                Object[] args = {server, port, Integer.valueOf(reply), smtpc.getReplyString()};
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

    /**
     * Create a new user.
     * 
     * @param userName the name of the user (without the space)
     * @param map extra datas to add to user profile object
     * @param context the XWiki context
     * @return <ul>
     *         <li>1: ok</li>
     *         <li>-3: user already exists</li>
     *         </ul>
     * @throws XWikiException failed to create the new user
     */
    public int createUser(String userName, Map<String, ? > map, XWikiContext context) throws XWikiException
    {
        return createUser(userName, map, "edit", context);
    }

    /**
     * Create a new user.
     * 
     * @param userName the name of the user (without the space)
     * @param map extra datas to add to user profile object
     * @param userRights the right of the user on his own profile page
     * @param context the XWiki context
     * @return <ul>
     *         <li>1: ok</li>
     *         <li>-3: user already exists</li>
     *         </ul>
     * @throws XWikiException failed to create the new user
     */
    public int createUser(String userName, Map<String, ? > map, String userRights, XWikiContext context)
        throws XWikiException
    {
        BaseClass userClass = context.getWiki().getUserClass(context);

        String content;
        Syntax syntax;
        if (!context.getWiki().getDefaultDocumentSyntax().equals(Syntax.XWIKI_1_0.toIdString())) {
            content = "{{include document=\"XWiki.XWikiUserSheet\"/}}";
            syntax = Syntax.XWIKI_2_0;
        } else {
            content = "#includeForm(\"XWiki.XWikiUserSheet\")";
            syntax = Syntax.XWIKI_1_0;
        }

        return createUser(userName, map, new EntityReference(userClass.getDocumentReference().getName(),
            EntityType.DOCUMENT), content, syntax, userRights, context);
    }

    /**
     * @deprecated since 2.4RC1 use
     *             {@link #createUser(String, Map, EntityReference, String, Syntax, String, XWikiContext)} instead
     */
    @Deprecated
    public int createUser(String userName, Map<String, ? > map, String parent, String content, String syntaxId,
        String userRights, XWikiContext context) throws XWikiException
    {
        Syntax syntax;

        try {
            syntax = this.syntaxFactory.createSyntaxFromIdString(syntaxId);
        } catch (ParseException e) {
            try {
                syntax = this.syntaxFactory.createSyntaxFromIdString(getDefaultDocumentSyntax());
            } catch (ParseException e1) {
                // Let's jope that never happen
                LOGGER.warn("Failed to set parse syntax [" + getDefaultDocumentSyntax() + "]", e);

                syntax = Syntax.XWIKI_2_0;
            }
        }

        return createUser(userName, map, this.relativeEntityReferenceResolver.resolve(parent, EntityType.DOCUMENT),
            content, syntax, userRights, context);
    }

    /**
     * Create a new user.
     * 
     * @param userName the name of the user (without the space)
     * @param map extra datas to add to user profile object
     * @param parentReference the parent of the user profile
     * @param content the content of the user profile
     * @param syntax the syntax of the provided content
     * @param userRights the right of the user on his own profile page
     * @param context the XWiki context
     * @return <ul>
     *         <li>1: ok</li>
     *         <li>-3: user already exists</li>
     *         </ul>
     * @throws XWikiException failed to create the new user
     */
    public int createUser(String userName, Map<String, ? > map, EntityReference parentReference, String content,
        Syntax syntax, String userRights, XWikiContext context) throws XWikiException
    {
        BaseClass baseclass = getUserClass(context);

        try {
            // TODO: Verify existing user
            XWikiDocument doc = getDocument(new DocumentReference(context.getDatabase(), "XWiki", userName), context);

            if (!doc.isNew()) {
                // TODO: throws Exception
                return -3;
            }

            BaseObject userObject =
                doc.newXObject(
                    this.localReferenceEntityReferenceSerializer.serialize(baseclass.getDocumentReference()), context);
            baseclass.fromMap(map, userObject);

            doc.setParentReference(parentReference);
            doc.setContent(content);
            doc.setSyntax(syntax);
            doc.setCreatorReference(doc.getDocumentReference());
            doc.setAuthorReference(doc.getDocumentReference());

            protectUserPage(doc.getFullName(), userRights, doc, context);

            saveDocument(doc, context.getMessageTool().get("core.comment.createdUser"), context);

            // Now let's add the user to XWiki.XWikiAllGroup
            setUserDefaultGroup(doc.getFullName(), context);

            return 1;
        } catch (Exception e) {
            Object[] args = {"XWiki." + userName};
            throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_CREATE,
                "Cannot create user {0}", e, args);
        }
    }

    /**
     * @deprecated starting with XE 1.8.1 use
     *             {@link #createUser(String, Map, String, String, String, String, XWikiContext)} instead
     */
    @Deprecated
    public int createUser(String xwikiname, Map map, String parent, String content, String userRights,
        XWikiContext context) throws XWikiException
    {
        return createUser(xwikiname, map, parent, content, Syntax.XWIKI_1_0.toIdString(), userRights, context);
    }

    public void setUserDefaultGroup(String fullwikiname, XWikiContext context) throws XWikiException
    {
        String groupsPreference = Param("xwiki.users.initialGroups", "XWiki.XWikiAllGroup");

        if (groupsPreference != null) {
            String[] groups = groupsPreference.split(",");
            for (String groupName : groups) {
                if (StringUtils.isNotBlank(groupName)) {
                    addUserToGroup(fullwikiname, groupName.trim(), context);
                }
            }
        }
    }

    protected void addUserToGroup(String userName, String groupName, XWikiContext context) throws XWikiException
    {
        BaseClass groupClass = getGroupClass(context);
        XWikiDocument groupDoc = getDocument(groupName, context);

        BaseObject memberObject =
            groupDoc.newXObject(
                this.localReferenceEntityReferenceSerializer.serialize(groupClass.getDocumentReference()), context);

        memberObject.setStringValue("member", userName);

        if (groupDoc.isNew()) {
            saveDocument(groupDoc, context.getMessageTool().get("core.comment.addedUserToGroup"), context);
        } else {
            // TODO Fix use of deprecated call.
            getHibernateStore().saveXWikiObject(memberObject, context, true);
        }

        try {
            XWikiGroupService gservice = getGroupService(context);
            gservice.addUserToGroup(userName, context.getDatabase(), groupName, context);
        } catch (Exception e) {
            LOGGER.error("Failed to update group service cache", e);
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

    public void protectUserPage(String userName, String userRights, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        BaseClass rclass = getRightsClass(context);

        EntityReference rightClassReference =
            this.localReferenceEntityReferenceSerializer.serialize(rclass.getDocumentReference());

        // Add protection to the page
        BaseObject newrightsobject = doc.newXObject(rightClassReference, context);
        newrightsobject.setLargeStringValue("groups", "XWiki.XWikiAdminGroup");
        newrightsobject.setStringValue("levels", userRights);
        newrightsobject.setIntValue("allow", 1);

        BaseObject newuserrightsobject = doc.newXObject(rightClassReference, context);
        newuserrightsobject.setLargeStringValue("users", userName);
        newuserrightsobject.setStringValue("levels", userRights);
        newuserrightsobject.setIntValue("allow", 1);
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
            @SuppressWarnings("unchecked")
            Map<String, Object> gcontext = (Map<String, Object>) context.get("gcontext");
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
        if (action.equals("skin") && (doc.getSpace().equals("skins") || doc.getSpace().equals("resources"))) {
            // We still need to call checkAuth to set the proper user.
            XWikiUser user = checkAuth(context);
            if (user != null) {
                context.setUser(user.getUser());
            }
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

    public String include(String topic, boolean isForm, XWikiContext context) throws XWikiException
    {
        String database = null, incdatabase = null;
        String prefixedTopic, localTopic;

        // Save current documents in the Velocity and Groovy contexts
        Document currentdoc = null, currentcdoc = null, currenttdoc = null;
        Document gcurrentdoc = null, gcurrentcdoc = null, gcurrenttdoc = null;
        VelocityContext vcontext = (VelocityContext) context.get("vcontext");
        String currentDocName = context.getDatabase() + ":" + context.getDoc().getFullName();
        if (vcontext != null) {
            currentdoc = (Document) vcontext.get("doc");
            currentcdoc = (Document) vcontext.get("cdoc");
            currenttdoc = (Document) vcontext.get("tdoc");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> gcontext = (Map<String, Object>) context.get("gcontext");
        if (gcontext != null) {
            gcurrentdoc = (Document) gcontext.get("doc");
            gcurrentcdoc = (Document) gcontext.get("cdoc");
            gcurrenttdoc = (Document) gcontext.get("tdoc");
        }

        try {
            int i0 = topic.indexOf(':');
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
                LOGGER.debug("Including Topic " + topic);
                try {
                    @SuppressWarnings("unchecked")
                    Set<String> includedDocs = (Set<String>) context.get("included_docs");
                    if (includedDocs == null) {
                        includedDocs = new HashSet<String>();
                        context.put("included_docs", includedDocs);
                    }

                    if (includedDocs.contains(prefixedTopic) || currentDocName.equals(prefixedTopic)) {
                        LOGGER.warn("Error on too many recursive includes for topic " + topic);
                        return "Cannot make recursive include";
                    }
                    includedDocs.add(prefixedTopic);
                } catch (Exception e) {
                }

                // Get document to include
                doc = getDocument(((XWikiDocument) context.get("doc")).getSpace(), localTopic, context);

                if (checkAccess("view", doc, context) == false) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS,
                        XWikiException.ERROR_XWIKI_ACCESS_DENIED, "Access to this document is denied: " + doc);
                }
            } catch (XWikiException e) {
                LOGGER.warn("Exception Including Topic " + topic, e);
                return "Topic " + topic + " does not exist";
            }

            XWikiDocument contentdoc = doc.getTranslatedDocument(context);

            String result;
            if (isForm) {
                // We do everything in the context of the including document
                if (database != null) {
                    context.setDatabase(database);
                }

                // Allow including document in the XWiki Syntax 1.0 but also other syntaxes using the new rendering.
                if (contentdoc.getSyntax().equals(Syntax.XWIKI_1_0)) {
                    result =
                        getRenderingEngine().renderText(contentdoc.getContent(), contentdoc,
                            (XWikiDocument) context.get("doc"), context);
                } else {
                    // Note: the Script macro in the new rendering checks for programming rights for the document in
                    // the xwiki context.
                    result = getRenderedContent(contentdoc, (XWikiDocument) context.get("doc"), context);
                }
            } else {
                // We stay in the included document context

                // Allow including document in the XWiki Syntax 1.0 but also other syntaxes using the new rendering.
                if (contentdoc.getSyntax().equals(Syntax.XWIKI_1_0)) {
                    result = getRenderingEngine().renderText(contentdoc.getContent(), contentdoc, doc, context);
                } else {
                    // Since the Script macro checks for programming rights in the current document, we need to
                    // temporarily set the contentdoc as the current doc before rendering it.
                    XWikiDocument originalDoc = null;
                    try {
                        originalDoc = context.getDoc();
                        context.put("doc", doc);
                        result = getRenderedContent(contentdoc, doc, context);
                    } finally {
                        context.put("doc", originalDoc);
                    }
                }
            }
            try {
                @SuppressWarnings("unchecked")
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

    /**
     * Render content from the passed included document, setting the correct security doc (sdoc) and including doc
     * (idoc). Note that this is needed for 2.0 syntax only since in 1.0 syntax the idoc and sdoc are set by
     * {@link com.xpn.xwiki.render.XWikiRenderingEngine#renderText}.
     * 
     * @since 2.2M2
     */
    private String getRenderedContent(XWikiDocument includedDoc, XWikiDocument includingDoc, XWikiContext context)
        throws XWikiException
    {
        String result;
        XWikiDocument idoc = (XWikiDocument) context.get("idoc");
        XWikiDocument sdoc = (XWikiDocument) context.get("sdoc");

        context.put("idoc", includingDoc);
        context.put("sdoc", includedDoc);
        try {
            result = includedDoc.getRenderedContent(Syntax.XHTML_1_0, false, context);
        } finally {
            // Remove including doc or set the previous one
            if (idoc == null) {
                context.remove("idoc");
            } else {
                context.put("idoc", idoc);
            }

            // Remove security doc or set the previous one
            if (sdoc == null) {
                context.remove("sdoc");
            } else {
                context.put("sdoc", sdoc);
            }
        }

        return result;
    }

    public void deleteDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        deleteDocument(doc, true, context);
    }

    public void deleteDocument(XWikiDocument doc, boolean totrash, XWikiContext context) throws XWikiException
    {
        ObservationManager om = Utils.getComponent(ObservationManager.class);

        // Inform notification mechanisms that a document is about to be deleted
        // Note that for the moment the event being send is a bridge event, as we are still passing around
        // an XWikiDocument as source and an XWikiContext as data.
        om.notify(new DocumentDeletingEvent(doc.getDocumentReference()), new XWikiDocument(doc.getDocumentReference()),
            context);

        if (hasRecycleBin(context) && totrash) {
            getRecycleBinStore().saveToRecycleBin(doc, context.getUser(), new Date(), context, true);
        }

        getStore().deleteXWikiDoc(doc, context);

        try {
            // Inform notification mecanisms that a document has been deleted
            // Note that for the moment the event being send is a bridge event, as we are still passing around
            // an XWikiDocument as source and an XWikiContext as data.
            // The source document is a new empty XWikiDocument to follow
            // DocumentUpdatedEvent policy: source document in new document and the old version is available using
            // doc.getOriginalDocument()
            if (om != null) {
                XWikiDocument blankDoc = new XWikiDocument(doc.getDocumentReference());
                // Again to follow general event policy, new document author is the user who modified the document (here
                // the modification is delete)
                blankDoc.setOriginalDocument(doc);
                blankDoc.setAuthor(context.getUser());
                blankDoc.setContentAuthor(context.getUser());
                om.notify(new DocumentDeletedEvent(doc.getDocumentReference()), blankDoc, context);
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to send document delete notifications for document [" + doc.getPrefixedFullName()
                + "]", ex);
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

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, null, true, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        boolean reset, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, null, reset, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        boolean reset, boolean force, boolean resetCreationData, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, null, reset, force, resetCreationData,
            context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilanguage, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, wikilanguage, true, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilanguage, boolean reset, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, wikilanguage, reset, false, context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilanguage, boolean reset, boolean force, XWikiContext context) throws XWikiException
    {
        return copyDocument(sourceDocumentReference, targetDocumentReference, wikilanguage, reset, force, false,
            context);
    }

    /**
     * @since 2.2M2
     */
    public boolean copyDocument(DocumentReference sourceDocumentReference, DocumentReference targetDocumentReference,
        String wikilanguage, boolean reset, boolean force, boolean resetCreationData, XWikiContext context)
        throws XWikiException
    {
        String db = context.getDatabase();
        String sourceWiki = sourceDocumentReference.getWikiReference().getName();
        String targetWiki = targetDocumentReference.getWikiReference().getName();

        String sourceStringReference = this.defaultEntityReferenceSerializer.serialize(sourceDocumentReference);

        try {
            context.setDatabase(sourceWiki);
            XWikiDocument sdoc = getDocument(sourceDocumentReference, context);
            if (!sdoc.isNew()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Copying document [" + sourceDocumentReference + "] to [" + targetDocumentReference
                        + "]");
                }

                // Let's switch to the other database to verify if the document already exists
                context.setDatabase(targetWiki);
                XWikiDocument tdoc = getDocument(targetDocumentReference, context);
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
                context.setDatabase(sourceWiki);

                if (wikilanguage == null) {
                    tdoc = sdoc.copyDocument(targetDocumentReference, context);
                    // forget past versions
                    if (reset) {
                        tdoc.setNew(true);
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

                    // We don't want to trigger a new version otherwise the version number will be wrong
                    tdoc.setMetaDataDirty(false);
                    tdoc.setContentDirty(false);

                    saveDocument(tdoc, "Copied from " + sourceStringReference, context);

                    if (!reset) {
                        context.setDatabase(sourceWiki);
                        XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                        context.setDatabase(targetWiki);
                        txda = txda.clone(tdoc.getId(), context);
                        getVersioningStore().saveXWikiDocArchive(txda, true, context);
                    } else {
                        getVersioningStore().resetRCSArchive(tdoc, true, context);
                    }

                    context.setDatabase(targetWiki);
                    for (XWikiAttachment attachment : tdoc.getAttachmentList()) {
                        getAttachmentStore().saveAttachmentContent(attachment, false, context, true);
                    }

                    // Now we need to copy the translations
                    context.setDatabase(sourceWiki);
                    List<String> tlist = sdoc.getTranslationList(context);
                    for (String clanguage : tlist) {
                        XWikiDocument stdoc = sdoc.getTranslatedDocument(clanguage, context);
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("Copying document [" + sourceWiki + "], language [" + clanguage + "] to ["
                                + targetDocumentReference + "]");
                        }

                        context.setDatabase(targetWiki);
                        XWikiDocument ttdoc = tdoc.getTranslatedDocument(clanguage, context);

                        // There is already an existing document
                        if (ttdoc != tdoc) {
                            return false;
                        }

                        // Let's switch back again to the original db
                        context.setDatabase(sourceWiki);

                        ttdoc = stdoc.copyDocument(targetDocumentReference, context);

                        // forget past versions
                        if (reset) {
                            ttdoc.setNew(true);
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

                        // we don't want to trigger a new version
                        // otherwise the version number will be wrong
                        tdoc.setMetaDataDirty(false);
                        tdoc.setContentDirty(false);

                        saveDocument(ttdoc, "Copied from " + sourceStringReference, context);

                        if (!reset) {
                            context.setDatabase(sourceWiki);
                            XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                            context.setDatabase(targetWiki);
                            txda = txda.clone(tdoc.getId(), context);
                            getVersioningStore().saveXWikiDocArchive(txda, true, context);
                        } else {
                            getVersioningStore().resetRCSArchive(tdoc, true, context);
                        }
                    }
                } else {
                    // We want only one language in the end
                    XWikiDocument stdoc = sdoc.getTranslatedDocument(wikilanguage, context);

                    tdoc = stdoc.copyDocument(targetDocumentReference, context);

                    // forget language
                    tdoc.setDefaultLanguage(wikilanguage);
                    tdoc.setLanguage("");
                    // forget past versions
                    if (reset) {
                        tdoc.setNew(true);
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

                    // we don't want to trigger a new version
                    // otherwise the version number will be wrong
                    tdoc.setMetaDataDirty(false);
                    tdoc.setContentDirty(false);

                    saveDocument(tdoc, "Copied from " + sourceStringReference, context);

                    if (!reset) {
                        context.setDatabase(sourceWiki);
                        XWikiDocumentArchive txda = getVersioningStore().getXWikiDocumentArchive(sdoc, context);
                        context.setDatabase(targetWiki);
                        txda = txda.clone(tdoc.getId(), context);
                        getVersioningStore().saveXWikiDocArchive(txda, true, context);
                    } else {
                        getVersioningStore().resetRCSArchive(tdoc, true, context);
                    }

                    context.setDatabase(targetWiki);
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

    public int copySpaceBetweenWikis(String space, String sourceWiki, String targetWiki, String language,
        XWikiContext context) throws XWikiException
    {
        return copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, false, context);
    }

    public int copySpaceBetweenWikis(String space, String sourceWiki, String targetWiki, String language,
        boolean clean, XWikiContext context) throws XWikiException
    {
        String db = context.getDatabase();
        int nb = 0;
        // Workaround for XWIKI-3915: Do not use XWikiStoreInterface#searchDocumentNames since currently it has the
        // side effect of hidding hidden documents and no other workaround exists than directly using
        // XWikiStoreInterface#search directly
        String sql = "select distinct doc.fullName from XWikiDocument as doc";
        List<String> parameters = new ArrayList<String>();
        if (space != null) {
            sql += " where doc.space = ?";
            parameters.add(space);
        }

        if (clean) {
            try {
                context.setDatabase(targetWiki);
                List<String> list = getStore().search(sql, 0, 0, parameters, context);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Deleting " + list.size() + " documents from wiki " + targetWiki);
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
            List<String> list = getStore().search(sql, 0, 0, context);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Copying " + list.size() + " documents from wiki " + sourceWiki + " to wiki " + targetWiki);
            }

            WikiReference sourceWikiReference = new WikiReference(sourceWiki);
            WikiReference targetWikiReference = new WikiReference(targetWiki);
            for (String docname : list) {
                DocumentReference sourceDocumentReference = this.currentMixedDocumentReferenceResolver.resolve(docname);
                sourceDocumentReference = sourceDocumentReference.replaceParent(
                    sourceDocumentReference.getWikiReference(), sourceWikiReference);
                DocumentReference targetDocumentReference = sourceDocumentReference.replaceParent(
                    sourceWikiReference, targetWikiReference);
                copyDocument(sourceDocumentReference, targetDocumentReference, language, context);
                nb++;
            }
            return nb;
        } finally {
            context.setDatabase(db);
        }
    }

    /**
     * Copy an entire wiki to a target wiki.
     * <p>
     * It does not override document already existing in target wiki.
     * 
     * @param sourceWiki the source wiki identifier
     * @param targetWiki the target wiki identifier
     * @param language the language to copy
     * @param context the XWiki context
     * @return the number of copied documents
     * @throws XWikiException failed to copy wiki
     */
    public int copyWiki(String sourceWiki, String targetWiki, String language, XWikiContext context)
        throws XWikiException
    {
        return copyWiki(sourceWiki, targetWiki, language, false, context);
    }

    /**
     * Copy an entire wiki to a target wiki.
     * 
     * @param sourceWiki the source wiki identifier
     * @param targetWiki the target wiki identifier
     * @param language the language to copy
     * @param clean clean the target wiki before copying
     * @param context the XWiki context
     * @return the number of copied documents
     * @throws XWikiException failed to copy wiki
     */
    public int copyWiki(String sourceWiki, String targetWiki, String language, boolean clean, XWikiContext context)
        throws XWikiException
    {
        return copySpaceBetweenWikis(null, sourceWiki, targetWiki, language, clean, context);
    }

    /**
     * @deprecated use WikiManager plugin instead
     */
    @Deprecated
    public int createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName,
        String description, String wikilanguage, boolean failOnExist, XWikiContext context) throws XWikiException
    {
        String database = context.getDatabase();
        wikiName = wikiName.toLowerCase();

        try {
            XWikiDocument userdoc = getDocument(wikiAdmin, context);

            // User does not exist
            if (userdoc.isNew()) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                        + "user does not exist");
                }
                return -2;
            }

            // User is not active
            if (!(userdoc.getIntValue("XWiki.XWikiUsers", "active") == 1)) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                        + "user is not active");
                }
                return -3;
            }

            String wikiForbiddenList = Param("xwiki.virtual.reserved_wikis");
            if (Util.contains(wikiName, wikiForbiddenList, ", ")) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                        + "wiki name is forbidden");
                }
                return -4;
            }

            String wikiServerPage = "XWikiServer" + wikiName.substring(0, 1).toUpperCase() + wikiName.substring(1);
            // Verify is server page already exist
            XWikiDocument serverdoc = getDocument(SYSTEM_SPACE, wikiServerPage, context);
            if (serverdoc.isNew()) {
                // clear entry in virtual wiki cache
                this.virtualWikiMap.remove(wikiUrl);

                // Create Wiki Server page
                serverdoc.setStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "server", wikiUrl);
                serverdoc.setStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "owner", wikiAdmin);
                if (description != null) {
                    serverdoc.setLargeStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "description", description);
                }
                if (wikilanguage != null) {
                    serverdoc.setStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "language", wikilanguage);
                }
                if (!getDefaultDocumentSyntax().equals(Syntax.XWIKI_1_0.toIdString())) {
                    serverdoc.setContent("{{include document=\"XWiki.XWikiServerForm\"/}}\n");
                    serverdoc.setSyntax(Syntax.XWIKI_2_0);
                } else {
                    serverdoc.setContent("#includeForm(\"XWiki.XWikiServerForm\")\n");
                    serverdoc.setSyntax(Syntax.XWIKI_1_0);
                }
                serverdoc.setParentReference(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
                saveDocument(serverdoc, context);
            } else {
                // If we are not allowed to continue if server page already exists
                if (failOnExist) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                            + "wiki server page already exists");
                    }
                    return -5;
                } else if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                        + "wiki server page already exists");
                }
            }

            // Create wiki database
            try {
                context.setDatabase(getDatabase());
                getStore().createWiki(wikiName, context);
            } catch (XWikiException e) {
                if (LOGGER.isErrorEnabled()) {
                    if (e.getCode() == 10010) {
                        LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                            + "wiki database already exists");
                    } else if (e.getCode() == 10011) {
                        LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                            + "wiki database creation failed");
                    } else {
                        LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                            + "wiki database creation threw exception", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                    + "wiki database creation threw exception", e);
            }

            try {
                updateDatabase(wikiName, true, false, context);
            } catch (Exception e) {
                LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
                    + "wiki database shema update threw exception", e);
                return -6;
            }

            // Copy base wiki
            int nb = copyWiki(baseWikiName, wikiName, wikilanguage, context);
            // Save the number of docs copied in the context
            context.put("nbdocs", Integer.valueOf(nb));

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
            LOGGER.error("Wiki creation (" + wikiName + "," + wikiUrl + "," + wikiAdmin + ") failed: "
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

        // In virtual wiki path mode the server is the standard one
        if ("1".equals(Param("xwiki.virtual.usepath", "0"))) {
            return null;
        }

        if (database != null) {
            String db = context.getDatabase();
            try {
                context.setDatabase(getDatabase());
                XWikiDocument doc = getDocument("XWiki.XWikiServer" + StringUtils.capitalize(database), context);
                BaseObject serverobject = doc.getXObject(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
                if (serverobject != null) {
                    String server = serverobject.getStringValue("server");
                    if (server != null) {
                        String protocol = context.getWiki().Param("xwiki.url.protocol", null);
                        if (protocol == null) {
                            int iSecure = serverobject.getIntValue("secure", -1);
                            // Check the request object if the "secure" property is undefined.
                            boolean secure = iSecure == 1 || (iSecure < 0 && context.getRequest().isSecure());
                            protocol = secure ? "https" : "http";
                        }
                        long port = context.getURL().getPort();
                        if (port == 80 || port == 443) {
                            port = -1;
                        }
                        if (port != -1) {
                            serverurl = protocol + "://" + server + ":" + port + "/";
                        } else {
                            serverurl = protocol + "://" + server + "/";
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

    public String getServletPath(String wikiName, XWikiContext context)
    {
        // unless we are in virtual wiki path mode we should return null
        if (!context.getMainXWiki().equalsIgnoreCase(wikiName) && "1".equals(Param("xwiki.virtual.usepath", "0"))) {
            String database = context.getDatabase();
            try {
                context.setDatabase(context.getMainXWiki());
                XWikiDocument doc = getDocument(getServerWikiPage(wikiName), context);
                BaseObject serverobject = doc.getXObject(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE);
                if (serverobject != null) {
                    String server = serverobject.getStringValue("server");
                    return "wiki/" + server + "/";
                }
            } catch (Exception e) {
                LOGGER.error("Failed to get URL for provided wiki [" + wikiName + "]", e);
            } finally {
                context.setDatabase(database);
            }
        }

        String servletPath = Param("xwiki.servletpath", "");

        if (context.getRequest() != null) {
            if (StringUtils.isEmpty(servletPath)) {
                String currentServletpath = context.getRequest().getServletPath();
                if (currentServletpath != null && currentServletpath.startsWith("/bin")) {
                    servletPath = "bin/";
                } else {
                    servletPath = Param("xwiki.defaultservletpath", "bin/");
                }
            }
        }

        return servletPath;
    }

    public String getWebAppPath(XWikiContext context)
    {
        String path = context.getURL().getPath();
        String contextPath = Param("xwiki.webapppath", "");
        if (contextPath.equals("")) {
            try {
                contextPath = context.getRequest().getContextPath();
                // TODO We're using URL parts in a wrong way, since contextPath and servletPath are
                // returned with a leading /, while we need a trailing /. This code moves the / from
                // the beginning to the end.
                // If the app is deployed as the ROOT ap, then there's no need to move the /.
                if (contextPath.length() > 0) {
                    contextPath = contextPath.substring(1) + "/";
                }
            } catch (Exception e) {
                contextPath = path.substring(0, path.indexOf('/', 1) + 1);
            }
        }

        return contextPath;
    }

    /**
     * @since 2.2.1
     */
    public String getURL(DocumentReference documentReference, String action, String queryString, String anchor,
        XWikiContext context)
    {
        XWikiDocument doc = new XWikiDocument(documentReference);

        URL url =
            context.getURLFactory().createURL(doc.getSpace(), doc.getName(), action, queryString, anchor,
                doc.getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    /**
     * @deprecated since 2.2.1 use {@link #getURL(DocumentReference, String, String, String, XWikiContext)}
     */
    @Deprecated
    public String getURL(String fullname, String action, String queryString, String anchor, XWikiContext context)
    {
        XWikiDocument doc = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(fullname));

        URL url =
            context.getURLFactory().createURL(doc.getSpace(), doc.getName(), action, queryString, anchor,
                doc.getDatabase(), context);
        return context.getURLFactory().getURL(url, context);
    }

    public String getURL(String fullname, String action, String querystring, XWikiContext context)
    {
        return getURL(fullname, action, querystring, null, context);
    }

    /**
     * @since 2.3M2
     */
    public String getURL(DocumentReference reference, String action, XWikiContext context)
    {
        return getURL(reference, action, null, null, context);
    }

    /**
     * @deprecated since 2.3M2 use {@link #getURL(DocumentReference, String, XWikiContext)}
     */
    @Deprecated
    public String getURL(String fullname, String action, XWikiContext context)
    {
        return getURL(fullname, action, null, null, context);
    }

    public String getExternalURL(String fullname, String action, XWikiContext context) throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(fullname));

        URL url =
            context.getURLFactory().createExternalURL(doc.getSpace(), doc.getName(), action, null, null,
                doc.getDatabase(), context);
        return url.toString();
    }

    public String getExternalURL(String fullname, String action, String querystring, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(fullname));

        URL url =
            context.getURLFactory().createExternalURL(doc.getSpace(), doc.getName(), action, querystring, null,
                doc.getDatabase(), context);
        return url.toString();
    }

    public String getAttachmentURL(String fullname, String filename, XWikiContext context) throws XWikiException
    {
        return getAttachmentURL(fullname, filename, null, context);
    }

    /**
     * @since 2.5RC1
     */
    public String getAttachmentURL(String fullname, String filename, String queryString, XWikiContext context)
        throws XWikiException
    {
        XWikiDocument doc = new XWikiDocument(this.currentMixedDocumentReferenceResolver.resolve(fullname));
        return doc.getAttachmentURL(filename, "download", queryString, context);
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
        return "1".equals(Param("xwiki.virtual"));
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

        // These users are necessarily active
        if (user.equals(XWikiRightService.GUEST_USER_FULLNAME)
            || (user.equals(XWikiRightService.SUPERADMIN_USER_FULLNAME))) {
            return active;
        }

        String checkactivefield = getXWikiPreference("auth_active_check", context);
        if (checkactivefield.equals("1")) {
            XWikiDocument userdoc = getDocument(user, context);
            active = userdoc.getIntValue("XWiki.XWikiUsers", "active");
        }

        return active;
    }

    /**
     * @since 2.3M1
     */
    public DocumentReference getDocumentReference(XWikiRequest request, XWikiContext context)
    {
        DocumentReference reference;
        if (context.getMode() == XWikiContext.MODE_PORTLET) {
            if (request.getParameter("topic") != null) {
                reference = this.currentMixedDocumentReferenceResolver.resolve(request.getParameter("topic"));
            } else {
                // Point to this wiki's home page
                reference =
                    new DocumentReference(context.getDatabase(),
                        this.defaultEntityReferenceValueProvider.getDefaultValue(EntityType.SPACE),
                        this.defaultEntityReferenceValueProvider.getDefaultValue(EntityType.DOCUMENT));
            }
        } else if (context.getMode() == XWikiContext.MODE_XMLRPC) {
            reference =
                new DocumentReference(context.getDatabase(), context.getDoc().getDocumentReference()
                    .getLastSpaceReference().getName(), context.getDoc().getDocumentReference().getName());
        } else {
            String action = context.getAction();
            if ((request.getParameter("topic") != null) && (action.equals("edit") || action.equals("inline"))) {
                reference = this.currentMixedDocumentReferenceResolver.resolve(request.getParameter("topic"));
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

                String path = request.getRequestURI();

                // Remove the (eventual) context path from the URI, usually /xwiki
                path = stripSegmentFromPath(path, request.getContextPath());

                // Remove the (eventual) servlet path from the URI, usually /bin
                String servletPath = request.getServletPath();
                path = stripSegmentFromPath(path, servletPath);

                // We need to get rid of the wiki name in case of a XEM in usepath mode
                if ("1".equals(Param("xwiki.virtual.usepath", "0"))
                    && servletPath.equals("/" + Param("xwiki.virtual.usepath.servletpath", "wiki"))) {
                    // Virtual mode, skip the wiki name
                    if (path.indexOf('/', 1) < 0) {
                        path = "";
                    } else {
                        path = path.substring(path.indexOf('/', 1));
                    }
                }

                // Fix error in some containers, which don't hide the jsessionid parameter from the URL
                if (path.indexOf(";jsessionid=") != -1) {
                    path = path.substring(0, path.indexOf(";jsessionid="));
                }
                reference = getDocumentReferenceFromPath(path, context);
            }
        }

        return reference;
    }

    /**
     * @deprecated since 2.3M1 use {@link #getDocumentReferenceFromPath(String, XWikiContext)} instead
     */
    @Deprecated
    public String getDocumentName(XWikiRequest request, XWikiContext context)
    {
        return this.localStringEntityReferenceSerializer.serialize(getDocumentReference(request, context));
    }

    /**
     * Helper method, removes a predefined path segment (the context path or the servel path) from the start of the
     * requested URI and returns the remainder. This method is needed because special characters in the path can be
     * URL-encoded, depending on whether the request is forwarded through the request dispatcher or not, and also
     * depending on the client (some browsers encode -, while some don't).
     * 
     * @param path the path, as taken from the requested URI
     * @param segment the segment to remove, as reported by the container
     * @return the path with the specified segment trimmed from its start
     */
    public static String stripSegmentFromPath(String path, String segment)
    {
        if (!path.startsWith(segment)) {
            // The context path probably contains special characters that are encoded in the URL
            try {
                segment = URIUtil.encodePath(segment);
            } catch (URIException e) {
                LOGGER.warn("Invalid path: [" + segment + "]");
            }
        }
        if (!path.startsWith(segment)) {
            // Some clients also encode -, although it's allowed in the path
            segment = segment.replaceAll("-", "%2D");
        }
        if (!path.startsWith(segment)) {
            // Can't find the context path in the URL (shouldn't happen), just skip to the next path segment
            return path.substring(path.indexOf('/', 1));
        }
        return path.substring(segment.length());
    }

    public boolean prepareDocuments(XWikiRequest request, XWikiContext context, VelocityContext vcontext)
        throws XWikiException
    {
        XWikiDocument doc;
        context.getWiki().prepareResources(context);
        DocumentReference reference = getDocumentReference(request, context);
        if (context.getAction().equals("register")) {
            setPhonyDocument(reference, context, vcontext);
            doc = context.getDoc();
        } else {
            try {
                doc = getDocument(reference, context);
            } catch (XWikiException e) {
                doc = context.getDoc();
                if (context.getAction().equals("delete")) {
                    if (doc == null) {
                        setPhonyDocument(reference, context, vcontext);
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
            setPhonyDocument(reference, context, vcontext);
            throw new XWikiException(XWikiException.MODULE_XWIKI_ACCESS, XWikiException.ERROR_XWIKI_ACCESS_DENIED,
                "Access to document {0} has been denied to user {1}", null, args);
        } else if (checkActive(context) == 0) { // if auth_active_check, check if user is inactive
            boolean allow = false;
            String action = context.getAction();
            /*
             * Allow inactive users to see skins, ressources, SSX, JSX and downloads they could have seen as guest. The
             * rational behind this behaviour is that inactive users should be able to access the same UI that guests
             * are used to see, including custom icons, panels, and so on...
             */
            if ((action.equals("skin") && (doc.getSpace().equals("skins") || doc.getSpace().equals("resources")))
                || ((action.equals("skin") || action.equals("download") || action.equals("ssx") || action.equals("jsx")) && getRightService()
                    .hasAccessLevel("view", XWikiRightService.GUEST_USER_FULLNAME, doc.getPrefixedFullName(), context))
                || ((action.equals("view") && doc.getFullName().equals("XWiki.AccountValidation")))) {
                allow = true;
            } else {
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
            }
            if (!allow) {
                Object[] args = {context.getUser()};
                setPhonyDocument(reference, context, vcontext);
                throw new XWikiException(XWikiException.MODULE_XWIKI_USER, XWikiException.ERROR_XWIKI_USER_INACTIVE,
                    "User {0} account is inactive", null, args);
            }
        }

        context.put("doc", doc);
        vcontext.put("doc", doc.newDocument(context));
        vcontext.put("cdoc", vcontext.get("doc"));
        XWikiDocument tdoc = doc.getTranslatedDocument(context);
        try {
            String rev = (String) context.get("rev");
            if (StringUtils.isNotEmpty(rev)) {
                tdoc = getDocument(tdoc, rev, context);
            }
        } catch (Exception ex) {
            // Invalid version, just use the most recent one
        }
        context.put("tdoc", tdoc);
        vcontext.put("tdoc", tdoc.newDocument(context));

        return true;
    }

    /**
     * @since 2.3M1
     */
    public void setPhonyDocument(DocumentReference reference, XWikiContext context, VelocityContext vcontext)
    {
        XWikiDocument doc = new XWikiDocument(reference);
        doc.setElements(XWikiDocument.HAS_ATTACHMENTS | XWikiDocument.HAS_OBJECTS);
        doc.setStore(getStore());
        context.put("doc", doc);
        vcontext.put("doc", doc.newDocument(context));
        vcontext.put("cdoc", vcontext.get("doc"));
        vcontext.put("tdoc", vcontext.get("doc"));
    }

    /**
     * @deprecated since 2.3M1 use {@link #setPhonyDocument(DocumentReference, XWikiContext, VelocityContext)}
     */
    @Deprecated
    public void setPhonyDocument(String docName, XWikiContext context, VelocityContext vcontext)
    {
        setPhonyDocument(this.currentMixedDocumentReferenceResolver.resolve(docName), context, vcontext);
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
                String groupClass =
                    Param("xwiki.authentication.groupclass", "com.xpn.xwiki.user.impl.xwiki.XWikiGroupServiceImpl");

                try {
                    this.groupService = (XWikiGroupService) Class.forName(groupClass).newInstance();
                } catch (Exception e) {
                    LOGGER.error("Failed to instantiate custom group service class: " + e.getMessage(), e);
                    this.groupService = new XWikiGroupServiceImpl();
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

                LOGGER.info("Initializing AuthService...");

                String authClass = Param("xwiki.authentication.authclass");
                if (StringUtils.isNotEmpty(authClass)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Using custom AuthClass " + authClass + ".");
                    }
                } else {
                    if (isLDAP()) {
                        authClass = "com.xpn.xwiki.user.impl.LDAP.XWikiLDAPAuthServiceImpl";
                    } else {
                        authClass = "com.xpn.xwiki.user.impl.xwiki.XWikiAuthServiceImpl";
                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Using default AuthClass " + authClass + ".");
                    }
                }

                try {
                    this.authService = (XWikiAuthService) Class.forName(authClass).newInstance();
                    LOGGER.debug("Initialized AuthService using Relfection.");
                } catch (Exception e) {
                    LOGGER.warn("Failed to initialize AuthService " + authClass
                        + " using Reflection, trying default implementations using 'new'.", e);

                    if (isLDAP()) {
                        this.authService = new XWikiLDAPAuthServiceImpl();
                    } else {
                        this.authService = new XWikiAuthServiceImpl();
                    }

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Initialized AuthService " + this.authService.getClass().getName()
                            + " using 'new'.");
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
                LOGGER.info("Initializing RightService...");

                String rightsClass = Param("xwiki.authentication.rightsclass");
                if (rightsClass != null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Using custom RightsClass " + rightsClass + ".");
                    }
                } else {
                    rightsClass = "com.xpn.xwiki.user.impl.xwiki.XWikiRightServiceImpl";
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Using default RightsClass " + rightsClass + ".");
                    }
                }

                try {
                    this.rightService = (XWikiRightService) Class.forName(rightsClass).newInstance();
                    LOGGER.debug("Initialized RightService using Reflection.");
                } catch (Exception e) {
                    LOGGER.warn("Failed to initialize RightService " + rightsClass
                        + " using Reflection, trying default implementation using 'new'.", e);

                    this.rightService = new XWikiRightServiceImpl();

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Initialized RightService " + this.rightService.getClass().getName()
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
                    LOGGER.info("Initializing URLFactory Service...");

                    XWikiURLFactoryService factoryService = null;

                    String urlFactoryServiceClass = Param("xwiki.urlfactory.serviceclass");
                    if (urlFactoryServiceClass != null) {
                        try {
                            if (LOGGER.isDebugEnabled()) {
                                LOGGER.debug("Using custom URLFactory Service Class [" + urlFactoryServiceClass + "]");
                            }
                            factoryService =
                                (XWikiURLFactoryService) Class.forName(urlFactoryServiceClass)
                                    .getConstructor(new Class< ? >[] {XWiki.class}).newInstance(new Object[] {this});
                        } catch (Exception e) {
                            factoryService = null;
                            LOGGER.warn("Failed to initialize URLFactory Service [" + urlFactoryServiceClass + "]", e);
                        }
                    }
                    if (factoryService == null) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Using default URLFactory Service Class [" + urlFactoryServiceClass + "]");
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

        Object dialect = getHibernateStore().getConfiguration().getProperties().get("dialect");
        return "org.hibernate.dialect.MySQLDialect".equals(dialect)
            || "net.sf.hibernate.dialect.MySQLDialect".equals(dialect);
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
        return docname.substring(docname.indexOf('.') + 1);
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
        if (StringUtils.isBlank(user)) {
            return "";
        }
        XWikiDocument userdoc = null;
        try {
            userdoc = getDocument(user, context);
            if (userdoc == null) {
                return XMLUtils.escape(user);
            }

            BaseObject userobj = userdoc.getObject("XWiki.XWikiUsers");
            if (userobj == null) {
                return XMLUtils.escape(userdoc.getDocumentReference().getName());
            }

            Set<String> proplist = userobj.getPropertyList();
            String text;

            if (format == null) {
                text = userobj.getStringValue("first_name") + " " + userobj.getStringValue("last_name");
                if (StringUtils.isBlank(text)) {
                    text = userdoc.getDocumentReference().getName();
                }
            } else {
                VelocityContext vcontext = new VelocityContext();
                for (String propname : proplist) {
                    vcontext.put(propname, userobj.getStringValue(propname));
                }
                text =
                    XWikiVelocityRenderer.evaluate(format, "<username formatting code in "
                        + context.getDoc().getDocumentReference() + ">", vcontext, context);
            }

            text = XMLUtils.escape(text.trim());

            if (link) {
                text =
                    "<span class=\"wikilink\"><a href=\"" + userdoc.getURL("view", context) + "\">" + text
                        + "</a></span>";
            }
            return text;
        } catch (Exception e) {
            LOGGER.error("Failed to get user profile page", e);

            if (userdoc != null) {
                return userdoc.getDocumentReference().getName();
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
            return getUserName(user.substring(user.indexOf(':') + 1), null, true, context);
        }
    }

    public String getLocalUserName(String user, String format, XWikiContext context)
    {
        if (hasCentralizedAuthentication(context)) {
            return getUserName(user, format, true, context);
        } else {
            return getUserName(user.substring(user.indexOf(':') + 1), format, true, context);
        }
    }

    public String getLocalUserName(String user, String format, boolean link, XWikiContext context)
    {
        if (hasCentralizedAuthentication(context)) {
            return getUserName(user, format, link, context);
        } else {
            return getUserName(user.substring(user.indexOf(':') + 1), format, link, context);
        }
    }

    public String formatDate(Date date, String format, XWikiContext context)
    {
        if (date == null) {
            return "";
        }
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
            LOGGER.info("Failed to format date [" + date + "] with pattern [" + xformat + "]: " + e.getMessage());
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

    /**
     * @deprecated since 2.2.1 use {@link #exists(DocumentReference, XWikiContext)}
     */
    @Deprecated
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

    public boolean exists(DocumentReference documentReference, XWikiContext context)
    {
        String server = null, database = null;
        try {
            XWikiDocument doc = new XWikiDocument(documentReference);
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
                adtype = wikiServer.getStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "adtype");
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
                adclientid = wikiServer.getStringValue(VIRTUAL_WIKI_DEFINITION_CLASS_REFERENCE, "adclientid");
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
        Vector<String> pluginlist = plugins.getPlugins();
        for (String pluginname : pluginlist) {
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

    /**
     * @return the cache factory.
     * @since 1.5M2.
     * @deprecated Since 1.7M1, use {@link CacheManager} component instead using {@link Utils#getComponent(Class)}
     */
    @Deprecated
    public CacheFactory getCacheFactory()
    {
        CacheFactory cacheFactory;

        String cacheHint = Param("xwiki.cache.cachefactory.hint", null);

        if (StringUtils.isEmpty(cacheHint) || Utils.getComponent(CacheFactory.class, cacheHint) == null) {
            CacheManager cacheManager = Utils.getComponent(CacheManager.class);
            try {
                cacheFactory = cacheManager.getCacheFactory();
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Failed to get cache factory component", e);
            }
        } else {
            cacheFactory = Utils.getComponent(CacheFactory.class, cacheHint);
        }

        return cacheFactory;
    }

    /**
     * @return the cache factory creating local caches.
     * @since 1.5M2.
     * @deprecated Since 1.7M1, use {@link CacheManager} component instead using {@link Utils#getComponent(Class)}
     */
    @Deprecated
    public CacheFactory getLocalCacheFactory()
    {
        CacheFactory localCacheFactory;

        String localCacheHint = Param("xwiki.cache.cachefactory.local.hint", null);

        if (StringUtils.isEmpty(localCacheHint) || Utils.getComponent(CacheFactory.class, localCacheHint) == null) {
            CacheManager cacheManager = Utils.getComponent(CacheManager.class);
            try {
                localCacheFactory = cacheManager.getLocalCacheFactory();
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Failed to get local cache factory component", e);
            }
        } else {
            localCacheFactory = Utils.getComponent(CacheFactory.class, localCacheHint);
        }

        return localCacheFactory;
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
        try {
            return getStore().getQueryManager().getNamedQuery("getSpaces").execute();
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    public List<String> getSpaceDocsName(String spaceName, XWikiContext context) throws XWikiException
    {
        try {
            return getStore().getQueryManager().getNamedQuery("getSpaceDocsName").bindValue("space", spaceName)
                .execute();
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    public List<String> getIncludedMacros(String defaultSpace, String content, XWikiContext context)
    {
        List<String> list;

        try {
            String pattern = "#includeMacros[ ]*\\([ ]*([\"'])(.*?)\\1[ ]*\\)";
            list = context.getUtil().getUniqueMatches(content, pattern, 2);
            for (int i = 0; i < list.size(); i++) {
                String name = list.get(i);
                if (name.indexOf('.') == -1) {
                    list.set(i, defaultSpace + "." + name);
                }
            }
        } catch (Exception e) {
            // This should never happen
            LOGGER.error("Failed to extract #includeMacros targets from provided content [" + content + "]", e);

            list = Collections.emptyList();
        }

        return list;
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
        try {
            // refreshes all Links of each doc of the wiki
            List<String> docs = getStore().getQueryManager().getNamedQuery("getAllDocuments").execute();
            for (int i = 0; i < docs.size(); i++) {
                XWikiDocument myDoc = this.getDocument(docs.get(i), context);
                myDoc.getStore().saveLinks(myDoc, context, true);
            }
        } catch (QueryException ex) {
            throw new XWikiException(0, 0, ex.getMessage(), ex);
        }
    }

    public boolean hasBacklinks(XWikiContext context)
    {
        if (this.hasBacklinks == null) {
            this.hasBacklinks = "1".equals(getXWikiPreference("backlinks", "xwiki.backlinks", "0", context));
        }
        return this.hasBacklinks;
    }

    public boolean hasTags(XWikiContext context)
    {
        return "1".equals(getXWikiPreference("tags", "xwiki.tags", "0", context));
    }

    public boolean hasCustomMappings()
    {
        return "1".equals(Param("xwiki.store.hibernate.custommapping", "1"));
    }

    public boolean hasDynamicCustomMappings()
    {
        return "1".equals(Param("xwiki.store.hibernate.custommapping.dynamic", "0"));
    }

    public String getDefaultSpace(XWikiContext context)
    {
        String defaultSpace = getXWikiPreference("defaultweb", "", context);
        if (StringUtils.isEmpty(defaultSpace)) {
            return Param("xwiki.defaultweb", DEFAULT_HOME_SPACE);
        }
        return defaultSpace;
    }

    public boolean skipDefaultSpaceInURLs(XWikiContext context)
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
        String defaultPage = getXWikiPreference("defaultpage", "", context);
        if (StringUtils.isEmpty(defaultPage)) {
            return Param("xwiki.defaultpage", DEFAULT_HOME_SPACE);
        }
        return defaultPage;
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

    /**
     * @since 2.2M2
     */
    public BaseClass getXClass(DocumentReference documentReference, XWikiContext context) throws XWikiException
    {
        // Used to avoid recursive loading of documents if there are recursives usage of classes
        BaseClass bclass = context.getBaseClass(documentReference);
        if (bclass != null) {
            return bclass;
        }

        return getDocument(documentReference, context).getXClass();
    }

    /**
     * @deprecated since 2.2M2 use {@link #getXClass(DocumentReference, XWikiContext)}
     */
    @Deprecated
    public BaseClass getClass(String fullName, XWikiContext context) throws XWikiException
    {
        DocumentReference reference = null;
        if (StringUtils.isNotEmpty(fullName)) {
            reference = this.currentMixedDocumentReferenceResolver.resolve(fullName);
        }
        return getXClass(reference, context);
    }

    public String getEditorPreference(XWikiContext context)
    {
        String pref = getUserPreference("editor", context);
        if (pref.equals("---")) {
            pref = getSpacePreference("editor", context);
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
        return parseGroovyFromString(context.getWiki().getDocument(fullName, context).getContent(), jarWikiPage,
            context);
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
        if (StringUtils.isNotBlank(context.getWiki().getXWikiPreference("convertmail", context))) {
            return context.getWiki().getXWikiPreference("convertmail", "0", context);
        }

        return context.getWiki().Param("xwiki.authentication.convertemail", "0");
    }

    public String convertUsername(String username, XWikiContext context)
    {
        if (username == null) {
            return null;
        }

        if (getConvertingUserNameType(context).equals("1") && (username.indexOf('@') != -1)) {
            String id = "" + username.hashCode();
            id = id.replace("-", "");
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

    /**
     * @return The maximum section depth for which section editing is available. This can be customized through the
     *         {@code xwiki.section.depth} configuration property. Defaults to 2 when not defined.
     */
    public long getSectionEditingDepth()
    {
        return ParamAsLong("xwiki.section.depth", 2);
    }

    public boolean hasCaptcha(XWikiContext context)
    {
        return (getXWikiPreferenceAsInt("captcha_enabled", "xwiki.plugin.captcha", 0, context) == 1);
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
            temp.replaceAll(
                "[\u00d2\u00d3\u00d4\u00d5\u00d6\u00d8\u014c\u014e\u0150\u01d1\u01ea\u01ec\u01fe\u020c\u020e\u022a\u022c"
                    + "\u022e\u0230]", "O");
        temp =
            temp.replaceAll(
                "[\u00f2\u00f3\u00f4\u00f5\u00f6\u00f8\u014d\u014f\u0151\u01d2\u01eb\u01ed\u01ff\u020d\u020f\u022b\u022d"
                    + "\u022f\u0231]", "o");
        temp = temp.replaceAll("[\u0156\u0158\u0210\u0212]", "R");
        temp = temp.replaceAll("[\u0157\u0159\u0211\u0213]", "r");
        temp = temp.replaceAll("[\u015a\u015c\u015e\u0160\u0218]", "S");
        temp = temp.replaceAll("[\u015b\u015d\u015f\u0161\u0219]", "s");
        temp = temp.replaceAll("[\u00de\u0162\u0164\u0166\u021a]", "T");
        temp = temp.replaceAll("[\u00fe\u0163\u0165\u0167\u021b\u0236]", "t");
        temp =
            temp.replaceAll(
                "[\u00d9\u00da\u00db\u00dc\u0168\u016a\u016c\u016e\u0170\u0172\u01d3\u01d5\u01d7\u01d9\u01db\u0214\u0216]",
                "U");
        temp =
            temp.replaceAll(
                "[\u00f9\u00fa\u00fb\u00fc\u0169\u016b\u016d\u016f\u0171\u0173\u01d4\u01d6\u01d8\u01da\u01dc\u0215\u0217]",
                "u");
        temp = temp.replaceAll("[\u0174]", "W");
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

    public PropertyClass getPropertyClassFromName(String propPath, XWikiContext context)
    {
        int i1 = propPath.indexOf('_');
        if (i1 == -1) {
            return null;
        } else {
            String className = propPath.substring(0, i1);
            String propName = propPath.substring(i1 + 1);
            try {
                return (PropertyClass) getDocument(className, context).getXClass().get(propName);
            } catch (XWikiException e) {
                return null;
            }
        }
    }

    public boolean validateDocument(XWikiDocument doc, XWikiContext context) throws XWikiException
    {
        return doc.validate(context);
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

    /**
     * @since 2.3M1
     */
    public boolean hasVersioning(XWikiContext context)
    {
        return ("1".equals(context.getWiki().Param("xwiki.store.versioning", "1")));
    }

    /**
     * @deprecated since 2.3M1 use {@link #hasVersioning(XWikiContext)} instead
     */
    @Deprecated
    public boolean hasVersioning(String fullName, XWikiContext context)
    {
        return hasVersioning(context);
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
                LOGGER.warn("xwiki.temp.dir set in xwiki.cfg : " + dirPath + " does not exist or is not writable", e);
            }
        }

        Object jsct = context.getEngineContext().getAttribute("javax.servlet.context.tempdir");

        // javax.servlet.context.tempdir (File)
        if (jsct != null && (jsct instanceof File)) {
            tempDir = (File) jsct;
            if (tempDir.isDirectory() && tempDir.canWrite()) {
                return tempDir;
            }
        }

        // javax.servlet.context.tempdir (String)
        if (jsct != null && (jsct instanceof String)) {
            tempDir = new File((String) jsct);

            if (tempDir.isDirectory() && tempDir.canWrite()) {
                return tempDir;
            }
        }

        // Let's make a tempdir in java.io.tmpdir
        tempDir = new File(System.getProperty("java.io.tmpdir"), "xwikiTemp");

        if (tempDir.exists()) {
            tempDir.deleteOnExit();
        } else {
            tempDir.mkdir();
            tempDir.deleteOnExit();
        }

        return tempDir;
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
                LOGGER.warn("xwiki.work.dir set in xwiki.cfg : " + dirPath + " does not exist or is not writable", e);

                workDir = null;
            }
        }

        // No choices left, retreiving the temp directory
        this.workDir = this.getTempDirectory(context);

        return this.workDir;
    }

    public XWikiDocument rollback(final XWikiDocument tdoc, String rev, XWikiContext context) throws XWikiException
    {
        LOGGER.debug("Rolling back [" + tdoc + "] to version " + rev);
        // Let's clone rolledbackDoc since we might modify it
        XWikiDocument rolledbackDoc = getDocument(tdoc, rev, context).clone();

        if ("1".equals(context.getWiki().Param("xwiki.store.rollbackattachmentwithdocuments", "1"))) {
            // Attachment handling strategy:
            // - Two lists: Old Attachments, Current Attachments
            // Goals:
            // 1. Attachments that are only in OA must be restored from the trash
            // 2. Attachments that are only in CA must be sent to the trash
            // 3. Attachments that are in both lists should be reverted to the right version
            // 4. Gotcha: deleted and re-uploaded attachments should be both trashed and restored.
            // Plan:
            // - Construct three lists: to restore, to delete, to revert
            // - Iterate over OA.
            // -- If the attachment is not in CA, add it to the restore list
            // -- If it is in CA, but the date of the first version of the current attachment is after the date of the
            // restored document version, add it to both the restore & delete lists
            // -- Otherwise, add it to the revert list
            // - Iterate over CA
            // -- If the attachment is not in OA, add it to the delete list

            List<XWikiAttachment> oldAttachments = rolledbackDoc.getAttachmentList();
            List<XWikiAttachment> currentAttachments = tdoc.getAttachmentList();
            List<XWikiAttachment> toRestore = new ArrayList<XWikiAttachment>();
            List<XWikiAttachment> toTrash = new ArrayList<XWikiAttachment>();
            List<XWikiAttachment> toRevert = new ArrayList<XWikiAttachment>();

            // First step, determine what to do with each attachment
            LOGGER.debug("Checking attachments");

            for (XWikiAttachment oldAttachment : oldAttachments) {
                String filename = oldAttachment.getFilename();
                XWikiAttachment equivalentAttachment = tdoc.getAttachment(filename);
                if (equivalentAttachment == null) {
                    // Deleted attachment
                    LOGGER.debug("Deleted attachment: " + filename);
                    toRestore.add(oldAttachment);
                    continue;
                }
                XWikiAttachment equivalentAttachmentRevision =
                    equivalentAttachment.getAttachmentRevision(oldAttachment.getVersion(), context);
                if (equivalentAttachmentRevision == null
                    || !equivalentAttachmentRevision.getDate().equals(oldAttachment.getDate())) {
                    // Recreated attachment
                    LOGGER.debug("Recreated attachment: " + filename);
                    // If the attachment trash is not available, don't lose the existing attachment
                    if (getAttachmentRecycleBinStore() != null) {
                        toTrash.add(equivalentAttachment);
                        toRestore.add(oldAttachment);
                    }
                    continue;
                }
                if (!StringUtils.equals(oldAttachment.getVersion(), equivalentAttachment.getVersion())) {
                    // Updated attachment
                    LOGGER.debug("Updated attachment: " + filename);
                    toRevert.add(equivalentAttachment);
                }
            }
            for (XWikiAttachment attachment : currentAttachments) {
                if (rolledbackDoc.getAttachment(attachment.getFilename()) == null) {
                    LOGGER.debug("New attachment: " + attachment.getFilename());
                    toTrash.add(attachment);
                }
            }

            // Second step, treat each affected attachment

            // Delete new attachments
            if (context.getWiki().hasAttachmentRecycleBin(context)) {
                for (XWikiAttachment attachmentToDelete : toTrash) {
                    // Nothing needed for the reverted document, but let's send the extra attachments to the trash
                    context.getWiki().getAttachmentRecycleBinStore()
                        .saveToRecycleBin(attachmentToDelete, context.getUser(), new Date(), context, true);
                }
            }

            // Revert updated attachments to the old version
            for (XWikiAttachment attachmentToRevert : toRevert) {
                String oldAttachmentVersion =
                    rolledbackDoc.getAttachment(attachmentToRevert.getFilename()).getVersion();
                XWikiAttachment oldAttachmentRevision =
                    attachmentToRevert.getAttachmentRevision(oldAttachmentVersion, context);
                if (oldAttachmentRevision == null) {
                    // Previous version is lost, just leave the current version in place
                    replaceAttachmentInPlace(rolledbackDoc, attachmentToRevert);
                    continue;
                }
                // We can't just leave the old version in place, since it will break the revision history, given the
                // current implementation, so we set the attachment version to the most recent version, mark the content
                // as dirty, and the storage will automatically bump up the version number.
                // This is a hack, to be fixed once the storage doesn't take care of updating the history and version,
                // and once the current attachment version can point to an existing version from the history.
                oldAttachmentRevision.setVersion(attachmentToRevert.getVersion());
                oldAttachmentRevision.setMetaDataDirty(true);
                oldAttachmentRevision.getAttachment_content().setContentDirty(true);
                replaceAttachmentInPlace(rolledbackDoc, oldAttachmentRevision);
            }

            // Restore deleted attachments from the trash
            if (getAttachmentRecycleBinStore() != null) {
                for (XWikiAttachment attachmentToRestore : toRestore) {
                    // There might be multiple versions of the attachment in the trash, search for the right one
                    List<DeletedAttachment> deletedVariants =
                        getAttachmentRecycleBinStore().getAllDeletedAttachments(attachmentToRestore, context, true);
                    DeletedAttachment correctVariant = null;
                    for (DeletedAttachment variant : deletedVariants) { // Reverse chronological order
                        if (variant.getDate().before(rolledbackDoc.getDate())) {
                            break;
                        }
                        correctVariant = variant;
                    }
                    if (correctVariant == null) {
                        // Not found in the trash, nothing left to do
                        continue;
                    }
                    XWikiAttachment restoredAttachment = correctVariant.restoreAttachment(null, context);
                    XWikiAttachment restoredAttachmentRevision =
                        restoredAttachment.getAttachmentRevision(attachmentToRestore.getVersion(), context);

                    if (restoredAttachmentRevision != null) {
                        restoredAttachmentRevision.setAttachment_archive(restoredAttachment.getAttachment_archive());
                        restoredAttachmentRevision.getAttachment_archive().setAttachment(restoredAttachmentRevision);
                        restoredAttachmentRevision.setVersion(restoredAttachment.getVersion());
                        restoredAttachmentRevision.setMetaDataDirty(true);
                        restoredAttachmentRevision.getAttachment_content().setContentDirty(true);
                        replaceAttachmentInPlace(rolledbackDoc, restoredAttachmentRevision);
                    } else {
                        // This particular version is lost, update to the one available
                        replaceAttachmentInPlace(rolledbackDoc, restoredAttachment);
                    }
                }
            } else {
                // No trash, can't restore. Remove the attachment references, so that the document is not broken
                for (XWikiAttachment attachmentToRestore : toRestore) {
                    rolledbackDoc.getAttachmentList().remove(attachmentToRestore);
                }
            }
        }

        // Special treatment for deleted objects
        rolledbackDoc.addXObjectsToRemoveFromVersion(tdoc);

        // now we save the final document..
        rolledbackDoc.setAuthorReference(context.getUserReference());
        rolledbackDoc.setRCSVersion(tdoc.getRCSVersion());
        rolledbackDoc.setVersion(tdoc.getVersion());
        rolledbackDoc.setContentDirty(true);
        List<Object> params = new ArrayList<Object>();
        params.add(rev);

        saveDocument(rolledbackDoc, context.getMessageTool().get("core.comment.rollback", params), context);

        return rolledbackDoc;
    }

    private void replaceAttachmentInPlace(XWikiDocument doc, XWikiAttachment attachment)
    {
        for (ListIterator<XWikiAttachment> it = doc.getAttachmentList().listIterator(); it.hasNext();) {
            if (StringUtils.equals(it.next().getFilename(), attachment.getFilename())) {
                it.remove();
                it.add(attachment);
                break;
            }
        }
    }

    /**
     * @return the ids of configured syntaxes for this wiki (eg "xwiki/1.0", "xwiki/2.0", "mediawiki/1.0", etc)
     */
    public List<String> getConfiguredSyntaxes()
    {
        return this.configuredSyntaxes;
    }

    /**
     * @return the syntax id of the syntax to use when creating new documents.
     */
    public String getDefaultDocumentSyntax()
    {
        // TODO: Fix this method to return a Syntax object instead of a String
        return Utils.getComponent(CoreConfiguration.class).getDefaultDocumentSyntax().toIdString();
    }

    /**
     * Set the fields of the class document passed as parameter. Can generate content for both XWiki Syntax 1.0 and
     * XWiki Syntax 2.0. If new documents are set to be created in XWiki Syntax 1.0 then generate XWiki 1.0 Syntax
     * otherwise generate XWiki Syntax 2.0.
     * 
     * @param title the page title to set
     * @return true if the document has been modified, false otherwise
     */
    private boolean setClassDocumentFields(XWikiDocument doc, String title)
    {
        boolean needsUpdate = false;

        if (StringUtils.isBlank(doc.getCreator())) {
            needsUpdate = true;
            doc.setCreator(XWikiRightService.SUPERADMIN_USER);
        }
        if (StringUtils.isBlank(doc.getAuthor())) {
            needsUpdate = true;
            doc.setAuthor(doc.getCreator());
        }
        if (StringUtils.isBlank(doc.getParent())) {
            needsUpdate = true;
            doc.setParent("XWiki.XWikiClasses");
        }
        if (StringUtils.isBlank(doc.getTitle())) {
            needsUpdate = true;
            doc.setTitle(title);
        }

        // Use ClassSheet to display the class document if no other sheet is explicitly specified.
        SheetBinder documentSheetBinder = Utils.getComponent(SheetBinder.class, "document");
        if (documentSheetBinder.getSheets(doc).isEmpty()) {
            String wikiName = doc.getDocumentReference().getWikiReference().getName();
            DocumentReference sheet = new DocumentReference(wikiName, SYSTEM_SPACE, "ClassSheet");
            needsUpdate |= documentSheetBinder.bind(doc, sheet);
        }

        return needsUpdate;
    }

    /**
     * Get the syntax of the document currently being executed.
     * <p>
     * The document currently being executed is not the same than the context document since when including a page with
     * velocity #includeForm(), method for example the context doc is the includer document even if includeForm() fully
     * execute and render the included document before insert it in the includer document.
     * <p>
     * If the current document can't be found, the method assume that the executed document is the context document
     * (it's generally the case when a document is directly rendered with
     * {@link XWikiDocument#getRenderedContent(XWikiContext)} for example).
     * 
     * @param defaultSyntaxId the default value to return if no document can be found
     * @return the syntax identifier
     */
    public String getCurrentContentSyntaxId(String defaultSyntaxId, XWikiContext context)
    {
        String syntaxId = getCurrentContentSyntaxIdInternal(context);

        if (syntaxId == null) {
            syntaxId = defaultSyntaxId;
        }

        return syntaxId;
    }

    /**
     * Get the syntax of the document currently being executed.
     * <p>
     * The document currently being executed is not the same than the context document since when including a page with
     * velocity #includeForm(), method for example the context doc is the includer document even if includeForm() fully
     * execute and render the included document before insert it in the includer document.
     * <p>
     * If the current document can't be found, the method assume that the executed document is the context document
     * (it's generally the case when a document is directly rendered with
     * {@link XWikiDocument#getRenderedContent(XWikiContext)} for example).
     * 
     * @return the syntax identifier
     */
    public String getCurrentContentSyntaxId(XWikiContext context)
    {
        String syntaxId = getCurrentContentSyntaxIdInternal(context);

        if (syntaxId == null) {
            throw new RuntimeException("Cannot get the current syntax since there's no current document set");
        }

        return syntaxId;
    }

    private String getCurrentContentSyntaxIdInternal(XWikiContext context)
    {
        String syntaxId = null;

        if (context.get("sdoc") != null) {
            // The content document
            syntaxId = ((XWikiDocument) context.get("sdoc")).getSyntax().toIdString();
        } else if (context.getDoc() != null) {
            // The context document
            syntaxId = context.getDoc().getSyntax().toIdString();
        }

        return syntaxId;
    }

    /**
     * @return true if title handling should be using the compatibility mode or not. When the compatibility mode is
     *         active, if the document's content first header (level 1 or level 2) matches the document's title the
     *         first header is stripped.
     */
    public boolean isTitleInCompatibilityMode()
    {
        return "1".equals(Param("xwiki.title.compatibility", "0"));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument doc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        if (event instanceof XObjectPropertyEvent) {
            EntityReference reference = ((XObjectPropertyEvent) event).getReference();
            String modifiedProperty = reference.getName();
            if ("plugins".equals(modifiedProperty)) {
                onPluginPreferenceEvent(event, doc, context);
            } else if ("backlinks".equals(modifiedProperty)) {
                this.hasBacklinks = doc.getXObject((ObjectReference) reference.getParent()).getIntValue("backlinks",
                    (int) ParamAsLong("xwiki.backlinks", 0)) == 1;
            }
        } else if (event instanceof XObjectEvent) {
            onServerObjectEvent(event, doc, context);
        }
    }

    private void onServerObjectEvent(Event event, XWikiDocument doc, XWikiContext context)
    {
        flushVirtualWikis(doc.getOriginalDocument());
        flushVirtualWikis(doc);
    }

    private void onPluginPreferenceEvent(Event event, XWikiDocument doc, XWikiContext context)
    {
        if (!isVirtualMode()) {
            // If the XWikiPreferences plugin propery is modified, reload all plugins.
            preparePlugins(context);
        }
    }

    /**
     * The reference to match class XWiki.XWikiServerClass on whatever wiki.
     */
    private static final RegexEntityReference SERVERCLASS_REFERENCE = new RegexEntityReference(
        Pattern.compile(".*:XWiki.XWikiServerClass\\[\\d*\\]"), EntityType.OBJECT);

    /**
     * The reference to match properties "plugins" and "backlinks" of class XWiki.XWikiPreference on whatever wiki.
     */
    private static final RegexEntityReference XWIKIPREFERENCE_PROPERTY_REFERENCE = new RegexEntityReference(
        Pattern.compile("plugins|backlinks"), EntityType.OBJECT_PROPERTY, new RegexEntityReference(
            Pattern.compile(".*:XWiki.XWikiPreferences\\[\\d*\\]"), EntityType.OBJECT));

    private static final List<Event> LISTENER_EVENTS = new ArrayList<Event>()
    {
        {
            add(new XObjectAddedEvent(SERVERCLASS_REFERENCE));
            add(new XObjectDeletedEvent(SERVERCLASS_REFERENCE));
            add(new XObjectUpdatedEvent(SERVERCLASS_REFERENCE));
            add(new XObjectPropertyAddedEvent(XWIKIPREFERENCE_PROPERTY_REFERENCE));
            add(new XObjectPropertyDeletedEvent(XWIKIPREFERENCE_PROPERTY_REFERENCE));
            add(new XObjectPropertyUpdatedEvent(XWIKIPREFERENCE_PROPERTY_REFERENCE));
        }
    };

    public List<Event> getEvents()
    {
        return LISTENER_EVENTS;
    }

    public String getName()
    {
        return "xwiki-core";
    }
}
