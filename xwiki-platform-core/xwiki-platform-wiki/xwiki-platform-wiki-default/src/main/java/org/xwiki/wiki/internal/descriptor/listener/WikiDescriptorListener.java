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
package org.xwiki.wiki.internal.descriptor.listener;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.internal.descriptor.DefaultWikiDescriptor;
import org.xwiki.wiki.internal.descriptor.builder.WikiDescriptorBuilder;
import org.xwiki.wiki.internal.descriptor.document.WikiDescriptorDocumentHelper;
import org.xwiki.wiki.internal.manager.WikiDescriptorCache;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Used to refresh the Wiki Descriptor Cache.
 *
 * @version $Id$
 * @since 5.3M2
 */
@Component
@Named("wikidescriptor")
@Singleton
@Priority(EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY)
public class WikiDescriptorListener implements EventListener
{
    /**
     * Relative reference to the XWiki.XWikiServerClass containing wiki descriptor metadata.
     */
    static final EntityReference SERVER_CLASS =
        new EntityReference("XWikiServerClass", EntityType.DOCUMENT, new EntityReference("XWiki", EntityType.SPACE));

    @Inject
    private WikiDescriptorBuilder builder;

    @Inject
    private WikiDescriptorCache cache;

    @Inject
    private WikiDescriptorDocumentHelper wikiDescriptorDocumentHelper;

    @Override
    public String getName()
    {
        return "wikidescriptor";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;

        // If the document is deleted then check the original document to see if it had XWiki Server objects and if
        // so then unregister them
        if (event instanceof DocumentDeletedEvent || event instanceof DocumentUpdatedEvent) {
            removeExistingDescriptor(document.getOriginalDocument());
        }

        // Register the new XWiki Server objects if any
        List<BaseObject> serverClassObjects = document.getXObjects(SERVER_CLASS);
        if (serverClassObjects != null && !serverClassObjects.isEmpty()) {
            DefaultWikiDescriptor descriptor = this.builder.buildDescriptorObject(serverClassObjects, document);
            if (descriptor != null) {
                this.cache.add(descriptor);
                this.cache.setWikiIds(null);
            }
        }
    }

    private void removeExistingDescriptor(XWikiDocument document)
    {
        List<BaseObject> existingServerClassObjects = document.getXObjects(SERVER_CLASS);
        if (existingServerClassObjects != null && !existingServerClassObjects.isEmpty()) {
            String wikiId =
                this.wikiDescriptorDocumentHelper.getWikiIdFromDocumentReference(document.getDocumentReference());
            DefaultWikiDescriptor existingDescriptor = this.cache.getFromId(wikiId);
            if (existingDescriptor != null) {
                this.cache.remove(wikiId, existingDescriptor.getAliases());
                this.cache.setWikiIds(null);
            }
        }
    }
}
