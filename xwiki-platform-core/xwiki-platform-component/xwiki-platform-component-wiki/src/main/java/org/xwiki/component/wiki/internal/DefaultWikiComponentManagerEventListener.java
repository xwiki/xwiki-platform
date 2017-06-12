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

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentBuilder;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.model.reference.DocumentReference;
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
     * The wiki component stores, used to retrieve component definitions.
     */
    @Inject
    private List<WikiComponentBuilder> wikiComponentProviders;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private WikiComponentManagerRegistrationHelper wikiComponentManagerRegistrationHelper;

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
            // Get the document reference
            DocumentModelBridge document = (DocumentModelBridge) source;
            DocumentReference documentReference = document.getDocumentReference();

            if (event instanceof  DocumentCreatedEvent || event instanceof DocumentUpdatedEvent) {
                registerDocumentComponents(document.getDocumentReference());
            } else if (event instanceof DocumentDeletedEvent) {
                // Unregister components from the deleted document, if any
                this.wikiComponentManagerRegistrationHelper.unregisterComponents(documentReference);
            }
        /* If we are at application startup time, we have to instanciate every document or object that we can find
         * in the wiki */
        } else if (event instanceof ApplicationReadyEvent || event instanceof WikiReadyEvent) {
            // These 2 events are created when the database is ready. We register all wiki components.
            registerAllDocumentComponents();
        }
    }

    /**
     * Register all the wiki components that come from a document in the current wiki.
     */
    private void registerAllDocumentComponents()
    {
        try {
            // Retrieve all components definitions and register them.
            this.wikiComponentProviders = this.componentManager.getInstanceList(WikiComponentBuilder.class);
            for (WikiComponentBuilder provider : this.wikiComponentProviders) {
                for (DocumentReference reference : provider.getDocumentReferences()) {
                    try {
                        List<WikiComponent> components = provider.buildComponents(reference);
                        this.wikiComponentManagerRegistrationHelper.registerComponentList(components);
                    } catch (WikiComponentException e) {
                        this.logger.warn("Failed to build the wiki component located in the document [{}]: {}",
                                reference, ExceptionUtils.getRootCauseMessage(e));
                    }
                }
            }
        } catch (ComponentLookupException e) {
            this.logger.warn(String.format("Unable to get a list of registered WikiComponentBuilder: %s", e));
        }
    }

    /**
     * Registers the components linked to the given document. For that, we get a list of providers that can build a
     * component using this document, then the first available provider is selected in order to register a component.
     *
     * @param documentReference the document used to create the component
     */
    private void registerDocumentComponents(DocumentReference documentReference)
    {
        // Unregister all wiki components registered under the given entity. We do this as otherwise we would need to
        // handle the specific cases of elements added, elements updated and elements deleted, etc.
        // Instead we unregister all wiki components and re-register them all.
        this.wikiComponentManagerRegistrationHelper.unregisterComponents(documentReference);

        // Re-register all wiki components in the passed document
        for (WikiComponentBuilder provider : this.wikiComponentProviders) {
            // Check whether the given document has a wiki component defined in it.
            if (provider.getDocumentReferences().contains(documentReference)) {
                try {
                    List<WikiComponent> components = provider.buildComponents(documentReference);
                    this.wikiComponentManagerRegistrationHelper.registerComponentList(components);
                } catch (WikiComponentException e) {
                    this.logger.warn("Failed to create wiki component(s) for document [{}]: {}", documentReference,
                            ExceptionUtils.getRootCauseMessage(e));
                }
                break;
            }
        }
    }


}
