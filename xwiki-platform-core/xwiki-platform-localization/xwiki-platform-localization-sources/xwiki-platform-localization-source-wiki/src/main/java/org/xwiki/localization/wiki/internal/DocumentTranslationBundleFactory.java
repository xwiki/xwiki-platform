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
package org.xwiki.localization.wiki.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.localization.TranslationBundle;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.localization.wiki.internal.TranslationDocumentModel.Scope;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.StringProperty;

/**
 * Generate and manage wiki document based translations bundles.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named(DocumentTranslationBundleFactory.ID)
@Singleton
public class DocumentTranslationBundleFactory implements TranslationBundleFactory, Initializable, Disposable
{
    /**
     * The identifier of this {@link TranslationBundleFactory}.
     */
    public final static String ID = "document";

    /**
     * The prefix to use in all wiki document based translations.
     */
    public static final String ID_PREFIX = ID + ':';

    private static final List<Event> EVENTS = Arrays.<Event>asList(new DocumentUpdatedEvent(),
        new DocumentDeletedEvent(), new DocumentCreatedEvent());

    private static final List<Event> WIKIEVENTS = Arrays.<Event>asList(new WikiReadyEvent());

    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    @Named("uid")
    private EntityReferenceSerializer<String> uidSerializer;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentResolver;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private ObservationManager observation;

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    @Named("messagetool/1.0")
    private TranslationMessageParser translationParser;

    @Inject
    private ComponentManagerManager cmManager;

    @Inject
    private WikiDescriptorManager wikiManager;

    @Inject
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private WikiTranslationConfiguration configuration;

    /**
     * Used to cache on demand document bundles (those that are not registered as components).
     */
    private Cache<TranslationBundle> onDemandBundleCache;

    private final EventListener listener = new EventListener()
    {
        @Override
        public void onEvent(Event event, Object arg1, Object arg2)
        {
            translationDocumentUpdated((XWikiDocument) arg1);
        }

        @Override
        public String getName()
        {
            return "localization.bundle.document";
        }

        @Override
        public List<Event> getEvents()
        {
            return EVENTS;
        }
    };

    private final EventListener wikilistener = new EventListener()
    {
        @Override
        public void onEvent(Event event, Object arg1, Object arg2)
        {
            loadTranslations(((WikiReadyEvent) event).getWikiId());
        }

        @Override
        public String getName()
        {
            return "localization.wikiready";
        }

        @Override
        public List<Event> getEvents()
        {
            return WIKIEVENTS;
        }
    };

    @Override
    public void initialize() throws InitializationException
    {
        // Cache
        CacheConfiguration cacheConfiguration = new CacheConfiguration("localization.bundle.document");

        try {
            this.onDemandBundleCache = this.cacheManager.createNewCache(cacheConfiguration);
        } catch (CacheException e) {
            this.logger.error("Failed to create cache [{}]", cacheConfiguration.getConfigurationId(), e);
        }

        // Load existing translations from main wiki, wait for WikiReaderEvent for other wikis

        loadTranslations(this.wikiManager.getMainWikiId());

        // Listeners
        this.observation.addListener(this.listener, EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY);
        this.observation.addListener(this.wikilistener);
    }

    private void loadTranslations(String wiki)
    {
        XWikiContext xcontext = this.xcontextProvider.get();
        WikiReference wikiReference = new WikiReference(wiki);

        List<String> documents;
        try {
            Query query =
                this.queryManager.createQuery(String.format(
                    "select distinct doc.fullName from Document doc, doc.object(%s) as translation",
                    TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE_STRING), Query.XWQL);

            query.setWiki(wiki);

            documents = query.execute();
        } catch (Exception e) {
            this.logger.error("Failed to find translation documents", e);

            return ;
        }

        for (String documentName : documents) {
            DocumentReference reference = this.currentResolver.resolve(documentName, wikiReference);

            try {
                XWikiDocument document = xcontext.getWiki().getDocument(reference, xcontext);

                registerTranslationBundle(document);
            } catch (Exception e) {
                this.logger.error("Failed to load and register the translation bundle from document [{}]", reference,
                    e);
            }
        }
    }

    @Override
    public TranslationBundle getBundle(String bundleId) throws TranslationBundleDoesNotExistsException
    {
        String roleHint = ID_PREFIX + bundleId;

        if (this.componentManagerProvider.get().hasComponent(TranslationBundle.class, roleHint)) {
            try {
                return this.componentManagerProvider.get().getInstance(TranslationBundle.class, roleHint);
            } catch (ComponentLookupException e) {
                this.logger.debug("Failed to lookup component [{}] with hint [{}].", TranslationBundle.class, bundleId,
                    e);
            }
        }

        return getOnDemandDocumentBundle(this.currentResolver.resolve(bundleId));
    }

    /**
     * Get non-component bundle.
     */
    private TranslationBundle getOnDemandDocumentBundle(DocumentReference documentReference)
        throws TranslationBundleDoesNotExistsException
    {
        String uid = this.uidSerializer.serialize(documentReference);

        TranslationBundle bundle = this.onDemandBundleCache.get(uid);
        if (bundle == null) {
            synchronized (this.onDemandBundleCache) {
                bundle = this.onDemandBundleCache.get(uid);
                if (bundle == null) {
                    bundle = createOnDemandDocumentBundle(documentReference, uid);
                    this.onDemandBundleCache.set(uid, bundle);
                }
            }
        }

        return bundle;
    }

    private OnDemandDocumentTranslationBundle createOnDemandDocumentBundle(DocumentReference documentReference,
        String uid) throws TranslationBundleDoesNotExistsException
    {
        XWikiContext context = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            throw new TranslationBundleDoesNotExistsException("Failed to get translation document", e);
        }

        if (document.isNew()) {
            throw new TranslationBundleDoesNotExistsException(String.format("Document [%s] does not exist",
                documentReference));
        }

        OnDemandDocumentTranslationBundle documentBundle;
        try {
            documentBundle =
                new OnDemandDocumentTranslationBundle(ID_PREFIX, document.getDocumentReference(),
                    this.componentManagerProvider.get(), this.translationParser, this, uid);
        } catch (ComponentLookupException e) {
            throw new TranslationBundleDoesNotExistsException("Failed to create document bundle", e);
        }

        return documentBundle;
    }

    private ComponentDocumentTranslationBundle createComponentDocumentBundle(XWikiDocument document,
        ComponentDescriptor<TranslationBundle> descriptor) throws TranslationBundleDoesNotExistsException
    {
        ComponentDocumentTranslationBundle documentBundle;
        try {
            documentBundle =
                new ComponentDocumentTranslationBundle(ID_PREFIX, document.getDocumentReference(),
                    this.componentManagerProvider.get(), this.translationParser, descriptor, this);
        } catch (ComponentLookupException e) {
            throw new TranslationBundleDoesNotExistsException("Failed to create document bundle", e);
        }

        return documentBundle;
    }

    /**
     * @param uid remove the bundle from the cache
     */
    void clear(String uid)
    {
        this.onDemandBundleCache.remove(uid);
    }

    /**
     * @param document the translation document
     */
    private void translationDocumentUpdated(XWikiDocument document)
    {
        if (!document.getOriginalDocument().isNew()) {
            unregisterTranslationBundle(document.getOriginalDocument());
        }

        if (!document.isNew()) {
            try {
                registerTranslationBundle(document);
            } catch (Exception e) {
                this.logger.error("Failed to register translation bundle from document [{}]",
                    document.getDocumentReference(), e);
            }
        }
    }

    private Scope getScope(XWikiDocument document)
    {
        BaseObject obj = document.getXObject(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE);

        if (obj != null) {
            return getScope(obj);
        }

        return null;
    }

    /**
     * @param obj the translation object
     * @return the {@link Scope} stored in the object, null not assigned or unknown
     */
    private Scope getScope(BaseObject obj)
    {
        if (obj != null) {
            StringProperty scopeProperty =
                (StringProperty) obj.getField(TranslationDocumentModel.TRANSLATIONCLASS_PROP_SCOPE);

            if (scopeProperty != null) {
                String scopeString = scopeProperty.getValue();

                return EnumUtils.getEnum(Scope.class, scopeString.toUpperCase());
            }
        }

        return null;
    }

    /**
     * @param document the translation document
     */
    private void unregisterTranslationBundle(XWikiDocument document)
    {
        Scope scope = getScope(document);

        // Unregister component
        if (scope != null && scope != Scope.ON_DEMAND) {
            ComponentDescriptor<TranslationBundle> descriptor =
                createComponentDescriptor(document.getDocumentReference());

            getComponentManager(document, scope, true).unregisterComponent(descriptor);
        }

        // Remove from cache
        this.onDemandBundleCache.remove(this.uidSerializer.serialize(document.getDocumentReference()));
    }

    /**
     * @param document the translation document
     * @throws TranslationBundleDoesNotExistsException when no translation bundle could be created from the provided
     *             document
     * @throws ComponentRepositoryException when the actual registration of the document bundle failed
     * @throws AccessDeniedException when the document author does not have enough right to register the translation
     *             bundle
     */
    private void registerTranslationBundle(XWikiDocument document) throws TranslationBundleDoesNotExistsException,
        ComponentRepositoryException, AccessDeniedException
    {
        Scope scope = getScope(document);

        if (scope != null && scope != Scope.ON_DEMAND) {
            checkRegistrationAuthorization(document, scope);

            ComponentDescriptor<TranslationBundle> descriptor =
                createComponentDescriptor(document.getDocumentReference());

            ComponentDocumentTranslationBundle bundle = createComponentDocumentBundle(document, descriptor);

            getComponentManager(document, scope, true).registerComponent(descriptor, bundle);
        }
    }

    /**
     * Checks that the author of a document defining a translation bundle has the necessary rights to make it
     * available, based on the scope of the default locale translation bundle.
     *
     * @param document the document defining the translation bundle to check
     * @param defaultLocaleDocument the document containing the default locale translation bundle
     * @throws AccessDeniedException when the document author does not have enough rights for the defined scope
     */
    protected void checkRegistrationAuthorizationForDocumentLocaleBundle(XWikiDocument document,
        XWikiDocument defaultLocaleDocument) throws AccessDeniedException
    {
        Scope scope = getScope(defaultLocaleDocument);
        if (scope != null && scope != Scope.ON_DEMAND) {
            checkRegistrationAuthorization(document, scope);
        }
    }

    /**
     * @param document the translation document
     * @param scope the scope
     * @throws AccessDeniedException thrown when the document author does not have enough right for the provided
     *             {@link Scope}
     */
    private void checkRegistrationAuthorization(XWikiDocument document, Scope scope) throws AccessDeniedException
    {
        EntityReference entityReference;
        switch (scope) {
            case GLOBAL:
                this.authorizationManager.checkAccess(Right.PROGRAM, document.getAuthorReference(), null);
                this.authorizationManager.checkAccess(Right.PROGRAM, document.getContentAuthorReference(), null);
                break;
            case WIKI:
                entityReference = document.getDocumentReference().getWikiReference();
                this.authorizationManager.checkAccess(Right.ADMIN, document.getAuthorReference(), entityReference);
                this.authorizationManager.checkAccess(Right.ADMIN, document.getContentAuthorReference(),
                    entityReference);
                break;
            case USER:
                if (this.configuration.isRestrictUserTranslations()) {
                    entityReference = document.getDocumentReference();
                    this.authorizationManager.checkAccess(Right.SCRIPT, document.getAuthorReference(), entityReference);
                    this.authorizationManager.checkAccess(Right.SCRIPT, document.getContentAuthorReference(),
                        entityReference);
                }
                break;
            default:
                break;
        }
    }

    /**
     * @param documentReference the translation document reference
     * @return the component descriptor to use to register/unregister the translation bundle
     */
    private ComponentDescriptor<TranslationBundle> createComponentDescriptor(DocumentReference documentReference)
    {
        DefaultComponentDescriptor<TranslationBundle> descriptor = new DefaultComponentDescriptor<TranslationBundle>();

        descriptor.setImplementation(ComponentDocumentTranslationBundle.class);
        descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        descriptor.setRoleHint(ID_PREFIX + this.serializer.serialize(documentReference));
        descriptor.setRoleType(TranslationBundle.class);

        return descriptor;
    }

    /**
     * Get the right component manager based on the scope.
     * 
     * @param document the translation document
     * @param scope the translation scope
     * @param create true if the component manager should be created if it does not exist
     * @return the component manager corresponding to the provided {@link Scope}
     */
    private ComponentManager getComponentManager(XWikiDocument document, Scope scope, boolean create)
    {
        String hint;

        switch (scope) {
            case WIKI:
                hint = "wiki:" + document.getDocumentReference().getWikiReference().getName();
                break;
            case USER:
                hint = "user:" + this.serializer.serialize(document.getAuthorReference());
                break;
            default:
                hint = null;
                break;
        }

        return this.cmManager.getComponentManager(hint, create);
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.observation.removeListener(this.listener.getName());
        this.observation.removeListener(this.wikilistener.getName());
    }
}
