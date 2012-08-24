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

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.xmlrpc.server.XmlRpcServer;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.util.Util;
import com.xpn.xwiki.validation.XWikiValidationStatus;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiEngineContext;
import com.xpn.xwiki.web.XWikiForm;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;
import com.xpn.xwiki.web.XWikiResponse;
import com.xpn.xwiki.web.XWikiURLFactory;

public class XWikiContext extends Hashtable<Object, Object>
{
    public static final int MODE_SERVLET = 0;

    public static final int MODE_PORTLET = 1;

    public static final int MODE_XMLRPC = 2;

    public static final int MODE_ATOM = 3;

    public static final int MODE_PDF = 4;

    public static final int MODE_GWT = 5;

    public static final int MODE_GWT_DEBUG = 6;

    public static final String EXECUTIONCONTEXT_KEY = "xwikicontext";

    private static final String WIKI_KEY = "wiki";

    private static final String ORIGINAL_WIKI_KEY = "originalWiki";

    private static final String USER_KEY = "user";

    private static final String USERREFERENCE_KEY = "userreference";

    private boolean finished = false;

    private XWiki wiki;

    private XWikiEngineContext engine_context;

    private XWikiRequest request;

    private XWikiResponse response;

    private XWikiForm form;

    private String action;

    private String orig_database;

    private String database;

    private DocumentReference userReference;

    private String language;

    private static final String LANGUAGE_KEY = "language";

    private String interfaceLanguage;

    private int mode;

    private URL url;

    private XWikiURLFactory URLFactory;

    private XmlRpcServer xmlRpcServer;

    private String wikiOwner;

    private XWikiDocument wikiServer;

    private int cacheDuration = 0;

    private int classCacheSize = 20;

    // Used to avoid recursive loading of documents if there are recursives usage of classes
    // FIXME: why synchronized since a context is supposed to be tied to a thread ?
    @SuppressWarnings("unchecked")
    private Map<DocumentReference, BaseClass> classCache = Collections.synchronizedMap(new LRUMap(this.classCacheSize));

