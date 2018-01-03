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

import java.lang.reflect.ParameterizedType;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Provider;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.localization.LocaleUtils;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

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

/**
 * Represents the execution environment for all the wiki pages. An instance of the <code>Context</code> class is
 * available as a predefined variable for scripting inside any wiki page. You can access it using <code>$xcontext</code>
 * in Velocity scripts or simply <code>xcontext</code> in Groovy ones. The <code>Context</code> class provides a means
 * of getting contextual information about the current request or configuring XWiki on the fly.
 *
 * @version $Id$
 */
public class XWikiContext extends HashMap<Object, Object>
{
    /**
     * Type instance for {@code Provider<XWikiContext>}.
     *
     * @since 5.0M1
     */
    public static final ParameterizedType TYPE_PROVIDER =
        new DefaultParameterizedType(null, Provider.class, XWikiContext.class);

    public static final int MODE_SERVLET = 0;

    public static final int MODE_PORTLET = 1;

    public static final int MODE_XMLRPC = 2;

    public static final int MODE_ATOM = 3;

    public static final int MODE_PDF = 4;

    public static final int MODE_GWT = 5;

    public static final int MODE_GWT_DEBUG = 6;

    public static final String EXECUTIONCONTEXT_KEY = "xwikicontext";

    /**
     * @deprecated use {@link VelocityManager#getVelocityContext()} instead
     */
    public static final String KEY_LEGACY_VELOCITYCONTEXT = "vcontext";

