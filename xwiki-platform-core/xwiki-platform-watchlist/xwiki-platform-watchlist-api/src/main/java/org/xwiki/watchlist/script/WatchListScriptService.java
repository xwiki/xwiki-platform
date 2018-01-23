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
package org.xwiki.watchlist.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.watchlist.WatchListConfiguration;
import org.xwiki.watchlist.internal.DefaultWatchListStore;
import org.xwiki.watchlist.internal.api.WatchList;
import org.xwiki.watchlist.internal.api.WatchedElementType;

import com.sun.syndication.feed.synd.SyndFeed;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Script service that offers WatchList features to XWiki. These feature allow users to build lists of pages and spaces
 * they want to follow. At a frequency chosen by the user XWiki will send an email notification to him with a list of
 * the elements that has been modified since the last notification. This is the wrapper accessible from in-document
 * scripts.
 *
 * @version $Id$
 * @since 7.0RC1
 */
@Component
@Named("watchlist")
@Singleton
public class WatchListScriptService implements ScriptService
{
    private static final String ERROR_KEY = "scriptservice.watchlist.error";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private WatchList watchlist;

    @Inject
    private ContextualAuthorizationManager authorizationManager;

    @Inject
    private Execution execution;

    @Inject
    private WatchListConfiguration configuration;

    /**
     * @return if the watchlist application is enabled
     *
     * @since 10.0
     * @since 9.11.2
     */
    public boolean isEnabled()
    {
        return configuration.isEnabled();
    }

    /**
     * @param type the type of element, as defined by {@link WatchedElementType}
     * @return true if the current (context) element of the specified type is watched by the current user, false
     *         otherwise or in case of error.
     */
    public boolean isWatched(WatchedElementType type)
    {
        return isWatched(null, type);
    }

