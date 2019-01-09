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
package com.xpn.xwiki.plugin.skinx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.web.Utils;

/**
 * Abstract SX plugin for wiki-document-based extensions (Extensions written as object of a XWiki Extension class).
 * Provides a generic method to initialize the XWiki class upon plugin initialization if needed. Provide a notification
 * mechanism for extensions marked as "use-always".
 * 
 * @version $Id$
 * @since 1.4
 * @see JsSkinExtensionPlugin
 * @see CssSkinExtensionPlugin
 */
public abstract class AbstractDocumentSkinExtensionPlugin extends AbstractSkinExtensionPlugin implements EventListener
{
    /**
     * Log helper for logging messages in this class.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractDocumentSkinExtensionPlugin.class);

    /**
     * The name of the field that indicates whether an extension should always be used, or only when explicitly pulled.
     */
    private static final String USE_FIELDNAME = "use";

    /**
     * A Map with wiki/database name as keys and sets of extensions to use always for this wiki as values.
     */
    private Map<String, Set<DocumentReference>> alwaysUsedExtensions;

    /**
     * Used to match events on "use" property.
     */
    private final List<Event> events = new ArrayList<Event>(3);

    /**
     * XWiki plugin constructor.
     * 
     * @param name The name of the plugin, which can be used for retrieving the plugin API from velocity. Unused.
     * @param className The canonical classname of the plugin. Unused.
     * @param context The current request context.
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#XWikiDefaultPlugin(String,String,com.xpn.xwiki.XWikiContext)
     */
    public AbstractDocumentSkinExtensionPlugin(String name, String className, XWikiContext context)
    {
        super(name, className, context);

        this.events.add(new DocumentCreatedEvent());
        this.events.add(new DocumentDeletedEvent());
        this.events.add(new DocumentUpdatedEvent());

        this.events.add(new WikiDeletedEvent());
    }

    @Override
    public List<Event> getEvents()
    {
        return this.events;
    }

    /**
     * The name of the XClass which holds extensions of this type.
     * 
     * @return A <code>String</code> representation of the XClass name, in the <code>Space.Document</code> format.
     */
    protected abstract String getExtensionClassName();

    /**
     * A user-friendly name for this type of resource, used in the auto-generated class document.
     * 
     * @return The user-friendly name for this type of resource.
     */
    protected abstract String getExtensionName();

