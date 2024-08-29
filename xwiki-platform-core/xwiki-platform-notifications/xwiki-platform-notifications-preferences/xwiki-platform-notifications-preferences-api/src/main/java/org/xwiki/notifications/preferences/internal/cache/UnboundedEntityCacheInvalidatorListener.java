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
package org.xwiki.notifications.preferences.internal.cache;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectPropertyUpdatedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * Automatically invalidate the {@link UnboundedEntityCacheManager} caches when entities are modified.
 * <p>
 * The listener react to:
 * <ul>
 * <li>Deleted wikis</li>
 * <li>Deleted document</li>
 * <li>User active status changed</li>
 * </ul>
 * 
 * @version $Id$
 * @since 13.8RC1
 * @since 13.4.4
 * @since 12.10.10
 */
@Component
@Named(UnboundedEntityCacheInvalidatorListener.NAME)
@Singleton
public class UnboundedEntityCacheInvalidatorListener extends AbstractEventListener
{
    /**
     * Unique name.
     */
    public static final String NAME = "PreferenceCacheInvalidatorListener";

    @Inject
    private UnboundedEntityCacheManager caches;

    /**
     * The default constructor.
     */
    public UnboundedEntityCacheInvalidatorListener()
    {
        super(NAME, new WikiDeletedEvent(), new DocumentDeletedEvent(), new DocumentUpdatedEvent(),
            new XObjectPropertyUpdatedEvent(new EntityReference("active", EntityType.OBJECT_PROPERTY,
                BaseObjectReference.any(XWikiUsersDocumentInitializer.CLASS_REFERENCE_STRING))));
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof WikiDeletedEvent) {
            // It's hard to find which cache entry is coming from wiki wiki so we remove them all
            this.caches.remove(((WikiDeletedEvent) event).getWikiId());
        } else if (event instanceof DocumentDeletedEvent) {
            this.caches.remove(((XWikiDocument) source).getDocumentReference());
        } else {
            this.caches.update(((XWikiDocument) source).getDocumentReference());
        }
    }
}