    /**
     * @param element the element to check. A {@code null} value will fallback on the current element for the specified
     *            type
     * @param type the type of element, as defined by {@link WatchedElementType}
     * @return true if the specified element of the specified type is watched by the current user, false otherwise or in
     *         case of error.
     */
    public boolean isWatched(String element, WatchedElementType type)
    {
        XWikiContext context = contextProvider.get();

        try {
            String safeElement = getSafeElement(element, type);

            return watchlist.getStore().isWatched(safeElement, context.getUser(), type);
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * @param element the element to check
     * @param type the type of element to check
     * @return the given element if it is not {@code null} or {@link #getCurrentElementOfType(WatchedElementType)}
     *         instead
     */
    private String getSafeElement(String element, WatchedElementType type)
    {
        String safeElement = element;
        if (safeElement == null) {
            safeElement = getCurrentElementOfType(type);
        }

        return safeElement;
    }

    /**
     * @param type the type of element, as defined by {@link WatchedElementType}
     * @return the current (context) element of the specified type
     */
    private String getCurrentElementOfType(WatchedElementType type)
    {
        XWikiContext context = contextProvider.get();
        XWikiDocument currentDocument = context.getDoc();

        String element = null;
        switch (type) {
            case DOCUMENT:
                element = currentDocument.getPrefixedFullName();
                break;
            case SPACE:
                element = context.getWikiId() + DefaultWatchListStore.WIKI_SPACE_SEP + currentDocument.getSpace();
                break;
            case WIKI:
                element = context.getWikiId();
                break;
            case USER:
                element = context.getUser();
                break;
            default:
                break;
        }

        return element;
    }

    /**
     * Add the specified element to the current user's WatchList.
     *
     * @param element the element to add. A {@code null} value will fallback on the current element for the specified
     *            type
     * @param type the type of element, as defined by {@link WatchedElementType}
     * @return true if the specified element wasn't already in the current user's WatchList, false otherwise or in case
     *         of an error
     */
    public boolean addWatchedElement(String element, WatchedElementType type)
    {
        return addWatchedElement(null, element, type);
    }

    /**
     * Adds the specified element to the specified user's WatchList.
     * 
     * @param user the user to add the element to. If specified, the current user's admin rights will be checked and, if
     *            they are not present, the operation will fail. A {@code null} value will fallback on the current user
     * @param element the element to add. A {@code null} value will fallback on the current element for the specified
     *            type
     * @param type the type of element, as defined by {@link WatchedElementType}
     * @return true if the specified element wasn't already in the specified user's WatchList, false otherwise or in
     *         case of an error
     */
    public boolean addWatchedElement(String user, String element, WatchedElementType type)
    {
        XWikiContext context = contextProvider.get();

        try {
            String safeUser = getSafeUser(user, context);
            String safeElement = getSafeElement(element, type);

            return watchlist.getStore().addWatchedElement(safeUser, safeElement, type);
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * @param user the user to check
     * @param context the context to use
     * @return the given user if it is not {@code null} or the current context user instead
     * @throws AccessDeniedException if a user was specified, but the current user executing this code does not have
     *             admin rights to act upon the given user
     */
    private String getSafeUser(String user, XWikiContext context) throws AccessDeniedException
    {
        String safeUser = user;

        if (safeUser == null) {
            safeUser = context.getUser();
        } else {
            authorizationManager.checkAccess(Right.ADMIN);
        }

        return safeUser;
    }

    /**
     * Removed the specified element from the current user's WatchList.
     *
     * @param element the element to remove. A {@code null} value will fallback on the current element for the specified
     *            type
     * @param type the type of element, as defined by {@link WatchedElementType}
     * @return true if the element was in the WatchList and has been removed, false otherwise or in case of an error
     */
    public boolean removeWatchedElement(String element, WatchedElementType type)
    {
        return removeWatchedElement(null, element, type);
    }

    /**
     * Allows Administrators to remove the specified element from the specified user's WatchList.
     *
     * @param user the user to remove the element from. If specified, the current user's admin rights will be checked
     *            and, if they are not present, the operation will fail. A {@code null} value will fallback on the
     *            current user
     * @param element the element to remove. A {@code null} value will fallback on the current element for the specified
     *            type
     * @param type the type of element, as defined by {@link WatchedElementType}
     * @return true if the element was in the WatchList and has been removed, false otherwise or in case of an error
     */
    public boolean removeWatchedElement(String user, String element, WatchedElementType type)
    {
        XWikiContext context = contextProvider.get();

        try {
            String safeUser = getSafeUser(user, context);
            String safeElement = getSafeElement(element, type);

            return watchlist.getStore().removeWatchedElement(safeUser, safeElement, type);
        } catch (Exception e) {
            setError(e);
            return false;
        }
    }

    /**
     * @param type the type of element, as defined by {@link WatchedElementType}
     * @return the elements of the specified type that are watched by the current user. An empty list may also be
     *         returned in case of error.
     */
    public Collection<String> getWatchedElements(WatchedElementType type)
    {
        XWikiContext context = contextProvider.get();

        try {
            return watchlist.getStore().getWatchedElements(context.getUser(), type);
        } catch (Exception e) {
            setError(e);
            return Collections.emptyList();
        }
    }

    /**
     * Get the elements (wikis + spaces + documents + users) watched by the current user.
     *
     * @return the list of the elements in the user's WatchList. An empty list may also be returned in case of error.
     */
    public List<String> getWatchedElements()
    {
        List<String> elements = new ArrayList<String>();
        for (WatchedElementType type : WatchedElementType.values()) {
            elements.addAll(getWatchedElements(type));
        }

        return elements;
    }

    /**
     * @param entryNumber number of entries to retrieve
     * @return the watchlist RSS feed for the current user
     */
    public SyndFeed getFeed(int entryNumber)
    {
        XWikiContext context = contextProvider.get();

        return getFeed(context.getUser(), entryNumber);
    }

    /**
     * @param user the user to retreive the RSS for
     * @param entryNumber number of entries to retrieve
     * @return the watchlist RSS feed for the given user
     */
    public SyndFeed getFeed(String user, int entryNumber)
    {
        try {
            return watchlist.getFeedManager().getFeed(user, entryNumber);
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    /**
     * Get the list of available notification intervals IDs. This can contain both IDs and document full names, example:
     * ("realtime", "Scheduler.WatchListHourlyNotifier", etc.).
     *
     * @return the list of available notification intervals. An empty list may also be returned in case of error.
     */
    public List<String> getIntervals()
    {
        return watchlist.getStore().getIntervals();
    }

    /**
     * Get the notification interval preference of a user.
     * 
     * @param user the user to check
     * @return the notification interval ID, which can be either an ID or a scheduler job document name
     */
    public String getInterval(String user)
    {
        return watchlist.getStore().getInterval(user);
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return the exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    protected void setError(Exception e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }
}
