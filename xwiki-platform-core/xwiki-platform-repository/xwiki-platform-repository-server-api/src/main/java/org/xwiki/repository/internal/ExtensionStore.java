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
package org.xwiki.repository.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.DefaultExtensionSupportPlan;
import org.xwiki.extension.DefaultExtensionSupportPlans;
import org.xwiki.extension.DefaultExtensionSupporter;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionSupportPlan;
import org.xwiki.extension.ExtensionSupportPlans;
import org.xwiki.extension.ExtensionSupporter;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.NumberProperty;

/**
 * Various helpers to manipulate the extension related pages.
 * 
 * @since 16.8.0RC1
 * @version $Id$
 */
@Component(roles = ExtensionStore.class)
@Singleton
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class ExtensionStore implements Initializable, Disposable
{
    /**
     * The reference to match property {@link XWikiRepositoryModel#PROP_EXTENSION_ID} of class
     * {@link XWikiRepositoryModel#EXTENSION_CLASSNAME} on whatever wiki.
     */
    private static final EntityReference EXTENSIONID_PROPERTY_REFERENCE =
        new EntityReference(XWikiRepositoryModel.PROP_EXTENSION_ID, EntityType.OBJECT_PROPERTY,
            XWikiRepositoryModel.EXTENSION_OBJECTREFERENCE);

    private static final List<Event> EVENTS =
        Arrays.<Event>asList(new XObjectPropertyAddedEvent(EXTENSIONID_PROPERTY_REFERENCE),
            new XObjectPropertyDeletedEvent(EXTENSIONID_PROPERTY_REFERENCE),
            new XObjectPropertyUpdatedEvent(EXTENSIONID_PROPERTY_REFERENCE));

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private QueryManager queryManager;

    @Inject
    private ObservationManager observation;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentStringResolver;

    /**
     * Link extension id to document reference. The tabe contains null if the id link to no extension.
     */
    private Cache<DocumentReference[]> documentReferenceCache;

    @Inject
    private Logger logger;

    private EventListener listener = new EventListener()
    {
        @Override
        public void onEvent(Event event, Object source, Object data)
        {
            // TODO: Improve a bit by removing only what's changed
            documentReferenceCache.removeAll();
        }

        @Override
        public String getName()
        {
            return "repository.DefaultRepositoryManager";
        }

        @Override
        public List<Event> getEvents()
        {
            return EVENTS;
        }
    };

    @Override
    public void initialize() throws InitializationException
    {
        // Init cache
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setConfigurationId("repository.extensionid.documentreference");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(10000);
        cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

        try {
            this.documentReferenceCache = this.cacheManager.createNewCache(cacheConfiguration);
        } catch (CacheException e) {
            throw new InitializationException("Failed to initialize cache", e);
        }

        // Listen to modifications
        this.observation.addListener(listener, EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.observation.removeListener(listener.getName());
        this.documentReferenceCache.dispose();
    }

    /**
     * @param <T> the expected type of the value to return
     * @param xobject the xobject
     * @param propertyName the property of the xobject
     * @return the value
     */
    public <T> T getValue(BaseObject xobject, String propertyName)
    {
        return getValue(xobject, propertyName, (T) null);
    }

    /**
     * @param <T> the expected type of the value to return
     * @param xobjects the xobject to try
     * @param propertyName the property of the xobject
     * @return the value
     * @since 17.9.0RC1
     */
    public <T> T getValue(List<BaseObject> xobjects, String propertyName)
    {
        return getValue(xobjects, propertyName, (T) null);
    }

    /**
     * @param <T> the expected type of the value to return
     * @param xobject the xobject
     * @param propertyName the property of the xobject
     * @param def the value to return if the property is not set
     * @return the value
     */
    public <T> T getValue(BaseObject xobject, String propertyName, T def)
    {
        BaseProperty<?> property = (BaseProperty<?>) xobject.safeget(propertyName);

        T value = def;
        if (property != null) {
            value = (T) property.getValue();
            if (value instanceof String stringValue) {
                value = (T) StringUtils.defaultIfEmpty(stringValue, (String) def);
            } else if (value instanceof Collection collectionValue) {
                value = collectionValue.isEmpty() ? def : (T) collectionValue;
            } else if (value == null) {
                value = def;
            }
        }

        return value;
    }

    /**
     * @param <T> the expected type of the value to return
     * @param xobjects the xobjects to try
     * @param propertyName the property of the xobject
     * @param def the value to return if the property is not set
     * @return the value
     * @since 17.9.0RC1
     */
    public <T> T getValue(List<BaseObject> xobjects, String propertyName, T def)
    {
        for (BaseObject xobject : xobjects) {
            T result = getValue(xobject, propertyName, null);

            if (result != null) {
                return result;
            }
        }

        return def;
    }

    private URL getURLValue(BaseProperty<?> property, boolean fallbackOnDocumentURL, XWikiContext xcontext)
    {
        URL url = null;

        String urlString = (String) property.getValue();
        if (StringUtils.isNotEmpty(urlString)) {
            try {
                url = new URL(urlString);
            } catch (MalformedURLException e) {
                this.logger.warn("The format of the URL property [{}] is wrong ({})", property.getReference(),
                    urlString);
            }
        }

        if (url == null && fallbackOnDocumentURL) {
            XWikiDocument document = property.getOwnerDocument();

            if (document != null) {
                try {
                    url = new URL(document.getExternalURL("view", xcontext));
                } catch (MalformedURLException e) {
                    this.logger.error("Failed to get the URL for document [{}]", document.getDocumentReference(), e);
                }
            }
        }

        return url;
    }

    /**
     * @param xobject the xobject
     * @param propertyName the property of the xobject
     * @param def the value to return if the property is not set
     * @return the value
     */
    public boolean getBooleanValue(BaseObject xobject, String propertyName, boolean def)
    {
        BaseProperty<?> property = (BaseProperty<?>) xobject.safeget(propertyName);

        if (property instanceof NumberProperty) {
            Number number = (Number) property.getValue();
            if (number != null) {
                return number.intValue() == 1;
            }
        }

        return def;
    }

    /**
     * @param xobject the xobject
     * @param propertyName the property of the xobject
     * @param fallbackOnDocumentURL true if the document's external URL should be used when the property URL is not
     *            provided or invalid
     * @param xcontext the XWiki context
     * @return the URL
     */
    public URL getURLValue(BaseObject xobject, String propertyName, boolean fallbackOnDocumentURL,
        XWikiContext xcontext)
    {
        URL url = null;

        BaseProperty<?> property = (BaseProperty<?>) xobject.safeget(propertyName);
        if (property != null) {
            return getURLValue(property, fallbackOnDocumentURL, xcontext);
        }

        return url;
    }

    /**
     * @param supportPlanIds the support plan identifiers
     * @return the support plans as a {@link ExtensionSupportPlans}
     */
    public ExtensionSupportPlans resolveExtensionSupportPlans(Collection<String> supportPlanIds)
    {
        if (supportPlanIds != null) {
            XWikiContext xcontext = this.xcontextProvider.get();

            List<ExtensionSupportPlan> plans = new ArrayList<>(supportPlanIds.size());
            for (String supportPlanId : supportPlanIds) {
                ExtensionSupportPlan plan = resolveExtensionSupportPlan(supportPlanId, xcontext);

                if (plan != null) {
                    plans.add(plan);
                }
            }

            return new DefaultExtensionSupportPlans(plans);
        }

        return new DefaultExtensionSupportPlans(Collections.emptyList());
    }

    /**
     * @param supportPlanId the support plan identifier
     * @param xcontext the XWiki context
     * @return the support plan as a {@link ExtensionSupportPlan}
     */
    public ExtensionSupportPlan resolveExtensionSupportPlan(String supportPlanId, XWikiContext xcontext)
    {
        if (supportPlanId != null) {
            try {
                XWikiDocument document = xcontext.getWiki().getDocument(supportPlanId, xcontext);

                BaseObject supportPlanObject = getExtensionSupportPlanObject(document);

                if (supportPlanObject != null) {
                    ExtensionSupporter supporter = resolveExtensionSupporter(
                        getValue(supportPlanObject, XWikiRepositoryModel.PROP_SUPPORTPLAN_SUPPORTER), xcontext);

                    if (supporter != null
                        && getBooleanValue(supportPlanObject, XWikiRepositoryModel.PROP_SUPPORTPLAN_ACTIVE, false)) {
                        ExtensionSupporter extensionSupporter = supporter;
                        String name = document.getTitle();
                        boolean paying =
                            getBooleanValue(supportPlanObject, XWikiRepositoryModel.PROP_SUPPORTPLAN_PAYING, false);
                        URL url =
                            getURLValue(supportPlanObject, XWikiRepositoryModel.PROP_SUPPORTPLAN_URL, true, xcontext);

                        return new DefaultExtensionSupportPlan(extensionSupporter, name, url, paying);
                    }
                }
            } catch (Exception e) {
                this.logger.error("Failed to resolve the support plan with id [{}]: {}", supportPlanId,
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }

        return null;
    }

    /**
     * @param supporterId the supporter identifier
     * @param xcontext the XWiki context
     * @return the supporter as a {@link ExtensionSupporter}
     * @throws XWikiException when failing to load the supporter document
     */
    public ExtensionSupporter resolveExtensionSupporter(String supporterId, XWikiContext xcontext) throws XWikiException
    {
        if (supporterId != null) {
            XWikiDocument document = xcontext.getWiki().getDocument(supporterId, xcontext);

            BaseObject supporterObject = getExtensionSupporterObject(document);

            if (supporterObject != null
                && getBooleanValue(supporterObject, XWikiRepositoryModel.PROP_SUPPORTER_ACTIVE, false)) {
                String name = document.getTitle();
                URL url = getURLValue(supporterObject, XWikiRepositoryModel.PROP_SUPPORTER_URL, true, xcontext);

                return new DefaultExtensionSupporter(name, url);
            }
        }

        return null;
    }

    /**
     * @param extensionSupportPlanDocument the document containing the support plan
     * @return the xobject containing the support plan
     */
    public BaseObject getExtensionSupportPlanObject(XWikiDocument extensionSupportPlanDocument)
    {
        return extensionSupportPlanDocument.getXObject(XWikiRepositoryModel.EXTENSIONSUPPORTPLAN_CLASSREFERENCE);
    }

    /**
     * @param extensionSupporterDocument the document containing the supporter
     * @return the xobject containing the supporter
     */
    public BaseObject getExtensionSupporterObject(XWikiDocument extensionSupporterDocument)
    {
        return extensionSupporterDocument.getXObject(XWikiRepositoryModel.EXTENSIONSUPPORTER_CLASSREFERENCE);
    }

    /**
     * Retrieve the extension version document for the given extension document and the given version.
     * 
     * @param extensionDocument the document of the extension
     * @param extensionVersion the version for which to retrieve the document
     * @param xcontext the current context
     * @return the version document or the given extension document
     * @throws XWikiException in case of problem for loading the document
     * @since 17.9.0RC1
     */
    public XWikiDocument getExtensionVersionDocument(XWikiDocument extensionDocument, Version extensionVersion,
        XWikiContext xcontext) throws XWikiException
    {
        return getExtensionVersionDocument(extensionDocument, extensionVersion.getValue(), xcontext);
    }

    /**
     * Retrieve the extension version document for the given extension document and the given version.
     * 
     * @param extensionDocument the document of the extension
     * @param extensionVersion the version for which to retrieve the document
     * @param xcontext the current context
     * @return the version document or the given extension document
     * @throws XWikiException in case of problem for loading the document
     * @since 17.9.0RC1
     */
    public XWikiDocument getExtensionVersionDocument(XWikiDocument extensionDocument, String extensionVersion,
        XWikiContext xcontext) throws XWikiException
    {
        if (isVersionPageEnabled(extensionDocument)) {
            return xcontext.getWiki()
                .getDocument(
                    new PageReference(extensionVersion, new PageReference(
                        XWikiRepositoryModel.EXTENSIONVERSIONS_SPACENAME, extensionDocument.getPageReference())),
                    xcontext);
        }

        return extensionDocument;
    }

    /**
     * Retrieve the xobject of the version for given extension.
     * 
     * @param extensionVersionDocument the document that might contain the version object
     * @param extensionVersion the version for which to retrieve the object
     * @param create {@code true} to create the object if it doesn't exist
     * @param xcontext the current context
     * @return the version object or {@code null} if it doesn't exist and shouldn't be created
     * @throws XWikiException in case of problem for loading the document
     * @since 17.9.0RC1
     */
    public BaseObject getExtensionVersionObject(XWikiDocument extensionVersionDocument, Extension extensionVersion,
        boolean create, XWikiContext xcontext) throws XWikiException
    {
        // Update version object
        BaseObject versionObject =
            getExtensionVersionObject(extensionVersionDocument, extensionVersion.getId().getVersion());
        if (versionObject == null && create) {
            versionObject =
                extensionVersionDocument.newXObject(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE, xcontext);

            versionObject.set(XWikiRepositoryModel.PROP_VERSION_VERSION,
                extensionVersion.getId().getVersion().getValue(), xcontext);
        }

        return versionObject;
    }

    /**
     * Retrieve the xobject of the version from the given document.
     * 
     * @param document the document where to retrieve the version object.
     * @param version the version for which to retrieve the object
     * @return the version object or {@code null} if it doesn't exist and shouldn't be created
     * @since 17.9.0RC1
     */
    public BaseObject getExtensionVersionObject(XWikiDocument document, String version)
    {
        return getExtensionVersionObject(document, new DefaultVersion(version));
    }

    /**
     * Retrieve the xobject of the version from the given document.
     * 
     * @param document the document where to retrieve the version object.
     * @param version the version for which to retrieve the object
     * @return the version object or {@code null} if it doesn't exist and shouldn't be created
     * @since 17.9.0RC1
     */
    public BaseObject getExtensionVersionObject(XWikiDocument document, Version version)
    {
        List<BaseObject> objects = document.getXObjects(XWikiRepositoryModel.EXTENSIONVERSION_CLASSREFERENCE);
        if (objects != null) {
            for (BaseObject versionObject : objects) {
                if (versionObject != null) {
                    String versionString =
                        getValue(versionObject, XWikiRepositoryModel.PROP_VERSION_VERSION, (String) null);

                    if (StringUtils.isNotEmpty(versionString) && version.equals(new DefaultVersion(versionString))) {
                        return versionObject;
                    }
                }
            }
        }

        return null;
    }

    /**
     * @param extensionDocument the document containing the extension metadata
     * @return true if the passed extension document indicate that version should be proxied
     * @since 17.9.0RC1
     */
    public boolean isVersionProxyingEnabled(XWikiDocument extensionDocument)
    {
        BaseObject extensionProxyObject =
            extensionDocument.getXObject(XWikiRepositoryModel.EXTENSIONPROXY_CLASSREFERENCE);
        if (extensionProxyObject == null) {
            return false;
        }

        return XWikiRepositoryModel.PROP_PROXY_PROXYLEVEL_VALUE_VERSION
            .equals(getValue(extensionProxyObject, XWikiRepositoryModel.PROP_PROXY_PROXYLEVEL, (String) null));
    }

    /**
     * @param extensionId the identifier of the extension
     * @return the main document holder the extension metadata, or null if none count be found
     * @throws QueryException when failing to search for the extension document
     * @throws XWikiException when failing to get the extension document
     * @since 17.9.0RC1
     */
    public XWikiDocument getExistingExtensionDocumentById(String extensionId) throws QueryException, XWikiException
    {
        XWikiContext xcontext = this.xcontextProvider.get();

        DocumentReference[] cachedDocumentReference = this.documentReferenceCache.get(extensionId);

        if (cachedDocumentReference == null) {
            Query query = this.queryManager.createQuery(
                "select doc.fullName from Document doc, doc.object(" + XWikiRepositoryModel.EXTENSION_CLASSNAME
                    + ") as extension where extension." + XWikiRepositoryModel.PROP_EXTENSION_ID + " = :extensionId",
                Query.XWQL);

            query.bindValue("extensionId", extensionId);

            List<String> documentNames = query.execute();

            if (!documentNames.isEmpty()) {
                cachedDocumentReference =
                    new DocumentReference[] {this.currentStringResolver.resolve(documentNames.get(0))};
            } else {
                cachedDocumentReference = new DocumentReference[1];
            }

            this.documentReferenceCache.set(extensionId, cachedDocumentReference);
        }

        return cachedDocumentReference[0] != null ? xcontext.getWiki().getDocument(cachedDocumentReference[0], xcontext)
            : null;
    }

    /**
     * @param extensionId the identifier of the extension
     * @return the latest version of the extension
     * @throws QueryException when failing to search for the extension latest version
     * @since 17.9.0RC1
     */
    public String getLastVersion(String extensionId) throws QueryException
    {
        Query query =
            this.queryManager.createQuery("select version.version, version.index from Document doc, doc.object("
                + XWikiRepositoryModel.EXTENSIONVERSION_CLASSNAME
                + ") as version where version.id = :versionId and version.index is not null "
                + "order by version.index desc", Query.XWQL);

        query.bindValue("versionId", extensionId);
        query.setLimit(1);

        List<Object[]> results = query.execute();

        if (results.isEmpty()) {
            return null;
        }

        return (String) results.get(0)[0];
    }

    /**
     * @param extensionDocument the document holder the extension metadata
     * @return the object holding the main extension metadata
     * @since 17.9.0RC1
     */
    public BaseObject getExtensionObject(XWikiDocument extensionDocument)
    {
        return extensionDocument.getXObject(XWikiRepositoryModel.EXTENSION_CLASSREFERENCE);
    }

    /**
     * @param extensionId the identifier of the extension
     * @return true if extension version should be stored in dedicated pages
     * @throws QueryException when failing to search for the extension page
     * @throws XWikiException when failing to get the extension page
     * @since 17.9.0RC1
     */
    public boolean isVersionPageEnabled(String extensionId) throws QueryException, XWikiException
    {
        XWikiDocument document = getExistingExtensionDocumentById(extensionId);

        return isVersionPageEnabled(document);
    }

    /**
     * @param extensionDocument the main page holding extension metadata
     * @return true if extension version should be stored in dedicated pages
     * @since 17.9.0RC1
     */
    public boolean isVersionPageEnabled(XWikiDocument extensionDocument)
    {
        BaseObject extensionObject = getExtensionObject(extensionDocument);

        return isVersionPageEnabled(extensionObject);
    }

    /**
     * @param extensionOject the main object holding extension metadata
     * @return true if extension version should be stored in dedicated pages
     * @since 17.9.0RC1
     */
    public boolean isVersionPageEnabled(BaseObject extensionOject)
    {
        return getBooleanValue(extensionOject, XWikiRepositoryModel.PROP_EXTENSION_VERSIONPAGE, false);
    }

    /**
     * @param extensionOject true if extension version should be stored in dedicated pages
     * @return true if the object has been modified, false otherwise
     * @since 17.9.0RC1
     */
    public boolean setVersionPageEnabled(BaseObject extensionOject)
    {
        if (!isVersionPageEnabled(extensionOject)) {
            extensionOject.setIntValue(XWikiRepositoryModel.PROP_EXTENSION_VERSIONPAGE, 1);

            return true;
        }

        return false;
    }

    /**
     * @param document the document holding the extension metadata
     * @return the identifier of the extension
     * @since 17.9.0RC1
     */
    public String getExtensionId(XWikiDocument document)
    {
        return document.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_ID);
    }

    /**
     * @param document the document holding the extension metadata
     * @return the name of the extension
     * @since 17.9.0RC1
     */
    public String getExtensionName(XWikiDocument document)
    {
        return document.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_NAME);
    }

    /**
     * @param document the document holding the extension metadata
     * @return the type of the extension
     * @since 17.9.0RC1
     */
    public String getExtensionType(XWikiDocument document)
    {
        return document.getStringValue(XWikiRepositoryModel.PROP_EXTENSION_TYPE);
    }
}
