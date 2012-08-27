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
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

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
     * Used to serializes wiki pages reference in the log.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event> asList(
            new DocumentCreatedEvent(),
            new DocumentUpdatedEvent(),
            new DocumentDeletedEvent(),
            new ApplicationReadyEvent());
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
                // Unregister any existing component registered under this document, if any
                unregisterComponents(documentReference);

                for (WikiComponentBuilder provider : wikiComponentProviders) {
                    // Check whether the given document has a wiki component defined in it.
                    if (provider.getDocumentReferences().contains(documentReference)) {
                        try {
                            List<WikiComponent> components = provider.buildComponents(documentReference);
                            for (WikiComponent component : components) {
                                // Register the component.
                                registerComponent(component);
                            }
                        } catch (WikiComponentException e) {
                            this.logger.warn("Failed to create wiki component(s) for document [{}]: {}",
                                compactWikiSerializer.serialize(documentReference), e.getMessage());
                        }
                    }
                }
            } else if (event instanceof DocumentDeletedEvent) {
                // Unregister components from the deleted document, if any
                unregisterComponents(documentReference);
            }
        } else if (event instanceof ApplicationReadyEvent) {
            registerComponents();
        }
    }

    /**
     * Register all the wiki components in the current wiki.
     */
    private void registerComponents()
    {
        // Retreive all components definitions and register them.
        for (WikiComponentBuilder provider : wikiComponentProviders) {
            for (DocumentReference reference : provider.getDocumentReferences()) {
                try {
                    List<WikiComponent> components = provider.buildComponents(reference);
                    for (WikiComponent component : components) {
                        this.wikiComponentManager.registerWikiComponent(component);
                    }
                } catch (WikiComponentException e) {
                    this.logger.warn("Failed to register wiki component(s) for document [{}]: {}",
                        compactWikiSerializer.serialize(reference), e.getMessage());
                }
            }
        }
    }


    /**
     * Helper method to register a wiki component.
     * 
     * @param wikiComponent the wikiComponent to register.
     */
    private void registerComponent(WikiComponent wikiComponent)
    {
        try {
            this.wikiComponentManager.registerWikiComponent(wikiComponent);
        } catch (WikiComponentException e) {
            this.logger.warn("Unable to register component(s) from document [{}]: {}",
                compactWikiSerializer.serialize(wikiComponent.getDocumentReference()), e.getMessage());
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
            this.logger.warn("Unable to unregister component(s) from document [{}]: {}",
                compactWikiSerializer.serialize(documentReference), e.getMessage());
        }
    }
}
