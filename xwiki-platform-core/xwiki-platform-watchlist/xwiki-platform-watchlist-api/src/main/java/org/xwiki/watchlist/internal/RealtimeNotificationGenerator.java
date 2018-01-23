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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.ObservationContext;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.watchlist.WatchListConfiguration;
import org.xwiki.watchlist.internal.api.WatchListEvent;
import org.xwiki.watchlist.internal.api.WatchListEventType;
import org.xwiki.watchlist.internal.api.WatchListNotifier;
import org.xwiki.watchlist.internal.api.WatchListStore;
import org.xwiki.watchlist.internal.notification.WatchListEventMimeMessageFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Generates notifications in real time for all the subscribers interested in the currently modified document.
 *
 * @version $Id$
 */
@Component
@Named(RealtimeNotificationGenerator.LISTENER_NAME)
@Singleton
public class RealtimeNotificationGenerator extends AbstractEventListener
{
    /**
     * The name of the listener.
     */
    public static final String LISTENER_NAME = "RealtimeNotificationGenerator";

    /**
     * The document containing the WatchList message template for realtime notifications.
     */
    public static final String REALTIME_EMAIL_TEMPLATE = "XWiki.WatchListRealtimeMessage";

    /**
     * The events to match.
     */
    private static final List<Event> EVENTS = Arrays.<Event>asList(new DocumentCreatedEvent(),
        new DocumentUpdatedEvent(), new DocumentDeletedEvent());

    /**
     * Used to detect if certain events are not independent, i.e. executed in the context of other events, case in which
     * they should be skipped.
     */
    @Inject
    private ObservationContext observationContext;

    /**
     * Used to obtain observation event context, i.e. if the event is remote.
     */
    @Inject
    private RemoteObservationManagerContext remoteObservationManagerContext;

    /**
     * Used to access watchlist data.
     */
    @Inject
    private WatchListStore store;

    /**
     * Used to actually deliver the notification to the user.
     */
    @Inject
    private WatchListNotifier notifier;

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Used to match {@link WatchListEvent}s with a user's watchlist.
     */
    @Inject
    private WatchListEventMatcher watchlistEventMatcher;

    @Inject
    private WatchListConfiguration configuration;

    /**
     * Allow processing of remote events.
     */
    private boolean allowRemote;

    /**
     * Default constructor.
     */
    public RealtimeNotificationGenerator()
    {
        super(LISTENER_NAME, EVENTS);
    }

    /**
     * Component manager initialize class. Called after all dependencies are injected.
     *
     * @throws InitializationException if component isn't properly initialized
     */
    public void initialize() throws InitializationException
    {
        this.allowRemote = configuration.allowRealtimeRemote();
    }

    @Override
    public List<Event> getEvents()
    {
        if (configuration.isRealtimeEnabled()) {
            // If the realtime notification feature is explicitly enabled (temporarily disabled by default), then enable
            // this event listener.
            return super.getEvents();
        } else {
            // Otherwise disabled this event listener from being notified.
            return Collections.emptyList();
        }
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Early check if event should be processed.
        if (this.remoteObservationManagerContext.isRemoteState() && !this.allowRemote) {
            // Don't handle remote events to avoid duplicated processing.
            return;
        }

        XWikiDocument currentDoc = (XWikiDocument) source;
        XWikiContext context = (XWikiContext) data;

        // Skip evens that are executed in the context of other event, thus not directly generated by a user.
        if (observationContext.isIn(AutomaticWatchModeListener.SKIPPED_EVENTS)) {
            return;
        }

        // Prepare the notification and send it for processing in a separate thread so that the UI does not block.

        try {
            // Get a corresponding watchlist event.
            WatchListEvent watchListEvent = getWatchListEvent(event, currentDoc, context);

            // Early optimization since this is not related to a user but to the event itself.
            if (watchlistEventMatcher.isEventSkipped(watchListEvent)) {
                // Stop here if the event is skipped.
                return;
            }

            // Get all the realtime notification subscribers.
            Collection<String> subscribers =
                store.getSubscribers(DefaultWatchListNotificationCache.REALTIME_INTERVAL_ID);
            if (subscribers.size() == 0) {
                // Stop here if no one is interested.
                return;
            }

            // Build the notification parameters.
            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER, REALTIME_EMAIL_TEMPLATE);
            notificationData.put(WatchListEventMimeMessageFactory.SKIP_CONTEXT_USER_PARAMETER, true);
            notificationData.put(WatchListEventMimeMessageFactory.ATTACH_AUTHOR_AVATARS_PARAMETER, true);

            // Send the notification for processing.
            notifier.sendNotification(subscribers, Arrays.asList(watchListEvent), notificationData);
        } catch (Exception e) {
            logger.error("Failed to send realtime notification to user [{}]", context.getUserReference(), e);
        }
    }

    /**
     * @param event the current event
     * @param currentDoc the affected document
     * @param context the context of the event
     * @return the {@link WatchListEvent} to use to notify watchers of the current document
     */
    private WatchListEvent getWatchListEvent(Event event, XWikiDocument currentDoc, XWikiContext context)
    {
        String type = null;
        DocumentReference documentReference = currentDoc.getDocumentReference();
        DocumentReference userReference = context.getUserReference();
        String version = currentDoc.getVersion();
        Date date = currentDoc.getDate();

        if (event instanceof DocumentCreatedEvent) {
            type = WatchListEventType.CREATE;
        } else if (event instanceof DocumentUpdatedEvent) {
            type = WatchListEventType.UPDATE;
        } else if (event instanceof DocumentDeletedEvent) {
            version = currentDoc.getOriginalDocument().getVersion();
            type = WatchListEventType.DELETE;
        }

        WatchListEvent watchListEvent = new WatchListEvent(documentReference, type, userReference, version, date);

        return watchListEvent;
    }
}