    // FIXME: why synchronized since a context is supposed to be tied to a thread ?
    private List<String> displayedFields = Collections.synchronizedList(new ArrayList<String>());

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver = Utils.getComponent(
        DocumentReferenceResolver.TYPE_STRING, "currentmixed");

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> localEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.TYPE_STRING, "local");

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part if it matches the current
     * wiki).
     */
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer = Utils.getComponent(
        EntityReferenceSerializer.TYPE_STRING, "compactwiki");

    /** The Execution so that we can check if permissions were dropped there. */
    private final Execution execution = Utils.getComponent(Execution.class);

    public XWikiContext()
    {
    }

    public XWiki getWiki()
    {
        return this.wiki;
    }

    public Util getUtil()
    {
        Util util = (Util) this.get("util");
        if (util == null) {
            util = new Util();
            this.put("util", util);
        }

        return util;
    }

    public void setWiki(XWiki wiki)
    {
        this.wiki = wiki;
    }

    public XWikiEngineContext getEngineContext()
    {
        return this.engine_context;
    }

    public void setEngineContext(XWikiEngineContext engine_context)
    {
        this.engine_context = engine_context;
    }

    public XWikiRequest getRequest()
    {
        return this.request;
    }

    public void setRequest(XWikiRequest request)
    {
        this.request = request;
    }

    public String getAction()
    {
        return this.action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public XWikiResponse getResponse()
    {
        return this.response;
    }

    public void setResponse(XWikiResponse response)
    {
        this.response = response;
    }

    public String getDatabase()
    {
        return this.database;
    }

    public void setDatabase(String database)
    {
        this.database = database;
        if (database == null) {
            super.remove(WIKI_KEY);
        } else {
            super.put(WIKI_KEY, database);
        }
        if (this.orig_database == null) {
            this.orig_database = database;
            if (database == null) {
                super.remove(ORIGINAL_WIKI_KEY);
            } else {
                super.put(ORIGINAL_WIKI_KEY, database);
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Make sure to keep {@link #database} fields and map synchronized.
     * 
     * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public synchronized Object put(Object key, Object value)
    {
        Object previous;

        if (WIKI_KEY.equals(key)) {
            previous = get(WIKI_KEY);
            setDatabase((String) value);
        } else {
            previous = super.put(key, value);
        }

        return previous;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Make sure to keep {@link #database} field and map synchronized.
     * 
     * @see java.util.Hashtable#remove(java.lang.Object)
     */
    @Override
    public synchronized Object remove(Object key)
    {
        Object previous;

        if (WIKI_KEY.equals(key)) {
            previous = get(WIKI_KEY);
            setDatabase(null);
        } else {
            previous = super.remove(key);
        }

        return previous;
    }

    /**
     * Get the "original" database name. In single wiki mode this will be "xwiki", but in virtual wiki mode this will be
     * the database name for the wiki which the user requested. If the database is switched to load some piece of data,
     * this will remember what it should be switched back to.
     * 
     * @return the db name originally requested by the user.
     */
    public String getOriginalDatabase()
    {
        return this.orig_database;
    }

    public void setOriginalDatabase(String database)
    {
        this.orig_database = database;
        if (database == null) {
            remove(ORIGINAL_WIKI_KEY);
        } else {
            put(ORIGINAL_WIKI_KEY, database);
        }
    }

    /**
     * @return true it's main wiki's context, false otherwise.
     */
    public boolean isMainWiki()
    {
        return isMainWiki(getDatabase());
    }

    /**
     * @param wikiName the name of the wiki.
     * @return true it's main wiki's context, false otherwise.
     */
    public boolean isMainWiki(String wikiName)
    {
        return (getWiki() != null && !getWiki().isVirtualMode())
            || StringUtils.equalsIgnoreCase(wikiName, getMainXWiki());
    }

    public XWikiDocument getDoc()
    {
        return (XWikiDocument) get("doc");
    }

    public void setDoc(XWikiDocument doc)
    {
        if (doc == null) {
            remove("doc");
        } else {
            put("doc", doc);
        }
    }

    public DocumentReference getUserReference()
    {
        return this.userReference;
    }

    public void setUserReference(DocumentReference userReference)
    {
        if (userReference == null) {
            this.userReference = null;
            remove(USER_KEY);
            remove(USERREFERENCE_KEY);
        } else {
            this.userReference = new DocumentReference(userReference);
            boolean ismain = isMainWiki(this.userReference.getWikiReference().getName());
            put(USER_KEY, new XWikiUser(getUser(), ismain));
            put(USERREFERENCE_KEY, this.userReference);
        }
    }

    private void setUserInternal(String user, boolean main)
    {
        if (user == null) {
            setUserReference(null);
        } else if (user.endsWith(XWikiRightService.GUEST_USER_FULLNAME) || user.equals(XWikiRightService.GUEST_USER)) {
            setUserReference(null);
            // retro-compatibilty hack: some code does not give the same meaning to null XWikiUser and XWikiUser
            // containing guest user
            put(USER_KEY, new XWikiUser(user, main));
        } else {
            setUserReference(resolveUserReference(user));
        }
    }

    /**
     * Make sure to use "XWiki" as default space when it's not provided in user name.
     */
    private DocumentReference resolveUserReference(String user)
    {
        return this.currentMixedDocumentReferenceResolver.resolve(user, new SpaceReference("XWiki", new WikiReference(
            getDatabase() == null ? "xwiki" : getDatabase())));
    }

    /**
     * @deprecated since 3.1M1 use {@link #setUserReference(DocumentReference)} instead
     */
    @Deprecated
    public void setUser(String user)
    {
        setUserInternal(user, false);
    }

    /**
     * @deprecated since 3.1M1 use {@link #getUserReference()} instead
     */
    @Deprecated
    public String getUser()
    {
        if (this.userReference != null) {
            if (getDatabase() == null) {
                return this.localEntityReferenceSerializer.serialize(this.userReference);
            } else {
                return this.compactWikiEntityReferenceSerializer.serialize(this.userReference, new WikiReference(
                    getDatabase()));
            }
        } else {
            return XWikiRightService.GUEST_USER_FULLNAME;
        }
    }

    /**
     * @deprecated since 3.1M1 use {@link #getUserReference()} instead
     */
    @Deprecated
    public String getLocalUser()
    {
        if (this.userReference != null) {
            return this.localEntityReferenceSerializer.serialize(this.userReference);
        } else {
            return XWikiRightService.GUEST_USER_FULLNAME;
        }
    }

    /**
     * @deprecated since 3.1M1 use {@link #getUserReference()} instead
     */
    @Deprecated
    public XWikiUser getXWikiUser()
    {
        if (this.userReference != null) {
            boolean ismain = isMainWiki(this.userReference.getWikiReference().getName());
            return new XWikiUser(getUser(), ismain);
        }

        return (XWikiUser) get(USER_KEY);
    }

    public String getLanguage()
    {
        return this.language;
    }

    public void setLanguage(String language)
    {
        this.language = Util.normalizeLanguage(language);
        if (language == null) {
            remove(LANGUAGE_KEY);
        } else {
            put(LANGUAGE_KEY, language);
        }
    }

    public String getInterfaceLanguage()
    {
        return this.interfaceLanguage;
    }

    public void setInterfaceLanguage(String interfaceLanguage)
    {
        this.interfaceLanguage = interfaceLanguage;
    }

    public int getMode()
    {
        return this.mode;
    }

    public void setMode(int mode)
    {
        this.mode = mode;
    }

    public URL getURL()
    {
        return this.url;
    }

    public void setURL(URL url)
    {
        this.url = url;
    }

    public XWikiURLFactory getURLFactory()
    {
        return this.URLFactory;
    }

    public void setURLFactory(XWikiURLFactory URLFactory)
    {
        this.URLFactory = URLFactory;
    }

    public XWikiForm getForm()
    {
        return this.form;
    }

    public void setForm(XWikiForm form)
    {
        this.form = form;
    }

    public boolean isFinished()
    {
        return this.finished;
    }

    public void setFinished(boolean finished)
    {
        this.finished = finished;
    }

    public XmlRpcServer getXMLRPCServer()
    {
        return this.xmlRpcServer;
    }

    public void setXMLRPCServer(XmlRpcServer xmlRpcServer)
    {
        this.xmlRpcServer = xmlRpcServer;
    }

    public void setWikiOwner(String wikiOwner)
    {
        this.wikiOwner = wikiOwner;
    }

    public String getWikiOwner()
    {
        return this.wikiOwner;
    }

    public void setWikiServer(XWikiDocument doc)
    {
        this.wikiServer = doc;
    }

    public XWikiDocument getWikiServer()
    {
        return this.wikiServer;
    }

    public int getCacheDuration()
    {
        return this.cacheDuration;
    }

    public void setCacheDuration(int cacheDuration)
    {
        this.cacheDuration = cacheDuration;
    }

    public String getMainXWiki()
    {
        return (String) get("mainxwiki");
    }

    public void setMainXWiki(String str)
    {
        put("mainxwiki", str);
    }

    // Used to avoid recursive loading of documents if there are recursives usage of classes
    public void addBaseClass(BaseClass bclass)
    {
        this.classCache.put(bclass.getDocumentReference(), bclass);
    }

    /**
     * @since 2.2M2
     */
    // Used to avoid recursive loading of documents if there are recursives usage of classes
    public BaseClass getBaseClass(DocumentReference documentReference)
    {
        return this.classCache.get(documentReference);
    }

    /**
     * Empty the class cache.
     */
    public void flushClassCache()
    {
        this.classCache.clear();
    }

    public void setLinksAction(String action)
    {
        put("links_action", action);
    }

    public void unsetLinksAction()
    {
        remove("links_action");
    }

    public String getLinksAction()
    {
        return (String) get("links_action");
    }

    public void setLinksQueryString(String value)
    {
        put("links_qs", value);
    }

    public void unsetLinksQueryString()
    {
        remove("links_qs");
    }

    public String getLinksQueryString()
    {
        return (String) get("links_qs");
    }

    public XWikiMessageTool getMessageTool()
    {
        XWikiMessageTool msg = ((XWikiMessageTool) get("msg"));
        if (msg == null) {
            getWiki().prepareResources(this);
            msg = ((XWikiMessageTool) get("msg"));
        }
        return msg;
    }

    public XWikiValidationStatus getValidationStatus()
    {
        return (XWikiValidationStatus) get("validation_status");
    }

    public void setValidationStatus(XWikiValidationStatus status)
    {
        put("validation_status", status);
    }

    public void addDisplayedField(String fieldname)
    {
        this.displayedFields.add(fieldname);
    }

    public List<String> getDisplayedFields()
    {
        return this.displayedFields;
    }

    public String getEditorWysiwyg()
    {
        return (String) get("editor_wysiwyg");
    }

    /**
     * Drop permissions for the remainder of the request cycle.
     * <p>
     * After this is called:
     * <ul>
     * <li>1. {@link com.xpn.xwiki.api.Api#hasProgrammingRights()} will always return false.</li>
     * <li>2. {@link com.xpn.xwiki.api.XWiki#getDocumentAsAuthor(org.xwiki.model.reference.DocumentReference)},
     * {@link com.xpn.xwiki.api.XWiki#getDocumentAsAuthor(String)}, {@link com.xpn.xwiki.api.Document#saveAsAuthor()},
     * {@link com.xpn.xwiki.api.Document#saveAsAuthor(String)},
     * {@link com.xpn.xwiki.api.Document#saveAsAuthor(String, boolean)}, and
     * {@link com.xpn.xwiki.api.Document#deleteAsAuthor()} will perform all of their actions as if the document's
     * content author was the guest user (XWiki.XWikiGuest).</li>
     * </ul>
     * <p>
     * In effect, no code requiring "programming right" will run, and if the document content author (see:
     * {@link com.xpn.xwiki.api.Document#getContentAuthor()}) is a user who has "programming right", there will be no
     * way for code following this call to save another document as this user, blessing it too with programming right.
     * <p>
     * Once dropped, permissions cannot be regained for the duration of the request.
     * <p>
     * If you are interested in a more flexable sandboxing method which sandboxed code only for the remainder of the
     * rendering cycle, consider using {@link com.xpn.xwiki.api.Document#dropPermissions()}.
     * 
     * @since 3.0M3
     */
    public void dropPermissions()
    {
        this.put(XWikiConstant.DROPPED_PERMISSIONS, Boolean.TRUE);
    }

    /**
     * @return true if {@link XWikiContext#dropPermissions()} has been called on this context, or if the
     *         {@link XWikiConstant.DROPPED_PERMISSIONS} key has been set in the
     *         {@link org.xwiki.context.ExecutionContext} for this thread. This is done by calling
     *         {@Document#dropPermissions()}
     */
    public boolean hasDroppedPermissions()
    {
        if (this.get(XWikiConstant.DROPPED_PERMISSIONS) != null) {
            return true;
        }

        final Object dropped = this.execution.getContext().getProperty(XWikiConstant.DROPPED_PERMISSIONS);

        if (dropped == null || !(dropped instanceof Integer)) {
            return false;
        }

        return ((Integer) dropped) == System.identityHashCode(this.execution.getContext());
    }

    // Object

    @Override
    public synchronized XWikiContext clone()
    {
        XWikiContext context = (XWikiContext) super.clone();

        // Make sure to have unique instances of the various caches
        context.displayedFields = Collections.synchronizedList(new ArrayList<String>(this.displayedFields));
        context.classCache = Collections.synchronizedMap(new LRUMap(this.classCacheSize));

        return context;
    }
}
