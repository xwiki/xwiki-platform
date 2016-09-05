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

import java.util.Date;
import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.watchlist.internal.api.WatchListEvent;

/**
 * Matcher for WatchList events. It can also retrieve all the events fired during a given interval. It also allows to
 * perform a match between events and the elements watched by a user.
 * 
 * @version $Id$
 */
@Role
public interface WatchListEventMatcher
{
    /**
     * Gets all the events fired during the interval between the given date and the current date.
     * 
     * @param start start date to use for document matching
     * @return the list of events after the given start date
     */
    List<WatchListEvent> getEventsSince(Date start);

    /**
     * @param events the events to filter
     * @param subscriber the subscriber whose watched elements to check against
     * @return a sublist of events matching the given user's watched elements and that occurred on documents that are
     *         visible to the given user
     */
    List<WatchListEvent> getMatchingVisibleEvents(List<WatchListEvent> events, String subscriber);

    /**
     * Checks if an event matches a subscriber's watched elements.
     * 
     * @param event the event to check
     * @param subscriber the subscriber to check
     * @return true if the given event matches the given subscriber's watched elements, false otherwise. In addition,
     *         view rights of the subscriber on the event's documents are checked and events on certain internal
     *         documents (e.g. watchlist scheduler jobs) are also skipped to avoid noise.
     */
    boolean isEventMatching(WatchListEvent event, String subscriber);

    /**
     * Checks if an event should be skipped for various reasons (performance, security, etc.).
     * <p>
     * Example: Watchlist job documents that are updated on each trigger should be skipped.
     * 
     * @param event the event to check
     * @return true if the event should be skipped, false otherwise
     */
    boolean isEventSkipped(WatchListEvent event);

    /**
     * Checks if the document of an event is viewable by a given subscriber. If it is not, then we can also say that the
     * event is not visible to the subscriber.
     * 
     * @param event the event to check
     * @param subscriber the subscriber to check
     * @return true if the event is visible, false otherwise
     */
    boolean isEventViewable(WatchListEvent event, String subscriber);
}
