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
package com.xpn.xwiki.plugin.watchlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.plugin.PluginApi;
import com.xpn.xwiki.plugin.watchlist.WatchListStore.ElementType;

/**
 * Plugin that offers WatchList features to XWiki. These feature allow users to build lists of pages and spaces they
 * want to follow. At a frequency choosen by the user XWiki will send an email notification to him with a list of the
 * elements that has been modified since the last notification. This is the wrapper accessible from in-document scripts.
 *
 * @version $Id$
 */
@Deprecated
public class WatchListPluginApi extends PluginApi<WatchListPlugin>
{
    /**
     * API constructor.
     *
     * @param plugin The wrapped plugin object
     * @param context Context of the request
     * @see PluginApi#PluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface, XWikiContext)
     */
    public WatchListPluginApi(WatchListPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Is current document within a space watched by the current user.
     *
     * @return True if the containing space is watched
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean isDocInWatchedSpaces() throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.SPACE,
                this.context).contains(
                this.context.getWikiId() + WatchListStore.WIKI_SPACE_SEP + this.context.getDoc().getSpace());
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Is current document watched by the current user.
     *
     * @return True if the document is in the current user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean isDocumentWatched() throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.DOCUMENT,
                this.context).contains(this.context.getDoc().getPrefixedFullName());
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Add the specified document to the current user's WatchList.
     *
     * @param wDoc Document to add
     * @return True if the document wasn't already in the WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean addDocument(String wDoc) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().addWatchedElement(this.context.getUser(), wDoc,
                ElementType.DOCUMENT, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Allows Administrators to add the specified document in the specified user's WatchList.
     *
     * @param user XWiki User
     * @param wDoc Document to add
     * @return True if the document wasn't already in the WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean addDocumentForUser(String user, String wDoc) throws XWikiException
    {
        try {
            return this.context.getWiki().getUser(this.context).hasAdminRights()
                && getProtectedPlugin().getStore().addWatchedElement(user, wDoc, ElementType.DOCUMENT, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Removed the specified document from the current user's WatchList.
     *
     * @param wDoc Document to remove
     * @return True if the document was in the WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean removeDocument(String wDoc) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().removeWatchedElement(this.context.getUser(), wDoc,
                ElementType.DOCUMENT, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Allows Adminstrators to remove the specified document from the specified user's WatchList.
     *
     * @param user XWiki User
     * @param wDoc Document to remove
     * @return True if the document was in the WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean removeDocumentForUser(String user, String wDoc) throws XWikiException
    {
        try {
            return this.context.getWiki().getUser(this.context).hasAdminRights()
                && getProtectedPlugin().getStore().removeWatchedElement(user, wDoc, ElementType.DOCUMENT, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Is the current space watched by the current user.
     *
     * @return True if the space is in the current user's watchlist
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean isSpaceWatched() throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.SPACE,
                this.context).contains(
                this.context.getWikiId() + WatchListStore.WIKI_SPACE_SEP + this.context.getDoc().getSpace());
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Add the current space to the current user's WatchList.
     *
     * @param wSpace Space to add
     * @return True if the space wasn't already in the user's WatchList and has been added
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean addSpace(String wSpace) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().addWatchedElement(this.context.getUser(), wSpace, ElementType.SPACE,
                this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Allows Administrators to add the specified space to the specified user's WatchList.
     *
     * @param user XWiki User
     * @param wSpace Space to add
     * @return True if the space wasn't already in the user's WatchList and has been added
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean addSpaceForUser(String user, String wSpace) throws XWikiException
    {
        try {
            return this.context.getWiki().getUser(this.context).hasAdminRights()
                && getProtectedPlugin().getStore().addWatchedElement(user, wSpace, ElementType.SPACE, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Remove the specified space from the current user's WatchList.
     *
     * @param wSpace Space to remove
     * @return True if the space was in the user's WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean removeSpace(String wSpace) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().removeWatchedElement(this.context.getUser(), wSpace,
                ElementType.SPACE, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Allows Administrators to remove the specified space from the specified user's WatchList.
     *
     * @param user XWiki User
     * @param wSpace Space to remove
     * @return True if the space was in the user's WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean removeSpaceForUser(String user, String wSpace) throws XWikiException
    {
        try {
            return this.context.getWiki().getUser(this.context).hasAdminRights()
                && getProtectedPlugin().getStore().removeWatchedElement(user, wSpace, ElementType.SPACE, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Is the current wiki watched by the current user.
     *
     * @return True if the wiki is in the current user's watchlist
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean isWikiWatched() throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.WIKI,
                this.context).contains(this.context.getWikiId());
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Add the current wiki to the current user's WatchList.
     *
     * @param wWiki Wiki to add
     * @return True if the wiki wasn't already in the user's WatchList and has been added
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean addWiki(String wWiki) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().addWatchedElement(this.context.getUser(), wWiki, ElementType.WIKI,
                this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Allows Administrators to add the specified wiki to the specified user's WatchList.
     *
     * @param user XWiki User
     * @param wWiki Wiki to add
     * @return True if the wiki wasn't already in the user's WatchList and has been added
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean addWikiForUser(String user, String wWiki) throws XWikiException
    {
        try {
            return this.context.getWiki().getUser(this.context).hasAdminRights()
                && getProtectedPlugin().getStore().addWatchedElement(user, wWiki, ElementType.WIKI, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Remove the specified wiki from the current user's WatchList.
     *
     * @param wWiki Wiki to remove
     * @return True if the wiki was in the user's WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean removeWiki(String wWiki) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().removeWatchedElement(this.context.getUser(), wWiki,
                ElementType.WIKI, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Allows Administrators to remove the specified wiki from the specified user's WatchList.
     *
     * @param user XWiki User
     * @param wWiki Wiki to remove
     * @return True if the wiki was in the user's WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean removeWikiForUser(String user, String wWiki) throws XWikiException
    {
        try {
            return this.context.getWiki().getUser(this.context).hasAdminRights()
                && getProtectedPlugin().getStore().removeWatchedElement(user, wWiki, ElementType.WIKI, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Is the given user watched by the current user.
     *
     * @param user the prefixed fullName of the user to test
     * @return True if the user is in the current user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean isUserWatched(String user) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.USER,
                this.context).contains(user);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Add the specified user to the current user's WatchList.
     *
     * @param user User to add
     * @return True if the user wasn't already in the WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean addUser(String user) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().addWatchedElement(this.context.getUser(), user, ElementType.USER,
                this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Allows Administrators to add the specified user in the specified user's WatchList.
     *
     * @param user XWiki User
     * @param userToWatch User to add
     * @return True if the user wasn't already in the WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean addUserForUser(String user, String userToWatch) throws XWikiException
    {
        try {
            return this.context.getWiki().getUser(this.context).hasAdminRights()
                && getProtectedPlugin().getStore().addWatchedElement(user, userToWatch, ElementType.USER, this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Removed the specified user from the current user's WatchList.
     *
     * @param user User to remove
     * @return True if the user was in the WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean removeUser(String user) throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().removeWatchedElement(this.context.getUser(), user, ElementType.USER,
                this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Allows Administrators to remove the specified user from the specified user's WatchList.
     *
     * @param user XWiki User
     * @param userToRemove User to remove
     * @return True if the user was in the WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public boolean removeUserForUser(String user, String userToRemove) throws XWikiException
    {
        try {
            return this.context.getWiki().getUser(this.context).hasAdminRights()
                && getProtectedPlugin().getStore().removeWatchedElement(user, userToRemove, ElementType.USER,
                    this.context);
        } catch (XWikiException ex) {
            return false;
        }
    }

    /**
     * Get the documents watched by the current user.
     *
     * @return The list of the documents in the user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public List<String> getWatchedDocuments() throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.DOCUMENT,
                this.context);
        } catch (XWikiException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Get the spaces watched by the current user.
     *
     * @return The list of the spaces in the user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public List<String> getWatchedSpaces() throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.SPACE,
                this.context);
        } catch (XWikiException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Get the list of wikis watched by the current user.
     *
     * @return The list of the wikis in the user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public List<String> getWatchedWikis() throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.WIKI,
                this.context);
        } catch (XWikiException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Get the list of users watched by the current user.
     *
     * @return The list of the users in the user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public List<String> getWatchedUsers() throws XWikiException
    {
        try {
            return getProtectedPlugin().getStore().getWatchedElements(this.context.getUser(), ElementType.USER,
                this.context);
        } catch (XWikiException ex) {
            return Collections.emptyList();
        }
    }

    /**
     * Get the elements (wikis + spaces + documents + users) watched by the current user.
     *
     * @return The list of the elements in the user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retrieved nor created
     */
    public List<String> getWatchedElements() throws XWikiException
    {
        List<String> wEls = new ArrayList<String>();
        wEls.addAll(getWatchedDocuments());
        wEls.addAll(getWatchedSpaces());
        wEls.addAll(getWatchedWikis());
        wEls.addAll(getWatchedUsers());

        return wEls;
    }

    /**
     * @param entryNumber number of entries to retrieve
     * @return the watchlist RSS feed for the current user
     * @throws XWikiException if the retrieval of RSS entries fails
     */
    public SyndFeed getFeed(int entryNumber) throws XWikiException
    {
        return getFeed(this.context.getUser(), entryNumber);
    }

    /**
     * @param user the user to retreive the RSS for
     * @param entryNumber number of entries to retrieve
     * @return the watchlist RSS feed for the given user
     * @throws XWikiException if the retrieval of RSS entries fails
     */
    public SyndFeed getFeed(String user, int entryNumber) throws XWikiException
    {
        try {
            return getProtectedPlugin().getFeedManager().getFeed(user, entryNumber, this.context);
        } catch (XWikiException ex) {
            return null;
        }
    }

    /**
     * Get the list of available notifiers (list of document full names, example: "Scheduler.WatchListHourlyNotifier").
     *
     * @return the list of available notifiers
     */
    public List<Document> getNotifiers()
    {
        return getProtectedPlugin().getJobManager().getJobs(this.context);
    }
}
