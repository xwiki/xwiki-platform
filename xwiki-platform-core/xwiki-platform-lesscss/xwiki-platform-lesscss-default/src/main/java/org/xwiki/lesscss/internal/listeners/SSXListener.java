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
package org.xwiki.lesscss.internal.listeners;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.lesscss.internal.cache.ColorThemeCache;
import org.xwiki.lesscss.internal.cache.LESSResourcesCache;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Listener that clears the cache of compiled LESS Skin resources when a skin extension object (SSX) is saved.
 *
 * @since 6.4M2
 * @version $Id$
 */
@Component
@Named("lessSSX")
@Singleton
public class SSXListener implements EventListener
{
    @Inject
    private LESSResourcesCache lessResourcesCache;

    @Inject
    private ColorThemeCache colorThemeCache;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;
    
    @Inject
    private LESSResourceReferenceFactory lessResourceReferenceFactory;

    @Override
    public String getName()
    {
        return "LESS SSX objects listener";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(
                new DocumentCreatedEvent(),
                new DocumentUpdatedEvent(),
                new DocumentDeletedEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument document = (XWikiDocument) source;
        String currentWiki = wikiDescriptorManager.getCurrentWikiId();

        DocumentReference ssxDocRef = new DocumentReference(currentWiki, "XWiki", "StyleSheetExtension");

        List<BaseObject> ssxObjects = document.getXObjects(ssxDocRef);
        if (ssxObjects != null && !ssxObjects.isEmpty()) {
            for (BaseObject obj : ssxObjects) {
                if (obj == null) {
                    continue;
                }
                if ("LESS".equals(obj.getStringValue("contentType"))) {
                    ObjectPropertyReference objectPropertyReference = new ObjectPropertyReference("code",
                            new BaseObjectReference(ssxDocRef, obj.getNumber(), document.getDocumentReference()));
                    LESSResourceReference lessResourceReference = 
                            lessResourceReferenceFactory.createReferenceForXObjectProperty(objectPropertyReference);
                    lessResourcesCache.clearFromLESSResource(lessResourceReference);
                    colorThemeCache.clearFromLESSResource(lessResourceReference);
                }
            }
        }
    }
}
