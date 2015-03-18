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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.watchlist.internal.api.WatchList;
import org.xwiki.watchlist.internal.api.WatchListEvent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEvent;
import com.xpn.xwiki.plugin.activitystream.api.ActivityEventType;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStream;
import com.xpn.xwiki.plugin.activitystream.api.ActivityStreamException;
import com.xpn.xwiki.plugin.activitystream.plugin.ActivityStreamPlugin;
import com.xpn.xwiki.web.Utils;

/**
 * Matcher for WatchList events. This class stores all the events fired during a given interval. It also allows to
 * perform a match between events and elements watched by a user.
 * 
 * @version $Id$
 */
@SuppressWarnings("serial")
public class WatchListEventMatcher
{
    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchListEventMatcher.class);

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
     * List of events which have occurred between the start date and the current time.
     */
    private final List<WatchListEvent> events = new ArrayList<WatchListEvent>();

    /**
     * Constructor. Gets all the events fired during the interval between the given date and the current date.
     * 
     * @param context the XWiki context
     * @param start start date to use for document matching
     */
    public WatchListEventMatcher(Date start, XWikiContext context)
    {
        ActivityStream actStream =
            ((ActivityStreamPlugin) context.getWiki().getPlugin(ActivityStreamPlugin.PLUGIN_NAME, context))
                .getActivityStream();
        List<Object> parameters = new ArrayList<Object>();
        List<ActivityEvent> rawEvents;

        parameters.add(start);

        try {
            // FIXME: Watch out for memory usage here, since the list of events could be huge in some cases.
            rawEvents =
                actStream.searchEvents(
                    "act.date > ? and act.type in ('" + StringUtils.join(MATCHING_EVENT_TYPES, "','") + "')", false,
                    true, 0, 0, parameters, context);

            // If the page has been modified several times we want to display only one diff, if the page has been
            // delete after update events we want to discard the update events since we won't be able to display
            // diff from a deleted document. See WatchListEvent#addEvent(WatchListEvent) and
            // WatchListEvent#equals(WatchListEvent).
            for (ActivityEvent rawEvent : rawEvents) {
                WatchListEvent event = new WatchListEvent(rawEvent, context);
                if (!events.contains(event)) {
                    events.add(new WatchListEvent(rawEvent, context));
                } else {
                    WatchListEvent existingCompositeEvent = events.get(events.indexOf(event));
                    existingCompositeEvent.addEvent(event);
                }
            }

        } catch (ActivityStreamException e) {
            LOGGER.error("Failed to retrieve updated documents from activity stream");
            e.printStackTrace();
        }
    }

    /**
     * @return the number of events the matcher will work with.
     */
    public int getEventNumber()
    {
        return events.size();
    }

    /**
     * Get the events matching criteria.
     * 
     * @param wikis the wikis from which events should match
     * @param spaces the spaces from which events should match
     * @param documents the documents from which events should match
     * @param users the users from which events should match
     * @param userName notification recipient
     * @param context the XWiki context
     * @return a list of events matching the given scopes
     */
    public List<WatchListEvent> getMatchingEvents(Collection<String> wikis, Collection<String> spaces,
        Collection<String> documents, Collection<String> users, String userName, XWikiContext context)
    {
        List<WatchListEvent> matchingEvents = new ArrayList<WatchListEvent>();
        WatchList watchlist = Utils.getComponent(WatchList.class);
        Collection<String> jobDocumentNames = watchlist.getStore().getJobDocumentNames();

        for (WatchListEvent event : events) {
            if (wikis.contains(event.getWiki()) || spaces.contains(event.getPrefixedSpace())
                || documents.contains(event.getPrefixedFullName())
                || CollectionUtils.intersection(users, event.getAuthors()).size() > 0) {
                try {
                    // We exclude watchlist jobs from notifications since they are modified each time they are fired,
                    // producing useless noise. We also ensure that users have the right to view documents we send
                    // notifications for.
                    if (!jobDocumentNames.contains(event.getFullName())
                        && context.getWiki().getRightService()
                            .hasAccessLevel("view", userName, event.getPrefixedFullName(), context)) {
                        matchingEvents.add(event);
                    }
                } catch (XWikiException e) {
                    // We're in a job, we don't throw exceptions
                    e.printStackTrace();
                }
            }
        }

        Collections.sort(matchingEvents);

        return matchingEvents;
    }
}
