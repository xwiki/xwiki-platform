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
import org.xwiki.localization.Bundle;
import org.xwiki.localization.BundleDoesNotExistsException;
import org.xwiki.localization.BundleFactory;
import org.xwiki.localization.message.TranslationMessageParser;
import org.xwiki.localization.wiki.internal.TranslationModel.Scope;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.RegexEntityReference;
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
 * Generate and manager wiki document based translations bundles.
 * 
 * @version $Id$
 * @since 4.3M2
 */
@Component
@Named("document")
@Singleton
public class DocumentBundleFactory implements BundleFactory, Initializable, Disposable
{
    private static final RegexEntityReference TRANSLATIONOBJET = new RegexEntityReference(Pattern.compile("[^:]+:"
        + TranslationModel.TRANSLATIONCLASS_REFERENCE_STRING + "\\[\\d*\\]"), EntityType.OBJECT);

    private static final List<Event> EVENTS = Arrays.<Event> asList(new XObjectAddedEvent(TRANSLATIONOBJET),
        new XObjectUpdatedEvent(TRANSLATIONOBJET), new XObjectDeletedEvent(TRANSLATIONOBJET));

    @Inject
    private ComponentManager componentManager;

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

    private Cache<Bundle> bundlesCache;

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
                    "select doc.space, doc.name from Document doc, doc.object(%s) as translation",
                    TranslationModel.TRANSLATIONCLASS_REFERENCE_STRING), Query.XWQL);

            query.setWiki(wiki);

            List<Object[]> documents = query.execute();
            for (Object[] documentName : documents) {
                DocumentReference reference =
                    new DocumentReference(wiki, (String) documentName[0], (String) documentName[1]);

                XWikiDocument document = xcontext.getWiki().getDocument(reference, xcontext);

                registerTranslationBundle(document);
            }
        } catch (Exception e) {
            this.logger.error("Failed to load eexisting translations", e);
        }
    }

    @Override
    public Bundle getBundle(String bundleId) throws BundleDoesNotExistsException
    {
        try {
            return this.componentManager.getInstance(Bundle.class, AbstractDocumentBundle.ID_PREFIX + bundleId);
        } catch (ComponentLookupException e) {
            this.logger.debug("Failed to lookup component [{}] with hint [{}].", Bundle.class, bundleId, e);
        }

        if (bundleId.startsWith(AbstractDocumentBundle.ID_PREFIX)) {
            String referenceString = bundleId.substring(AbstractDocumentBundle.ID_PREFIX.length());

            return getDocumentBundle(this.currentResolver.resolve(referenceString));
        }

        throw new BundleDoesNotExistsException(String.format(
            "Unsupported bundle identifier [%s]. Should start with [%s]", bundleId, AbstractDocumentBundle.ID_PREFIX));
    }

    private Bundle getDocumentBundle(DocumentReference documentReference) throws BundleDoesNotExistsException
    {
        String uid = this.uidSerializer.serialize(documentReference);

        Bundle bundle = this.bundlesCache.get(uid);
        if (bundle == null) {
            synchronized (this.bundlesCache) {
                bundle = this.bundlesCache.get(uid);
                if (bundle == null) {
                    bundle = createDocumentBundle(documentReference);
                }
            }
        }

        return bundle;
    }

    private DefaultDocumentBundle createDocumentBundle(DocumentReference documentReference)
        throws BundleDoesNotExistsException
    {
        XWikiContext context = this.xcontextProvider.get();

        XWikiDocument document;
        try {
            document = context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            throw new BundleDoesNotExistsException("Failed to get translation document", e);
        }

        if (document.isNew()) {
            throw new BundleDoesNotExistsException(String.format("Document [%s] does not exists", documentReference));
        }

        return createDocumentBundle(document);
    }

    private DefaultDocumentBundle createDocumentBundle(XWikiDocument document) throws BundleDoesNotExistsException
    {
        BaseObject translationObject = document.getXObject(TranslationModel.TRANSLATIONCLASS_REFERENCE);

        if (translationObject == null) {
            throw new BundleDoesNotExistsException(String.format("[%s] is not a translation document", document));
        }

        DefaultDocumentBundle documentBundle;
        try {
            documentBundle =
                new DefaultDocumentBundle(document.getDocumentReference(), this.componentManager,
                    this.translationParser);
        } catch (ComponentLookupException e) {
            throw new BundleDoesNotExistsException("Failed to create document bundle", e);
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
            StringProperty scopeProperty = (StringProperty) obj.getField(TranslationModel.TRANSLATIONCLASS_PROP_SCOPE);

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
        Scope scope = getScope(document.getXObject(TranslationModel.TRANSLATIONCLASS_REFERENCE));

        if (scope != null) {
            ComponentDescriptor<Bundle> descriptor = createComponentDescriptor(document.getDocumentReference());

            getComponentManager(document, scope, true).unregisterComponent(descriptor);
        }
    }

    /**
     * @param document the translation document
     * @throws BundleDoesNotExistsException when no translation bundle could be created from the provided document
     * @throws ComponentRepositoryException when the actual registration of the document bundle failed
     * @throws AccessDeniedException when the document author does not have enough right to register the translation
     *             bundle
     */
    private void registerTranslationBundle(XWikiDocument document) throws BundleDoesNotExistsException,
        ComponentRepositoryException, AccessDeniedException
    {
        Scope scope = getScope(document.getXObject(TranslationModel.TRANSLATIONCLASS_REFERENCE));

        if (scope != null) {
            checkRegistrationAuthorization(document, scope);

            DefaultDocumentBundle bundle = createDocumentBundle(document);

            ComponentDescriptor<Bundle> descriptor = createComponentDescriptor(document.getDocumentReference());

            getComponentManager(document, scope, true).registerComponent(descriptor, bundle);
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
                this.authorizationManager.checkAccess(Right.PROGRAM, document.getAuthorReference(), null);
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
    private ComponentDescriptor<Bundle> createComponentDescriptor(DocumentReference documentReference)
    {
        DefaultComponentDescriptor<Bundle> descriptor = new DefaultComponentDescriptor<Bundle>();

        descriptor.setImplementation(DefaultDocumentBundle.class);
        descriptor.setInstantiationStrategy(ComponentInstantiationStrategy.SINGLETON);
        descriptor.setRoleHint(AbstractDocumentBundle.ID_PREFIX + this.serializer.serialize(documentReference));
        descriptor.setRoleType(Bundle.class);

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
                hint = "root";
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