    /** Logging helper object. */
    protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiContext.class);

    private static final String WIKI_KEY = "wiki";

    private static final String ORIGINAL_WIKI_KEY = "originalWiki";

    private static final String USER_KEY = "user";

    private static final String USERREFERENCE_KEY = "userreference";

    /**
     * Used to resolve a string into a proper Document Reference using the current document's reference to fill the
     * blanks, except for the page name for which the default page name is used instead and for the wiki name for which
     * the current wiki is used instead of the current document reference's wiki.
     */
    private DocumentReferenceResolver<String> currentMixedDocumentReferenceResolver;

    /**
     * Used to convert a proper Document Reference to a string but without the wiki name.
     */
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part if it matches the current
     * wiki).
     */
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    /** The Execution so that we can check if permissions were dropped there. */
    private Execution execution;

    private boolean finished = false;

    private XWiki wiki;

    private XWikiEngineContext engine_context;

    private XWikiRequest request;

    private XWikiResponse response;

    private XWikiForm form;

    private String action;

    private String orig_wikiId;

    private WikiReference wikiReference;

    private DocumentReference userReference;

    private Locale locale;

    private static final String LANGUAGE_KEY = "language";

    private Locale interfaceLocale;

    private int mode;

    private URL url;

    private XWikiURLFactory URLFactory;

    private int cacheDuration = 0;

    private int classCacheSize = 20;

    // Used to avoid recursive loading of documents if there are recursives usage of classes
    // FIXME: why synchronized since a context is supposed to be tied to a thread ?
    @SuppressWarnings("unchecked")
    private Map<DocumentReference, BaseClass> classCache = Collections.synchronizedMap(new LRUMap(this.classCacheSize));

    // FIXME: why synchronized since a context is supposed to be tied to a thread ?
    private List<String> displayedFields = Collections.synchronizedList(new ArrayList<String>());

    public XWikiContext()
    {
    }

    private DocumentReferenceResolver<String> getCurrentMixedDocumentReferenceResolver()
    {
        if (this.currentMixedDocumentReferenceResolver == null) {
            this.currentMixedDocumentReferenceResolver =
                Utils.getComponent(DocumentReferenceResolver.TYPE_STRING, "currentmixed");
        }

        return this.currentMixedDocumentReferenceResolver;
    }

    private EntityReferenceSerializer<String> getLocalEntityReferenceSerializer()
    {
        if (this.localEntityReferenceSerializer == null) {
            this.localEntityReferenceSerializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "local");
        }

        return this.localEntityReferenceSerializer;
    }

    private EntityReferenceSerializer<String> getCompactWikiEntityReferenceSerializer()
    {
        if (this.compactWikiEntityReferenceSerializer == null) {
            this.compactWikiEntityReferenceSerializer =
                Utils.getComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        }

        return this.compactWikiEntityReferenceSerializer;
    }

    private Execution getExecution()
    {
        if (this.execution == null) {
            this.execution = Utils.getComponent(Execution.class);
        }

        return this.execution;
    }

    private ExecutionContext getExecutionContext()
    {
        if (getExecution() != null) {
            return getExecution().getContext();
        }

        return null;
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

    /**
     * @deprecated since 6.1M1, use {@link #getWikiId()} instead
     */
    @Deprecated
    public String getDatabase()
    {
        return getWikiId();
    }

    /**
     * @return the id of the current wiki, or null if none is set
     * @since 6.1M1
     */
    public String getWikiId()
    {
        return this.wikiReference != null ? this.wikiReference.getName() : null;
    }

    /**
     * @return the reference of the current wiki or null if none is set
     * @since 7.2M1
     */
    public WikiReference getWikiReference()
    {
        return this.wikiReference;
    }

    /**
     * @param wikiId the current wiki id
     * @deprecated since 6.1M1, use {@link #setWikiId(String)} instead
     */
    @Deprecated
    public void setDatabase(String wikiId)
    {
        setWikiId(wikiId);
    }

    /**
     * @param wikiId the current wiki id
     * @since 6.1M1
     */
    public void setWikiId(String wikiId)
    {
        setWikiReference(wikiId != null ? new WikiReference(wikiId) : null);
    }

    /**
     * @param wikiReference the current wiki reference
     * @since 7.2M1
     */
    public void setWikiReference(WikiReference wikiReference)
    {
        this.wikiReference = wikiReference;

        if (this.wikiReference == null) {
            super.remove(WIKI_KEY);
        } else {
            super.put(WIKI_KEY, this.wikiReference.getName());

            if (this.orig_wikiId == null) {
                this.orig_wikiId = this.wikiReference.getName();
                super.put(ORIGINAL_WIKI_KEY, this.wikiReference.getName());
            }
        }
    }

    @Override
    public synchronized Object get(Object key)
    {
        Object value;

        if (WIKI_KEY.equals(key)) {
            value = getWikiId();
        } else if (KEY_LEGACY_VELOCITYCONTEXT.equals(key)) {
            ExecutionContext executionContext = getExecutionContext();
            if (executionContext != null) {
                value = executionContext.getProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID);
            } else {
                value = null;
            }
        } else {
            value = super.get(key);
        }

        return value;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Make sure to keep wiki field and map synchronized.
     * </p>
     *
     * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public synchronized Object put(Object key, Object value)
    {
        Object previous;

        if (WIKI_KEY.equals(key)) {
            previous = get(WIKI_KEY);
            setWikiId((String) value);
        } else if (KEY_LEGACY_VELOCITYCONTEXT.equals(key)) {
            ExecutionContext executionContext = getExecutionContext();
            if (executionContext != null) {
                previous = executionContext.getProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID);
                executionContext.setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID, value);
            } else {
                previous = null;
            }
        } else {
            if (value != null) {
                previous = super.put(key, value);
            } else {
                previous = super.remove(key);
            }
        }

        return previous;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Make sure to keep wiki field and map synchronized.
     * </p>
     *
     * @see java.util.Hashtable#remove(java.lang.Object)
     */
    @Override
    public synchronized Object remove(Object key)
    {
        Object previous;

        if (WIKI_KEY.equals(key)) {
            previous = get(WIKI_KEY);
            setWikiId(null);
        } else if (KEY_LEGACY_VELOCITYCONTEXT.equals(key)) {
            ExecutionContext executionContext = getExecutionContext();
            if (executionContext != null) {
                previous = executionContext.getProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID);
                executionContext.removeProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID);
            } else {
                previous = null;
            }
        } else {
            previous = super.remove(key);
        }

        return previous;
    }

    /**
     * Get the "original" wiki id. This will be the wiki id for the wiki which the user requested. If the wiki is
     * switched to load some piece of data, this will remember what it should be switched back to.
     *
     * @return the wiki id originally requested by the user.
     * @deprecated since 6.1M1, use {@link #getOriginalWikiId()} instead
     */
    @Deprecated
    public String getOriginalDatabase()
    {
        return getOriginalWikiId();
    }

    /**
     * Get the "original" wiki id. This will be the wiki id for the wiki which the user requested. If the wiki is
     * switched to load some piece of data, this will remember what it should be switched back to.
     *
     * @return the wiki id originally requested by the user.
     * @since 6.1M1
     */
    public String getOriginalWikiId()
    {
        return this.orig_wikiId;
    }

    /**
     * @deprecated since 6.1M1, use {@link #setOriginalWikiId(String)} instead
     */
    @Deprecated
    public void setOriginalDatabase(String wikiId)
    {
        setOriginalWikiId(wikiId);
    }

    /**
     * Set the "original" wiki id. This will be the wiki id for the wiki which the user requested. If the wiki is
     * switched to load some piece of data, this will remember what it should be switched back to.
     *
     * @param wikiId the wiki id originally requested by the user
     * @since 6.1M1
     */
    public void setOriginalWikiId(String wikiId)
    {
        this.orig_wikiId = wikiId;
        if (wikiId == null) {
            remove(ORIGINAL_WIKI_KEY);
        } else {
            put(ORIGINAL_WIKI_KEY, wikiId);
        }
    }

    /**
     * @return true it's main wiki's context, false otherwise.
     */
    public boolean isMainWiki()
    {
        return isMainWiki(getWikiId());
    }

    /**
     * @param wikiName the name of the wiki.
     * @return true it's main wiki's context, false otherwise.
     */
    public boolean isMainWiki(String wikiName)
    {
        return StringUtils.equalsIgnoreCase(wikiName, getMainXWiki());
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

            // Log this since it's probably a mistake so that we find who is doing bad things
            if (this.userReference.getName().equals(XWikiRightService.GUEST_USER)) {
                LOGGER.warn("A reference to XWikiGuest user has been set instead of null. This is probably a mistake.",
                    new Exception("See stack trace"));
            }
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
        return getCurrentMixedDocumentReferenceResolver().resolve(user,
            new SpaceReference("XWiki", new WikiReference(getWikiId() == null ? "xwiki" : getWikiId())));
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
            if (getWikiId() == null) {
                return getLocalEntityReferenceSerializer().serialize(this.userReference);
            } else {
                return getCompactWikiEntityReferenceSerializer().serialize(this.userReference,
                    new WikiReference(getWikiId()));
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
            return getLocalEntityReferenceSerializer().serialize(this.userReference);
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

    /**
     * @deprecated since 4.3M1 use {@link #getLocale()} instead
     */
    @Deprecated
    public String getLanguage()
    {
        return this.locale != null ? this.locale.toString() : null;
    }

    /**
     * @deprecated since 4.3M1 use {@link #setLocale(Locale)} instead
     */
    @Deprecated
    public void setLanguage(String language)
    {
        setLocale(LocaleUtils.toLocale(Util.normalizeLanguage(language)));

    }

    /**
     * @return the current locale
     * @since 4.3M1
     */
    public Locale getLocale()
    {
        return this.locale;
    }

    /**
     * @param locale the current locale
     * @since 4.3M1
     */
    public void setLocale(Locale locale)
    {
        this.locale = locale;

        if (locale == null) {
            remove(LANGUAGE_KEY);
        } else {
            put(LANGUAGE_KEY, locale.toString());
        }
    }

    /**
     * @deprecated since 6.0M1, use {@link #getInterfaceLocale()} instead
     */
    @Deprecated
    public String getInterfaceLanguage()
    {
        return this.interfaceLocale != null ? this.interfaceLocale.toString() : null;
    }

    /**
     * @return the {@link Locale} to use to display the user interface
     * @since 6.0M1
     */
    public Locale getInterfaceLocale()
    {
        return this.interfaceLocale;
    }

    /**
     * @deprecated since 6.0M1, use {@link #setInterfaceLocale(Locale)} instead
     */
    @Deprecated
    public void setInterfaceLanguage(String interfaceLanguage)
    {
        setInterfaceLocale(LocaleUtils.toLocale(Util.normalizeLanguage(interfaceLanguage)));
    }

    /**
     * @param interfaceLocale the {@link Locale} to use to display the content
     * @since 6.0M1
     */
    public void setInterfaceLocale(Locale interfaceLocale)
    {
        this.interfaceLocale = interfaceLocale;
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

    /**
     * @deprecated never made any sense since the context wiki can change any time
     */
    @Deprecated
    public void setWikiOwner(String wikiOwner)
    {
        // Cannot do anything
    }

    /**
     * @deprecated use {@link XWiki#getWikiOwner(String, XWikiContext)} instead
     */
    @Deprecated
    public String getWikiOwner()
    {
        try {
            return getWiki().getWikiOwner(getWikiId(), this);
        } catch (XWikiException e) {
            LOGGER.error("Failed to get owner for wiki [{}]", getWikiId(), e);
        }

        return null;
    }

    public XWikiDocument getWikiServer()
    {
        String currentWiki = getWikiId();
        try {
            setWikiId(getMainXWiki());

            return getWiki().getDocument(XWiki.getServerWikiPage(currentWiki), this);
        } catch (XWikiException e) {
            LOGGER.error("Failed to get wiki descriptor for wiki [{}]", currentWiki, e);
        } finally {
            setWikiId(currentWiki);
        }

        return null;
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
    public BaseClass getBaseClass(DocumentReference classReference)
    {
        return this.classCache.get(classReference);
    }

    /**
     * @param classReference the reference of the class
     * @since 9.0RC1
     */
    public void removeBaseClass(DocumentReference classReference)
    {
        this.classCache.remove(classReference);
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

    /**
     * @deprecated since 4.3M2 use {@link org.xwiki.localization.ContextualLocalizationManager} component instead
     */
    @Deprecated
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

    /**
     * Returns the list of TextArea fields that use the WYSIWYG editor. This list is automatically built when displaying
     * TextArea properties.
     *
     * @deprecated since 8.2RC1 when we started using the Edit Module to load the configured WYSIWYG editor
     * @return a string containing a comma-separated list of TextArea field names for which the WYSIWYG editor should be
     *         enabled
     */
    @Deprecated
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
     *         {@link XWikiConstant#DROPPED_PERMISSIONS} key has been set in the
     *         {@link org.xwiki.context.ExecutionContext} for this thread. This is done by calling
     *         {Document#dropPermissions()}
     */
    public boolean hasDroppedPermissions()
    {
        if (this.get(XWikiConstant.DROPPED_PERMISSIONS) != null) {
            return true;
        }

        final Object dropped = getExecution().getContext().getProperty(XWikiConstant.DROPPED_PERMISSIONS);

        if (dropped == null || !(dropped instanceof Integer)) {
            return false;
        }

        return ((Integer) dropped) == System.identityHashCode(getExecution().getContext());
    }

    // Object

    @Override
    public synchronized XWikiContext clone()
    {
        XWikiContext context = (XWikiContext) super.clone();

        // Make sure to have unique instances of the various caches
        context.displayedFields = Collections.synchronizedList(new ArrayList<String>(this.displayedFields));
        context.classCache = Collections.synchronizedMap(new LRUMap<DocumentReference, BaseClass>(this.classCacheSize));

        return context;
    }

    /**
     * There are several places where the XWiki context needs to be declared in the execution, so we add a common method
     * here.
     *
     * @param executionContext The execution context.
     */
    public void declareInExecutionContext(ExecutionContext executionContext)
    {
        if (!executionContext.hasProperty(XWikiContext.EXECUTIONCONTEXT_KEY)) {
            executionContext.newProperty(XWikiContext.EXECUTIONCONTEXT_KEY).initial(this).inherited().declare();
        } else {
            executionContext.setProperty(XWikiContext.EXECUTIONCONTEXT_KEY, this);
        }
    }

    /**
     * @return the reference of the user being used to check script and programming right (i.e. the author of the
     *         current script)
     * @since 8.3M2
     */
    public DocumentReference getAuthorReference()
    {
        XWikiDocument sdoc = (XWikiDocument) get("sdoc");
        if (sdoc == null) {
            sdoc = getDoc();
        }

        return sdoc != null ? sdoc.getContentAuthorReference() : getUserReference();
    }
}
