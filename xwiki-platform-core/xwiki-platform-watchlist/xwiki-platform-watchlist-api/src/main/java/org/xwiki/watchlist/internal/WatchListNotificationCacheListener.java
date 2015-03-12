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
package org.xwiki.watchlist.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.watchlist.internal.documents.WatchListClassDocumentInitializer;
import org.xwiki.watchlist.internal.documents.WatchListJobClassDocumentInitializer;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.MandatoryDocumentInitializer;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener that maintains the {@link WatchListNotificationCache} up to date with the changes in the documents.
 * 
 * @version $Id$
 */
@Component
@Named(WatchListNotificationCacheListener.LISTENER_NAME)
@Singleton
public class WatchListNotificationCacheListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String LISTENER_NAME = "WatchListNotificationCacheListener";

    /**
     * The events to match.
     */
    private static final List<Event> LISTENER_EVENTS = Arrays.<Event>asList(new DocumentCreatedEvent(),
        new DocumentUpdatedEvent(), new DocumentDeletedEvent());

    /**
     * The cache to maintain. Make sure we use a provider to do lazy instantiation/initialization so that we do not
     * trigger an unwanted initialization of the cache when the listener is initialized by observation manager, as it
     * would be too soon and the cache would not be able to access the db.
     */
    @Inject
    private Provider<WatchListNotificationCache> notificationCacheProvider;

    /**
     * Needed to reinitialize the watchlist class.
     */
    @Inject
    @Named(WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME)
    private MandatoryDocumentInitializer watchListClassInitializer;

    /**
     * Used to get the list of all wikis.
     */
    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public WatchListNotificationCacheListener()
    {
        super(LISTENER_NAME, LISTENER_EVENTS);
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        XWikiDocument currentDoc = (XWikiDocument) source;
        XWikiDocument originalDoc = currentDoc.getOriginalDocument();
        XWikiContext context = (XWikiContext) data;

        watchListJobObjectsEventHandler(originalDoc, currentDoc, context);
        watchListObjectsEventHandler(originalDoc, currentDoc, context);
    }

    /**
     * Manage events affecting watchlist job objects.
     * 
     * @param originalDoc document version before the event occurred
     * @param currentDoc document version after event occurred
     * @param context the XWiki context
     */
    private void watchListJobObjectsEventHandler(XWikiDocument originalDoc, XWikiDocument currentDoc,
        XWikiContext context)
    {
        BaseObject originalJob = originalDoc.getXObject(WatchListJobClassDocumentInitializer.DOCUMENT_REFERENCE);
        BaseObject currentJob = currentDoc.getXObject(WatchListJobClassDocumentInitializer.DOCUMENT_REFERENCE);

        boolean reinitWatchListClass = false;

        // WatchListJob deleted
        if (originalJob != null && currentJob == null) {
            String deletedJobDocument = originalDoc.getFullName();

            reinitWatchListClass |= notificationCacheProvider.get().removeJobDocument(deletedJobDocument);
        }

        // WatchListJob created
        if (originalJob == null && currentJob != null) {
            String newJobDocument = currentDoc.getFullName();

            reinitWatchListClass |= notificationCacheProvider.get().addJobDocument(newJobDocument);
        }

        // If the list of WatchListJob documents was altered, the WatchListClass "interval" property needs to be updated
        // for all existing wikis.
        if (reinitWatchListClass) {
            // TODO: Maybe this can be moved inside WatchListNotificationCache.add/removeJobDocument(), since it would
            // benefit from the synchronization done there.
            reinitializeWatchListClass(context);
        }
    }

    /**
     * @param context the context to use. Since everything happens in the same thread, it's safe to assume that this is
     *            the request's context and not a synthetic one.
     */
    private void reinitializeWatchListClass(XWikiContext context)
    {
        String currentWikiId = context.getWikiId();
        try {
            // Reinitialize on all wikis.
            for (String wikiId : wikiDescriptorManager.getAllIds()) {
                try {
                    context.setWikiId(wikiId);

                    XWikiDocument document =
                        context.getWiki().getDocument(watchListClassInitializer.getDocumentReference(), context);

                    if (watchListClassInitializer.updateDocument(document)) {
                        context.getWiki().saveDocument(document, context);
                    }
                } catch (XWikiException e) {
                    logger.error("Failed to re-initialize mandatory document [{}] on wiki [{}]",
                        WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME, wikiId, e);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to re-initialize mandatory document [{}]",
                WatchListClassDocumentInitializer.DOCUMENT_FULL_NAME, e);
        } finally {
            // Restore the contex wiki.
            context.setWikiId(currentWikiId);
        }
    }

    /**
     * Manage events affecting watchlist objects.
     * 
     * @param originalDoc document version before the event occurred
     * @param currentDoc document version after event occurred
     * @param context the XWiki context
     */
    private void watchListObjectsEventHandler(XWikiDocument originalDoc, XWikiDocument currentDoc, XWikiContext context)
    {
        BaseObject originalWatchListObj = originalDoc.getXObject(WatchListClassDocumentInitializer.DOCUMENT_REFERENCE);
        BaseObject currentWatchListObj = currentDoc.getXObject(WatchListClassDocumentInitializer.DOCUMENT_REFERENCE);

        if (originalWatchListObj != null) {
            // Existing subscriber

            String oriInterval =
                originalWatchListObj.getStringValue(WatchListClassDocumentInitializer.INTERVAL_PROPERTY);

            // If a subscriber has been deleted, remove it from our cache and exit
            if (currentWatchListObj == null) {
                notificationCacheProvider.get().removeSubscriber(oriInterval, originalDoc.getPrefixedFullName());
                return;
            }

            // Modification of the interval
            String newInterval =
                currentWatchListObj.getStringValue(WatchListClassDocumentInitializer.INTERVAL_PROPERTY);

            if (!newInterval.equals(oriInterval)) {
                notificationCacheProvider.get().moveSubscriber(oriInterval, newInterval,
                    originalDoc.getPrefixedFullName());
            }
        }

        if ((originalWatchListObj == null || originalDoc == null) && currentWatchListObj != null) {
            // New subscriber
            String newInterval =
                currentWatchListObj.getStringValue(WatchListClassDocumentInitializer.INTERVAL_PROPERTY);

            notificationCacheProvider.get().addSubscriber(newInterval, currentDoc.getPrefixedFullName());
        }
    }
}
