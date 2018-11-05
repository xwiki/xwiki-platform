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
package org.xwiki.rendering.async.internal;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.event.ComponentDescriptorAddedEvent;
import org.xwiki.component.event.ComponentDescriptorEvent;
import org.xwiki.component.event.ComponentDescriptorRemovedEvent;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.EntityEvent;
import com.xpn.xwiki.internal.event.XClassPropertyAddedEvent;
import com.xpn.xwiki.internal.event.XClassPropertyDeletedEvent;
import com.xpn.xwiki.internal.event.XClassPropertyUpdatedEvent;
import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Invalidate the cache when entities are modified.
 * 
 * @version $Id$
 * @since 10.10RC1
 */
@Component
@Singleton
@Named(AsyncRendererCacheListener.NAME)
public class AsyncRendererCacheListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String NAME = "org.xwiki.rendering.async.internal.AsyncRendererCacheListener";

    @Inject
    private AsyncRendererCache cache;

    /**
     * Default constructor.
     */
    public AsyncRendererCacheListener()
    {
        super(NAME, new WikiDeletedEvent(), new XClassPropertyAddedEvent(), new XClassPropertyDeletedEvent(),
            new XClassPropertyUpdatedEvent(), new XObjectAddedEvent(), new XObjectDeletedEvent(),
            new XObjectUpdatedEvent(), new DocumentCreatedEvent(), new DocumentUpdatedEvent(),
            new DocumentDeletedEvent(), new ComponentDescriptorAddedEvent(), new ComponentDescriptorRemovedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof ComponentDescriptorEvent) {
            ComponentDescriptorEvent componentEvent = ((ComponentDescriptorEvent) event);
            this.cache.cleanCache(componentEvent.getRoleType(), componentEvent.getRoleHint());
        } else if (event instanceof WikiDeletedEvent) {
            WikiReference wikiReference = new WikiReference(((WikiDeletedEvent) event).getWikiId());

            this.cache.cleanCache(wikiReference.getName());
        } else {
            XWikiDocument document = (XWikiDocument) source;

            // Clean entries associated to modified entity
            this.cache.cleanCache(document.getDocumentReference());

            // Clean entries associated to the exact entry
            if (event instanceof EntityEvent) {
                onEntityEvent((EntityEvent) event, document);
            }
        }
    }

    private void onEntityEvent(EntityEvent event, XWikiDocument document)
    {
        // Clean entries associated to the entity
        this.cache.cleanCache(event.getReference());

        // Clean entries associated to modified object class reference
        if (event instanceof XObjectEvent) {
            XObjectEvent objectEvent = (XObjectEvent) event;

            BaseObject obj = document.getXObject(objectEvent.getReference());

            this.cache.cleanCache(obj.getXClassReference());
        }
    }
}
