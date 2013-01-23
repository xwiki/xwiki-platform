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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
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
import org.xwiki.localization.TranslationBundleContext;
import org.xwiki.localization.TranslationBundleDoesNotExistsException;
import org.xwiki.localization.TranslationBundleFactory;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.localization.wiki.internal.TranslationDocumentModel.Scope;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.RegexEntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
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

    private static final RegexEntityReference TRANSLATIONOBJET = new RegexEntityReference(Pattern.compile("[^:]+:"
        + TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE_STRING + "\\[\\d*\\]"), EntityType.OBJECT);

    private static final List<Event> EVENTS = Arrays.<Event> asList(new XObjectAddedEvent(TRANSLATIONOBJET),
        new XObjectUpdatedEvent(TRANSLATIONOBJET), new XObjectDeletedEvent(TRANSLATIONOBJET));

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
    private Logger logger;

    @Inject
    private QueryManager queryManager;

    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Used to access the current bundles.
     */
    @Inject
    private TranslationBundleContext bundleContext;

    private Cache<TranslationBundle> bundlesCache;

    private EventListener listener = new EventListener()
    {
        @Override
        public void onEvent(Event event, Object arg1, Object arg2)
        {
            XWikiDocument document = (XWikiDocument) arg1;

            if (event instanceof XObjectAddedEvent) {
                translationObjectAdded(document);
            } else if (event instanceof XObjectDeletedEvent) {
                translationObjectDeleted(document);
            } else {
                translationObjectUpdated(document);
            }
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

    @Override
    public void initialize() throws InitializationException
    {
        // Cache
        CacheConfiguration cacheConfiguration = new CacheConfiguration("localization.bundle.document");

        try {
            this.bundlesCache = this.cacheManager.createNewCache(cacheConfiguration);
        } catch (CacheException e) {
            this.logger.error("Failed to create cache [{}]", cacheConfiguration.getConfigurationId(), e);
        }

        // Load existing translations

        XWikiContext xcontext = this.xcontextProvider.get();

        Set<String> wikis;
        try {
            wikis = new HashSet<String>(xcontext.getWiki().getVirtualWikisDatabaseNames(xcontext));
        } catch (XWikiException e) {
            this.logger.error("Failed to list existing wikis", e);
            wikis = new HashSet<String>();
        }

        if (!wikis.contains(xcontext.getMainXWiki())) {
            wikis.add(xcontext.getMainXWiki());
        }

        for (String wiki : wikis) {
            loadTranslations(wiki, xcontext);
        }

        // Listener
        this.observation.addListener(this.listener);
    }

    private void loadTranslations(String wiki, XWikiContext xcontext)
    {
        try {
            Query query =
                this.queryManager.createQuery(String.format(
                    "select distinct doc.space, doc.name from Document doc, doc.object(%s) as translation",
                    TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE_STRING), Query.XWQL);

            query.setWiki(wiki);

            List<Object[]> documents = query.execute();
            for (Object[] documentName : documents) {
                DocumentReference reference =
                    new DocumentReference(wiki, (String) documentName[0], (String) documentName[1]);

                XWikiDocument document = xcontext.getWiki().getDocument(reference, xcontext);

                registerTranslationBundle(document);
            }
        } catch (Exception e) {
            this.logger.error("Failed to load existing translations", e);
        }
    }

    @Override
    public TranslationBundle getBundle(String bundleId) throws TranslationBundleDoesNotExistsException
    {
        String id = AbstractDocumentTranslationBundle.ID_PREFIX + bundleId;

        if (this.componentManagerProvider.get().hasComponent(TranslationBundle.class, id)) {
            try {
                return this.componentManagerProvider.get().getInstance(TranslationBundle.class, id);
            } catch (ComponentLookupException e) {
                this.logger.debug("Failed to lookup component [{}] with hint [{}].", TranslationBundle.class, bundleId,
                    e);
            }
        }

        return getDocumentBundle(this.currentResolver.resolve(bundleId));
    }

    private TranslationBundle getDocumentBundle(DocumentReference documentReference)
        throws TranslationBundleDoesNotExistsException
    {
        String uid = this.uidSerializer.serialize(documentReference);

        TranslationBundle bundle = this.bundlesCache.get(uid);
        if (bundle == null) {
            synchronized (this.bundlesCache) {
                bundle = this.bundlesCache.get(uid);
                if (bundle == null) {
                    bundle = createDocumentBundle(documentReference);
                    this.bundlesCache.set(uid, bundle);
                }
            }
        }

        return bundle;
    }

    private DefaultDocumentTranslationBundle createDocumentBundle(DocumentReference documentReference)
        throws TranslationBundleDoesNotExistsException
    {
        XWikiContext context = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            throw new TranslationBundleDoesNotExistsException("Failed to get translation document", e);
        }

        if (document.isNew()) {
            throw new TranslationBundleDoesNotExistsException(String.format("Document [%s] does not exists",
                documentReference));
        }

        return createDocumentBundle(document);
    }

    private DefaultDocumentTranslationBundle createDocumentBundle(XWikiDocument document)
        throws TranslationBundleDoesNotExistsException
    {
        BaseObject translationObject = document.getXObject(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE);

        if (translationObject == null) {
            throw new TranslationBundleDoesNotExistsException(String.format("[%s] is not a translation document",
                document));
        }

        DefaultDocumentTranslationBundle documentBundle;
        try {
            documentBundle =
                new DefaultDocumentTranslationBundle(document.getDocumentReference(),
                    this.componentManagerProvider.get(), this.translationParser);
        } catch (ComponentLookupException e) {
            throw new TranslationBundleDoesNotExistsException("Failed to create document bundle", e);
        }

        return documentBundle;
    }

    /**
     * @param document the translation document
     */
    private void translationObjectUpdated(XWikiDocument document)
    {
        unregisterTranslationBundle(document.getOriginalDocument());
        try {
            registerTranslationBundle(document);
        } catch (Exception e) {
            this.logger.error("Failed to register translation bundle from document [{}]",
                document.getDocumentReference(), e);
        }
    }

    /**
     * @param document the translation document
     */
    private void translationObjectDeleted(XWikiDocument document)
    {
        unregisterTranslationBundle(document.getOriginalDocument());
    }

    /**
     * @param document the translation document
     */
    private void translationObjectAdded(XWikiDocument document)
    {
        try {
            registerTranslationBundle(document);
        } catch (Exception e) {
            this.logger.error("Failed to register translation bundle from document [{}]",
                document.getDocumentReference(), e);
        }
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
        Scope scope = getScope(document.getXObject(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE));

        // Unregister component
        if (scope != null && scope != Scope.ON_DEMAND) {
            ComponentDescriptor<TranslationBundle> descriptor =
                createComponentDescriptor(document.getDocumentReference());

            getComponentManager(document, scope, true).unregisterComponent(descriptor);
        }

        // Remove from cache
        this.bundlesCache.remove(this.uidSerializer.serialize(document.getDocumentReference()));
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
        Scope scope = getScope(document.getXObject(TranslationDocumentModel.TRANSLATIONCLASS_REFERENCE));

        if (scope != null && scope != Scope.ON_DEMAND) {
            checkRegistrationAuthorization(document, scope);

            DefaultDocumentTranslationBundle bundle = createDocumentBundle(document);

            ComponentDescriptor<TranslationBundle> descriptor =
                createComponentDescriptor(document.getDocumentReference());

            getComponentManager(document, scope, true).registerComponent(descriptor, bundle);

            this.bundleContext.addBundle(bundle);
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
        switch (scope) {
            case GLOBAL:
                this.authorizationManager.checkAccess(Right.PROGRAM, document.getAuthorReference(), new WikiReference(
                    this.xcontextProvider.get().getMainXWiki()));
                break;
            case WIKI:
                this.authorizationManager.checkAccess(Right.ADMIN, document.getAuthorReference(), document
                    .getDocumentReference().getWikiReference());
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

        descriptor.setImplementation(DefaultDocumentTranslationBundle.class);
        descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        descriptor.setRoleHint(AbstractDocumentTranslationBundle.ID_PREFIX
            + this.serializer.serialize(documentReference));
        descriptor.setRoleType(TranslationBundle.class);

        return descriptor;
    }

    /**
     * Get the right component manager based on the scope.
     * 
     * @param document the translation document
     * @param scope the translation scope
     * @param create true if the component manager should be created if it does not exists
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
    }
}
