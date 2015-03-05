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

import java.io.UnsupportedEncodingException;
import java.io.File;
import java.lang.reflect.Type;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.io.IOException;

import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.commons.lang3.StringUtils;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.environment.Environment;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.xml.XMLUtils;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.model.EntityType;
import org.xwiki.url.XWikiEntityURL;
import org.apache.velocity.VelocityContext;

import com.xpn.xwiki.cache.api.XWikiCache;
import com.xpn.xwiki.cache.api.XWikiCacheService;
import com.xpn.xwiki.cache.api.internal.XWikiCacheServiceStub;
import com.xpn.xwiki.cache.api.internal.XWikiCacheStub;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.notify.XWikiNotificationManager;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.PropertyClass;
import com.xpn.xwiki.plugin.query.QueryPlugin;
import com.xpn.xwiki.plugin.query.XWikiCriteria;
import com.xpn.xwiki.plugin.query.XWikiQuery;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;

/**
 * Add a backward compatibility layer to the {@link com.xpn.xwiki.XWiki} class.
 * 
 * @version $Id$
 */
public privileged aspect XWikiCompatibilityAspect
{
    private static Map XWiki.threadMap = new HashMap();

    private XWikiNotificationManager XWiki.notificationManager;

    private EntityReferenceResolver<EntityReference> XWiki.defaultReferenceEntityReferenceResolver = Utils.getComponent(
        EntityReferenceResolver.TYPE_REFERENCE);

    private EntityReferenceSerializer<String> XWiki.localStringEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.TYPE_STRING, "local");

    
    /**
     * Used to get the temporary and permanent directory.
     */
    private Environment XWiki.environment = Utils.getComponent((Type) Environment.class);

    /** Is the wiki running in test mode? Deprecated, was used when running Cactus tests. */
    private boolean XWiki.test = false;

    /**
     * Transform a text in a URL compatible text
     *
     * @param content text to transform
     * @return encoded result
     * @deprecated replaced by Util#encodeURI since 1.3M2
     */
    @Deprecated
    public String XWiki.getURLEncoded(String content)
    {
        try {
            return URLEncoder.encode(content, this.getEncoding());
        } catch (UnsupportedEncodingException e) {
            return content;
        }
    }

    /**
     * @return true for multi-wiki/false for mono-wiki
     * @deprecated replaced by {@link XWiki#isVirtualMode()} since 1.4M1.
     */
    @Deprecated
    public boolean XWiki.isVirtual()
    {
        return isVirtualMode();
    }

    /**
     * @deprecated Virtual mode is on by default, starting with XWiki 5.0M2. Use
     *             {@link #getVirtualWikisDatabaseNames(XWikiContext)} to get the list of wikis if needed.
     * @return true for multi-wiki/false for mono-wiki
     */
    @Deprecated
    public boolean XWiki.isVirtualMode()
    {
        return true;
    }

    /**
     * @deprecated Removed since it isn't used; since 1.5M1.
     */
    @Deprecated
    public static Map XWiki.getThreadMap()
    {
        return XWiki.threadMap;
    }

    /**
     * @deprecated Removed since it isn't used; since 1.5M1.
     */
    @Deprecated
    public static void XWiki.setThreadMap(Map threadMap)
    {
        XWiki.threadMap = threadMap;
    }

    /**
     * @return the cache service.
     * @deprecated replaced by {@link XWiki#getCacheFactory(XWikiContext)} or
     *             {@link XWiki#getLocalCacheFactory(XWikiContext)} since 1.5M2.
     */
    @Deprecated
    public XWikiCacheService XWiki.getCacheService()
    {
        return new XWikiCacheServiceStub(getCacheFactory(), getLocalCacheFactory());
    }

    /**
     * @deprecated replaced by {@link XWiki#getVirtualWikiCache(XWikiContext)} since 1.5M2.
     */
    @Deprecated
    public XWikiCache XWiki.getVirtualWikiMap()
    {
        return new XWikiCacheStub(this.virtualWikiMap);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpaceCopyright(XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebCopyright(XWikiContext context)
    {
        return this.getSpaceCopyright(context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreference(String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreference(String preference, XWikiContext context)
    {
        return this.getSpacePreference(preference, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreference(String, String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreference(String preference, String defaultValue, XWikiContext context)
    {
        return this.getSpacePreference(preference, defaultValue, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreference(String, String, String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getWebPreference(String preference, String space, String defaultValue, XWikiContext context)
    {
        return this.getSpacePreference(preference, space, defaultValue, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsLong(String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public long XWiki.getWebPreferenceAsLong(String preference, XWikiContext context)
    {
        return this.getSpacePreferenceAsLong(preference, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsLong(String, long, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public long XWiki.getWebPreferenceAsLong(String preference, long defaultValue, XWikiContext context)
    {
        return this.getSpacePreferenceAsLong(preference, defaultValue, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsInt(String, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public int XWiki.getWebPreferenceAsInt(String preference, XWikiContext context)
    {
        return this.getSpacePreferenceAsInt(preference, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getSpacePreferenceAsInt(String, int, XWikiContext)} since 2.3M1
     */
    @Deprecated
    public int XWiki.getWebPreferenceAsInt(String preference, int defaultValue, XWikiContext context)
    {
        return this.getSpacePreferenceAsInt(preference, defaultValue, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#copySpaceBetweenWikis(String, String, String, String, XWikiContext)} since
     *             2.3M1
     */
    @Deprecated
    public int XWiki.copyWikiWeb(String space, String sourceWiki, String targetWiki, String language, XWikiContext context)
        throws XWikiException
    {
        return this.copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, context);
    }

    /**
     * @deprecated replaced by
     *             {@link XWiki#copySpaceBetweenWikis(String, String, String, String, boolean, XWikiContext)} since
     *             2.3M1
     */
    @Deprecated
    public int XWiki.copyWikiWeb(String space, String sourceWiki, String targetWiki, String language, boolean clean,
        XWikiContext context) throws XWikiException
    {
        return this.copySpaceBetweenWikis(space, sourceWiki, targetWiki, language, clean, context);
    }

    /**
     * @deprecated replaced by {@link XWiki#getDefaultSpace(XWikiContext)} since 2.3M1
     */
    @Deprecated
    public String XWiki.getDefaultWeb(XWikiContext context)
    {
        return this.getDefaultSpace(context);
    }

    /**
     * @deprecated replaced by {@link XWiki#skipDefaultSpaceInURLs(XWikiContext)} since 2.3M1
     */
    @Deprecated
    public boolean XWiki.useDefaultWeb(XWikiContext context)
    {
        return this.skipDefaultSpaceInURLs(context);
    }

    /**
     * @deprecated use {@link XWikiMessageTool#get(String, List)} instead. You can access message tool using
     *             {@link XWikiContext#getMessageTool()}.
     */
    @Deprecated
    public String XWiki.getMessage(String item, XWikiContext context)
    {
        XWikiMessageTool msg = context.getMessageTool();
        if (msg == null) {
            return item;
        } else {
            return msg.get(item);
        }
    }

    /**
     * @deprecated Removed since it isn't used; since 3.1M2.
     */
    @Deprecated
    public String XWiki.getHTMLArea(String content, XWikiContext context)
    {
        StringBuilder result = new StringBuilder();

        String scontent = XMLUtils.escape(content);
        scontent = scontent.replaceAll("\r?+\n", "<br class=\"htmlarea\"/>");

        result.append("<textarea name=\"content\" id=\"content\" rows=\"25\" cols=\"80\">");
        result.append(scontent);
        result.append("</textarea>");

        return result.toString();
    }

    @Deprecated
    public XWikiNotificationManager XWiki.getNotificationManager()
    {
        if (this.notificationManager == null) {
          this.notificationManager = new XWikiNotificationManager();
        }

        return this.notificationManager;
    }

    @Deprecated
    public void XWiki.setNotificationManager(XWikiNotificationManager notificationManager)
    {
        this.notificationManager = notificationManager;
    }
    
    @Deprecated
    public String XWiki.displaySearch(String fieldname, String className, XWikiCriteria criteria, XWikiContext context)
        throws XWikiException
    {
        return displaySearch(fieldname, className, "", criteria, context);
    }

    @Deprecated
    public String XWiki.displaySearch(String fieldname, String className, XWikiContext context) throws XWikiException
    {
        return displaySearch(fieldname, className, "", new XWikiCriteria(), context);
    }

    @Deprecated
    public String XWiki.displaySearch(String fieldname, String className, String prefix, XWikiCriteria criteria,
        XWikiContext context) throws XWikiException
    {
        BaseClass bclass = getDocument(className, context).getXClass();
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

    @Deprecated
    public String XWiki.displaySearchColumns(String className, XWikiQuery query, XWikiContext context) throws XWikiException
    {
        return displaySearchColumns(className, "", query, context);
    }

    @Deprecated
    public String XWiki.displaySearchColumns(String className, String prefix, XWikiQuery query, XWikiContext context)
        throws XWikiException
    {
        BaseClass bclass = getDocument(className, context).getXClass();

        if (query == null) {
            query = new XWikiQuery();
        }

        return bclass.displaySearchColumns(className + "_" + prefix, query, context);
    }

    @Deprecated
    public String XWiki.displaySearchOrder(String className, XWikiQuery query, XWikiContext context) throws XWikiException
    {
        return displaySearchOrder(className, "", query, context);
    }

    @Deprecated
    public String XWiki.displaySearchOrder(String className, String prefix, XWikiQuery query, XWikiContext context)
        throws XWikiException
    {
        BaseClass bclass = getDocument(className, context).getXClass();

        if (query == null) {
            query = new XWikiQuery();
        }

        return bclass.displaySearchOrder(className + "_" + prefix, query, context);
    }

    @Deprecated
    public <T> List<T> XWiki.search(XWikiQuery query, XWikiContext context) throws XWikiException
    {
        QueryPlugin qp = (QueryPlugin) getPlugin("query", context);
        if (qp == null) {
            return null;
        }

        return qp.search(query);
    }

    @Deprecated
    public XWikiQuery XWiki.createQueryFromRequest(String className, XWikiContext context) throws XWikiException
    {
        return new XWikiQuery(context.getRequest(), className, context);
    }

    @Deprecated
    public String XWiki.searchAsTable(XWikiQuery query, XWikiContext context) throws XWikiException
    {
        QueryPlugin qp = (QueryPlugin) getPlugin("query", context);
        if (qp == null) {
            return null;
        }

        List<String> list = qp.search(query);
        String result = "{table}\r\n";
        List<String> headerColumns = new ArrayList<String>();
        List<String> displayProperties = query.getDisplayProperties();
        for (String propname : displayProperties) {
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
        for (String docname : list) {
            List<String> rowColumns = new ArrayList<String>();
            XWikiDocument doc = getDocument(docname, context);
            for (String propname : displayProperties) {
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

    @Deprecated
    public String XWiki.getDocLanguagePreference(XWikiContext context)
    {
        return getLanguagePreference(context);
    }

    @Deprecated
    public void XWiki.flushCache()
    {
        Execution execution = Utils.getComponent(Execution.class);

        ExecutionContext ec = execution.getContext();

        flushCache(ec != null ? (XWikiContext) ec.getProperty("xwikicontext") : null);
    }

    /**
     * @deprecated use WikiManager plugin instead
     */
    @Deprecated
    public int XWiki.createNewWiki(String wikiName, String wikiUrl, String wikiAdmin, String baseWikiName,
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

    /**
     * Get the XWiki temporary filesystem directory (cleaned up automatically by XWiki).
     *
     * @param context
     * @return temporary directory
     * @since 1.1 Milestone 4
     * @deprecated starting with 4.2M1 use {@link org.xwiki.environment.Environment#getTemporaryDirectory()}
     */
    @Deprecated
    public File XWiki.getTempDirectory(XWikiContext context)
    {
        return this.environment.getTemporaryDirectory();
    }

    /**
     * Get a new directory in the xwiki work directory
     *
     * @param subdir desired directory name
     * @param context
     * @return work subdirectory
     * @since 1.1 Milestone 4
     * @deprecated starting with 4.2M1 use {@link org.xwiki.environment.Environment#getPermanentDirectory()}
     */
    @Deprecated
    public File XWiki.getWorkSubdirectory(String subdir, XWikiContext context)
    {
        File fdir = new File(this.environment.getPermanentDirectory().getAbsolutePath(), subdir);
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
     * @deprecated starting with 4.2M1 use {@link org.xwiki.environment.Environment#getPermanentDirectory()}
     */
    @Deprecated
    public File XWiki.getWorkDirectory(XWikiContext context)
    {
        return this.environment.getPermanentDirectory();
    }

    /**
     * @deprecated starting with 5.1M1 use {@code org.xwiki.url.XWikiURLFactory} instead and starting with 5.3M1 use
     *             {@link ResourceFactory} and since 6.1M2 use {@link org.xwiki.resource.ResourceReferenceResolver}
     */
    @Deprecated
    public XWikiDocument XWiki.getDocumentFromPath(String path, XWikiContext context) throws XWikiException
    {
        return getDocument(getDocumentReferenceFromPath(path, context), context);
    }

    /**
     * @since 2.3M1
     * @deprecated starting with 5.1M1 use {@code org.xwiki.url.XWikiURLFactory} instead and starting with 5.3M1 use
     *             {@link ResourceFactory} and since 6.1M2 use {@link org.xwiki.resource.ResourceReferenceResolver}
     */
    @Deprecated
    public DocumentReference XWiki.getDocumentReferenceFromPath(String path, XWikiContext context)
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

        XWikiEntityURL entityURL = buildEntityURLFromPathSegments(new WikiReference(context.getDatabase()), segments);

        return new DocumentReference(entityURL.getEntityReference().extractReference(EntityType.DOCUMENT));
    }

    /**
     * @deprecated since 2.3M1 use {@link #getDocumentReferenceFromPath(String, XWikiContext)} instead
     */
    @Deprecated
    public String XWiki.getDocumentNameFromPath(String path, XWikiContext context)
    {
        return this.localStringEntityReferenceSerializer.serialize(getDocumentReferenceFromPath(path, context));
    }

    /**
     * @deprecated since 2.3M1 use {@link #getDocumentReferenceFromPath(String, XWikiContext)} instead
     */
    @Deprecated
    public String XWiki.getDocumentName(XWikiRequest request, XWikiContext context)
    {
        return this.localStringEntityReferenceSerializer.serialize(getDocumentReference(request, context));
    }

    /**
     * @deprecated starting with 5.1M1 use {@code org.xwiki.url.XWikiURLFactory} instead and starting with 5.3M1 use
     *             {@link ResourceFactory} and since 6.1M2 use {@link org.xwiki.resource.ResourceReferenceResolver}
     */
    @Deprecated
    private XWikiEntityURL XWiki.buildEntityURLFromPathSegments(WikiReference wikiReference, List<String> pathSegments)
    {
        XWikiEntityURL entityURL;

        // Rules based on counting the url segments:
        // - 0 segments (e.g. ""): default document reference, "view" action
        // - 1 segment (e.g. "/", "/Document"): default space, specified document (and default if empty), "view" action
        // - 2 segments (e.g. "/Space/", "/Space/Document"): specified space, document (and default doc if empty),
        //   "view" action
        // - 3 segments (e.g. "/action/Space/Document"): specified space, document (and default doc if empty),
        //   specified action
        // - 4 segments (e.g. "/download/Space/Document/attachment"): specified space, document and attachment (and
        //   default doc if empty), "download" action
        // - 4 segments or more (e.g. "/action/Space/Document/whatever/else"): specified space, document (and default
        //     doc if empty), specified "action" (if action != "download"), trailing segments ignored

        String spaceName = null;
        String pageName = null;
        String attachmentName = null;
        String action = "view";

        if (pathSegments.size() == 1) {
            pageName = pathSegments.get(0);
        } else if (pathSegments.size() == 2) {
            spaceName = pathSegments.get(0);
            pageName = pathSegments.get(1);
        } else if (pathSegments.size() >= 3) {
            action = pathSegments.get(0);
            spaceName = pathSegments.get(1);
            pageName = pathSegments.get(2);
            if (action.equals("download") && pathSegments.size() >= 4) {
                attachmentName = pathSegments.get(3);
            }
        }

        // Normalize the extracted space/page to resolve empty/null values and replace them with default values.
        EntityReference reference = wikiReference;
        EntityType entityType = EntityType.DOCUMENT;
        if (!StringUtils.isEmpty(spaceName)) {
            reference = new EntityReference(spaceName, EntityType.SPACE, reference);
        }
        if (!StringUtils.isEmpty(pageName)) {
            reference = new EntityReference(pageName, EntityType.DOCUMENT, reference);
        }
        if (!StringUtils.isEmpty(attachmentName)) {
            reference = new EntityReference(attachmentName, EntityType.ATTACHMENT, reference);
            entityType = EntityType.ATTACHMENT;
        }
        reference = this.defaultReferenceEntityReferenceResolver.resolve(reference, entityType);

        entityURL = new XWikiEntityURL(reference);
        entityURL.setAction(action);

        return entityURL;
    }

    /**
     * Extracts the name of the wiki from a context's request. In some cases, including autowww, the main wiki may be
     * returned instead of what was requested, as a result of some assumptions. Even so, the resulting wiki name is not
     * guaranteed to exist, it is just what XWiki understood from the request.
     *
     * @param context the context which contains the request
     * @return the name of the wiki that was requested
     * @throws XWikiException if problems occur
     * @deprecated starting with 5.2M1 use  use {@link ResourceFactory} instead and since 6.1M2 use
     *             {@link org.xwiki.resource.ResourceReferenceResolver} instead
     */
    @Deprecated
    public String XWiki.getRequestWikiName(XWikiContext context) throws XWikiException
    {
        // Host is full.host.name in DNS-based multiwiki, and wikiname in path-based multiwiki.
        String host = "";
        // Canonical name of the wiki (database).
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
        if ("1".equals(this.Param("xwiki.virtual.usepath", "1"))) {
            String uri = request.getRequestURI();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Request uri is: " + uri);
            }
            // Remove the (eventual) context path from the URI, usually /xwiki
            uri = stripSegmentFromPath(uri, request.getContextPath());
            // Remove the (eventual) servlet path from the URI, usually /wiki
            String servletPath = request.getServletPath();
            uri = stripSegmentFromPath(uri, servletPath);

            if (servletPath.equals("/" + this.Param("xwiki.virtual.usepath.servletpath", "wiki"))) {
                // Requested path corresponds to a path-based wiki, now the wiki name is between the first and
                // second "/"
                host = StringUtils.substringBefore(StringUtils.removeStart(uri, "/"), "/");
            }
        }

        if (StringUtils.isEmpty(host) || host.equals(context.getMainXWiki())) {
            // Can't find any wiki name, return the main wiki
            return context.getMainXWiki();
        }

        // Try to use the full domain name/path wiki name and see if it corresponds to any existing wiki descriptors
        wikiDefinition = this.findWikiServer(host, context);

        if (wikiDefinition == null) {
            // No definition found based on the full domain name/path wiki name, try to use the first part of the domain
            // name as the wiki name
            String servername = StringUtils.substringBefore(host, ".");

            // Note: Starting 5.0M2, the autowww behavior is default and the ability to disable it is now removed.
            if ("0".equals(this.Param("xwiki.virtual.autowww"))) {
                LOGGER.warn(String.format("%s %s", "'xwiki.virtual.autowww' is no longer supported.",
                    "Please update your configuration and/or see XWIKI-8877 for more details."));
            }

            // As a convenience, we do not require the creation of an xwiki:XWiki.XWikiServerXwiki page for the main
            // wiki and automatically go to the main wiki in certain cases:
            // - "www.<anyDomain>.<domainExtension>" - if it starts with www, we first check if a subwiki with that
            // name exists; if yes, the go to the "www" subwiki, if not, go to the main wiki
            // - "localhost"
            // - IP address
            if ("www".equals(servername)) {
                // Check that "www" is not actually the name of an existing subwiki.
                wikiDefinition = this.findWikiServer(servername, context);
                if (wikiDefinition == null) {
                    // Not the case, use the main wiki.
                    return context.getMainXWiki();
                }
            } else if ("localhost".equals(host) || host.matches("[0-9]{1,3}(?:\\.[0-9]{1,3}){3}")) {
                // Direct access to the main wiki.
                return context.getMainXWiki();
            }

            // Use the name from the subdomain
            wikiName = servername;

            if (!context.isMainWiki(wikiName)
                && !"1".equals(context.getWiki().Param("xwiki.virtual.failOnWikiDoesNotExist", "0"))) {
                // Check if the wiki really exists
                if (!exists(getServerWikiPage(wikiName), context)) {
                    // Fallback on main wiki
                    wikiName = context.getMainXWiki();
                }
            }
        } else {
            // Use the name from the located wiki descriptor
            wikiName = StringUtils.removeStart(wikiDefinition.getName(), "XWikiServer").toLowerCase();
        }

        return wikiName;
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
    private DocumentReference XWiki.findWikiServer(String host, XWikiContext context) throws XWikiException
    {
        ensureVirtualWikiMapExists();
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

    private void XWiki.ensureVirtualWikiMapExists() throws XWikiException
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

    /**
     * @deprecated replaced by {@link #setUserDefaultGroup(String fullwikiname, XWikiContext context)}
     * @param context
     * @param fullwikiname
     * @throws XWikiException
     */
    @Deprecated
    public void XWiki.SetUserDefaultGroup(XWikiContext context, String fullwikiname) throws XWikiException
    {
        this.setUserDefaultGroup(fullwikiname, context);
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
    public void XWiki.ProtectUserPage(XWikiContext context, String fullwikiname, String userRights, XWikiDocument doc)
        throws XWikiException
    {
        this.protectUserPage(fullwikiname, userRights, doc, context);
    }

    /**
     * @return the cache factory.
     * @since 1.5M2.
     * @deprecated Since 1.7M1, use {@link CacheManager} component instead using {@link Utils#getComponent(Class)}
     */
    @Deprecated
    public CacheFactory XWiki.getCacheFactory()
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
    public CacheFactory XWiki.getLocalCacheFactory()
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

    /**
     * @deprecated since 2.3M1 use {@link #hasVersioning(XWikiContext)} instead
     */
    @Deprecated
    public boolean XWiki.hasVersioning(String fullName, XWikiContext context)
    {
        return hasVersioning(context);
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
    public String XWiki.include(String topic, XWikiContext context, boolean isForm) throws XWikiException
    {
        return include(topic, isForm, context);
    }
    
    /**
     * Checks if the wiki is running in test mode.
     * 
     * @return {@code true} if the wiki is running Cactus tests, {@code false} otherwise
     * @deprecated No longer used.
     */
    @Deprecated
    public boolean XWiki.isTest()
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
    public void XWiki.setTest(boolean test)
    {
        this.test = test;
    }

    /**
     * @deprecated use {@link org.xwiki.localization.LocalizationManager} instead. From velocity you can access it using
     *             the {@code $services.localization} binding, see {@code LocalizationScriptService}
     */
    @Deprecated
    public String XWiki.parseMessage(XWikiContext context)
    {
        String message = (String) context.get("message");
        if (message == null) {
            return null;
        }

        return parseMessage(message, context);
    }

    /**
     * @deprecated use {@link org.xwiki.localization.LocalizationManager} instead. From velocity you can access it using
     *             the {@code $services.localization} binding, see {@code LocalizationScriptService}
     */
    @Deprecated
    public String XWiki.parseMessage(String id, XWikiContext context)
    {
        XWikiMessageTool msg = context.getMessageTool();

        List<?> parameters = (List<?>) context.get("messageParameters");

        String translatedMessage;
        if (parameters != null) {
            translatedMessage = msg.get(id, parameters);
        } else {
            translatedMessage = msg.get(id);
        }

        return parseContent(translatedMessage, context);
    }

    /**
     * @deprecated starting with 6.1M2 this method shouldn't be used. There's no replacement, it's just not the right
     *             way to do this anymore and the flash.vm template doesn't exist anymore
     */
    @Deprecated
    public String XWiki.getFlash(String url, String width, String height, XWikiContext context)
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
     * @deprecated replaced by the <a href="http://code.xwiki.org/xwiki/bin/view/Plugins/MailSenderPlugin">Mail Sender
     *             Plugin</a>
     */
    @Deprecated
    public void XWiki.sendMessage(String sender, String[] recipients, String rawMessage, XWikiContext context)
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
    private void XWiki.sendMessageOld(String sender, String[] recipient, String message, XWikiContext context)
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
    public void XWiki.sendMessage(String sender, String recipient, String message, XWikiContext context)
        throws XWikiException
    {
        String[] recip = recipient.split(",");
        sendMessage(sender, recip, message, context);
    }

    /**
     * @deprecated since 7.0M1. This method should have actually been deprecated since 2.3M1, but it was left forgotten
     *             and unused.
     */
    @Deprecated
    public boolean XWiki.hasCaptcha(XWikiContext context)
    {
        return (this.getXWikiPreferenceAsInt("captcha_enabled", "xwiki.plugin.captcha", 0, context) == 1);
    }
}
