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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.internal.bridge.WikiObjectComponentManagerEventListenerProxy;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

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
    private WikiObjectComponentManagerEventListenerProxy wikiObjectComponentManagerEventListenerProxy;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

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
            // Get the entity source
            XObjectEvent xObjectEvent = (XObjectEvent) event;

            EntityReference xObject = xObjectEvent.getReference();

            // If the modified XObject can produce a WikiComponent
            if (this.wikiObjectComponentManagerEventListenerProxy.getWikiObjectsList()
                    .contains(entityReferenceSerializer.serialize(
                                    ((BaseObjectReference) xObject).getXClassReference()))) {
                if (event instanceof XObjectAddedEvent || event instanceof XObjectUpdatedEvent) {
                    this.wikiObjectComponentManagerEventListenerProxy
                            .registerObjectComponents((ObjectReference) xObject, (XWikiDocument) source);
                } else if (event instanceof XObjectDeletedEvent) {
                    this.wikiObjectComponentManagerEventListenerProxy
                            .unregisterObjectComponents((ObjectReference) xObject);
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
}
