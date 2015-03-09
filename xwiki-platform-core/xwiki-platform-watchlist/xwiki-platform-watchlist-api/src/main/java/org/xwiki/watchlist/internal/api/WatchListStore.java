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
package org.xwiki.watchlist.internal.api;

import java.util.Collection;

import org.xwiki.component.annotation.Role;

import com.xpn.xwiki.XWikiException;

/**
 * WatchList store class. Handles user subscription storage.
 * 
 * @version $Id$
 */
@Role
public interface WatchListStore
{
    /**
     * @param user user to match
     * @param type element type to match
     * @return watched elements for the given element type by the given user
     * @throws XWikiException if retrieval of elements fails
     */
    Collection<String> getWatchedElements(String user, WatchedElementType type) throws XWikiException;

    /**
     * @param element the element to look for
     * @param user user to check
     * @param type type of the element
     * @return true if the element is watched by the user, false otherwise
     * @throws XWikiException if the retrieval of watched elements fails
     */
    boolean isWatched(String element, String user, WatchedElementType type) throws XWikiException;

    /**
     * Add the specified element (document or space) to the corresponding list in the user's WatchList.
     * 
     * @param user the reference to the user
     * @param newWatchedElement The name of the element to add (document, space, wiki, user)
     * @param type type of the element to remove
     * @return true if the element wasn't already in watched list or false otherwise
     * @throws XWikiException if the modification hasn't been saved
     */
    boolean addWatchedElement(String user, String newWatchedElement, WatchedElementType type) throws XWikiException;

    /**
     * Remove the specified element (document or space) from the corresponding list in the user's WatchList.
     * 
     * @param user XWiki User
     * @param watchedElement The name of the element to remove (document or space)
     * @param type type of the element to remove
     * @return true if the element was in list and has been removed, false if the element wasn't in the list
     * @throws XWikiException If the WatchList Object cannot be retrieved or if the user's profile cannot be saved
     */
    boolean removeWatchedElement(String user, String watchedElement, WatchedElementType type) throws XWikiException;

    /**
     * Get the automatic document watching mode preference of a user.
     * 
     * @param user the user
     * @return the mode, if not set return the default one which is {@link AutomaticWatchMode#MAJOR}
     */
    AutomaticWatchMode getAutomaticWatchMode(String user);

    /**
     * @return names of documents which contain a watchlist job object
     */
    Collection<String> getJobDocumentNames();

    /**
     * @param intervalId ID of the interval
     * @return subscribers to be notified for the given interval
     */
    Collection<String> getSubscribers(String intervalId);
}
