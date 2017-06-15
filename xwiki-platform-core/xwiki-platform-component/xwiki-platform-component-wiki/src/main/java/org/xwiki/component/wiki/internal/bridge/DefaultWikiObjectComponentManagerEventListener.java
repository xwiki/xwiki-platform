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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
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

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Builds a new {@link DefaultWikiObjectComponentManagerEventListener}.
     */
    public DefaultWikiObjectComponentManagerEventListener()
    {
        super(DefaultWikiObjectComponentManagerEventListener.EVENT_LISTENER_NAME,
                new ApplicationReadyEvent(), new WikiReadyEvent(),
                new XObjectAddedEvent(), new XObjectUpdatedEvent(), new XObjectDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        /* If we are dealing with an XObject related event, then it might be related to a
         * WikiObjectComponentBuilder */
        if (event instanceof XObjectEvent) {
            XWikiDocument document = (XWikiDocument) source;
            // If we are dealing with a XObjectDeletedEvent, we have to retrieve the original XObject document.
            if (event instanceof XObjectDeletedEvent) {
                document = document.getOriginalDocument();
            }
            BaseObject baseObject =
                    document.getXObject(((XObjectEvent) event).getReference());

            // If the modified XObject can produce a WikiComponent
            WikiObjectComponentBuilder componentBuilder =
                    this.getAssociatedComponentBuilder(baseObject.getXClassReference());
            if (componentBuilder != null) {
                if (event instanceof XObjectAddedEvent || event instanceof XObjectUpdatedEvent) {
                    this.wikiObjectComponentManagerEventListenerProxy
                            .registerObjectComponents(baseObject.getReference(), baseObject, componentBuilder);
                } else if (event instanceof XObjectDeletedEvent) {
                    this.wikiObjectComponentManagerEventListenerProxy
                            .unregisterObjectComponents(baseObject.getReference());
                }
            }
        /* If we are at application startup time, we have to instanciate every document or object that we can find
         * in the wiki */
        } else if (event instanceof ApplicationReadyEvent || event instanceof WikiReadyEvent) {
            // These 2 events are created when the database is ready. We register all wiki components.
            // Collect every WikiObjectComponentBuilder
            this.wikiObjectComponentManagerEventListenerProxy.registerAllObjectComponents();
        }
    }

    /**
     * Get the {@link WikiObjectComponentBuilder} associated to the given {@link DocumentReference}.
     * This method checks if a component builder exists for the document reference and then for the local document
     * reference. If no builder can is found, returns null.
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
                serializedXClassReference =
                        this.localEntityReferenceSerializer.serialize(xClassReference);
                if (this.componentManager.hasComponent(WikiObjectComponentBuilder.class, serializedXClassReference)) {
                    return this.componentManager.getInstance(
                            WikiObjectComponentBuilder.class, serializedXClassReference);
                } else {
                    return null;
                }
            }
        } catch (ComponentLookupException e) {
            /* As we test if a component is present in the component manager before fetching it, we shouldn't get any
             * exception at this point. */
            this.logger.error(String.format(
                    "Unable to find a WikiObjectComponentBuilder associated to the helper [%s]: %s",
                    xClassReference, e));
            return null;
        }
    }
}