    /**
     * {@inheritDoc}
     * <p>
     * Create/update the XClass corresponding to this kind of extension, and register the listeners that update the list
     * of always used extensions.
     * </p>
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#init(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void init(XWikiContext context)
    {
        super.init(context);

        this.alwaysUsedExtensions = new HashMap<String, Set<DocumentReference>>();
        getExtensionClass(context);

        Utils.getComponent(ObservationManager.class).addListener(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Create/update the XClass corresponding to this kind of extension in this virtual wiki.
     * </p>
     * 
     * @see com.xpn.xwiki.plugin.XWikiDefaultPlugin#virtualInit(com.xpn.xwiki.XWikiContext)
     */
    @Override
    public void virtualInit(XWikiContext context)
    {
        super.virtualInit(context);

        getExtensionClass(context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * For this kind of resources, an XObject property (<tt>use</tt>) with the value <tt>always</tt> indicates always
     * used extensions. The list of extensions for each wiki is lazily placed in a cache: if the extension set for the
     * context wiki is null, then they will be looked up in the database and added to it. The cache is invalidated using
     * the notification mechanism.
     * </p>
     * 
     * @see AbstractSkinExtensionPlugin#getAlwaysUsedExtensions(XWikiContext)
     */
    @Override
    public Set<String> getAlwaysUsedExtensions(XWikiContext context)
    {
        EntityReferenceSerializer<String> serializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        Set<DocumentReference> references = getAlwaysUsedExtensions();
        Set<String> names = new HashSet<String>(references.size());
        for (DocumentReference reference : references) {
            names.add(serializer.serialize(reference));
        }
        return names;
    }

    /**
     * Returns the list of always used extensions of this type as a set of document references. For this kind of
     * resources, an XObject property (<tt>use</tt>) with the value <tt>always</tt> indicates always used extensions.
     * The list of extensions for each wiki is lazily placed in a cache: if the extension set for the context wiki is
     * null, then they will be looked up in the database and added to it. The cache is invalidated using the
     * notification mechanism. Note that this method is called for each request, as the list might change in time, and
     * it can be different for each wiki in a farm.
     *
     * @return a set of document references that should be pulled in the current response
     */
    public Set<DocumentReference> getAlwaysUsedExtensions()
    {
        XWikiContext context = Utils.getContext();
        // Retrieve the current wiki name from the XWiki context
        String currentWiki = StringUtils.defaultIfEmpty(context.getWikiId(), context.getMainXWiki());
        // If we already have extensions defined for this wiki, we return them
        if (this.alwaysUsedExtensions.get(currentWiki) != null) {
            return this.alwaysUsedExtensions.get(currentWiki);
        } else {
            // Otherwise, we look them up in the database.
            Set<DocumentReference> extensions = new HashSet<DocumentReference>();
            String query =
                ", BaseObject as obj, StringProperty as use where obj.className='" + getExtensionClassName() + "'"
                    + " and obj.name=doc.fullName and use.id.id=obj.id and use.id.name='use' and use.value='always'";
            try {
                for (DocumentReference extension : context.getWiki().getStore()
                    .searchDocumentReferences(query, context)) {
                    try {
                        XWikiDocument doc = context.getWiki().getDocument(extension, context);
                        // Only add the extension as being "always used" if the page holding it has been saved with
                        // programming rights.
                        if (Utils.getComponent(AuthorizationManager.class).hasAccess(Right.PROGRAM,
                            doc.getContentAuthorReference(), doc.getDocumentReference())) {
                            extensions.add(extension);
                        }
                    } catch (XWikiException e1) {
                        LOGGER.error("Error while adding skin extension [{}] as always used. It will be ignored.",
                            extension, e1);
                    }
                }
                this.alwaysUsedExtensions.put(currentWiki, extensions);
                return extensions;
            } catch (XWikiException e) {
                LOGGER.error("Error while retrieving always used JS extensions", e);
                return Collections.emptySet();
            }
        }
    }

    @Override
    public boolean hasPageExtensions(XWikiContext context)
    {
        XWikiDocument doc = context.getDoc();
        List<BaseObject> objects = doc.getObjects(getExtensionClassName());
        if (objects != null) {
            for (BaseObject obj : objects) {
                if (obj == null) {
                    continue;
                }
                if (obj.getStringValue(USE_FIELDNAME).equals("currentPage")) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void use(String resource, XWikiContext context)
    {
        String canonicalResource = getCanonicalDocumentName(resource);

        super.use(canonicalResource, context);
    }

    @Override
    public void use(String resource, Map<String, Object> parameters, XWikiContext context)
    {
        String canonicalResource = getCanonicalDocumentName(resource);

        super.use(canonicalResource, parameters, context);
    }

    /**
     * {@inheritDoc}
     * <p>
     * We must override this method since the plugin manager only calls it for classes that provide their own
     * implementation, and not an inherited one.
     * </p>
     * 
     * @see AbstractSkinExtensionPlugin#endParsing(String, XWikiContext)
     */
    @Override
    public String endParsing(String content, XWikiContext context)
    {
        return super.endParsing(content, context);
    }

    /**
     * Creates or updates the XClass used for this type of extension. Usually called on {@link #init(XWikiContext)} and
     * {@link #virtualInit(XWikiContext)}.
     * 
     * @param context The current request context, which gives access to the wiki.
     * @return The XClass for this extension.
     */
    public BaseClass getExtensionClass(XWikiContext context)
    {
        try {
            XWikiDocument doc = context.getWiki().getDocument(getExtensionClassName(), context);

            return doc.getXClass();
        } catch (Exception ex) {
            LOGGER.error("Cannot get skin extension class [{}]", getExtensionClassName(), ex);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Make sure to keep the {@link #alwaysUsedExtensions} map consistent when the database changes.
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            this.alwaysUsedExtensions.remove(((WikiDeletedEvent) event).getWikiId());
        } else {
            onDocumentEvent((XWikiDocument) source, (XWikiContext) data);
        }
    }

    /**
     * A document related event has been received.
     * 
     * @param document the modified document
     * @param context the XWiki context
     */
    private void onDocumentEvent(XWikiDocument document, XWikiContext context)
    {
        boolean remove = false;
        if (document.getObject(getExtensionClassName()) != null) {
            // new or already existing object
            if (document.getObject(getExtensionClassName(), USE_FIELDNAME, "always", false) != null) {
                if (Utils.getComponent(AuthorizationManager.class).hasAccess(Right.PROGRAM,
                    document.getContentAuthorReference(), document.getDocumentReference())) {
                    getAlwaysUsedExtensions().add(document.getDocumentReference());

                    return;
                } else {
                    // in case the extension lost its programming rights upon this save.
                    remove = true;
                }
            } else {
                // remove if exists but use onDemand
                remove = true;
            }
        } else if (document.getOriginalDocument().getObject(getExtensionClassName()) != null) {
            // object removed
            remove = true;
        }

        if (remove) {
            getAlwaysUsedExtensions().remove(document.getDocumentReference());
        }
    }

    /**
     * Get the canonical serialization of a document name, in the {@code wiki:Space.Document} format.
     *
     * @param documentName the original document name to fix
     * @return fixed document name
     */
    private String getCanonicalDocumentName(String documentName)
    {
        @SuppressWarnings("unchecked")
        EntityReferenceResolver<String> resolver = Utils.getComponent(EntityReferenceResolver.TYPE_STRING, "current");
        @SuppressWarnings("unchecked")
        EntityReferenceSerializer<String> serializer = Utils.getComponent(EntityReferenceSerializer.TYPE_STRING);
        return serializer.serialize(resolver.resolve(documentName, EntityType.DOCUMENT));
    }

    /**
     * @param documentName the Skin Extension's document name
     * @param context the XWiki Context
     * @return true if the specified document is accessible (i.e. has view rights) by the current user; false otherwise
     */
    protected boolean isAccessible(String documentName, XWikiContext context)
    {
        return isAccessible(getCurrentDocumentReferenceResolver().resolve(documentName), context);
    }

    /**
     * @param documentReference the Skin Extension's document reference
     * @param context the XWiki Context
     * @return true if the specified document is accessible (i.e. has view rights) by the current user; false otherwise
     * @since 7.4.1
     */
    protected boolean isAccessible(DocumentReference documentReference, XWikiContext context)
    {
        if (!Utils.getComponent(ContextualAuthorizationManager.class).hasAccess(Right.VIEW, documentReference)) {
            LOGGER.debug("[{}] The current user [{}] does not have 'view' rights on the Skin Extension document [{}]",
                getName(), context.getUserReference(), documentReference);

            return false;
        }

        return true;
    }

    /**
     * @param documentReference the Skin Extension's document reference
     * @param context the XWiki Context
     * @return the version of the document
     */
    private String getDocumentVersion(DocumentReference documentReference, XWikiContext context)
    {
        try {
            return context.getWiki().getDocument(documentReference, context).getVersion();
        } catch (XWikiException e) {
            LOGGER.error("Failed to load document [{}].", documentReference, e);
        }
        return "";
    }

    /**
     * Return the query string part with the version of the document, to add to the URL of a resource. The objective is
     * to generate an URL specific to this version to avoid browsers using an outdated version from their cache.
     *  
     * @param documentReference the Skin Extension's document reference
     * @param context the XWiki Context
     * @return the query string part handling the version of the document
     */
    private String getDocumentVersionQueryString(DocumentReference documentReference, XWikiContext context)
    {
        return "docVersion=" + sanitize(getDocumentVersion(documentReference, context));
    }

    /**
     * Return the query string part with the version of the wiki itself, to add to the URL of a resource.
     * As for {@link #getDocumentVersionQueryString(DocumentReference, XWikiContext)} the goal here is to avoid browsers
     * using an outdated version from their cache in some other usecases.
     *
     * @param context the XWiki context
     * @return the query string part handling the version of the document
     */
    private String getWikiVersionQueryString(XWikiContext context)
    {
        return "wikiVersion=" + sanitize(context.getWiki().getVersion());
    }

    /**
     * Return the query string part with the language of the document (if any).
     *  
     * @param context the XWiki Context
     * @return the query string handling the language of the document
     */
    private String getLanguageQueryString(XWikiContext context)
    {
        Locale locale = context.getLocale();
        if (locale != null) {
            return "language=" + sanitize(locale.toString());
        }
        return "";
    }

    /**
     * Return the URL to a document skin extension.
     *
     * @param documentReference the Skin Extension's document reference
     * @param documentName the Skin Extension's document name
     * @param pluginName the name of the plugin
     * @param context the XWiki Context
     * @return the URL to the document skin extension.
     *
     * @since 7.4.1 
     */
    protected String getDocumentSkinExtensionURL(DocumentReference documentReference, String documentName,
            String pluginName, XWikiContext context)
    {
        String queryString = String.format("%s&amp;%s&amp;%s%s",
                getLanguageQueryString(context),
                getDocumentVersionQueryString(documentReference, context),
                getWikiVersionQueryString(context),
                parametersAsQueryString(documentName, context));

        return context.getWiki().getURL(documentReference, pluginName, queryString, "", context);
    }
    
    
}
