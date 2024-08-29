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
package org.xwiki.like.internal;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.like.LikeManager;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.mandatory.XWikiUsersDocumentInitializer;
import com.xpn.xwiki.objects.BaseObjectReference;

/**
 * This listener is in charge of clearing {@link LikeManager} cache in case of entity deletion.
 * This component is currently listening of 3 kinds of deletion:
 * <ul>
 *     <li>deletion of documents</li>
 *     <li>deletion of entire wiki</li>
 *     <li>deletion of users</li>
 * </ul>
 *
 * The deletion of a documents will perform a partial clean of cache, to remove the information related to that
 * document (see {@link LikeManager#clearCache(EntityReference)}), while the other kinds lead to full clean of the
 * cache (see {@link LikeManager#clearCache()}).
 *
 * @version $Id$
 * @since 12.9RC1
 */
@Component
@Named(DeletedEntityLikeListener.NAME)
@Singleton
@Priority(EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY)
public class DeletedEntityLikeListener extends AbstractEventListener
{
    static final String NAME = "DeletedEntityLikeListener";
    private static final List<Event> EVENT_LIST = Arrays.asList(
        new DocumentDeletedEvent(),
        new WikiDeletedEvent(),
        new XObjectDeletedEvent(BaseObjectReference.any(XWikiUsersDocumentInitializer.CLASS_REFERENCE_STRING))
    );

    @Inject
    private Provider<LikeManager> likeManagerProvider;

    /**
     * Default constructor.
     */
    public DeletedEntityLikeListener()
    {
        super(NAME, EVENT_LIST);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof DocumentDeletedEvent) {
            XWikiDocument sourceDoc = (XWikiDocument) source;
            this.likeManagerProvider.get().clearCache(sourceDoc.getDocumentReference());
        } else {
            this.likeManagerProvider.get().clearCache();
        }
    }
}
