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
package org.xwiki.watchlist.internal.notification;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.text.XWikiToStringBuilder;
import org.xwiki.watchlist.internal.api.WatchListEvent;

/**
 * Represents the inputs of the {@link WatchListEventMimeMessageFactory}: a list of events and a list of subscribers
 * that might be interested.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class EventsAndSubscribersSource
{
    /**
     * The list of subscribers to process.
     */
    public static final String SUBSCRIBERS_PARAMETER = "subscribers";

    /**
     * The list of events to process.
     */
    public static final String EVENTS_PARAMETER = "events";

    private List<WatchListEvent> events;

    private List<DocumentReference> subscribers;

    /**
     * @param events see {@link #getEvents()}
     * @param subscribers see {@link #getSubscribers()}
     */
    public EventsAndSubscribersSource(List<WatchListEvent> events, List<DocumentReference> subscribers)
    {
        this.events = events;
        this.subscribers = subscribers;
    }

    /**
     * @return the list of events that should be considered
     */
    public List<WatchListEvent> getEvents()
    {
        return events;
    }

    /**
     * @return the list of subscribers that might be interested in the events
     */
    public List<DocumentReference> getSubscribers()
    {
        return subscribers;
    }

    /**
     * @param sourceMap a Map containing the list of events and subscribers to iterate over. The supported map keys are
     *            {@code events} and {@code subscribers}
     * @return the typed instance representing the inputs passed
     */
    public static EventsAndSubscribersSource parse(Map<String, Object> sourceMap)
    {
        List<WatchListEvent> events = ListUtils.emptyIfNull((List<WatchListEvent>) sourceMap.get(EVENTS_PARAMETER));
        List<DocumentReference> subscribers =
            ListUtils.emptyIfNull((List<DocumentReference>) sourceMap.get(SUBSCRIBERS_PARAMETER));

        return new EventsAndSubscribersSource(events, subscribers);
    }

    @Override
    public String toString()
    {
        ToStringBuilder builder = new XWikiToStringBuilder(this);
        builder.append(EVENTS_PARAMETER, getEvents());
        builder.append(SUBSCRIBERS_PARAMETER, getSubscribers());
        return builder.toString();
    }
}
