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

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.plugin.PluginApi;

import java.util.ArrayList;
import java.util.List;

/**
 * Plugin that offers WatchList features to XWiki. These feature allow users to build lists of pages and spaces they
 * want to follow. At a frequency choosen by the user XWiki will send an email notification to him with a list of the
 * elements that has been modified since the last notification.
 *
 * This is the wrapper accessible from in-document scripts.
 *
 * @version $Id: $
 */
public class WatchListPluginApi extends PluginApi
{
    /**
     * API constructor.
     *
     * @param plugin The wrapped plugin object.
     * @param context Context of the request.
     * @see PluginApi#PluginApi(com.xpn.xwiki.plugin.XWikiPluginInterface, XWikiContext)
     */
    public WatchListPluginApi(WatchListPlugin plugin, XWikiContext context)
    {
        super(plugin, context);
    }

    /**
     * Is current document within a space watched by the current user
     *
     * @return True if the containing space is watched
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean isDocInWatchedSpaces() throws XWikiException
    {
        return getWatchListPlugin().getWatchedSpaces(getXWikiContext().getUser(),
            getXWikiContext()).contains(context.getDatabase() + ":" + context.getDoc().getSpace());
    }

    /**
     * Is current document watched by the current user
     *
     * @return True if the document is in the current user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean isDocumentWatched() throws XWikiException
    {
        return getWatchListPlugin().getWatchedDocuments(context.getUser(), context)
            .contains(context.getDatabase() + ":" + context.getDoc().getFullName());
    }

    /**
     * Add the specified document to the current user's WatchList
     *
     * @param wDoc Document to add
     * @return True if the document wasn't already in the WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean addDocument(String wDoc) throws XWikiException
    {
        return getWatchListPlugin().addWatchedElement(getXWikiContext().getUser(),
            wDoc, false, getXWikiContext());
    }

    /**
     * Allows Adminstrators to add the specified document in the specified user's WatchList
     *
     * @param user XWiki User
     * @param wDoc Document to add
     * @return True if the document wasn't already in the WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean addDocumentForUser(String user, String wDoc) throws XWikiException
    {
        return context.getWiki().getUser(context).hasAdminRights() &&
            getWatchListPlugin().addWatchedElement(user, wDoc, false,
                getXWikiContext());
    }

    /**
     * Removed the specified document from the current user's WatchList
     *
     * @param wDoc Document to remove
     * @return True if the document was in the WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean removeDocument(String wDoc) throws XWikiException
    {
        return getWatchListPlugin().removeWatchedElement(getXWikiContext().getUser(),
            wDoc, false, getXWikiContext());
    }

    /**
     * Allows Adminstrators to remove the specified document from the specified user's WatchList
     *
     * @param user XWiki User
     * @param wDoc Document to remove
     * @return True if the document was in the WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean removeDocumentForUser(String user, String wDoc) throws XWikiException
    {
        return context.getWiki().getUser(context).hasAdminRights() &&
            getWatchListPlugin().removeWatchedElement(user, wDoc,
                false, getXWikiContext());
    }

    /**
     * Is the current space watched by the current user
     *
     * @return True if the document is in the current user's watchlist
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean isSpaceWatched() throws XWikiException
    {
        return getWatchListPlugin().getWatchedSpaces(context.getUser(), context)
            .contains(context.getDatabase() + ":" + context.getDoc().getSpace());
    }

    /**
     * Add the current space to the current user's WatchList
     *
     * @param wSpace Space to add
     * @return True if the space wasn't already in the user's WatchList and has been added
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean addSpace(String wSpace) throws XWikiException
    {
        return getWatchListPlugin().addWatchedElement(getXWikiContext().getUser(),
            wSpace, true, getXWikiContext());
    }

    /**
     * Allows Administrators to add the specified space to the specified user's WatchList
     *
     * @param user XWiki User
     * @param wSpace Space to add
     * @return True if the space wasn't already in the user's WatchList and has been added
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean addSpaceForUser(String user, String wSpace) throws XWikiException
    {
        return context.getWiki().getUser(context).hasAdminRights() &&
            getWatchListPlugin().addWatchedElement(user, wSpace, true,
                getXWikiContext());
    }

    /**
     * Remove the specified space from the current user's WatchList
     *
     * @param wSpace Space to remove
     * @return True if the space was in the user's WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean removeSpace(String wSpace) throws XWikiException
    {
        return getWatchListPlugin().removeWatchedElement(getXWikiContext().getUser(),
            wSpace, true, getXWikiContext());
    }

    /**
     * Allows Administrators to remove the specified space from the specified user's WatchList
     *
     * @param user XWiki User
     * @param wSpace Space to remove
     * @return True if the space was in the user's WatchList and has been removed
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public boolean removeSpaceForUser(String user, String wSpace) throws XWikiException
    {
        return context.getWiki().getUser(context).hasAdminRights() &&
            getWatchListPlugin().removeWatchedElement(user, wSpace,
                true, getXWikiContext());
    }

    /**
     * Get the documents watched by the current user
     *
     * @return The list of the documents in the user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public List getWatchedDocuments() throws XWikiException
    {
        return getWatchListPlugin().getWatchedDocuments(getXWikiContext().getUser(), context);
    }

    /**
     * Get the spaces watched by the current user
     *
     * @return The list of the spaces in the user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public List getWatchedSpaces() throws XWikiException
    {
        return getWatchListPlugin().getWatchedSpaces(getXWikiContext().getUser(), context);
    }

    /**
     * Get the elements (documents + spaces) watched by the current user
     *
     * @return The list of the elements in the user's WatchList
     * @throws XWikiException If the user's WatchList Object cannot be retreived nor created
     */
    public List getWatchedElements() throws XWikiException
    {
        List wEls = new ArrayList();
        wEls.addAll(getWatchedDocuments());
        wEls.addAll(getWatchedSpaces());
        return wEls;
    }

    /**
     * Get the list of the elements watched by user ordered by last modification date, descending
     *
     * @param user XWiki User
     * @return the list of the elements watched by user ordered by last modification date, descending
     * @throws XWikiException If the search request fails
     */
    public List getWatchListWhatsNew(String user) throws Exception
    {
        return getWatchListPlugin().getWatchListWhatsNew(user, context);
    }

    /**
     * Get the WatchList plugin
     *
     * @return the WatchList plugin
     */
    private WatchListPlugin getWatchListPlugin()
    {
        return (WatchListPlugin) getProtectedPlugin();
    }
}
