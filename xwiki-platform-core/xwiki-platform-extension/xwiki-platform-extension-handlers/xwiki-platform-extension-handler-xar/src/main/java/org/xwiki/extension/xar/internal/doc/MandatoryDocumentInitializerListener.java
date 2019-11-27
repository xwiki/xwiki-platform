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
package org.xwiki.extension.xar.internal.doc;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.event.ComponentDescriptorEvent;
import org.xwiki.component.event.ComponentDescriptorRemovedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.internal.event.MandatoryDocumentsInitializedEvent;

/**
 * Keeps the {@link InstalledExtensionDocumentTree} up to date with respect to documents created by
 * {@link MandatoryDocumentInitializer}s.
 * 
 * @version $Id$
 * @since 11.10
 */
@Component
@Named(MandatoryDocumentInitializerListener.HINT)
@Singleton
public class MandatoryDocumentInitializerListener extends AbstractEventListener
{
    /**
     * The component hint.
     */
    public static final String HINT = "InstalledExtensionDocumentTree.MandatoryDocumentInitializerListener";

    @Inject
    private Logger logger;

    @Inject
    private InstalledExtensionDocumentTree tree;

    @Inject
    @Named("relative")
    private EntityReferenceResolver<String> relativeStringEntityReferenceResolver;

    @Inject
    private DocumentReferenceResolver<EntityReference> defaultReferenceDocumentReferenceResolver;

    @Inject
    private Provider<WikiDescriptorManager> wikiDescriptorManagerProvider;

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * Used to check if the XWiki database is ready when {@link ComponentDescriptorAddedEvent} and
     * {@link ComponentDescriptorRemovedEvent} are fired, because we need to fetch the list of wikis from the database
     * when the {@link MandatoryDocumentInitializer} specifies a relative document reference.
     */
    @Inject
    @Named("readonly")
    private Provider<XWikiContext> readOnlyXWikiContextProvider;

    /**
     * Default constructor.
     */
    public MandatoryDocumentInitializerListener()
    {
        super(HINT, new MandatoryDocumentsInitializedEvent(),
            new ComponentDescriptorAddedEvent(MandatoryDocumentInitializer.class),
            new ComponentDescriptorRemovedEvent(MandatoryDocumentInitializer.class));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof MandatoryDocumentsInitializedEvent) {
            getMandatoryDocuments(this.wikiDescriptorManagerProvider.get().getCurrentWikiId()).stream()
                .forEach(this.tree::addExtensionPage);
        } else if (event instanceof ComponentDescriptorAddedEvent) {
            getMandatoryDocuments(event).stream().forEach(this.tree::addExtensionPage);
        } else if (event instanceof ComponentDescriptorRemovedEvent) {
            getMandatoryDocuments(event).stream().forEach(this.tree::removeExtensionPage);
        }
    }

    private Set<DocumentReference> getMandatoryDocuments(String wiki)
    {
        try {
            Collection<MandatoryDocumentInitializer> mandatoryDocumentInitializers =
                this.contextComponentManagerProvider.get().getInstanceList(MandatoryDocumentInitializer.class);
            WikiReference wikiReference = new WikiReference(wiki);
            Set<DocumentReference> mandatoryDocuments = new HashSet<>();
            for (MandatoryDocumentInitializer mandatoryDocumentInitializer : mandatoryDocumentInitializers) {
                DocumentReference documentReference = this.defaultReferenceDocumentReferenceResolver
                    .resolve(mandatoryDocumentInitializer.getDocumentReference(), wikiReference);
                if (documentReference.getWikiReference().equals(wikiReference)) {
                    mandatoryDocuments.add(documentReference);
                }
            }
            return mandatoryDocuments;
        } catch (Exception e) {
            this.logger.error("Failed to retrieve the list of mandatory documents for wiki [{}].", wiki, e);
            return Collections.emptySet();
        }
    }

    private Set<DocumentReference> getMandatoryDocuments(Event event)
    {
        EntityReference relativeDocumentReference = this.relativeStringEntityReferenceResolver
            .resolve(((ComponentDescriptorEvent) event).getRoleHint(), EntityType.DOCUMENT);
        EntityReference wikiReference = relativeDocumentReference.extractReference(EntityType.WIKI);
        if (wikiReference != null) {
            return Collections.singleton(new DocumentReference(relativeDocumentReference));
        } else if (this.readOnlyXWikiContextProvider.get() != null) {
            // Skip this mandatory document if the XWiki database is not ready (it's too early to get the list of
            // wikis). It will be handled later when the MandatoryDocumentsInitializedEvent is fired.
            try {
                return this.wikiDescriptorManagerProvider.get().getAllIds().stream()
                    .map(wikiId -> this.defaultReferenceDocumentReferenceResolver.resolve(relativeDocumentReference,
                        new WikiReference(wikiId)))
                    .collect(Collectors.toSet());
            } catch (WikiManagerException e) {
                this.logger.error("Failed to get the list of wikis.", e);
            }
        }
        return Collections.emptySet();
    }
}
