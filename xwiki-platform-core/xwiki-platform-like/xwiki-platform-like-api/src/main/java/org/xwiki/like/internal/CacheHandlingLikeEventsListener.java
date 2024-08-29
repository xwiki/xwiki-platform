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
import java.util.Locale;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.like.LikeManager;
import org.xwiki.like.events.LikeEvent;
import org.xwiki.like.events.UnlikeEvent;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.async.internal.AsyncRendererCache;

/**
 * Ensure that the cache are properly clean when a like operation is performed.
 *
 * @version $Id$
 * @since 13.5RC1
 * @since 12.10.8
 */
@Component
@Named(CacheHandlingLikeEventsListener.NAME)
@Singleton
@Priority(EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY)
public class CacheHandlingLikeEventsListener extends AbstractEventListener
{
    static final String NAME = "CacheHandlingLikeEventsListener";

    private static final List<Event> EVENT_LIST = Arrays.asList(
        new LikeEvent(),
        new UnlikeEvent()
    );

    private static final LocalDocumentReference UIX_REFERENCE =
        new LocalDocumentReference(new LocalDocumentReference(Arrays.asList("XWiki", "Like"), "LikeUIX"), Locale.ROOT);


    @Inject
    private Provider<AsyncRendererCache> asyncRendererCacheProvider;

    @Inject
    private Provider<LikeManager> likeManagerProvider;

    /**
     * Default constructor.
     */
    public CacheHandlingLikeEventsListener()
    {
        super(NAME, EVENT_LIST);
    }

    private void cleanCacheUIX(WikiReference wikiReference)
    {
        DocumentReference localUIXReference = new DocumentReference(UIX_REFERENCE, wikiReference);
        this.asyncRendererCacheProvider.get().cleanCache(localUIXReference);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (data instanceof EntityReference) {
            EntityReference target = (EntityReference) data;
            this.likeManagerProvider.get().clearCache(target);
            this.cleanCacheUIX((WikiReference) target.extractReference(EntityType.WIKI));
        }
    }
}
