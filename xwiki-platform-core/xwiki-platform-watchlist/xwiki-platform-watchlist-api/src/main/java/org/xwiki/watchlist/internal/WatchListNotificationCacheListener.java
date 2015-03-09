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
import javax.inject.Provider;

import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.watchlist.internal.documents.WatchListClassDocumentInitializer;
import org.xwiki.watchlist.internal.documents.WatchListJobClassDocumentInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Listener that maintains the {@link WatchListNotificationCache} up to date with the changes in the documents.
 * 
 * @version $Id$
 */
public class WatchListNotificationCacheListener extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    private static final String LISTENER_NAME = "watchlistNotificationCache";

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

        // WatchListJob deleted
        if (originalJob != null && currentJob == null) {
            String deletedJobDocument = originalDoc.getFullName();

            notificationCacheProvider.get().removeJobDocument(deletedJobDocument);
        }

        // WatchListJob created
        if (originalJob == null && currentJob != null) {
            String newJobDocument = currentDoc.getFullName();

            notificationCacheProvider.get().addJobDocument(newJobDocument);
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
