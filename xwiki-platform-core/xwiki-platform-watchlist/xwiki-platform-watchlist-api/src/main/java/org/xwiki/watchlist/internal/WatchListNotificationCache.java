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

import java.util.Collection;

import org.xwiki.component.annotation.Role;

/**
 * Caches information that is useful when notifying users of changes on the watched elements.
 * 
 * @version $Id$
 */
@Role
public interface WatchListNotificationCache
{
    /**
     * @param intervalId ID of the interval
     * @return subscribers to be notified for the given interval
     */
    Collection<String> getSubscribers(String intervalId);

    /**
     * Register a new subscriber for the given interval.
     * 
     * @param intervalId ID of the interval
     * @param user subscriber to add
     * @return true if the interval was valid and the subscriber was added, false otherwise
     */
    boolean addSubscriber(String intervalId, String user);

    /**
     * Move a subscriber from one interval to another.
     * 
     * @param oldIntervalId the interval from which to move the subscriber
     * @param newIntervalId the new interval to move the subscriber to
     * @param user subscriber to add
     * @return true if at least the new interval was valid and the subscriber is in the new interval (i.e. was moved),
     *         false otherwise
     */
    boolean moveSubscriber(String oldIntervalId, String newIntervalId, String user);

    /**
     * Remove a subscriber for the given interval.
     * 
     * @param intervalId ID of the interval
     * @param user subscriber to remove
     * @return true if the interval was valid and the subscriber was removed, false otherwise
     */
    boolean removeSubscriber(String intervalId, String user);

    /**
     * @return names of documents which contain a watchlist job object
     */
    Collection<String> getJobDocumentNames();

    /**
     * Add a document containing a watchlist job object.
     * 
     * @param jobDocument the document to add
     * @return true if the document was added, false otherwise
     */
    boolean addJobDocument(String jobDocument);

    /**
     * Remove a document containing a watchlist job object.
     * 
     * @param jobDocument the document to add
     * @return true if the document was removed, false otherwise
     */
    boolean removeJobDocument(String jobDocument);
}
