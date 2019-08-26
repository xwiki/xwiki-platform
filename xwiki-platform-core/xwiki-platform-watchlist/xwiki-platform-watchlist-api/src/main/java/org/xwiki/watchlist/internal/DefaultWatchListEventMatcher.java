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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.watchlist.internal.api.WatchListEvent;
import org.xwiki.watchlist.internal.api.WatchListStore;
import org.xwiki.watchlist.internal.api.WatchedElementType;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventType;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStream;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityStreamPlugin;

/**
 * Default implementation for {@link WatchListEventMatcher}.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWatchListEventMatcher implements WatchListEventMatcher
{

    /**
     * Events to match.
     */
    private static final List<String> MATCHING_EVENT_TYPES = new ArrayList<String>()
    {
        {
            add(ActivityEventType.CREATE);
            add(ActivityEventType.UPDATE);
            add(ActivityEventType.DELETE);
        }
    };

    /**
     * Logging framework.
     */
    @Inject
    private Logger logger;

    /**
     * Used to access the user's watched elements.
     */
    @Inject
    private WatchListStore store;

    /**
     * Context provider.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Used to check view rights on the matched events.
     */
    @Inject
    private AuthorizationManager authorizationManager;

    /**
     * Used to resolve string references.
     */
    @Inject
    private DocumentReferenceResolver<String> resolver;

    /**
     * Used to convert {@link ActivityEvent}s to {@link WatchListEvent}s.
     */
    @Inject
    private WatchListEventConverter<ActivityEvent> eventConverter;

    @Override
    public List<WatchListEvent> getEventsSince(Date start)
    {
        List<WatchListEvent> events = new ArrayList<>();

        XWikiContext context = getXWikiContext();

        ActivityStream actStream =
            ((ActivityStreamPlugin) context.getWiki().getPlugin(ActivityStreamPlugin.PLUGIN_NAME, context))
                .getActivityStream();
        List<Object> parameters = new ArrayList<>();
        parameters.add(start);

        try {
            // FIXME: Watch out for memory usage here, since the list of events could be huge in some cases.
            List<ActivityEvent> rawEvents = actStream.searchEvents(
                "act.date > ?1 and act.type in ('" + StringUtils.join(MATCHING_EVENT_TYPES, "','") + "')", false, true,
                0, 0, parameters, context);

            // If the page has been modified several times we want to display only one diff, if the page has been
            // delete after update events we want to discard the update events since we won't be able to display
            // diff from a deleted document. See WatchListEvent#addEvent(WatchListEvent) and
            // WatchListEvent#equals(WatchListEvent).
            for (ActivityEvent rawEvent : rawEvents) {
                WatchListEvent event = this.eventConverter.convert(rawEvent);

                int existingIndex = events.indexOf(event);
                if (existingIndex == -1) {
                    // An event on a new document, add the new event.
                    events.add(event);
                } else {
                    // An event on an existing document, add to the events of that document.
                    WatchListEvent existingCompositeEvent = events.get(existingIndex);
                    existingCompositeEvent.addEvent(event);
                }
            }
        } catch (Exception e) {
            logger.error("Failed to retrieve updated documents from activity stream since [{}]", start, e);
        }

        return events;
    }

    @Override
    public List<WatchListEvent> getMatchingVisibleEvents(List<WatchListEvent> events, String subscriber)
    {
        List<WatchListEvent> result = new ArrayList<WatchListEvent>();

        for (WatchListEvent event : events) {
            if (isEventSkipped(event)) {
                // Skip events that are on a blacklist for various reasons (performance, security, etc.)
                continue;
            }

            if (!isEventViewable(event, subscriber)) {
                // Skip events on documents that are not visible to the subscriber.
                continue;
            }

            if (!isEventMatching(event, subscriber)) {
                // Skip events that are not interesting to the subscriber.
                continue;
            }

            result.add(event);
        }

        // Sort the matching events by document.
        Collections.sort(result);

        return result;
    }

    @Override
    public boolean isEventMatching(WatchListEvent event, String subscriber)
    {
        boolean isWatched = false;

        try {
            // The subscriber's watched users, since each event can be a composite event.
            Collection<String> watchedUsers = store.getWatchedElements(subscriber, WatchedElementType.USER);

            isWatched |= store.isWatched(event.getWiki(), subscriber, WatchedElementType.WIKI);
            isWatched |= store.isWatched(event.getPrefixedSpace(), subscriber, WatchedElementType.SPACE);
            isWatched |= store.isWatched(event.getPrefixedFullName(), subscriber, WatchedElementType.DOCUMENT);
            isWatched |= CollectionUtils.intersection(watchedUsers, event.getAuthors()).size() > 0;
        } catch (Exception e) {
            logger.error("Failed to determine if an event for the document [{}] is interesting to [{}]",
                event.getDocumentReference(), subscriber, e);
        }

        return isWatched;
    }

    @Override
    public boolean isEventSkipped(WatchListEvent event)
    {
        // We exclude watchlist jobs from notifications since they are modified each time they are fired,
        // producing useless noise.
        Collection<String> possibleIntervals = store.getIntervals();
        return possibleIntervals.contains(event.getFullName());
    }

    @Override
    public boolean isEventViewable(WatchListEvent event, String subscriber)
    {
        DocumentReference userReference = resolver.resolve(subscriber);
        return authorizationManager.hasAccess(Right.VIEW, userReference, event.getDocumentReference());
    }

    private XWikiContext getXWikiContext()
    {
        return contextProvider.get();
    }
}
