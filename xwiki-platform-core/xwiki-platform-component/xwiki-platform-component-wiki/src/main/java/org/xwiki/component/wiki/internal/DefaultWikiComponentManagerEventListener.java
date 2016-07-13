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
package org.xwiki.component.wiki.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * An {@link EventListener} responsible for registering all the wiki components when the application starts. It also
 * dynamically registers and unregisters wiki components when documents holding those components are created, updated,
 * or deleted.
 * 
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("defaultWikiComponentManagerEventListener")
@Singleton
public class DefaultWikiComponentManagerEventListener implements EventListener
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The wiki component manager that knows how to register component definition against the underlying CM.
     */
    @Inject
    private WikiComponentManager wikiComponentManager;

    /**
     * The wiki component stores, used to retrieve component definitions.
     */
    @Inject
    private List<WikiComponentBuilder> wikiComponentProviders;

    /**
     * Used to access the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private ObservationContext observationContext;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.asList(
            new DocumentCreatedEvent(),
            new DocumentUpdatedEvent(),
            new DocumentDeletedEvent(),
            new ApplicationReadyEvent(),
            new WikiReadyEvent());
    }

    @Override
    public String getName()
    {
        return "defaultWikiComponentManagerEventListener";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (source instanceof DocumentModelBridge) {
            DocumentModelBridge document = (DocumentModelBridge) source;
            DocumentReference documentReference = document.getDocumentReference();

            if (event instanceof DocumentCreatedEvent || event instanceof DocumentUpdatedEvent) {
                registerComponents(document);
            } else if (event instanceof DocumentDeletedEvent) {
                // Unregister components from the deleted document, if any
                unregisterComponents(documentReference);
            }
        } else if (event instanceof ApplicationReadyEvent || event instanceof WikiReadyEvent) {
            // These 2 events are created when the database is ready. We register all wiki components.
            registerAllComponents();
        }
    }

    /**
     * Register all the wiki components in the current wiki.
     */
    private void registerAllComponents()
    {
        // Retrieve all components definitions and register them.
        for (WikiComponentBuilder provider : this.wikiComponentProviders) {
            for (DocumentReference reference : provider.getDocumentReferences()) {
                try {
                    List<WikiComponent> components = provider.buildComponents(reference);
                    for (WikiComponent component : components) {
                        this.wikiComponentManager.registerWikiComponent(component);
                    }
                } catch (Exception e) {
                    this.logger.warn("Failed to register the wiki component located in the document [{}]: {}",
                        reference, ExceptionUtils.getRootCauseMessage(e));
                }
            }
        }
    }

    /**
     * Register wiki components from a given document.
     * 
     * @param document the document to register the components for
     */
    private void registerComponents(DocumentModelBridge document)
    {
        DocumentReference documentReference = document.getDocumentReference();

        // Unregister all wiki components registered under this document. We do this as otherwise we would need to
        // handle the specific cases of xobject added, xobject updated, xobject deleted, etc. Instead we unregister
        // all wiki components and re-register them all.
        unregisterComponents(documentReference);

        // Re-register all wiki components in the passed document
        for (WikiComponentBuilder provider : this.wikiComponentProviders) {
            // Check whether the given document has a wiki component defined in it.
            if (provider.getDocumentReferences().contains(documentReference)) {
                try {
                    List<WikiComponent> components = provider.buildComponents(documentReference);
                    for (WikiComponent component : components) {
                        // Register the component.
                        try {
                            this.wikiComponentManager.registerWikiComponent(component);
                        } catch (WikiComponentException e) {
                            this.logger.warn("Unable to register component(s) from document [{}]: {}",
                                component.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                } catch (WikiComponentException e) {
                    this.logger.warn("Failed to create wiki component(s) for document [{}]: {}", documentReference,
                        ExceptionUtils.getRootCauseMessage(e));
                }
                break;
            }
        }
    }

    /**
     * Helper method to unregister a wiki component.
     * 
     * @param documentReference the reference to the document for which to unregister the held wiki component.
     */
    private void unregisterComponents(DocumentReference documentReference)
    {
        try {
            this.wikiComponentManager.unregisterWikiComponents(documentReference);
        } catch (WikiComponentException e) {
            this.logger.warn("Unable to unregister component(s) from document [{}]: {}", documentReference,
                ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
