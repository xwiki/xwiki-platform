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
package org.xwiki.component.wiki.internal.bridge;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.AbstractDocumentEvent;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiObjectComponentBuilder;
import org.xwiki.component.wiki.internal.DefaultWikiComponentManagerEventListener;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * This {@link EventListener} is responsible for registering wiki components derived from XObjects using the same
 * principle as {@link DefaultWikiComponentManagerEventListener}.
 * 
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named(DefaultWikiObjectComponentManagerEventListener.EVENT_LISTENER_NAME)
@Singleton
public class DefaultWikiObjectComponentManagerEventListener extends AbstractEventListener
{
    /**
     * The event listener name.
     */
    public static final String EVENT_LISTENER_NAME = "defaultWikiObjectComponentManagerEventListener";

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    private WikiObjectComponentManagerEventListenerProxy wikiObjectComponentManagerEventListenerProxy;

    @Inject
    private EntityReferenceSerializer<String> defaultEntityReferenceSerializer;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> localEntityReferenceSerializer;

    @Inject
    private Logger logger;

    /**
     * Builds a new {@link DefaultWikiObjectComponentManagerEventListener}.
     */
    public DefaultWikiObjectComponentManagerEventListener()
    {
        super(DefaultWikiObjectComponentManagerEventListener.EVENT_LISTENER_NAME, new ApplicationReadyEvent(),
            new WikiReadyEvent(), new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof AbstractDocumentEvent) {
            handleDocumentEvents((AbstractDocumentEvent) event, (XWikiDocument) source);

            /*
             * If we are at application startup time, we have to instantiate every document or object that we can find
             * in the wiki
             */
        } else if (event instanceof ApplicationReadyEvent || event instanceof WikiReadyEvent) {
            // These 2 events are created when the database is ready. We register all wiki components.
            // Collect every WikiObjectComponentBuilder
            this.wikiObjectComponentManagerEventListenerProxy.registerAllObjectComponents();
        }
    }

    private void handleDocumentEvents(AbstractDocumentEvent event, XWikiDocument document)
    {
        if (event instanceof DocumentUpdatedEvent || event instanceof DocumentDeletedEvent) {
            unRegisterObjectComponents(document.getOriginalDocument());
        }

        if (event instanceof DocumentCreatedEvent || event instanceof DocumentUpdatedEvent) {
            registerObjectComponents(document);
        }
    }

    private void registerObjectComponents(XWikiDocument document)
    {
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : document.getXObjects().entrySet()) {
            WikiObjectComponentBuilder componentBuilder = getAssociatedComponentBuilder(entry.getKey());

            if (componentBuilder != null) {
                for (BaseObject baseObject : entry.getValue()) {
                    if (baseObject != null) {
                        this.wikiObjectComponentManagerEventListenerProxy
                            .registerObjectComponents(baseObject.getReference(), baseObject, componentBuilder);
                    }
                }
            }
        }
    }

    private void unRegisterObjectComponents(XWikiDocument document)
    {
        for (Map.Entry<DocumentReference, List<BaseObject>> entry : document.getXObjects().entrySet()) {
            WikiObjectComponentBuilder componentBuilder = getAssociatedComponentBuilder(entry.getKey());

            if (componentBuilder != null) {
                for (BaseObject baseObject : entry.getValue()) {
                    if (baseObject != null) {
                        this.wikiObjectComponentManagerEventListenerProxy
                            .unregisterObjectComponents(baseObject.getReference());
                    }
                }
            }
        }
    }

    /**
     * Get the {@link WikiObjectComponentBuilder} associated to the given {@link DocumentReference}. This method checks
     * if a component builder exists for the document reference and then for the local document reference. If no builder
     * can is found, returns null.
     *
     * @param xClassReference the class reference used when searching for the component builder
     * @return the first component builder that has been found, null if no builder is found
     */
    private WikiObjectComponentBuilder getAssociatedComponentBuilder(DocumentReference xClassReference)
    {
        try {
            // First, try to find a component that has a hint related to the DocumentReference of the XClass
            String serializedXClassReference = this.defaultEntityReferenceSerializer.serialize(xClassReference);
            if (this.componentManager.hasComponent(WikiObjectComponentBuilder.class, serializedXClassReference)) {
                return this.componentManager.getInstance(WikiObjectComponentBuilder.class, serializedXClassReference);
            } else {
                // If no component has been found, try again with a LocalDocumentReference
                serializedXClassReference = this.localEntityReferenceSerializer.serialize(xClassReference);
                if (this.componentManager.hasComponent(WikiObjectComponentBuilder.class, serializedXClassReference)) {
                    return this.componentManager.getInstance(WikiObjectComponentBuilder.class,
                        serializedXClassReference);
                } else {
                    return null;
                }
            }
        } catch (ComponentLookupException e) {
            /*
             * As we test if a component is present in the component manager before fetching it, we shouldn't get any
             * exception at this point.
             */
            this.logger.error(String.format(
                "Unable to find a WikiObjectComponentBuilder associated to the helper [%s]: %s", xClassReference, e));

            return null;
        }
    }
}
